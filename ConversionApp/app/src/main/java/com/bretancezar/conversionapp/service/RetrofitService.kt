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
    fun convert(dto: ConversionDTO): Call<ConversionDTO>

    companion object {

        private var _retrofitService: RetrofitService? = null

        private operator fun invoke(): RetrofitService {

            // TODO replace with server IP
            val baseURLString = "192.168.149.49:2305/api/"
            val serverTimeoutInterval: Long = 3

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

            if (_retrofitService == null) {

                val new = RetrofitService()

                _retrofitService = new

                return new
            }

            throw IllegalStateException("Error on Retrofit instance creation.")
        }
    }
}