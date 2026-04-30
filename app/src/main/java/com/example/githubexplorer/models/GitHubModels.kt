package com.example.githubexplorer.models

import com.google.gson.annotations.SerializedName

data class User(
    val login: String,
    val id: Long,
    @SerializedName("avatar_url") val avatarUrl: String
)

data class Repo(
    val id: Long,
    val name: String,
    @SerializedName("full_name") val fullName: String,
    val owner: Owner,
    @SerializedName("default_branch") val defaultBranch: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    val description: String?
)

data class Owner(
    val login: String,
    val id: Long
)

data class SearchRepoResponse(
    @SerializedName("total_count") val totalCount: Int,
    val items: List<Repo>
)

data class TreeEntry(
    val path: String,
    val type: String,
    val sha: String?
)

data class TreeResponse(
    val sha: String,
    val tree: List<TreeEntry>
)

data class ContentResponse(
    val name: String,
    val path: String,
    val type: String,
    val encoding: String?,
    val size: Int,
    val content: String?,
    @SerializedName("download_url") val downloadUrl: String?
)

data class Workflow(
    val id: Long,
    val name: String,
    val path: String,
    val state: String
)

data class WorkflowListResponse(
    @SerializedName("total_count") val totalCount: Int,
    val workflows: List<Workflow>
)

data class WorkflowRun(
    val id: Long,
    val status: String,
    val conclusion: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("head_branch") val headBranch: String?
)

data class WorkflowRunsResponse(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("workflow_runs") val workflowRuns: List<WorkflowRun>
)

data class Job(
    val id: Long,
    @SerializedName("run_id") val runId: Long,
    val name: String,
    val status: String,
    val conclusion: String?
)

data class JobsResponse(
    @SerializedName("total_count") val totalCount: Int,
    val jobs: List<Job>
)

data class Artifact(
    val id: Long,
    val name: String,
    @SerializedName("size_in_bytes") val sizeInBytes: Long,
    @SerializedName("archive_download_url") val archiveDownloadUrl: String
)

data class ArtifactsResponse(
    @SerializedName("total_count") val totalCount: Int,
    val artifacts: List<Artifact>
)
