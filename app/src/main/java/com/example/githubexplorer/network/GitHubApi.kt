package com.example.githubexplorer.network

import com.example.githubexplorer.models.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface GitHubApi {
    @GET("user")
    suspend fun getUser(): User

    @GET("user/repos")
    suspend fun listUserRepos(
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<Repo>

    @GET("search/repositories")
    suspend fun searchRepos(
        @Query("q") query: String,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): SearchRepoResponse

    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Repo

    @GET("repos/{owner}/{repo}/git/trees/{sha}")
    suspend fun getTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String,
        @Query("recursive") recursive: Int = 1
    ): TreeResponse

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String
    ): ContentResponse

    @GET("repos/{owner}/{repo}/actions/workflows")
    suspend fun listWorkflows(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): WorkflowListResponse

    @POST("repos/{owner}/{repo}/actions/workflows/{workflowId}/dispatches")
    suspend fun dispatchWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflowId") workflowId: Long,
        @Body body: DispatchBody
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/actions/workflows/{workflowId}/runs")
    suspend fun listWorkflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflowId") workflowId: Long,
        @Query("status") status: String? = null,
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): WorkflowRunsResponse

    @GET("repos/{owner}/{repo}/actions/runs/{runId}/jobs")
    suspend fun listJobs(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long
    ): JobsResponse

    @GET("repos/{owner}/{repo}/actions/jobs/{jobId}/logs")
    suspend fun getJobLog(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("jobId") jobId: Long
    ): ResponseBody

    @GET("repos/{owner}/{repo}/actions/runs/{runId}/artifacts")
    suspend fun listArtifacts(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long
    ): ArtifactsResponse

    @GET("repos/{owner}/{repo}/actions/artifacts/{artifactId}/zip")
    suspend fun downloadArtifact(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("artifactId") artifactId: Long
    ): ResponseBody

    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    suspend fun getBranchRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): BranchRefResponse
}

data class DispatchBody(val ref: String)
data class BranchRefResponse(val `object`: RefObject)
data class RefObject(val sha: String, val type: String, val url: String)
