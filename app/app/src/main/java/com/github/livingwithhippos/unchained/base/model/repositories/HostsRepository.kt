package com.github.livingwithhippos.unchained.base.model.repositories

import com.github.livingwithhippos.unchained.base.model.network.Host
import com.github.livingwithhippos.unchained.base.model.network.HostsApiHelper
import javax.inject.Inject

class HostsRepository @Inject constructor(private val hostsApiHelper: HostsApiHelper) :
    BaseRepository() {

    suspend fun getHostsStatus(token: String): Host? {

        val hostResponse = safeApiCall(
            call = { hostsApiHelper.getHostsStatus("Bearer $token") },
            errorMessage = "Error Fetching Streaming Info"
        )

        return hostResponse;

    }
}