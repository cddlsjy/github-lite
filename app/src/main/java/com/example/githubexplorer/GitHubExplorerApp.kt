package com.example.githubexplorer

import android.app.Application
import com.example.githubexplorer.util.PreferenceHelper

class GitHubExplorerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        PreferenceHelper.init(this)
    }
}
