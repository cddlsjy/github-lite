package com.example.githubexplorer.ui.repodetail

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.githubexplorer.databinding.FragmentFileTreeBinding
import com.example.githubexplorer.models.TreeEntry
import com.example.githubexplorer.network.GitHubHttpClient
import com.example.githubexplorer.network.RetrofitProvider
import com.example.githubexplorer.util.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

class FileTreeFragment : Fragment() {
    private var _binding: FragmentFileTreeBinding? = null
    private val binding get() = _binding!!
    private lateinit var contentSplitLayout: LinearLayout
    private val treeNodes = mutableListOf<TreeEntry>()
    private lateinit var treeAdapter: TreeAdapter

    private lateinit var createDocumentLauncher: ActivityResultLauncher<String>
    private var pendingDownloadUrl: String? = null
    private var pendingDownloadName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
            uri?.let {
                pendingDownloadUrl?.let { url ->
                    lifecycleScope.launch {
                        performActualDownload(url, uri)
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFileTreeBinding.inflate(inflater, container, false)
        contentSplitLayout = binding.contentSplitLayout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.downloadRepoButton.setOnClickListener { downloadRepo() }
        loadRepoTree()
        updateLayoutOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLayoutOrientation()
    }

    private fun updateLayoutOrientation() {
        if (!::contentSplitLayout.isInitialized) return
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        contentSplitLayout.orientation = if (isLandscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

        val treeParams = binding.treeContainer.layoutParams as LinearLayout.LayoutParams
        val scrollParams = binding.fileContentScrollView.layoutParams as LinearLayout.LayoutParams

        if (isLandscape) {
            treeParams.width = 0
            treeParams.height = LinearLayout.LayoutParams.MATCH_PARENT
            treeParams.weight = 1f

            scrollParams.width = 0
            scrollParams.height = LinearLayout.LayoutParams.MATCH_PARENT
            scrollParams.weight = 1f
        } else {
            treeParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            treeParams.height = 0
            treeParams.weight = 1f

            scrollParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            scrollParams.height = 0
            scrollParams.weight = 1f
        }
        binding.treeContainer.layoutParams = treeParams
        binding.fileContentScrollView.layoutParams = scrollParams
    }

    private fun downloadRepo() {
        val activity = requireActivity() as RepoDetailActivity
        val downloadUrl = "https://api.github.com/repos/${activity.owner}/${activity.repo}/zipball/${activity.branch}"
        val fileName = "${activity.repo}-${activity.branch}.zip"
        pendingDownloadUrl = downloadUrl
        pendingDownloadName = fileName
        createDocumentLauncher.launch(fileName)
    }

    private suspend fun performActualDownload(url: String, fileUri: Uri) {
        val token = PreferenceHelper.token ?: return

        withContext(Dispatchers.IO) {
            try {
                val client = GitHubHttpClient.getPlainClient(token)
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("Download failed: ${response.code} ${response.message}")
                }

                response.body?.byteStream()?.use { inputStream ->
                    requireContext().contentResolver.openOutputStream(fileUri)?.use {
                        outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw Exception("Response body is empty")

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download completed!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadRepoTree() {
        val activity = requireActivity() as RepoDetailActivity
        val owner = activity.owner
        val repo = activity.repo
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val repoInfo = RetrofitProvider.api.getRepo(owner, repo)
                val branch = repoInfo.defaultBranch ?: "main"
                activity.branch = branch

                val branchRef = RetrofitProvider.api.getBranchRef(owner, repo, branch)
                val commitSha = branchRef.`object`.sha

                val treeResp = RetrofitProvider.api.getTree(owner, repo, commitSha, 1)
                treeNodes.clear()
                treeNodes.addAll(treeResp.tree)
                buildTree()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load tree: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun buildTree() {
        val rootGroups = mutableListOf<TreeAdapter.TreeGroup>()
        val processed = mutableSetOf<String>()

        for (entry in treeNodes) {
            if (!entry.path.contains("/")) {
                val group = TreeAdapter.TreeGroup(
                    name = entry.path,
                    path = entry.path,
                    isDirectory = entry.type == "tree",
                    children = mutableListOf()
                )
                rootGroups.add(group)
                processed.add(entry.path)
            }
        }

        fun addChildren(group: TreeAdapter.TreeGroup) {
            val prefix = group.path + "/"
            treeNodes.filter { it.path.startsWith(prefix) && !processed.contains(it.path) }.forEach { child ->
                val childGroup = TreeAdapter.TreeGroup(
                    name = child.path.substringAfterLast("/"),
                    path = child.path,
                    isDirectory = child.type == "tree",
                    children = mutableListOf()
                )
                group.children.add(childGroup)
                processed.add(child.path)
                if (child.type == "tree") {
                    addChildren(childGroup)
                }
            }
        }

        rootGroups.filter { it.isDirectory }.forEach { addChildren(it) }

        treeAdapter = TreeAdapter(requireContext(), rootGroups) { path ->
            loadFileContent(path)
        }
        binding.expandableTreeView.setAdapter(treeAdapter)

        binding.expandableTreeView.setOnChildClickListener { _, _, groupPos, childPos, _ ->
            val group = rootGroups[groupPos]
            if (childPos < group.children.size) {
                val child = group.children[childPos]
                if (!child.isDirectory) {
                    loadFileContent(child.path)
                    return@setOnChildClickListener true
                }
            }
            false
        }

        binding.expandableTreeView.setOnGroupClickListener { _, _, groupPos, _ ->
            val group = rootGroups[groupPos]
            if (!group.isDirectory) {
                loadFileContent(group.path)
                true
            } else {
                false
            }
        }

        val readmeEntry = treeNodes.firstOrNull {
            it.type == "blob" && it.path.equals("README.md", ignoreCase = true)
        }
        if (readmeEntry != null) {
            loadFileContent(readmeEntry.path)
        } else {
            binding.emptyHintText.visibility = View.VISIBLE
        }
    }

    private fun loadFileContent(path: String) {
        val activity = requireActivity() as RepoDetailActivity
        binding.fileContentScrollView.visibility = View.VISIBLE
        binding.emptyHintText.visibility = View.GONE
        binding.fileContentTextView.text = "Loading..."
        lifecycleScope.launch {
            try {
                val contentResponse = RetrofitProvider.api.getContent(
                    activity.owner, activity.repo, path, activity.branch
                )
                val text = if (contentResponse.encoding == "base64" && contentResponse.content != null) {
                    val bytes = android.util.Base64.decode(
                        contentResponse.content.replace("\n", ""),
                        android.util.Base64.DEFAULT
                    )
                    try {
                        String(bytes, Charsets.UTF_8)
                    } catch (e: Exception) {
                        "[Binary file, cannot be previewed directly]"
                    }
                } else {
                    "Unable to preview"
                }
                binding.fileContentTextView.text = text
            } catch (e: Exception) {
                binding.fileContentTextView.text = "Load failed: ${e.message}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}