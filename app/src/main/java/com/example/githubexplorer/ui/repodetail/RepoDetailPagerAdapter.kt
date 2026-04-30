package com.example.githubexplorer.ui.repodetail

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class RepoDetailPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FileTreeFragment()
            1 -> ActionsFragment()
            else -> throw IllegalArgumentException()
        }
    }
}
