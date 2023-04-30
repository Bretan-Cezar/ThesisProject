package com.bretancezar.conversionapp.service

import com.bretancezar.conversionapp.service.dto.ConversionDTO
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface RetrofitService {

    @POST("convert/")
    fun convert(@Body dto: ConversionDTO): Call<ConversionDTO>

    companion object {

        private var _retrofit: RetrofitService? = null

        private operator fun invoke(): RetrofitService {

            // TODO replace with server IP
            val baseURLString = "192.168.1.67:8090/api/"
            val serverTimeoutInterval: Long = 8

            val http = OkHttpClient.Builder()
                .connectTimeout(serverTimeoutInterval, TimeUnit.SECONDS)
                .callTimeout(serverTimeoutInterval, TimeUnit.SECONDS)
                .readTimeout(serverTimeoutInterval, TimeUnit.SECONDS)
                .writeTimeout(serverTimeoutInterval, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("http://$baseURLString")
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(RetrofitService::class.java)
        }

        fun getInstance(): RetrofitService {

            return _retrofit ?: synchronized(this) {

                val new = RetrofitService()
                _retrofit = new

                return new
            }
        }
    }
}