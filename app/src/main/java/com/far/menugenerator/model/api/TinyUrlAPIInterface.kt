package com.far.menugenerator.model.api

import com.far.menugenerator.model.api.model.TinyUrlRequest
import com.far.menugenerator.model.api.model.TinyUrlResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface TinyUrlAPIInterface {
    //https://tinyurl.com/app/dev
    @POST("/create")
    fun createPost(@Body body: TinyUrlRequest,
                   @Query("api_token") token: String): Call<TinyUrlResponse>

    //pendiente implementar DELETE al eliminar menu con short URL
}