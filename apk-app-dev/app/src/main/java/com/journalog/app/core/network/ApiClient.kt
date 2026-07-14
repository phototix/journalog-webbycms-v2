package com.journalog.app.core.network

import com.journalog.app.core.debug.DebugLogStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var token: String? = null
    private var baseUrl: String = "https://journalog.webbypage.com/api/v1/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val debugInterceptor = Interceptor { chain ->
        val request = chain.request()
        val requestBody = DebugLogStore.requestBodyToString(request.body)

        var responseCode = 0
        var responseBody = ""
        var errorMsg = ""

        try {
            val response = chain.proceed(request)
            responseCode = response.code
            responseBody = DebugLogStore.responseBodyToString(response.body)
            DebugLogStore.add(
                method = request.method,
                url = request.url.toString(),
                requestHeaders = request.headers.toString(),
                requestBody = requestBody,
                responseCode = responseCode,
                responseBody = responseBody
            )
            response
        } catch (e: Exception) {
            errorMsg = e.message ?: e.javaClass.simpleName
            DebugLogStore.add(
                method = request.method,
                url = request.url.toString(),
                requestHeaders = request.headers.toString(),
                requestBody = requestBody,
                error = errorMsg
            )
            throw e
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                token?.let {
                    request.addHeader("Authorization", "Bearer $it")
                }
                request.addHeader("Accept", "application/json")
                chain.proceed(request.build())
            }
            .addInterceptor(debugInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> create(service: Class<T>): T = retrofit.create(service)

    fun setToken(newToken: String?) {
        token = newToken
    }

    fun getToken(): String? = token

    fun setBaseUrl(url: String) {
        baseUrl = url
    }
}
