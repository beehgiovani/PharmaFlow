package com.developersbeeh.pharmaflow.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

interface EanPicturesApi {
    @GET("api/desc/{ean}")
    suspend fun getProductDescription(@Path("ean") ean: String): EanPictureResponse
}

data class EanPictureResponse(
    @SerializedName("nome") val nome: String? = null,
    @SerializedName("descricao") val descricao: String? = null,
    @SerializedName("gtin") val gtin: String? = null,
    @SerializedName("imagem") val imagem: String? = null
)