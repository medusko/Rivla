package com.medusko.rivla.model

import kotlin.math.roundToInt

enum class ProductType(
    val title: String,
    val badge: String,
    val defaultIconHint: String
) {
    RIVIAN_VEHICLE("Rivian Vehicle", "RIVIAN", "Adventure EV (GraphQL)"),
    TESLA_VEHICLE("Tesla Vehicle", "TESLA EV", "Fleet API / BYOK Proxy"),
    TESLA_POWERWALL("Tesla Powerwall", "POWERWALL", "Home Energy (LAN or Cloud)"),
    HOME_ASSISTANT("Home Assistant Entity", "HA SENSOR", "Unified REST State Sensor")
}

enum class TeslaProviderType(
    val title: String,
    val badge: String,
    val baseUrl: String,
    val description: String
) {
    FLEET_NA(
        title = "Tesla Fleet API (North America)",
        badge = "\$10/mo Free Credit",
        baseUrl = "https://fleet-api.prd.na.vn.cloud.tesla.com",
        description = "Official Tesla Fleet API using your own developer Client ID & Bearer Token."
    ),
    FLEET_EU(
        title = "Tesla Fleet API (Europe / ME / Africa)",
        badge = "\$10/mo Free Credit",
        baseUrl = "https://fleet-api.prd.eu.vn.cloud.tesla.com",
        description = "Official EU regional Fleet endpoint using your personal Tesla developer credentials."
    ),
    TESSIE(
        title = "Tessie Personal API (BYOK Proxy)",
        badge = "Zero Domain Setup",
        baseUrl = "https://api.tessie.com",
        description = "Paste your Tessie API token to query Tesla vehicles & Powerwalls without hosting a .well-known public key."
    ),
    TESLEMETRY(
        title = "Teslemetry Personal API (BYOK Proxy)",
        badge = "Zero Domain Setup",
        baseUrl = "https://api.teslemetry.com",
        description = "Paste your Teslemetry API token for direct Fleet telemetry compatibility."
    )
}

enum class VehicleSleepState(
    val label: String,
    val badgeText: String
) {
    CHARGING("Charging ⚡", "CHARGING"),
    AWAKE("Awake / Online", "ONLINE"),
    ASLEEP("Sleeping (Cached)", "ASLEEP"),
    STANDBY("Backup Standby", "STANDBY")
}

enum class DistanceUnit(
    val label: String,
    val shortUnit: String
) {
    MILES("Miles (mi)", "mi"),
    KILOMETERS("Kilometers (km)", "km")
}

enum class PollingInterval(
    val title: String,
    val badge: String,
    val minutes: Int,
    val description: String
) {
    MANUAL(
        title = "Manual Refresh Only",
        badge = "0% Drain",
        minutes = 0,
        description = "Only fetches telemetry when you tap Sync in the app or on the 4x2 widget."
    ),
    MINUTES_30(
        title = "Every 30 Minutes",
        badge = "Balanced",
        minutes = 30,
        description = "Updates widget every 30 min. Uses non-waking vehicle state check when Sleep Protection is enabled."
    ),
    MINUTES_60(
        title = "Every 60 Minutes (Recommended)",
        badge = "EV Safe",
        minutes = 60,
        description = "Conservative hourly widget refresh designed to preserve EV deep sleep and Tesla free API tier."
    ),
    MINUTES_120(
        title = "Every 2 Hours",
        badge = "Ultra Low",
        minutes = 120,
        description = "Minimal background polling for multi-day parking and maximum API credit conservation."
    )
}

data class EnergyProductSlot(
    val slotIndex: Int, // 0, 1, 2, 3 (max 4 in 2x2 grid)
    val id: String,
    val name: String,
    val productType: ProductType,
    val batteryPercent: Int,
    val estimatedRangeMiles: Int? = null,
    val powerFlowKw: Double? = null,
    val sleepState: VehicleSleepState = VehicleSleepState.AWAKE,
    val statusDetail: String = "",
    val sourceLabel: String = "Local Vault",
    val externalId: String = "",
    val lastUpdatedEpochMs: Long = System.currentTimeMillis(),
    val isLiveSynced: Boolean = false
) {
    fun formattedSecondaryLine(unit: DistanceUnit): String {
        val parts = mutableListOf<String>()
        if (estimatedRangeMiles != null && estimatedRangeMiles > 0) {
            val dist = if (unit == DistanceUnit.KILOMETERS) {
                (estimatedRangeMiles * 1.60934).roundToInt()
            } else {
                estimatedRangeMiles
            }
            parts.add("$dist ${unit.shortUnit}")
        }
        if (powerFlowKw != null && powerFlowKw != 0.0) {
            val sign = if (powerFlowKw > 0) "+" else ""
            parts.add(String.format("%s%.1f kW", sign, powerFlowKw))
        }
        if (statusDetail.isNotBlank()) {
            parts.add(statusDetail)
        } else {
            parts.add(sleepState.label)
        }
        return parts.joinToString(" • ")
    }
}

data class LocalAuthVaultState(
    // 1. Rivian Direct GraphQL (100% Client-Side)
    val rivianEmail: String = "",
    val rivianPassword: String = "",
    val rivianAccessToken: String = "",
    val rivianRefreshToken: String = "",
    val rivianUserSessionToken: String = "",
    val rivianCsrfToken: String = "",
    val rivianAppSessionToken: String = "",
    val rivianPendingOtpToken: String = "",
    val rivianVehicleId: String = "",
    val rivianVehicleName: String = "Rivian R1S",

    // 2. Tesla Cloud BYOK (Fleet API / Tessie / Teslemetry)
    val teslaProvider: TeslaProviderType = TeslaProviderType.FLEET_NA,
    val teslaClientId: String = "",
    val teslaAccessToken: String = "",
    val teslaRefreshToken: String = "",
    val teslaVehicleId: String = "",
    val teslaVehicleName: String = "Tesla Model Y",
    val teslaEnergySiteId: String = "",

    // 3. Tesla Powerwall Local LAN Gateway
    val powerwallGatewayIp: String = "",
    val powerwallEmail: String = "",
    val powerwallCustomerPassword: String = "",
    val powerwallAuthCookie: String = "",
    val powerwallSiteName: String = "Powerwall+ Gateway",

    // 4. Home Assistant Unified Long-Lived Token (Optional)
    val haBaseUrl: String = "",
    val haAccessToken: String = "",
    val haSlot1Entity: String = "sensor.rivian_battery_level",
    val haSlot2Entity: String = "sensor.tesla_battery_level",
    val haSlot3Entity: String = "sensor.powerwall_charge",
    val haSlot4Entity: String = "sensor.powerwall_expansion_charge"
) {
    val isRivianConfigured: Boolean
        get() = rivianAccessToken.isNotBlank() || rivianUserSessionToken.isNotBlank() ||
            (rivianEmail.isNotBlank() && rivianPassword.isNotBlank())

    val isTeslaCloudConfigured: Boolean
        get() = teslaAccessToken.isNotBlank()

    val isPowerwallLanConfigured: Boolean
        get() = powerwallGatewayIp.isNotBlank() &&
            (powerwallCustomerPassword.isNotBlank() || powerwallAuthCookie.isNotBlank())

    val isHomeAssistantConfigured: Boolean
        get() = haBaseUrl.isNotBlank() && haAccessToken.isNotBlank()

    val configuredProviderCount: Int
        get() = listOf(
            isRivianConfigured,
            isTeslaCloudConfigured,
            isPowerwallLanConfigured,
            isHomeAssistantConfigured
        ).count { it }
}

data class CaveatGuideItem(
    val id: String,
    val badge: String,
    val title: String,
    val summary: String,
    val bulletPoints: List<String>
)

object ByokCaveatCatalog {
    val items: List<CaveatGuideItem> = listOf(
        CaveatGuideItem(
            id = "phantom_drain",
            badge = "CRITICAL EV RULE",
            title = "Vehicle Sleep State & Avoiding Phantom Drain",
            summary = "Repeatedly polling a parked EV with wake commands keeps its high-voltage computers active and drains 1%–3% of battery per day.",
            bulletPoints = listOf(
                "Sleep-Check First: Rivla queries Tesla's lightweight /api/1/vehicles state list before requesting telemetry. If the car reports 'asleep' or 'offline' and 'Allow Waking Sleeping Vehicles' is OFF, Rivla displays the cached battery percentage without waking the vehicle.",
                "Wake Billing Weight: On the Tesla Fleet API, waking a sleeping vehicle costs ~20x more API credit (~37–50 wakes per \$1) than reading cached state.",
                "Conservative Widget Intervals: Keep widget polling at 30–60 minutes or Manual Sync and use the widget's ↻ Sync button when you need an immediate reading."
            )
        ),
        CaveatGuideItem(
            id = "byok_model",
            badge = "ZERO SERVER COST",
            title = "Why BYOK (Bring Your Own Key) & Local Device Storage?",
            summary = "Manufacturers do not provide a single free, public multi-brand API key. Rivla stores your session tokens strictly on your phone inside Android Keystore EncryptedSharedPreferences.",
            bulletPoints = listOf(
                "No Middleman Secrets: Your Rivian credentials, Tesla tokens, and Powerwall LAN passwords never leave your phone or pass through a third-party developer server.",
                "No Shared Developer Billing Cap: In 2024, Tesla replaced the free Owner API with the usage-billed Fleet API. If an app routes thousands of users through one shared client_id, the developer is billed once aggregate traffic exceeds \$10/month.",
                "Personal Free Credit: With BYOK, your personal Tesla Developer account receives its own recurring \$10/month free credit (with a \$0 default billing cap so you cannot be charged)."
            )
        ),
        CaveatGuideItem(
            id = "tesla_fleet_gotcha",
            badge = "TESLA FLEET API",
            title = "Tesla Fleet API vs. Tessie / Teslemetry Proxy Keys",
            summary = "Direct username/password login to Tesla was shut down in 2024. You can connect Tesla Vehicles & Cloud Powerwalls in two ways:",
            bulletPoints = listOf(
                "Option A — Personal Tesla Developer App (Free \$10/mo Credit): Register at developer.tesla.com, obtain a client_id and Bearer token, and request scopes vehicle_device_data, energy_device_data, and offline_access. Note: Tesla requires hosting a public key at /.well-known/appspecific/com.tesla.3p.public-key.pem for command signing.",
                "Option B — Tessie / Teslemetry API Token (Easiest BYOK): If you already use Tessie or Teslemetry, paste your personal API key in Setup -> Tesla. It uses the exact same /api/1/vehicles and /api/1/energy_sites endpoints with zero domain setup."
            )
        ),
        CaveatGuideItem(
            id = "rivian_graphql",
            badge = "RIVIAN GRAPHQL",
            title = "Rivian Unofficial GraphQL API & MFA Sessions",
            summary = "Rivian does not offer an official public developer portal, so community integrations authenticate directly with the mobile app's GraphQL gateway.",
            bulletPoints = listOf(
                "100% Client-Side Handshake: Rivla calls CreateCSRFToken -> Login -> LoginWithOTP directly against rivian.com/api/gql/gateway/graphql and caches the resulting userSessionToken and accessToken locally.",
                "GetVehicleState Telemetry: Queries batteryLevel, distanceToEmpty, powerState, and chargerState directly from your phone.",
                "Unofficial Endpoint Caveat: Because Rivian's GraphQL schema is undocumented, avoid aggressive polling (<15 min) so your account is never rate-limited."
            )
        ),
        CaveatGuideItem(
            id = "powerwall_lan",
            badge = "LOCAL LAN GATEWAY",
            title = "Tesla Powerwall Local Gateway (192.168.x.x)",
            summary = "On firmware 20.49.0+, Tesla Energy Gateways expose a fast, zero-cost local HTTPS API on your home Wi-Fi network.",
            bulletPoints = listOf(
                "Customer Sticker Password: Log in via POST https://<gateway-ip>/api/login/Basic using username 'customer', your Tesla email, and the password printed on the physical Powerwall Gateway sticker.",
                "Self-Signed SSL Certificate: The Powerwall Gateway serves a local self-signed TLS certificate. Rivla scopes custom SSL trust strictly to your configured private LAN IP.",
                "Real-Time SoE & Solar Meters: Reads /api/system_status/soe for exact battery percentage and /api/meters/aggregates for live kW solar/home flow."
            )
        )
    )
}
