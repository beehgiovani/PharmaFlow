package com.developersbeeh.pharmaflow.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

interface ProductApiService {
    @GET("api/v0/product/{ean}.json")
    suspend fun getProductByEan(@Path("ean") ean: String): OpenFoodResponse
}

data class OpenFoodResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("product") val product: RemoteProduct?
)

data class RemoteProduct(
    @SerializedName("product_name") val productName: String?,
    @SerializedName("image_url") val imageUrl: String?
)