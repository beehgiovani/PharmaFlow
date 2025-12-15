package com.developersbeeh.pharmaflow.features.marketing

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.model.Banner
import com.developersbeeh.pharmaflow.data.model.Coupon
import com.developersbeeh.pharmaflow.data.repository.MarketingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BannerManagerViewModel @Inject constructor(
    private val repository: MarketingRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // --- BANNERS STATE ---
    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners = _banners.asStateFlow()

    // --- COUPONS STATE ---
    private val _coupons = MutableStateFlow<List<Coupon>>(emptyList())
    val coupons = _coupons.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        loadBanners()
        loadCoupons()
    }

    // --- BANNERS LOGIC ---

    fun loadBanners() {
        val storeId = sessionManager.getCurrentStoreId()
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getBanners(storeId)
            if (result.isSuccess) {
                _banners.value = result.getOrDefault(emptyList())
            }
            _isLoading.value = false
        }
    }

    fun uploadBanner(uri: Uri) {
        val storeId = sessionManager.getCurrentStoreId()
        if (storeId.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            val uploadResult = repository.uploadBannerImage(uri)

            if (uploadResult.isSuccess) {
                val url = uploadResult.getOrDefault("")
                val newBanner = Banner(
                    imageUrl = url,
                    storeId = storeId,
                    active = true,
                    order = _banners.value.size + 1
                )
                repository.saveBanner(newBanner)
                loadBanners()
            }
            _isLoading.value = false
        }
    }

    fun deleteBanner(banner: Banner) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteBanner(banner.id)
            loadBanners()
            _isLoading.value = false
        }
    }

    // --- COUPONS LOGIC (NOVO) ---

    fun loadCoupons() {
        viewModelScope.launch {
            val result = repository.getAllCoupons() // Admin vê todos
            if (result.isSuccess) {
                _coupons.value = result.getOrDefault(emptyList())
            }
        }
    }

    fun createCoupon(code: String, type: String, value: Double, minPurchase: Double) {
        val storeId = sessionManager.getCurrentStoreId()
        // Para simplificar, o cupom criado pelo admin será global ou da loja dele
        // Vamos fazer Global por padrão nesta versão MVP

        viewModelScope.launch {
            _isLoading.value = true
            val newCoupon = Coupon(
                code = code,
                type = type, // "PERCENT" ou "FIXED"
                value = value,
                minPurchase = minPurchase,
                isActive = true,
                storeId = "" // Global
            )
            repository.saveCoupon(newCoupon)
            loadCoupons()
            _isLoading.value = false
        }
    }

    fun deleteCoupon(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteCoupon(code)
            loadCoupons()
            _isLoading.value = false
        }
    }
}