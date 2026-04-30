package com.example.githubexplorer.ui.repodetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.githubexplorer.databinding.ActivityRepoDetailBinding
import com.google.android.material.tabs.TabLayoutMediator

class RepoDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRepoDetailBinding
    lateinit var owner: String
    lateinit var repo: String
    lateinit var branch: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRepoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        owner = intent.getStringExtra(EXTRA_OWNER)!!
        repo = intent.getStringExtra(EXTRA_REPO)!!
        branch = intent.getStringExtra(EXTRA_BRANCH) ?: "main"

        supportActionBar?.title = "$owner/$repo"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val pagerAdapter = RepoDetailPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Files" else "Actions"
        }.attach()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        private const val EXTRA_OWNER = "owner"
        private const val EXTRA_REPO = "repo"
        private const val EXTRA_BRANCH = "branch"
        fun newIntent(context: Context, owner: String, repo: String, branch: String = "main"): Intent {
            return Intent(context, RepoDetailActivity::class.java).apply {
                putExtra(EXTRA_OWNER, owner)
                putExtra(EXTRA_REPO, repo)
                putExtra(EXTRA_BRANCH, branch)
            }
        }
    }
}
