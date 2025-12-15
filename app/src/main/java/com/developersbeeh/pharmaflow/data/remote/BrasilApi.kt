package com.developersbeeh.pharmaflow.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

data class CepResponse(
    @SerializedName("cep") val cep: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("neighborhood") val neighborhood: String? = null,
    @SerializedName("street") val street: String? = null,
    @SerializedName("service") val service: String? = null
)

interface BrasilApi {
    // API Gratuita e Open Source, excelente uptime
    @GET("api/cep/v1/{cep}")
    suspend fun getAddressByCep(@Path("cep") cep: String): CepResponse
}