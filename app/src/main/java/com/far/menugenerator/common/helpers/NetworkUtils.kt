package com.far.menugenerator.common.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object NetworkUtils {

    suspend fun isConnectedToInternet():Boolean = withContext(Dispatchers.IO){
        val url = "https://www.google.com" // Replace with desired server
        val timeout = 1500 // Milliseconds

        val response = try {
            URL(url).openConnection().apply {
                connectTimeout = timeout
                readTimeout = timeout
            }.getInputStream().bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            return@withContext false
        }

        return@withContext response.isNotEmpty()
    }
}