package com.medusko.rivla

import com.medusko.rivla.data.AppPreferences
import com.medusko.rivla.data.RivlaTelemetryRepository
import com.medusko.rivla.model.*
import org.junit.Assert.*
import org.junit.Test

class RivlaTelemetryTest {

    private val defaultSlots = AppPreferences.defaultProductSlots()

    @Test
    fun `default product slots contain 4 items for 2x2 grid`() {
        assertEquals(4, defaultSlots.size)
        assertEquals(ProductType.RIVIAN_VEHICLE, defaultSlots[0].productType)
        assertEquals(ProductType.TESLA_VEHICLE, defaultSlots[1].productType)
        assertEquals(ProductType.TESLA_POWERWALL, defaultSlots[2].productType)
        assertEquals(ProductType.TESLA_POWERWALL, defaultSlots[3].productType)
    }

    @Test
    fun `parseRivianVehicleStateJson extracts battery percentage and converts km to miles`() {
        val rawJson = """
            {
              "data": {
                "vehicleState": {
                  "powerState": { "value": "sleep" },
                  "batteryLevel": { "value": 84.6 },
                  "distanceToEmpty": { "value": 420.0 },
                  "chargerState": { "value": "chrgr_sts_not_connected" }
                }
              }
            }
        """.trimIndent()

        val parsed = RivlaTelemetryRepository.parseRivianVehicleStateJson(rawJson, defaultSlots[0])
        assertNotNull(parsed)
        assertEquals(85, parsed!!.batteryPercent)
        assertEquals(261, parsed.estimatedRangeMiles)
        assertEquals(VehicleSleepState.ASLEEP, parsed.sleepState)
        assertTrue(parsed.isLiveSynced)
    }

    @Test
    fun `parseTeslaVehicleDataJson extracts charging state and battery level`() {
        val rawJson = """
            {
              "response": {
                "state": "online",
                "charge_state": {
                  "battery_level": 91,
                  "battery_range": 292.4,
                  "charging_state": "Charging",
                  "charger_power": 11.5
                }
              }
            }
        """.trimIndent()

        val parsed = RivlaTelemetryRepository.parseTeslaVehicleDataJson(rawJson, defaultSlots[1])
        assertNotNull(parsed)
        assertEquals(91, parsed!!.batteryPercent)
        assertEquals(292, parsed.estimatedRangeMiles)
        assertEquals(VehicleSleepState.CHARGING, parsed.sleepState)
        assertEquals(11.5, parsed.powerFlowKw ?: 0.0, 0.01)
    }

    @Test
    fun `parsePowerwallLocalSoeJson extracts local gateway percentage and solar power`() {
        val soeJson = """{"percentage": 96.4}"""
        val metersJson = """{"solar": {"instant_power": 4250.0}}"""

        val parsed = RivlaTelemetryRepository.parsePowerwallLocalSoeJson(
            soeRawJson = soeJson,
            metersRawJson = metersJson,
            fallbackSlot = defaultSlots[2]
        )
        assertNotNull(parsed)
        assertEquals(96, parsed!!.batteryPercent)
        assertEquals(4.3, parsed.powerFlowKw ?: 0.0, 0.01)
        assertEquals(VehicleSleepState.CHARGING, parsed.sleepState)
    }

    @Test
    fun `parseHomeAssistantStateJson extracts sensor percentage and friendly name`() {
        val haJson = """
            {
              "entity_id": "sensor.rivian_battery_level",
              "state": "73.2",
              "attributes": {
                "friendly_name": "R1T Adventure Battery",
                "unit_of_measurement": "%"
              }
            }
        """.trimIndent()

        val parsed = RivlaTelemetryRepository.parseHomeAssistantStateJson(haJson, defaultSlots[0])
        assertNotNull(parsed)
        assertEquals("R1T Adventure Battery", parsed!!.name)
        assertEquals(73, parsed.batteryPercent)
    }

    @Test
    fun `formattedSecondaryLine converts miles to kilometers when metric selected`() {
        val slot = defaultSlots[0].copy(
            estimatedRangeMiles = 200,
            statusDetail = "Sleeping"
        )
        assertTrue(slot.formattedSecondaryLine(DistanceUnit.MILES).contains("200 mi"))
        assertTrue(slot.formattedSecondaryLine(DistanceUnit.KILOMETERS).contains("322 km"))
    }
}
