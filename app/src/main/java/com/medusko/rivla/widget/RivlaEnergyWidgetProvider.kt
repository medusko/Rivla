package com.medusko.rivla.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.medusko.rivla.MainActivity
import com.medusko.rivla.R
import com.medusko.rivla.data.AppPreferences
import com.medusko.rivla.data.RivlaTelemetryRepository
import com.medusko.rivla.model.DistanceUnit
import com.medusko.rivla.model.EnergyProductSlot
import com.medusko.rivla.model.VehicleSleepState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RivlaEnergyWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.medusko.rivla.action.REFRESH_WIDGET"

        fun notifyWidgetsChanged(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, RivlaEnergyWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val prefs = AppPreferences(appContext)
                val slots = prefs.loadProductSlots()
                val unit = prefs.getDistanceUnit()
                for (id in ids) {
                    updateSingleWidget(appContext, manager, id, slots, unit)
                }
            }
        }

        internal fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            slots: List<EnergyProductSlot>,
            unit: DistanceUnit
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_rivla_energy_4x2)

            // Tap root or cards to launch MainActivity
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val launchPendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent)
            views.setOnClickPendingIntent(R.id.slot1_card, launchPendingIntent)
            views.setOnClickPendingIntent(R.id.slot2_card, launchPendingIntent)
            views.setOnClickPendingIntent(R.id.slot3_card, launchPendingIntent)
            views.setOnClickPendingIntent(R.id.slot4_card, launchPendingIntent)

            // Tap "↻ Sync" to refresh widget telemetry
            val refreshIntent = Intent(context, RivlaEnergyWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPendingIntent)

            val latestEpoch = slots.maxOfOrNull { it.lastUpdatedEpochMs } ?: System.currentTimeMillis()
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(latestEpoch))
            views.setTextViewText(R.id.widget_refresh_btn, "↻ $timeStr")

            val nameIds = intArrayOf(R.id.slot1_name, R.id.slot2_name, R.id.slot3_name, R.id.slot4_name)
            val pctIds = intArrayOf(R.id.slot1_pct, R.id.slot2_pct, R.id.slot3_pct, R.id.slot4_pct)
            val subIds = intArrayOf(R.id.slot1_sub, R.id.slot2_sub, R.id.slot3_sub, R.id.slot4_sub)
            val barIds = intArrayOf(R.id.slot1_bar, R.id.slot2_bar, R.id.slot3_bar, R.id.slot4_bar)

            val padded = if (slots.size >= 4) slots.take(4) else AppPreferences.defaultProductSlots()

            for (i in 0 until 4) {
                val slot = padded[i]
                val pct = slot.batteryPercent.coerceIn(0, 100)
                views.setTextViewText(nameIds[i], slot.name)
                views.setTextViewText(pctIds[i], "$pct%")
                views.setTextViewText(subIds[i], slot.formattedSecondaryLine(unit))
                views.setProgressBar(barIds[i], 100, pct, false)

                val pctColor = when {
                    slot.sleepState == VehicleSleepState.CHARGING -> Color.parseColor("#38BDF8") // AccentBlue
                    pct >= 70 -> Color.parseColor("#00C853") // TailwindGreen
                    pct >= 30 -> Color.parseColor("#E89D24") // MeduskoGold
                    else -> Color.parseColor("#FF3D57")      // HeadwindRed
                }
                views.setTextColor(pctIds[i], pctColor)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = AppPreferences(context)
        val slots = prefs.loadProductSlots()
        val unit = prefs.getDistanceUnit()
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId, slots, unit)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val prefs = AppPreferences(appContext)
                    val repo = RivlaTelemetryRepository()
                    val report = repo.syncAllProducts(
                        vault = prefs.loadVaultState(),
                        currentSlots = prefs.loadProductSlots(),
                        allowWakeSleepingVehicles = prefs.allowWakeSleepingVehicles()
                    )
                    prefs.saveProductSlots(report.updatedSlots)
                    notifyWidgetsChanged(appContext)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
