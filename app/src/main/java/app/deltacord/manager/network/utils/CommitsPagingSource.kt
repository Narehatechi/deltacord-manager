package app.deltacord.manager.network.utils

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.deltacord.manager.BuildConfig
import app.deltacord.manager.domain.repository.RestRepository
import app.deltacord.manager.network.dto.Commit

class CommitsPagingSource(
    private val repo: RestRepository
) : PagingSource<Int, Commit>() {

    override fun getRefreshKey(state: PagingState<Int, Commit>): Int? =
        state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Commit> {
        // GitHub pages start at 1, and page=0 returns the same commits as page=1.
        // Starting at 0 loaded the first page twice and crashed the list on duplicate keys.
        val page = params.key ?: 1

        return when (val response = repo.getCommits(BuildConfig.REPO, page)) {
            is ApiResponse.Success -> LoadResult.Page(
                data = response.data,
                prevKey = if (page > 1) page - 1 else null,
                nextKey = if (response.data.isNotEmpty()) page + 1 else null
            )

            is ApiResponse.Failure -> LoadResult.Error(response.error)
            is ApiResponse.Error -> LoadResult.Error(response.error)
        }
    }

}