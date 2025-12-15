package com.developersbeeh.pharmaflow.di

import com.developersbeeh.pharmaflow.data.remote.BrasilApi
import com.developersbeeh.pharmaflow.data.remote.EanPicturesApi
import com.developersbeeh.pharmaflow.data.remote.ProductApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // 1. Cliente Padrão (HTTPS, Seguro)
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val headerInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "PharmaFlowApp - Android - Version 1.0")
                .build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // 2. Cliente Inseguro/Específico (Para EanPictures HTTP)
    @Provides
    @Singleton
    @Named("InsecureClient")
    fun provideInsecureOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // 3. API Open Food Facts
    @Provides
    @Singleton
    fun provideProductApiService(client: OkHttpClient): ProductApiService {
        return Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProductApiService::class.java)
    }

    // 4. API Ean Pictures
    @Provides
    @Singleton
    fun provideEanPicturesApi(@Named("InsecureClient") client: OkHttpClient): EanPicturesApi {
        return Retrofit.Builder()
            .baseUrl("http://www.eanpictures.com.br:9000/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EanPicturesApi::class.java)
    }

    // 5. NOVA: Brasil API (CEP) - https://brasilapi.com.br/
    @Provides
    @Singleton
    fun provideBrasilApi(client: OkHttpClient): BrasilApi {
        return Retrofit.Builder()
            .baseUrl("https://brasilapi.com.br/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BrasilApi::class.java)
    }
}