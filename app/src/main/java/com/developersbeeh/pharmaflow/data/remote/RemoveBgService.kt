package com.developersbeeh.pharmaflow.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

// Interface da API
interface RemoveBgApi {
    @Multipart
    @POST("v1.0/removebg")
    suspend fun removeBackground(
        @Header("X-Api-Key") apiKey: String,
        @Part image: MultipartBody.Part,
        @Part("size") size: okhttp3.RequestBody
    ): ResponseBody
}

@Singleton
class RemoveBgService @Inject constructor() {

    // --- SUA CHAVE CONFIGURADA ---
    private val apiKey = "ykwg5nsZHGHghAsTk1vpNJ3U"

    private val client = OkHttpClient.Builder().build()

    private val api = Retrofit.Builder()
        .baseUrl("https://api.remove.bg/")
        .client(client)
        .build()
        .create(RemoveBgApi::class.java)

    suspend fun processImage(bitmap: Bitmap): Result<Bitmap> {
        return try {
            // 1. Converte Bitmap para Bytes
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            val requestFile = byteArray.toRequestBody("image/png".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image_file", "product.png", requestFile)
            // "auto" deixa a API decidir a melhor resolução (até 25 megapixels no plano free, mas geralmente retorna preview)
            val size = "auto".toRequestBody("text/plain".toMediaTypeOrNull())

            // 2. Chama a API
            val response = api.removeBackground(apiKey, body, size)

            // 3. Converte a resposta (Bytes) de volta para Bitmap
            val bytes = response.bytes()
            val resultBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            if (resultBitmap != null) {
                Result.success(resultBitmap)
            } else {
                Result.failure(Exception("Falha ao decodificar imagem da API"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}