package com.medusko.rivla.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medusko.rivla.data.AppPreferences
import com.medusko.rivla.data.RivlaTelemetryRepository
import com.medusko.rivla.model.*
import com.medusko.rivla.widget.RivlaEnergyWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RivlaViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val repository = RivlaTelemetryRepository()

    private val _isDarkMode = MutableStateFlow(prefs.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _vaultState = MutableStateFlow(prefs.loadVaultState())
    val vaultState: StateFlow<LocalAuthVaultState> = _vaultState.asStateFlow()

    private val _productSlots = MutableStateFlow(prefs.loadProductSlots())
    val productSlots: StateFlow<List<EnergyProductSlot>> = _productSlots.asStateFlow()

    private val _allowWakeSleepingVehicles = MutableStateFlow(prefs.allowWakeSleepingVehicles())
    val allowWakeSleepingVehicles: StateFlow<Boolean> = _allowWakeSleepingVehicles.asStateFlow()

    private val _wifiOnlyLocalGateway = MutableStateFlow(prefs.wifiOnlyLocalGateway())
    val wifiOnlyLocalGateway: StateFlow<Boolean> = _wifiOnlyLocalGateway.asStateFlow()

    private val _pollingInterval = MutableStateFlow(prefs.getPollingInterval())
    val pollingInterval: StateFlow<PollingInterval> = _pollingInterval.asStateFlow()

    private val _distanceUnit = MutableStateFlow(prefs.getDistanceUnit())
    val distanceUnit: StateFlow<DistanceUnit> = _distanceUnit.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _rivianOtpPending = MutableStateFlow(false)
    val rivianOtpPending: StateFlow<Boolean> = _rivianOtpPending.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow(
        if (_vaultState.value.configuredProviderCount > 0) {
            "On-Device Encrypted Vault active (${_vaultState.value.configuredProviderCount}/4 sources configured)"
        } else {
            "BYOK Client-Only Mode • Enter logins/keys in Setup to save locally"
        }
    )
    val statusBannerMessage: StateFlow<String> = _statusBannerMessage.asStateFlow()

    init {
        RivlaEnergyWidgetProvider.notifyWidgetsChanged(getApplication())
    }

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        prefs.setDarkMode(next)
    }

    fun setAllowWakeSleepingVehicles(allow: Boolean) {
        _allowWakeSleepingVehicles.value = allow
        prefs.setAllowWakeSleepingVehicles(allow)
        _statusBannerMessage.value = if (allow) {
            "Warning: Vehicle Wake enabled (may cause 1–3%/day phantom battery drain)"
        } else {
            "Phantom-Drain Sleep Guard enabled (sleeping EVs will not be woken)"
        }
    }

    fun setWifiOnlyLocalGateway(wifiOnly: Boolean) {
        _wifiOnlyLocalGateway.value = wifiOnly
        prefs.setWifiOnlyLocalGateway(wifiOnly)
    }

    fun setPollingInterval(interval: PollingInterval) {
        _pollingInterval.value = interval
        prefs.setPollingInterval(interval)
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        _distanceUnit.value = unit
        prefs.setDistanceUnit(unit)
        RivlaEnergyWidgetProvider.notifyWidgetsChanged(getApplication())
    }

    fun saveVaultAndSync(
        newState: LocalAuthVaultState,
        bannerMessage: String = "Credentials saved locally in Android EncryptedSharedPreferences"
    ) {
        _vaultState.value = newState
        prefs.saveVaultState(newState)
        _statusBannerMessage.value = bannerMessage
        syncAllTelemetry(customPrefix = bannerMessage)
    }

    fun authenticateRivianDirect(
        email: String,
        password: String,
        vehicleName: String,
        vehicleId: String
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            val baseVault = _vaultState.value.copy(
                rivianEmail = email.trim(),
                rivianPassword = password,
                rivianVehicleName = vehicleName.trim().ifBlank { "Rivian R1S" },
                rivianVehicleId = vehicleId.trim()
            )
            _vaultState.value = baseVault
            prefs.saveVaultState(baseVault)

            val result = repository.authenticateRivian(email, password)
            val updatedVault = baseVault.copy(
                rivianCsrfToken = result.csrfToken.ifBlank { baseVault.rivianCsrfToken },
                rivianAppSessionToken = result.appSessionToken.ifBlank { baseVault.rivianAppSessionToken },
                rivianPendingOtpToken = result.otpToken,
                rivianAccessToken = result.accessToken.ifBlank { baseVault.rivianAccessToken },
                rivianRefreshToken = result.refreshToken.ifBlank { baseVault.rivianRefreshToken },
                rivianUserSessionToken = result.userSessionToken.ifBlank { baseVault.rivianUserSessionToken }
            )
            _vaultState.value = updatedVault
            prefs.saveVaultState(updatedVault)
            _rivianOtpPending.value = result.otpRequired
            _statusBannerMessage.value = result.message
            _isSyncing.value = false

            if (result.success && !result.otpRequired) {
                syncAllTelemetry()
            }
        }
    }

    fun verifyRivianOtpCode(otpCode: String) {
        val current = _vaultState.value
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.verifyRivianOtp(
                email = current.rivianEmail,
                otpCode = otpCode,
                otpToken = current.rivianPendingOtpToken,
                csrfToken = current.rivianCsrfToken,
                appSessionToken = current.rivianAppSessionToken
            )
            if (result.success) {
                val nextVault = current.copy(
                    rivianAccessToken = result.accessToken,
                    rivianRefreshToken = result.refreshToken,
                    rivianUserSessionToken = result.userSessionToken,
                    rivianPendingOtpToken = ""
                )
                _vaultState.value = nextVault
                prefs.saveVaultState(nextVault)
                _rivianOtpPending.value = false
                _statusBannerMessage.value = result.message
                syncAllTelemetry()
            } else {
                _statusBannerMessage.value = result.message
            }
            _isSyncing.value = false
        }
    }

    fun updateProductSlot(
        slotIndex: Int,
        name: String,
        productType: ProductType,
        batteryPercent: Int,
        rangeMiles: Int?,
        powerKw: Double?,
        sleepState: VehicleSleepState,
        statusDetail: String
    ) {
        val current = _productSlots.value.toMutableList()
        if (slotIndex in current.indices) {
            current[slotIndex] = current[slotIndex].copy(
                name = name.trim().ifBlank { "Slot ${slotIndex + 1}" },
                productType = productType,
                batteryPercent = batteryPercent.coerceIn(0, 100),
                estimatedRangeMiles = rangeMiles,
                powerFlowKw = powerKw,
                sleepState = sleepState,
                statusDetail = statusDetail.trim().ifBlank { sleepState.label },
                lastUpdatedEpochMs = System.currentTimeMillis()
            )
            _productSlots.value = current
            prefs.saveProductSlots(current)
            RivlaEnergyWidgetProvider.notifyWidgetsChanged(getApplication())
            _statusBannerMessage.value = "Updated 2x2 Widget Slot ${slotIndex + 1} (${current[slotIndex].name})"
        }
    }

    fun syncAllTelemetry(customPrefix: String? = null) {
        viewModelScope.launch {
            _isSyncing.value = true
            val report = repository.syncAllProducts(
                vault = _vaultState.value,
                currentSlots = _productSlots.value,
                allowWakeSleepingVehicles = _allowWakeSleepingVehicles.value
            )
            _productSlots.value = report.updatedSlots
            prefs.saveProductSlots(report.updatedSlots)
            RivlaEnergyWidgetProvider.notifyWidgetsChanged(getApplication())
            _statusBannerMessage.value = customPrefix ?: report.statusMessage
            _isSyncing.value = false
        }
    }

    fun resetToDefaultDemoSlots() {
        val defaults = AppPreferences.defaultProductSlots()
        _productSlots.value = defaults
        prefs.saveProductSlots(defaults)
        RivlaEnergyWidgetProvider.notifyWidgetsChanged(getApplication())
        _statusBannerMessage.value = "Restored 4-product 2x2 grid preview telemetry & updated widgets"
    }

    fun clearAllLocalSecrets() {
        prefs.clearAllSecrets()
        _vaultState.value = prefs.loadVaultState()
        _productSlots.value = prefs.loadProductSlots()
        RivlaEnergyWidgetProvider.notifyWidgetsChanged(getApplication())
        _statusBannerMessage.value = "Cleared all locally stored tokens & credentials from device vault"
    }
}
