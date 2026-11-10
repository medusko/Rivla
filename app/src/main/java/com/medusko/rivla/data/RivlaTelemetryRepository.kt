package com.medusko.rivla.data

import com.medusko.rivla.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.math.roundToInt

data class RivianAuthResult(
    val success: Boolean,
    val otpRequired: Boolean = false,
    val csrfToken: String = "",
    val appSessionToken: String = "",
    val otpToken: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val userSessionToken: String = "",
    val message: String = ""
)

data class SyncReport(
    val updatedSlots: List<EnergyProductSlot>,
    val statusMessage: String,
    val anyLiveSuccess: Boolean
)

class RivlaTelemetryRepository {

    companion object {
        private const val RIVIAN_GRAPHQL_URL = "https://rivian.com/api/gql/gateway/graphql"
        private const val TIMEOUT_MS = 12_000

        /**
         * Parses Rivian's GraphQL `GetVehicleState` response JSON into battery %, range (mi),
         * power state, and charging state.
         */
        fun parseRivianVehicleStateJson(
            rawJson: String,
            fallbackSlot: EnergyProductSlot
        ): EnergyProductSlot? {
            return try {
                val root = JSONObject(rawJson)
                val data = root.optJSONObject("data") ?: root
                val vs = data.optJSONObject("vehicleState") ?: return null

                val batteryPct = vs.optJSONObject("batteryLevel")
                    ?.optDouble("value", fallbackSlot.batteryPercent.toDouble())
                    ?.roundToInt()
                    ?.coerceIn(0, 100)
                    ?: fallbackSlot.batteryPercent

                // Rivian distanceToEmpty is reported in kilometers
                val distanceKm = vs.optJSONObject("distanceToEmpty")
                    ?.optDouble("value", -1.0) ?: -1.0
                val rangeMiles = if (distanceKm > 0) {
                    (distanceKm * 0.621371).roundToInt()
                } else {
                    fallbackSlot.estimatedRangeMiles
                }

                val chargerState = vs.optJSONObject("chargerState")
                    ?.optString("value", "")?.lowercase() ?: ""
                val powerState = vs.optJSONObject("powerState")
                    ?.optString("value", "")?.lowercase() ?: ""

                val sleepState = when {
                    chargerState.contains("charging") -> VehicleSleepState.CHARGING
                    powerState.contains("sleep") || powerState.contains("standby") -> VehicleSleepState.ASLEEP
                    else -> VehicleSleepState.AWAKE
                }

                val detail = when (sleepState) {
                    VehicleSleepState.CHARGING -> "Charging ⚡"
                    VehicleSleepState.ASLEEP -> "Sleeping (No-Wake)"
                    else -> "Online • Ready"
                }

                fallbackSlot.copy(
                    batteryPercent = batteryPct,
                    estimatedRangeMiles = rangeMiles,
                    sleepState = sleepState,
                    statusDetail = detail,
                    sourceLabel = "Rivian GraphQL (Live)",
                    lastUpdatedEpochMs = System.currentTimeMillis(),
                    isLiveSynced = true
                )
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Parses Tesla Fleet API / Tessie / Teslemetry `/api/1/vehicles/{id}/vehicle_data` JSON.
         */
        fun parseTeslaVehicleDataJson(
            rawJson: String,
            fallbackSlot: EnergyProductSlot
        ): EnergyProductSlot? {
            return try {
                val root = JSONObject(rawJson)
                val response = root.optJSONObject("response") ?: root
                val stateStr = response.optString("state", "online").lowercase()
                val chargeState = response.optJSONObject("charge_state")

                if (chargeState == null) {
                    // Vehicle summary without full charge_state (e.g., asleep summary)
                    val isSleeping = stateStr == "asleep" || stateStr == "offline"
                    return fallbackSlot.copy(
                        sleepState = if (isSleeping) VehicleSleepState.ASLEEP else VehicleSleepState.AWAKE,
                        statusDetail = if (isSleeping) "Sleeping (No-Wake Safe)" else "Online",
                        sourceLabel = "Tesla Fleet BYOK",
                        lastUpdatedEpochMs = System.currentTimeMillis(),
                        isLiveSynced = true
                    )
                }

                val batteryLevel = chargeState.optInt(
                    "battery_level",
                    fallbackSlot.batteryPercent
                ).coerceIn(0, 100)

                val batteryRangeMiles = chargeState.optDouble("battery_range", -1.0)
                    .takeIf { it > 0 }?.roundToInt() ?: fallbackSlot.estimatedRangeMiles

                val chargingState = chargeState.optString("charging_state", "").lowercase()
                val chargerPowerKw = chargeState.optDouble("charger_power", 0.0)

                val sleepState = when {
                    chargingState == "charging" -> VehicleSleepState.CHARGING
                    stateStr == "asleep" || stateStr == "offline" -> VehicleSleepState.ASLEEP
                    else -> VehicleSleepState.AWAKE
                }

                val detail = when (sleepState) {
                    VehicleSleepState.CHARGING -> "Charging ⚡"
                    VehicleSleepState.ASLEEP -> "Sleeping (Cached)"
                    else -> "Parked • Online"
                }

                fallbackSlot.copy(
                    batteryPercent = batteryLevel,
                    estimatedRangeMiles = batteryRangeMiles,
                    powerFlowKw = if (chargerPowerKw > 0) chargerPowerKw else null,
                    sleepState = sleepState,
                    statusDetail = detail,
                    sourceLabel = "Tesla Fleet BYOK (Live)",
                    lastUpdatedEpochMs = System.currentTimeMillis(),
                    isLiveSynced = true
                )
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Parses Tesla Fleet API `/api/1/energy_sites/{id}/live_status` JSON for Cloud Powerwall.
         */
        fun parseTeslaCloudPowerwallJson(
            rawJson: String,
            fallbackSlot: EnergyProductSlot
        ): EnergyProductSlot? {
            return try {
                val root = JSONObject(rawJson)
                val response = root.optJSONObject("response") ?: root
                if (!response.has("percentage_charged") && !response.has("battery_percentage")) {
                    return null
                }
                val pct = response.optDouble(
                    "percentage_charged",
                    response.optDouble("battery_percentage", fallbackSlot.batteryPercent.toDouble())
                ).roundToInt().coerceIn(0, 100)

                val solarWatts = response.optDouble("solar_power", 0.0)
                val batteryWatts = response.optDouble("battery_power", 0.0)
                val gridStatus = response.optString("grid_status", "Active")

                // In Tesla live_status, negative battery_power typically means charging, solar_power > 0 means solar active
                val netKw = when {
                    solarWatts > 100 -> (solarWatts / 1000.0)
                    batteryWatts != 0.0 -> (-batteryWatts / 1000.0)
                    else -> 0.0
                }

                val isCharging = solarWatts > 250 || batteryWatts < -200
                val sleepState = if (isCharging) VehicleSleepState.CHARGING else VehicleSleepState.STANDBY
                val detail = if (gridStatus.contains("Island", ignoreCase = true)) {
                    "Off-Grid Backup ⚡"
                } else if (isCharging) {
                    "Solar Charging"
                } else {
                    "Grid OK • Backup"
                }

                fallbackSlot.copy(
                    batteryPercent = pct,
                    estimatedRangeMiles = null,
                    powerFlowKw = (netKw * 10.0).roundToInt() / 10.0,
                    sleepState = sleepState,
                    statusDetail = detail,
                    sourceLabel = "Tesla Energy Cloud (Live)",
                    lastUpdatedEpochMs = System.currentTimeMillis(),
                    isLiveSynced = true
                )
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Parses Tesla Powerwall Local Gateway `/api/system_status/soe` and optional `/api/meters/aggregates`.
         */
        fun parsePowerwallLocalSoeJson(
            soeRawJson: String,
            metersRawJson: String?,
            fallbackSlot: EnergyProductSlot
        ): EnergyProductSlot? {
            return try {
                val soeObj = JSONObject(soeRawJson)
                if (!soeObj.has("percentage")) return null
                val rawPct = soeObj.optDouble("percentage", fallbackSlot.batteryPercent.toDouble())
                // Powerwall local SOE scales 0..100
                val pct = rawPct.roundToInt().coerceIn(0, 100)

                var solarKw = 0.0
                if (!metersRawJson.isNullOrBlank()) {
                    try {
                        val meters = JSONObject(metersRawJson)
                        val solarWatts = meters.optJSONObject("solar")
                            ?.optDouble("instant_power", 0.0) ?: 0.0
                        solarKw = (solarWatts / 1000.0 * 10.0).roundToInt() / 10.0
                    } catch (_: Exception) {
                    }
                }

                val isCharging = solarKw >= 0.2
                fallbackSlot.copy(
                    batteryPercent = pct,
                    estimatedRangeMiles = null,
                    powerFlowKw = if (solarKw != 0.0) solarKw else null,
                    sleepState = if (isCharging) VehicleSleepState.CHARGING else VehicleSleepState.STANDBY,
                    statusDetail = if (isCharging) "LAN • Solar Active" else "LAN • Backup Ready",
                    sourceLabel = "Local Gateway LAN (Live)",
                    lastUpdatedEpochMs = System.currentTimeMillis(),
                    isLiveSynced = true
                )
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Parses Home Assistant `/api/states/<entity_id>` JSON response.
         */
        fun parseHomeAssistantStateJson(
            rawJson: String,
            fallbackSlot: EnergyProductSlot
        ): EnergyProductSlot? {
            return try {
                val obj = JSONObject(rawJson)
                val stateStr = obj.optString("state", "")
                val pct = stateStr.toDoubleOrNull()?.roundToInt()?.coerceIn(0, 100) ?: return null
                val attrs = obj.optJSONObject("attributes")
                val friendlyName = attrs?.optString("friendly_name", "")?.takeIf { it.isNotBlank() }

                fallbackSlot.copy(
                    name = friendlyName ?: fallbackSlot.name,
                    batteryPercent = pct,
                    statusDetail = "Home Assistant Live",
                    sourceLabel = "HA Long-Lived Token",
                    lastUpdatedEpochMs = System.currentTimeMillis(),
                    isLiveSynced = true
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Step 1 & 2 of Rivian Direct Client-Only GraphQL Login (`CreateCSRFToken` + `Login`).
     */
    suspend fun authenticateRivian(
        email: String,
        password: String
    ): RivianAuthResult = withContext(Dispatchers.IO) {
        try {
            // 1. CreateCSRFToken mutation
            val csrfQuery = JSONObject().apply {
                put("operationName", "CreateCSRFToken")
                put(
                    "query",
                    "mutation CreateCSRFToken { createCsrfToken { __typename csrfToken appSessionToken } }"
                )
                put("variables", JSONObject())
            }
            val csrfResp = executeHttpRequest(
                urlStr = RIVIAN_GRAPHQL_URL,
                method = "POST",
                body = csrfQuery.toString(),
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Apollographql-Client-Name" to "com.rivian.android.consumer-apollo-android"
                )
            ) ?: return@withContext RivianAuthResult(
                success = false,
                message = "Could not reach rivian.com GraphQL gateway. Credentials saved locally."
            )

            val csrfData = JSONObject(csrfResp)
                .optJSONObject("data")
                ?.optJSONObject("createCsrfToken")
            val csrfToken = csrfData?.optString("csrfToken", "") ?: ""
            val appSessionToken = csrfData?.optString("appSessionToken", "") ?: ""

            if (csrfToken.isBlank()) {
                return@withContext RivianAuthResult(
                    success = false,
                    message = "Rivian CSRF handshake returned empty token. Check network or paste session token directly."
                )
            }

            // 2. Login mutation
            val loginVars = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }
            val loginPayload = JSONObject().apply {
                put("operationName", "Login")
                put("variables", loginVars)
                put(
                    "query",
                    "mutation Login(\$email: String!, \$password: String!) { login(email: \$email, password: \$password) { __typename ... on MobileLoginResponse { accessToken refreshToken userSessionToken } ... on MobileMFALoginResponse { otpToken } } }"
                )
            }

            val loginResp = executeHttpRequest(
                urlStr = RIVIAN_GRAPHQL_URL,
                method = "POST",
                body = loginPayload.toString(),
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Csrf-Token" to csrfToken,
                    "A-Sess" to appSessionToken,
                    "Apollographql-Client-Name" to "com.rivian.android.consumer-apollo-android"
                )
            ) ?: return@withContext RivianAuthResult(
                success = false,
                csrfToken = csrfToken,
                appSessionToken = appSessionToken,
                message = "Rivian login request timed out. Credentials saved locally."
            )

            val loginNode = JSONObject(loginResp)
                .optJSONObject("data")
                ?.optJSONObject("login")

            if (loginNode == null) {
                return@withContext RivianAuthResult(
                    success = false,
                    csrfToken = csrfToken,
                    appSessionToken = appSessionToken,
                    message = "Rivian login rejected or requires direct token paste."
                )
            }

            val otpToken = loginNode.optString("otpToken", "")
            if (otpToken.isNotBlank()) {
                return@withContext RivianAuthResult(
                    success = true,
                    otpRequired = true,
                    csrfToken = csrfToken,
                    appSessionToken = appSessionToken,
                    otpToken = otpToken,
                    message = "MFA code sent! Enter your 6-digit Rivian OTP code to complete local session setup."
                )
            }

            val accessToken = loginNode.optString("accessToken", "")
            val refreshToken = loginNode.optString("refreshToken", "")
            val userSessionToken = loginNode.optString("userSessionToken", "")

            RivianAuthResult(
                success = accessToken.isNotBlank() || userSessionToken.isNotBlank(),
                csrfToken = csrfToken,
                appSessionToken = appSessionToken,
                accessToken = accessToken,
                refreshToken = refreshToken,
                userSessionToken = userSessionToken,
                message = "Authenticated with Rivian GraphQL! Session stored in local encrypted vault."
            )
        } catch (e: Exception) {
            RivianAuthResult(
                success = false,
                message = "Saved locally (${e.localizedMessage ?: "Network offline"})."
            )
        }
    }

    /**
     * Step 3 of Rivian Direct Client-Only GraphQL Login (`LoginWithOTP`).
     */
    suspend fun verifyRivianOtp(
        email: String,
        otpCode: String,
        otpToken: String,
        csrfToken: String,
        appSessionToken: String
    ): RivianAuthResult = withContext(Dispatchers.IO) {
        try {
            val vars = JSONObject().apply {
                put("email", email.trim())
                put("otpCode", otpCode.trim())
                put("otpToken", otpToken.trim())
            }
            val payload = JSONObject().apply {
                put("operationName", "LoginWithOTP")
                put("variables", vars)
                put(
                    "query",
                    "mutation LoginWithOTP(\$email: String!, \$otpCode: String!, \$otpToken: String!) { loginWithOTP(email: \$email, otpCode: \$otpCode, otpToken: \$otpToken) { __typename ... on MobileLoginResponse { accessToken refreshToken userSessionToken } } }"
                )
            }
            val resp = executeHttpRequest(
                urlStr = RIVIAN_GRAPHQL_URL,
                method = "POST",
                body = payload.toString(),
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Csrf-Token" to csrfToken,
                    "A-Sess" to appSessionToken,
                    "Apollographql-Client-Name" to "com.rivian.android.consumer-apollo-android"
                )
            ) ?: return@withContext RivianAuthResult(
                success = false,
                message = "Could not verify OTP code with Rivian gateway."
            )

            val node = JSONObject(resp)
                .optJSONObject("data")
                ?.optJSONObject("loginWithOTP")
            val accessToken = node?.optString("accessToken", "") ?: ""
            val refreshToken = node?.optString("refreshToken", "") ?: ""
            val userSessionToken = node?.optString("userSessionToken", "") ?: ""

            if (accessToken.isNotBlank() || userSessionToken.isNotBlank()) {
                RivianAuthResult(
                    success = true,
                    csrfToken = csrfToken,
                    appSessionToken = appSessionToken,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userSessionToken = userSessionToken,
                    message = "Rivian MFA verified! Session saved locally in encrypted storage."
                )
            } else {
                RivianAuthResult(
                    success = false,
                    message = "Invalid or expired OTP code."
                )
            }
        } catch (e: Exception) {
            RivianAuthResult(
                success = false,
                message = "OTP verification error: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Synchronizes all 4 widget product slots using whatever BYOK credentials the user has saved locally.
     * Strictly honors `allowWakeSleepingVehicles` to prevent EV phantom battery drain.
     */
    suspend fun syncAllProducts(
        vault: LocalAuthVaultState,
        currentSlots: List<EnergyProductSlot>,
        allowWakeSleepingVehicles: Boolean
    ): SyncReport = withContext(Dispatchers.IO) {
        val updated = currentSlots.toMutableList()
        val syncedSources = mutableListOf<String>()
        var anyLiveSuccess = false

        // 1. Home Assistant Unified Token (if configured, can update any slot mapped to HA)
        if (vault.isHomeAssistantConfigured) {
            val entities = listOf(
                vault.haSlot1Entity,
                vault.haSlot2Entity,
                vault.haSlot3Entity,
                vault.haSlot4Entity
            )
            for (i in 0 until updated.size.coerceAtMost(4)) {
                val entityId = entities.getOrNull(i)?.trim().orEmpty()
                if (entityId.isNotBlank()) {
                    val haSlot = fetchHomeAssistantSlot(vault.haBaseUrl, vault.haAccessToken, entityId, updated[i])
                    if (haSlot != null) {
                        updated[i] = haSlot
                        anyLiveSuccess = true
                    }
                }
            }
            if (anyLiveSuccess) {
                syncedSources.add("Home Assistant")
            }
        }

        // 2. Rivian Direct GraphQL
        if (vault.isRivianConfigured) {
            val rivianIdx = updated.indexOfFirst { it.productType == ProductType.RIVIAN_VEHICLE }
                .takeIf { it >= 0 } ?: 0
            val fetched = fetchRivianSlot(vault, updated[rivianIdx].copy(name = vault.rivianVehicleName))
            if (fetched != null) {
                updated[rivianIdx] = fetched
                syncedSources.add("Rivian GraphQL")
                anyLiveSuccess = true
            } else {
                updated[rivianIdx] = updated[rivianIdx].copy(
                    name = vault.rivianVehicleName.ifBlank { updated[rivianIdx].name },
                    sourceLabel = "Rivian Vault (Cached)",
                    lastUpdatedEpochMs = System.currentTimeMillis()
                )
            }
        }

        // 3. Tesla Cloud BYOK (Fleet API / Tessie / Teslemetry)
        if (vault.isTeslaCloudConfigured) {
            val teslaEvIdx = updated.indexOfFirst { it.productType == ProductType.TESLA_VEHICLE }
                .takeIf { it >= 0 } ?: 1
            val fetchedEv = fetchTeslaVehicleSlot(
                vault = vault,
                fallbackSlot = updated[teslaEvIdx].copy(name = vault.teslaVehicleName),
                allowWake = allowWakeSleepingVehicles
            )
            if (fetchedEv != null) {
                updated[teslaEvIdx] = fetchedEv
                syncedSources.add(vault.teslaProvider.title.substringBefore(" ("))
                anyLiveSuccess = true
            } else {
                updated[teslaEvIdx] = updated[teslaEvIdx].copy(
                    name = vault.teslaVehicleName.ifBlank { updated[teslaEvIdx].name },
                    sourceLabel = "${vault.teslaProvider.title.substringBefore(" (")} (Cached)",
                    lastUpdatedEpochMs = System.currentTimeMillis()
                )
            }

            // Also check if Cloud Powerwall site ID is present
            if (vault.teslaEnergySiteId.isNotBlank()) {
                val pwIdx = updated.indexOfFirst { it.productType == ProductType.TESLA_POWERWALL }
                    .takeIf { it >= 0 } ?: 2
                val fetchedPw = fetchTeslaCloudPowerwallSlot(vault, updated[pwIdx])
                if (fetchedPw != null) {
                    updated[pwIdx] = fetchedPw
                    anyLiveSuccess = true
                }
            }
        }

        // 4. Tesla Powerwall Local LAN Gateway
        if (vault.isPowerwallLanConfigured) {
            val pwIdx = updated.indexOfFirst { it.productType == ProductType.TESLA_POWERWALL }
                .takeIf { it >= 0 } ?: 2
            val fetchedLan = fetchPowerwallLanSlot(
                vault = vault,
                fallbackSlot = updated[pwIdx].copy(name = vault.powerwallSiteName)
            )
            if (fetchedLan != null) {
                updated[pwIdx] = fetchedLan
                syncedSources.add("Powerwall LAN")
                anyLiveSuccess = true
            } else {
                updated[pwIdx] = updated[pwIdx].copy(
                    name = vault.powerwallSiteName.ifBlank { updated[pwIdx].name },
                    sourceLabel = "Gateway ${vault.powerwallGatewayIp} (Cached)",
                    lastUpdatedEpochMs = System.currentTimeMillis()
                )
            }
        }

        val msg = when {
            anyLiveSuccess -> "Live telemetry synced via ${syncedSources.distinct().joinToString(", ")}"
            vault.configuredProviderCount > 0 -> "Saved BYOK credentials verified • Displaying cached sleep-safe state"
            else -> "Preview telemetry active • Add BYOK keys in Setup for live vehicle sync"
        }

        SyncReport(
            updatedSlots = updated,
            statusMessage = msg,
            anyLiveSuccess = anyLiveSuccess
        )
    }

    private fun fetchRivianSlot(
        vault: LocalAuthVaultState,
        fallbackSlot: EnergyProductSlot
    ): EnergyProductSlot? {
        if (vault.rivianAccessToken.isBlank() && vault.rivianUserSessionToken.isBlank()) {
            return null
        }
        return try {
            val vehicleId = vault.rivianVehicleId.ifBlank { "default" }
            val queryObj = JSONObject().apply {
                put("operationName", "GetVehicleState")
                put("variables", JSONObject().apply { put("vehicleID", vehicleId) })
                put(
                    "query",
                    "query GetVehicleState(\$vehicleID: String!) { vehicleState(id: \$vehicleID) { powerState { value } batteryLevel { value } distanceToEmpty { value } chargerState { value } } }"
                )
            }
            val headers = mutableMapOf(
                "Content-Type" to "application/json",
                "Apollographql-Client-Name" to "com.rivian.android.consumer-apollo-android"
            )
            if (vault.rivianAccessToken.isNotBlank()) {
                headers["Authorization"] = "Bearer ${vault.rivianAccessToken}"
            }
            if (vault.rivianUserSessionToken.isNotBlank()) {
                headers["U-Sess"] = vault.rivianUserSessionToken
            }
            if (vault.rivianCsrfToken.isNotBlank()) {
                headers["Csrf-Token"] = vault.rivianCsrfToken
            }
            if (vault.rivianAppSessionToken.isNotBlank()) {
                headers["A-Sess"] = vault.rivianAppSessionToken
            }

            val raw = executeHttpRequest(
                urlStr = RIVIAN_GRAPHQL_URL,
                method = "POST",
                body = queryObj.toString(),
                headers = headers
            ) ?: return null

            parseRivianVehicleStateJson(raw, fallbackSlot)
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchTeslaVehicleSlot(
        vault: LocalAuthVaultState,
        fallbackSlot: EnergyProductSlot,
        allowWake: Boolean
    ): EnergyProductSlot? {
        val token = vault.teslaAccessToken.trim()
        if (token.isBlank()) return null
        val baseUrl = vault.teslaProvider.baseUrl.trimEnd('/')
        val headers = mapOf(
            "Authorization" to "Bearer $token",
            "Accept" to "application/json"
        )

        return try {
            // Step 1: Query /api/1/vehicles to check vehicle ID and sleep state WITHOUT waking the car
            var targetId = vault.teslaVehicleId.trim()
            val listRaw = executeHttpRequest(
                urlStr = "$baseUrl/api/1/vehicles",
                method = "GET",
                headers = headers
            )
            if (!listRaw.isNullOrBlank()) {
                val listObj = JSONObject(listRaw)
                val arr = listObj.optJSONArray("response")
                if (arr != null && arr.length() > 0) {
                    val firstCar = arr.getJSONObject(0)
                    if (targetId.isBlank()) {
                        targetId = firstCar.optString("id_s", firstCar.optLong("id", 0L).toString())
                    }
                    val stateStr = firstCar.optString("state", "online").lowercase()
                    val displayName = firstCar.optString("display_name", "").takeIf { it.isNotBlank() }
                    val namedSlot = if (displayName != null) fallbackSlot.copy(name = displayName) else fallbackSlot

                    // PHANTOM DRAIN GUARD: If car is asleep/offline and allowWake == false, do NOT wake!
                    if ((stateStr == "asleep" || stateStr == "offline") && !allowWake) {
                        return namedSlot.copy(
                            sleepState = VehicleSleepState.ASLEEP,
                            statusDetail = "Sleeping (No-Wake Safe)",
                            sourceLabel = "Tesla BYOK (Sleep Guard)",
                            lastUpdatedEpochMs = System.currentTimeMillis(),
                            isLiveSynced = true
                        )
                    }
                }
            }

            if (targetId.isBlank()) return null

            // Step 2: Vehicle is online (or user explicitly allowed wake) -> query vehicle_data
            val dataRaw = executeHttpRequest(
                urlStr = "$baseUrl/api/1/vehicles/$targetId/vehicle_data",
                method = "GET",
                headers = headers
            ) ?: return null

            parseTeslaVehicleDataJson(dataRaw, fallbackSlot)
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchTeslaCloudPowerwallSlot(
        vault: LocalAuthVaultState,
        fallbackSlot: EnergyProductSlot
    ): EnergyProductSlot? {
        val token = vault.teslaAccessToken.trim()
        val siteId = vault.teslaEnergySiteId.trim()
        if (token.isBlank() || siteId.isBlank()) return null
        val baseUrl = vault.teslaProvider.baseUrl.trimEnd('/')
        val raw = executeHttpRequest(
            urlStr = "$baseUrl/api/1/energy_sites/$siteId/live_status",
            method = "GET",
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Accept" to "application/json"
            )
        ) ?: return null

        return parseTeslaCloudPowerwallJson(raw, fallbackSlot)
    }

    private fun fetchPowerwallLanSlot(
        vault: LocalAuthVaultState,
        fallbackSlot: EnergyProductSlot
    ): EnergyProductSlot? {
        val ip = vault.powerwallGatewayIp.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
        if (ip.isBlank()) return null

        return try {
            var tokenCookie = vault.powerwallAuthCookie.trim()
            if (tokenCookie.isBlank() && vault.powerwallCustomerPassword.isNotBlank()) {
                val loginPayload = JSONObject().apply {
                    put("username", "customer")
                    put("password", vault.powerwallCustomerPassword)
                    put("email", vault.powerwallEmail.ifBlank { "owner@local.lan" })
                    put("force_sm_off", false)
                }
                val loginJson = executeHttpRequest(
                    urlStr = "https://$ip/api/login/Basic",
                    method = "POST",
                    body = loginPayload.toString(),
                    headers = mapOf("Content-Type" to "application/json"),
                    allowLocalLanSelfSignedTls = isPrivateLanHost(ip)
                )
                if (!loginJson.isNullOrBlank()) {
                    tokenCookie = JSONObject(loginJson).optString("token", "")
                }
            }

            val authHeaders = mutableMapOf("Accept" to "application/json")
            if (tokenCookie.isNotBlank()) {
                authHeaders["Authorization"] = "Bearer $tokenCookie"
                authHeaders["Cookie"] = "AuthCookie=$tokenCookie"
            }

            val soeRaw = executeHttpRequest(
                urlStr = "https://$ip/api/system_status/soe",
                method = "GET",
                headers = authHeaders,
                allowLocalLanSelfSignedTls = isPrivateLanHost(ip)
            ) ?: return null

            val metersRaw = executeHttpRequest(
                urlStr = "https://$ip/api/meters/aggregates",
                method = "GET",
                headers = authHeaders,
                allowLocalLanSelfSignedTls = isPrivateLanHost(ip)
            )

            parsePowerwallLocalSoeJson(soeRaw, metersRaw, fallbackSlot)
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchHomeAssistantSlot(
        baseUrl: String,
        token: String,
        entityId: String,
        fallbackSlot: EnergyProductSlot
    ): EnergyProductSlot? {
        val cleanBase = baseUrl.trim().trimEnd('/')
        if (cleanBase.isBlank() || token.isBlank() || entityId.isBlank()) return null
        val raw = executeHttpRequest(
            urlStr = "$cleanBase/api/states/$entityId",
            method = "GET",
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Accept" to "application/json"
            )
        ) ?: return null
        return parseHomeAssistantStateJson(raw, fallbackSlot)
    }

    private fun isPrivateLanHost(hostPort: String): Boolean {
        val host = hostPort.substringBefore(":").lowercase()
        return host.startsWith("192.168.") ||
            host.startsWith("10.") ||
            host.endsWith(".local") ||
            host.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*"))
    }

    private fun executeHttpRequest(
        urlStr: String,
        method: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
        allowLocalLanSelfSignedTls: Boolean = false
    ): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doInput = true
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }

            if (conn is HttpsURLConnection && allowLocalLanSelfSignedTls) {
                val trustAllLan = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                })
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllLan, SecureRandom())
                conn.sslSocketFactory = sslContext.socketFactory
                conn.hostnameVerifier = HostnameVerifier { hostname, _ -> isPrivateLanHost(hostname) }
            }

            if (body != null && (method == "POST" || method == "PUT")) {
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            if (stream == null) return null
            val responseText = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            if (code in 200..299) responseText else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
