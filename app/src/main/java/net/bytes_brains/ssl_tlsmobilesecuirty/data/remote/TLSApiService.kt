package net.bytes_brains.ssl_tlsmobilesecuirty.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET

interface TLSApiService {

    @GET("/")
    suspend fun invokeAlpahBill(): ResponseBody
}