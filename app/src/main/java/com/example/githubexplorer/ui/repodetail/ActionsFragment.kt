package com.example.githubexplorer.ui.repodetail

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.githubexplorer.databinding.FragmentActionsBinding
import com.example.githubexplorer.models.Artifact
import com.example.githubexplorer.models.Workflow
import com.example.githubexplorer.models.WorkflowRun
import com.example.githubexplorer.network.DispatchBody
import com.example.githubexplorer.network.GitHubHttpClient
import com.example.githubexplorer.network.RetrofitProvider
import com.example.githubexplorer.util.PreferenceHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

class ActionsFragment : Fragment() {
    private var _binding: FragmentActionsBinding? = null
    private val binding get() = _binding!!
    private var workflows = listOf<Workflow>()
    private var selectedWorkflow: Workflow? = null
    private var runs = listOf<WorkflowRun>()

    private lateinit var createDocumentLauncher: ActivityResultLauncher<String>
    private var pendingArtifactUrl: String? = null
    private var pendingArtifactName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
            uri?.let {
                pendingArtifactUrl?.let { url ->
                    lifecycleScope.launch {
                        performActualDownload(url, uri)
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        binding.loadWorkflowsButton.setOnClickListener { loadWorkflows() }
        binding.triggerButton.setOnClickListener { triggerWorkflow() }
        binding.refreshRunsButton.setOnClickListener { loadRuns() }
        loadWorkflows()
    }

    private fun setupSpinners() {
        val filterOptions = arrayOf("All", "In Progress", "Success", "Failure")
        binding.filterSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (workflows.isNotEmpty()) {
                    loadRuns()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadWorkflows() {
        val activity = requireActivity() as RepoDetailActivity
        lifecycleScope.launch {
            try {
                val resp = RetrofitProvider.api.listWorkflows(activity.owner, activity.repo)
                workflows = resp.workflows
                val names = workflows.map { it.name }
                binding.workflowSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names).also {
                    it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                binding.workflowSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedWorkflow = if (workflows.isNotEmpty() && position >= 0) workflows[position] else null
                        loadRuns()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                if (workflows.isNotEmpty()) {
                    selectedWorkflow = workflows[0]
                    loadRuns()
                } else {
                    Toast.makeText(context, "No workflows found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load workflows: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadRuns() {
        if (selectedWorkflow == null) return
        val activity = requireActivity() as RepoDetailActivity
        val statusFilter = when (binding.filterSpinner.selectedItemPosition) {
            1 -> "in_progress"
            2 -> "success"
            3 -> "failure"
            else -> null
        }
        lifecycleScope.launch {
            try {
                val resp = RetrofitProvider.api.listWorkflowRuns(
                    activity.owner, activity.repo, selectedWorkflow!!.id, statusFilter
                )
                runs = resp.workflowRuns
                val display = runs.map { run ->
                    val conclusion = run.conclusion ?: run.status
                    "ID: ${run.id} | ${conclusion.uppercase()} | ${run.createdAt.take(19).replace("T", " ")}"
                }
                binding.runsListView.adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, display) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getView(position, convertView, parent) as TextView
                        view.text = display[position]
                        val run = runs[position]
                        view.setCompoundDrawablesWithIntrinsicBounds(0, 0, if (run.status == "completed" && run.conclusion == "success") android.R.drawable.ic_menu_save else 0, 0)
                        view.setOnClickListener {
                            if (run.status == "completed" && run.conclusion == "success") {
                                downloadArtifact(run.id)
                            } else {
                                showRunDetails(run.id)
                            }
                        }
                        return view
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load runs: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun triggerWorkflow() {
        if (selectedWorkflow == null) {
            Toast.makeText(context, "Please select a workflow", Toast.LENGTH_SHORT).show()
            return
        }
        val activity = requireActivity() as RepoDetailActivity
        lifecycleScope.launch {
            try {
                val response = RetrofitProvider.api.dispatchWorkflow(
                    activity.owner, activity.repo, selectedWorkflow!!.id,
                    DispatchBody(activity.branch)
                )
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, "Trigger failed: ${response.code()} - $errorBody", Toast.LENGTH_LONG).show()
                    return@launch
                }
                Toast.makeText(context, "Triggered successfully", Toast.LENGTH_SHORT).show()
                delay(2000)
                loadRuns()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showRunDetails(runId: Long) {
        val activity = requireActivity() as RepoDetailActivity
        lifecycleScope.launch {
            try {
                val jobsResp = RetrofitProvider.api.listJobs(activity.owner, activity.repo, runId)
                val logBuilder = StringBuilder()
                for (job in jobsResp.jobs) {
                    logBuilder.append("--- Job: ${job.name} ---\n")
                    try {
                        val logBody = RetrofitProvider.api.getJobLog(activity.owner, activity.repo, job.id)
                        logBuilder.append(logBody.string().take(5000))
                    } catch (e: Exception) {
                        logBuilder.append("Unable to load log")
                    }
                    logBuilder.append("\n\n")
                }
                // Show in dialog
                showLogDialog(logBuilder.toString())
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load details: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLogDialog(logContent: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, logContent)
        }
        startActivity(Intent.createChooser(intent, "Share Log"))
    }

    private fun downloadArtifact(runId: Long) {
        val activity = requireActivity() as RepoDetailActivity
        lifecycleScope.launch {
            try {
                val artifactsResp = RetrofitProvider.api.listArtifacts(activity.owner, activity.repo, runId)
                val artifacts = artifactsResp.artifacts
                if (artifacts.isEmpty()) {
                    Toast.makeText(context, "No artifacts for this run", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                // 优先显示 APK 相关
                val apkList = artifacts.filter { it.name.contains("apk", true) }
                val targetList = apkList.ifEmpty { artifacts }
                if (targetList.size == 1) {
                    startDownload(targetList[0])
                } else {
                    showArtifactSelectionDialog(targetList)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to get artifacts: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startDownload(artifact: Artifact) {
        pendingArtifactUrl = artifact.archiveDownloadUrl
        pendingArtifactName = "${artifact.name}.zip"
        createDocumentLauncher.launch(pendingArtifactName!!)
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

    private fun showArtifactSelectionDialog(artifacts: List<Artifact>) {
        val names = artifacts.map { "${it.name} (${it.sizeInBytes / 1024} KB)" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Select artifact to download")
            .setItems(names) { _, which ->
                startDownload(artifacts[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
