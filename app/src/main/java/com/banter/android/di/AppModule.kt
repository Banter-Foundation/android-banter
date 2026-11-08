package com.banter.android.di

import com.banter.android.data.api.BanterApi
import com.banter.android.data.api.OkHttp3Client
import com.banter.android.data.local.SessionStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val DEFAULT_SERVER_URL = "http://127.0.0.1:3970/"

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .serializeNulls()
        .create()

    @Provides
    @Singleton
    fun provideOkHttp3Client(sessionStore: SessionStore): OkHttp3Client {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val token = runBlocking { sessionStore.token.first() }
                val req = if (token != null) {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(req)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        return OkHttp3Client(client)
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttp3Client: OkHttp3Client,
        gson: Gson,
        sessionStore: SessionStore
    ): Retrofit {
        val baseUrl = runBlocking {
            sessionStore.serverUrl.first() ?: DEFAULT_SERVER_URL
        }.let { if (it.endsWith("/")) it else "$it/" }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp3Client.client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideBanterApi(retrofit: Retrofit): BanterApi =
        retrofit.create(BanterApi::class.java)
}
