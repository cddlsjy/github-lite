package com.example.githubexplorer.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.githubexplorer.databinding.ActivityMainBinding
import com.example.githubexplorer.models.Repo
import com.example.githubexplorer.network.RetrofitProvider
import com.example.githubexplorer.ui.repodetail.RepoDetailActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val reposList = mutableListOf<Repo>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.repoListView.adapter = adapter
        binding.repoListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (position < reposList.size) {
                val repo = reposList[position]
                startActivity(RepoDetailActivity.newIntent(this, repo.owner.login, repo.name))
            }
        }

        binding.searchButton.setOnClickListener { searchRepos() }
        binding.myReposButton.setOnClickListener { loadMyRepos() }
        binding.refreshButton.setOnClickListener { loadMyRepos() }

        loadMyRepos()
    }

    private fun loadMyRepos() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val repos = RetrofitProvider.api.listUserRepos()
                displayRepos(repos)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Load failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun searchRepos() {
        val query = binding.searchEditText.text.toString().trim()
        if (query.isEmpty()) return
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitProvider.api.searchRepos(query)
                displayRepos(response.items)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Search failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayRepos(repos: List<Repo>) {
        reposList.clear()
        reposList.addAll(repos)
        val names = repos.map { it.fullName }
        adapter.clear()
        adapter.addAll(names)
        adapter.notifyDataSetChanged()
        if (repos.isEmpty()) {
            adapter.add("No repositories found")
        }
    }

    companion object {
        fun newIntent(context: android.content.Context) = Intent(context, MainActivity::class.java)
    }
}
