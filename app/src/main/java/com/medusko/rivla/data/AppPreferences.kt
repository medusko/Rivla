package com.medusko.rivla.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.medusko.rivla.model.*
import org.json.JSONArray
import org.json.JSONObject

class AppPreferences(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                "rivla_encrypted_byok_vault",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            appContext.getSharedPreferences("rivla_byok_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", true)
    fun setDarkMode(enabled: Boolean) = prefs.edit().putBoolean("dark_mode", enabled).apply()

    fun allowWakeSleepingVehicles(): Boolean = prefs.getBoolean("allow_wake_sleeping", false)
    fun setAllowWakeSleepingVehicles(allow: Boolean) =
        prefs.edit().putBoolean("allow_wake_sleeping", allow).apply()

    fun wifiOnlyLocalGateway(): Boolean = prefs.getBoolean("wifi_only_lan_gateway", true)
    fun setWifiOnlyLocalGateway(wifiOnly: Boolean) =
        prefs.edit().putBoolean("wifi_only_lan_gateway", wifiOnly).apply()

    fun getDistanceUnit(): DistanceUnit {
        val raw = prefs.getString("distance_unit", DistanceUnit.MILES.name)
        return DistanceUnit.entries.find { it.name == raw } ?: DistanceUnit.MILES
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        prefs.edit().putString("distance_unit", unit.name).apply()
    }

    fun getPollingInterval(): PollingInterval {
        val raw = prefs.getString("polling_interval", PollingInterval.MINUTES_60.name)
        return PollingInterval.entries.find { it.name == raw } ?: PollingInterval.MINUTES_60
    }

    fun setPollingInterval(interval: PollingInterval) {
        prefs.edit().putString("polling_interval", interval.name).apply()
    }

    fun loadVaultState(): LocalAuthVaultState {
        val providerRaw = prefs.getString("tesla_provider", TeslaProviderType.FLEET_NA.name)
        val provider = TeslaProviderType.entries.find { it.name == providerRaw }
            ?: TeslaProviderType.FLEET_NA

        return LocalAuthVaultState(
            rivianEmail = prefs.getString("rivian_email", "") ?: "",
            rivianPassword = prefs.getString("rivian_password", "") ?: "",
            rivianAccessToken = prefs.getString("rivian_access_token", "") ?: "",
            rivianRefreshToken = prefs.getString("rivian_refresh_token", "") ?: "",
            rivianUserSessionToken = prefs.getString("rivian_user_session_token", "") ?: "",
            rivianCsrfToken = prefs.getString("rivian_csrf_token", "") ?: "",
            rivianAppSessionToken = prefs.getString("rivian_app_session_token", "") ?: "",
            rivianPendingOtpToken = prefs.getString("rivian_pending_otp_token", "") ?: "",
            rivianVehicleId = prefs.getString("rivian_vehicle_id", "") ?: "",
            rivianVehicleName = prefs.getString("rivian_vehicle_name", "Rivian R1S") ?: "Rivian R1S",

            teslaProvider = provider,
            teslaClientId = prefs.getString("tesla_client_id", "") ?: "",
            teslaAccessToken = prefs.getString("tesla_access_token", "") ?: "",
            teslaRefreshToken = prefs.getString("tesla_refresh_token", "") ?: "",
            teslaVehicleId = prefs.getString("tesla_vehicle_id", "") ?: "",
            teslaVehicleName = prefs.getString("tesla_vehicle_name", "Tesla Model Y") ?: "Tesla Model Y",
            teslaEnergySiteId = prefs.getString("tesla_energy_site_id", "") ?: "",

            powerwallGatewayIp = prefs.getString("pw_gateway_ip", "") ?: "",
            powerwallEmail = prefs.getString("pw_email", "") ?: "",
            powerwallCustomerPassword = prefs.getString("pw_customer_password", "") ?: "",
            powerwallAuthCookie = prefs.getString("pw_auth_cookie", "") ?: "",
            powerwallSiteName = prefs.getString("pw_site_name", "Powerwall+ #1") ?: "Powerwall+ #1",

            haBaseUrl = prefs.getString("ha_base_url", "") ?: "",
            haAccessToken = prefs.getString("ha_access_token", "") ?: "",
            haSlot1Entity = prefs.getString("ha_slot1_entity", "sensor.rivian_battery_level") ?: "sensor.rivian_battery_level",
            haSlot2Entity = prefs.getString("ha_slot2_entity", "sensor.tesla_battery_level") ?: "sensor.tesla_battery_level",
            haSlot3Entity = prefs.getString("ha_slot3_entity", "sensor.powerwall_charge") ?: "sensor.powerwall_charge",
            haSlot4Entity = prefs.getString("ha_slot4_entity", "sensor.powerwall_expansion_charge") ?: "sensor.powerwall_expansion_charge"
        )
    }

    fun saveVaultState(state: LocalAuthVaultState) {
        prefs.edit()
            .putString("rivian_email", state.rivianEmail.trim())
            .putString("rivian_password", state.rivianPassword)
            .putString("rivian_access_token", state.rivianAccessToken.trim())
            .putString("rivian_refresh_token", state.rivianRefreshToken.trim())
            .putString("rivian_user_session_token", state.rivianUserSessionToken.trim())
            .putString("rivian_csrf_token", state.rivianCsrfToken.trim())
            .putString("rivian_app_session_token", state.rivianAppSessionToken.trim())
            .putString("rivian_pending_otp_token", state.rivianPendingOtpToken.trim())
            .putString("rivian_vehicle_id", state.rivianVehicleId.trim())
            .putString("rivian_vehicle_name", state.rivianVehicleName.trim().ifBlank { "Rivian R1S" })
            .putString("tesla_provider", state.teslaProvider.name)
            .putString("tesla_client_id", state.teslaClientId.trim())
            .putString("tesla_access_token", state.teslaAccessToken.trim())
            .putString("tesla_refresh_token", state.teslaRefreshToken.trim())
            .putString("tesla_vehicle_id", state.teslaVehicleId.trim())
            .putString("tesla_vehicle_name", state.teslaVehicleName.trim().ifBlank { "Tesla Model Y" })
            .putString("tesla_energy_site_id", state.teslaEnergySiteId.trim())
            .putString("pw_gateway_ip", state.powerwallGatewayIp.trim())
            .putString("pw_email", state.powerwallEmail.trim())
            .putString("pw_customer_password", state.powerwallCustomerPassword)
            .putString("pw_auth_cookie", state.powerwallAuthCookie.trim())
            .putString("pw_site_name", state.powerwallSiteName.trim().ifBlank { "Powerwall+ #1" })
            .putString("ha_base_url", state.haBaseUrl.trim().trimEnd('/'))
            .putString("ha_access_token", state.haAccessToken.trim())
            .putString("ha_slot1_entity", state.haSlot1Entity.trim())
            .putString("ha_slot2_entity", state.haSlot2Entity.trim())
            .putString("ha_slot3_entity", state.haSlot3Entity.trim())
            .putString("ha_slot4_entity", state.haSlot4Entity.trim())
            .apply()
    }

    fun clearAllSecrets() {
        val dark = isDarkMode()
        val unit = getDistanceUnit()
        prefs.edit().clear().apply()
        setDarkMode(dark)
        setDistanceUnit(unit)
    }

    fun loadProductSlots(): List<EnergyProductSlot> {
        val raw = prefs.getString("energy_product_slots_v1", null) ?: return defaultProductSlots()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<EnergyProductSlot>()
            for (i in 0 until arr.length().coerceAtMost(4)) {
                val obj = arr.getJSONObject(i)
                val typeName = obj.optString("productType", ProductType.RIVIAN_VEHICLE.name)
                val sleepName = obj.optString("sleepState", VehicleSleepState.AWAKE.name)
                list.add(
                    EnergyProductSlot(
                        slotIndex = i,
                        id = obj.optString("id", "slot_$i"),
                        name = obj.optString("name", "Product ${i + 1}"),
                        productType = ProductType.entries.find { it.name == typeName } ?: ProductType.RIVIAN_VEHICLE,
                        batteryPercent = obj.optInt("batteryPercent", 80).coerceIn(0, 100),
                        estimatedRangeMiles = if (obj.has("estimatedRangeMiles") && !obj.isNull("estimatedRangeMiles")) {
                            obj.optInt("estimatedRangeMiles")
                        } else null,
                        powerFlowKw = if (obj.has("powerFlowKw") && !obj.isNull("powerFlowKw")) {
                            obj.optDouble("powerFlowKw")
                        } else null,
                        sleepState = VehicleSleepState.entries.find { it.name == sleepName } ?: VehicleSleepState.AWAKE,
                        statusDetail = obj.optString("statusDetail", ""),
                        sourceLabel = obj.optString("sourceLabel", "Local Vault"),
                        externalId = obj.optString("externalId", ""),
                        lastUpdatedEpochMs = obj.optLong("lastUpdatedEpochMs", System.currentTimeMillis()),
                        isLiveSynced = obj.optBoolean("isLiveSynced", false)
                    )
                )
            }
            if (list.size == 4) list else defaultProductSlots()
        } catch (_: Exception) {
            defaultProductSlots()
        }
    }

    fun saveProductSlots(slots: List<EnergyProductSlot>) {
        val arr = JSONArray()
        slots.take(4).forEachIndexed { idx, slot ->
            val obj = JSONObject().apply {
                put("slotIndex", idx)
                put("id", slot.id)
                put("name", slot.name)
                put("productType", slot.productType.name)
                put("batteryPercent", slot.batteryPercent.coerceIn(0, 100))
                if (slot.estimatedRangeMiles != null) put("estimatedRangeMiles", slot.estimatedRangeMiles)
                if (slot.powerFlowKw != null) put("powerFlowKw", slot.powerFlowKw)
                put("sleepState", slot.sleepState.name)
                put("statusDetail", slot.statusDetail)
                put("sourceLabel", slot.sourceLabel)
                put("externalId", slot.externalId)
                put("lastUpdatedEpochMs", slot.lastUpdatedEpochMs)
                put("isLiveSynced", slot.isLiveSynced)
            }
            arr.put(obj)
        }
        prefs.edit().putString("energy_product_slots_v1", arr.toString()).apply()
    }

    companion object {
        fun defaultProductSlots(): List<EnergyProductSlot> {
            val now = System.currentTimeMillis()
            return listOf(
                EnergyProductSlot(
                    slotIndex = 0,
                    id = "slot_rivian",
                    name = "Rivian R1S",
                    productType = ProductType.RIVIAN_VEHICLE,
                    batteryPercent = 78,
                    estimatedRangeMiles = 248,
                    powerFlowKw = null,
                    sleepState = VehicleSleepState.ASLEEP,
                    statusDetail = "Sleeping (Cached)",
                    sourceLabel = "Rivian GraphQL",
                    lastUpdatedEpochMs = now,
                    isLiveSynced = false
                ),
                EnergyProductSlot(
                    slotIndex = 1,
                    id = "slot_tesla",
                    name = "Tesla Model Y",
                    productType = ProductType.TESLA_VEHICLE,
                    batteryPercent = 82,
                    estimatedRangeMiles = 265,
                    powerFlowKw = 7.6,
                    sleepState = VehicleSleepState.CHARGING,
                    statusDetail = "Charging ⚡",
                    sourceLabel = "Tesla Fleet BYOK",
                    lastUpdatedEpochMs = now,
                    isLiveSynced = false
                ),
                EnergyProductSlot(
                    slotIndex = 2,
                    id = "slot_pw1",
                    name = "Powerwall+ #1",
                    productType = ProductType.TESLA_POWERWALL,
                    batteryPercent = 94,
                    estimatedRangeMiles = null,
                    powerFlowKw = 3.4,
                    sleepState = VehicleSleepState.CHARGING,
                    statusDetail = "Solar Charging",
                    sourceLabel = "Local Gateway LAN",
                    lastUpdatedEpochMs = now,
                    isLiveSynced = false
                ),
                EnergyProductSlot(
                    slotIndex = 3,
                    id = "slot_pw2",
                    name = "Powerwall 3 #2",
                    productType = ProductType.TESLA_POWERWALL,
                    batteryPercent = 100,
                    estimatedRangeMiles = null,
                    powerFlowKw = 0.0,
                    sleepState = VehicleSleepState.STANDBY,
                    statusDetail = "Backup Ready • Full",
                    sourceLabel = "Tesla Energy / LAN",
                    lastUpdatedEpochMs = now,
                    isLiveSynced = false
                )
            )
        }
    }
}
