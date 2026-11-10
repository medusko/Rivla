package com.medusko.rivla.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medusko.rivla.BuildConfig
import com.medusko.rivla.R
import com.medusko.rivla.data.AppPreferences
import com.medusko.rivla.model.*
import com.medusko.rivla.ui.components.*
import com.medusko.rivla.ui.theme.*
import com.medusko.rivla.widget.RivlaEnergyWidgetProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MainScreen(viewModel: RivlaViewModel) {
    val context = LocalContext.current

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val vaultState by viewModel.vaultState.collectAsState()
    val productSlots by viewModel.productSlots.collectAsState()
    val allowWakeSleepingVehicles by viewModel.allowWakeSleepingVehicles.collectAsState()
    val wifiOnlyLocalGateway by viewModel.wifiOnlyLocalGateway.collectAsState()
    val pollingInterval by viewModel.pollingInterval.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsState()
    val rivianOtpPending by viewModel.rivianOtpPending.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var slotBeingEdited by remember { mutableStateOf<EnergyProductSlot?>(null) }

    Scaffold(
        topBar = {
            // Velocast-style Clean Top Bar: App Icon + "Rivla" Title + Refresh, About Info & Dark/Light Mode Icons
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.clickable { showAboutDialog = true }
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_gold),
                                contentDescription = "Rivla Icon",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Rivla",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 19.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(5.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "BYOK",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.syncAllTelemetry() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync Energy Telemetry",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = { showAboutDialog = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "About Rivla",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleDarkMode() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Widgets, contentDescription = "Status") },
                    label = { Text("Status", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Key, contentDescription = "Setup") },
                    label = { Text("Setup", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> StatusTab(
                    slots = productSlots,
                    vaultState = vaultState,
                    distanceUnit = distanceUnit,
                    allowWakeSleepingVehicles = allowWakeSleepingVehicles,
                    isSyncing = isSyncing,
                    statusBannerMessage = statusBannerMessage,
                    onSyncNow = { viewModel.syncAllTelemetry() },
                    onNavigateToSetup = { selectedTab = 1 },
                    onOpenAbout = { showAboutDialog = true },
                    onEditSlot = { slotBeingEdited = it },
                    onPinWidget = {
                        requestPinRivlaWidget(context)
                    }
                )

                1 -> SetupTab(
                    vaultState = vaultState,
                    slots = productSlots,
                    isSyncing = isSyncing,
                    rivianOtpPending = rivianOtpPending,
                    statusBannerMessage = statusBannerMessage,
                    onOpenAbout = { showAboutDialog = true },
                    onSaveVault = { updated, msg ->
                        viewModel.saveVaultAndSync(updated, msg)
                    },
                    onAuthenticateRivian = { email, password, name, vehicleId ->
                        viewModel.authenticateRivianDirect(email, password, name, vehicleId)
                    },
                    onVerifyRivianOtp = { code ->
                        viewModel.verifyRivianOtpCode(code)
                    },
                    onEditSlot = { slotBeingEdited = it }
                )

                2 -> SettingsTab(
                    isDarkMode = isDarkMode,
                    allowWakeSleepingVehicles = allowWakeSleepingVehicles,
                    wifiOnlyLocalGateway = wifiOnlyLocalGateway,
                    pollingInterval = pollingInterval,
                    distanceUnit = distanceUnit,
                    configuredProviders = vaultState.configuredProviderCount,
                    onOpenAbout = { showAboutDialog = true },
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onAllowWakeChanged = { viewModel.setAllowWakeSleepingVehicles(it) },
                    onWifiOnlyChanged = { viewModel.setWifiOnlyLocalGateway(it) },
                    onPollingIntervalSelected = { viewModel.setPollingInterval(it) },
                    onDistanceUnitSelected = { viewModel.setDistanceUnit(it) },
                    onResetDemoSlots = { viewModel.resetToDefaultDemoSlots() },
                    onClearAllSecrets = { viewModel.clearAllLocalSecrets() }
                )
            }
        }
    }

    if (showAboutDialog) {
        AboutRivlaDialog(
            configuredProviders = vaultState.configuredProviderCount,
            sleepGuardEnabled = !allowWakeSleepingVehicles,
            pollingInterval = pollingInterval,
            distanceUnit = distanceUnit,
            onDismiss = { showAboutDialog = false }
        )
    }

    slotBeingEdited?.let { slot ->
        EditProductSlotDialog(
            slot = slot,
            onDismiss = { slotBeingEdited = null },
            onSave = { name, type, pct, rangeMi, kw, sleepState, detail ->
                viewModel.updateProductSlot(
                    slotIndex = slot.slotIndex,
                    name = name,
                    productType = type,
                    batteryPercent = pct,
                    rangeMiles = rangeMi,
                    powerKw = kw,
                    sleepState = sleepState,
                    statusDetail = detail
                )
                slotBeingEdited = null
            }
        )
    }
}

private fun requestPinRivlaWidget(context: android.content.Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val provider = ComponentName(context, RivlaEnergyWidgetProvider::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
        appWidgetManager.requestPinAppWidget(provider, null, null)
    } else {
        Toast.makeText(
            context,
            "Long-press your Home or Lock Screen -> Widgets -> Rivla 4x2 Energy Grid",
            Toast.LENGTH_LONG
        ).show()
    }
}

// ============================================================================
// TOP ABOUT & BYOK IDENTITY BANNER (Consistent Medusko Header Card)
// ============================================================================
@Composable
private fun TopAboutSummaryStrip(
    configuredCount: Int,
    sleepGuardEnabled: Boolean,
    onOpenAbout: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenAbout() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_gold),
                    contentDescription = "Rivla Emblem",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Rivla by Medusko",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "BYOK Rivian • Tesla • Powerwall 4x2 Widget Hub ($configuredCount/4 Vaults • ${if (sleepGuardEnabled) "Sleep Guard ON" else "Wake ON"})",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "About",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ============================================================================
// TAB 1: STATUS (4x2 2x2 WIDGET GRID PREVIEW + PRODUCT CARDS + QUICK LOGIN CTA)
// ============================================================================
@Composable
private fun StatusTab(
    slots: List<EnergyProductSlot>,
    vaultState: LocalAuthVaultState,
    distanceUnit: DistanceUnit,
    allowWakeSleepingVehicles: Boolean,
    isSyncing: Boolean,
    statusBannerMessage: String,
    onSyncNow: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onOpenAbout: () -> Unit,
    onEditSlot: (EnergyProductSlot) -> Unit,
    onPinWidget: () -> Unit
) {
    val latestSyncEpoch = slots.maxOfOrNull { it.lastUpdatedEpochMs } ?: System.currentTimeMillis()
    val syncTimeFormatted = remember(latestSyncEpoch) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(latestSyncEpoch))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            TopAboutSummaryStrip(
                configuredCount = vaultState.configuredProviderCount,
                sleepGuardEnabled = !allowWakeSleepingVehicles,
                onOpenAbout = onOpenAbout
            )
        }

        // LIVE 4x2 HOME & LOCK SCREEN WIDGET PREVIEW (2x2 GRID, MAX 4 PRODUCTS)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = DarkBackground,
                border = BorderStroke(1.5.dp, MeduskoGold.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Widget Header Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_gold),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = "4x2 LOCK & HOME WIDGET (2x2 GRID)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                color = MeduskoGold,
                                letterSpacing = 0.6.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable { onSyncNow() }
                        ) {
                            StatusColorDot(
                                color = if (isSyncing) AccentBlue else TailwindGreen,
                                size = 7.dp
                            )
                            Text(
                                text = if (isSyncing) "Syncing..." else "↻ $syncTimeFormatted",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue
                            )
                        }
                    }

                    val paddedSlots = if (slots.size >= 4) slots.take(4) else AppPreferences.defaultProductSlots()

                    // Row 1 of 2x2 Grid (Slots 0 & 1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WidgetGridCellPreview(
                            slot = paddedSlots[0],
                            distanceUnit = distanceUnit,
                            modifier = Modifier.weight(1f),
                            onClick = { onEditSlot(paddedSlots[0]) }
                        )
                        WidgetGridCellPreview(
                            slot = paddedSlots[1],
                            distanceUnit = distanceUnit,
                            modifier = Modifier.weight(1f),
                            onClick = { onEditSlot(paddedSlots[1]) }
                        )
                    }

                    // Row 2 of 2x2 Grid (Slots 2 & 3)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WidgetGridCellPreview(
                            slot = paddedSlots[2],
                            distanceUnit = distanceUnit,
                            modifier = Modifier.weight(1f),
                            onClick = { onEditSlot(paddedSlots[2]) }
                        )
                        WidgetGridCellPreview(
                            slot = paddedSlots[3],
                            distanceUnit = distanceUnit,
                            modifier = Modifier.weight(1f),
                            onClick = { onEditSlot(paddedSlots[3]) }
                        )
                    }

                    // Action Buttons under 4x2 Widget Preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPinWidget,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextPrimary)
                        ) {
                            Icon(Icons.Default.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(16.dp), tint = MeduskoGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add 4x2 Widget", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onSyncNow,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MeduskoGold,
                                contentColor = DarkBackground
                            )
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync & Update", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // BYOK VAULT STATUS & SETUP CALLOUT CARD
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EnhancedEncryption,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "ON-DEVICE BYOK AUTHENTICATION VAULT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.7.sp
                            )
                        }

                        Text(
                            text = "${vaultState.configuredProviderCount}/4 Configured",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (vaultState.configuredProviderCount > 0) TailwindGreen else CrosswindAmber
                        )
                    }

                    Text(
                        text = statusBannerMessage,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VaultProviderPill("Rivian", vaultState.isRivianConfigured, Modifier.weight(1f))
                        VaultProviderPill("Tesla BYOK", vaultState.isTeslaCloudConfigured, Modifier.weight(1f))
                        VaultProviderPill("PW LAN", vaultState.isPowerwallLanConfigured, Modifier.weight(1f))
                        VaultProviderPill("Home Asst", vaultState.isHomeAssistantConfigured, Modifier.weight(1f))
                    }

                    Button(
                        onClick = onNavigateToSetup,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Configure Logins, Sessions & BYOK Keys in Setup →", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // INDIVIDUAL TELEMETRY CARDS FOR ALL 4 PRODUCTS
        item {
            Text(
                text = "TELEMETRY SLOTS (TAP ANY PRODUCT TO CUSTOMIZE)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        items(slots) { slot ->
            DetailedTelemetryCard(
                slot = slot,
                distanceUnit = distanceUnit,
                onEdit = { onEditSlot(slot) }
            )
        }
    }
}

@Composable
private fun WidgetGridCellPreview(
    slot: EnergyProductSlot,
    distanceUnit: DistanceUnit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val pct = slot.batteryPercent.coerceIn(0, 100)
    val accentColor = when {
        slot.sleepState == VehicleSleepState.CHARGING -> AccentBlue
        pct >= 70 -> TailwindGreen
        pct >= 30 -> MeduskoGold
        else -> HeadwindRed
    }

    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (slot.productType == ProductType.TESLA_POWERWALL) {
                        PowerwallVectorIcon(color = accentColor, size = 15.dp)
                    } else {
                        EvVehicleVectorIcon(color = accentColor, size = 16.dp)
                    }
                    Text(
                        text = slot.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "$pct%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor
                )
            }

            Text(
                text = slot.formattedSecondaryLine(distanceUnit),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = DarkTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            LinearProgressIndicator(
                progress = { pct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accentColor,
                trackColor = Color(0xFF253047)
            )
        }
    }
}

@Composable
private fun VaultProviderPill(
    label: String,
    configured: Boolean,
    modifier: Modifier = Modifier
) {
    val dotColor = if (configured) TailwindGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(
            width = 1.dp,
            color = if (configured) TailwindGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            StatusColorDot(color = dotColor, size = 6.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DetailedTelemetryCard(
    slot: EnergyProductSlot,
    distanceUnit: DistanceUnit,
    onEdit: () -> Unit
) {
    val pct = slot.batteryPercent.coerceIn(0, 100)
    val accentColor = when {
        slot.sleepState == VehicleSleepState.CHARGING -> AccentBlue
        pct >= 70 -> TailwindGreen
        pct >= 30 -> MeduskoGold
        else -> HeadwindRed
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                    ) {
                        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                            if (slot.productType == ProductType.TESLA_POWERWALL) {
                                PowerwallVectorIcon(color = accentColor, size = 22.dp)
                            } else {
                                EvVehicleVectorIcon(color = accentColor, size = 22.dp)
                            }
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = slot.name,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "SLOT ${slot.slotIndex + 1} • ${slot.productType.badge}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${slot.formattedSecondaryLine(distanceUnit)} • ${slot.sourceLabel}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$pct%",
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 21.sp,
                        color = accentColor
                    )
                    Text(
                        text = slot.sleepState.badgeText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            LinearProgressIndicator(
                progress = { pct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

// ============================================================================
// TAB 2: SETUP (COLLECTS LOGINS, SESSIONS & BYOK KEYS AND SAVES LOCALLY)
// ============================================================================
@Composable
private fun SetupTab(
    vaultState: LocalAuthVaultState,
    slots: List<EnergyProductSlot>,
    isSyncing: Boolean,
    rivianOtpPending: Boolean,
    statusBannerMessage: String,
    onOpenAbout: () -> Unit,
    onSaveVault: (LocalAuthVaultState, String) -> Unit,
    onAuthenticateRivian: (String, String, String, String) -> Unit,
    onVerifyRivianOtp: (String) -> Unit,
    onEditSlot: (EnergyProductSlot) -> Unit
) {
    // Local editable fields initialized from vaultState
    var rivianEmail by remember(vaultState.rivianEmail) { mutableStateOf(vaultState.rivianEmail) }
    var rivianPassword by remember(vaultState.rivianPassword) { mutableStateOf(vaultState.rivianPassword) }
    var rivianVehicleName by remember(vaultState.rivianVehicleName) { mutableStateOf(vaultState.rivianVehicleName) }
    var rivianVehicleId by remember(vaultState.rivianVehicleId) { mutableStateOf(vaultState.rivianVehicleId) }
    var rivianAccessToken by remember(vaultState.rivianAccessToken) { mutableStateOf(vaultState.rivianAccessToken) }
    var rivianUserSessionToken by remember(vaultState.rivianUserSessionToken) { mutableStateOf(vaultState.rivianUserSessionToken) }
    var rivianRefreshToken by remember(vaultState.rivianRefreshToken) { mutableStateOf(vaultState.rivianRefreshToken) }
    var rivianOtpCode by remember { mutableStateOf("") }
    var showRivianTokenAdvanced by remember { mutableStateOf(false) }

    var teslaProvider by remember(vaultState.teslaProvider) { mutableStateOf(vaultState.teslaProvider) }
    var teslaClientId by remember(vaultState.teslaClientId) { mutableStateOf(vaultState.teslaClientId) }
    var teslaAccessToken by remember(vaultState.teslaAccessToken) { mutableStateOf(vaultState.teslaAccessToken) }
    var teslaRefreshToken by remember(vaultState.teslaRefreshToken) { mutableStateOf(vaultState.teslaRefreshToken) }
    var teslaVehicleName by remember(vaultState.teslaVehicleName) { mutableStateOf(vaultState.teslaVehicleName) }
    var teslaVehicleId by remember(vaultState.teslaVehicleId) { mutableStateOf(vaultState.teslaVehicleId) }
    var teslaEnergySiteId by remember(vaultState.teslaEnergySiteId) { mutableStateOf(vaultState.teslaEnergySiteId) }

    var pwGatewayIp by remember(vaultState.powerwallGatewayIp) { mutableStateOf(vaultState.powerwallGatewayIp) }
    var pwEmail by remember(vaultState.powerwallEmail) { mutableStateOf(vaultState.powerwallEmail) }
    var pwCustomerPassword by remember(vaultState.powerwallCustomerPassword) { mutableStateOf(vaultState.powerwallCustomerPassword) }
    var pwSiteName by remember(vaultState.powerwallSiteName) { mutableStateOf(vaultState.powerwallSiteName) }

    var haBaseUrl by remember(vaultState.haBaseUrl) { mutableStateOf(vaultState.haBaseUrl) }
    var haAccessToken by remember(vaultState.haAccessToken) { mutableStateOf(vaultState.haAccessToken) }
    var haSlot1Entity by remember(vaultState.haSlot1Entity) { mutableStateOf(vaultState.haSlot1Entity) }
    var haSlot2Entity by remember(vaultState.haSlot2Entity) { mutableStateOf(vaultState.haSlot2Entity) }
    var haSlot3Entity by remember(vaultState.haSlot3Entity) { mutableStateOf(vaultState.haSlot3Entity) }
    var haSlot4Entity by remember(vaultState.haSlot4Entity) { mutableStateOf(vaultState.haSlot4Entity) }

    var showSecretsPlain by remember { mutableStateOf(false) }

    val buildCurrentVault: () -> LocalAuthVaultState = {
        vaultState.copy(
            rivianEmail = rivianEmail,
            rivianPassword = rivianPassword,
            rivianVehicleName = rivianVehicleName,
            rivianVehicleId = rivianVehicleId,
            rivianAccessToken = rivianAccessToken,
            rivianUserSessionToken = rivianUserSessionToken,
            rivianRefreshToken = rivianRefreshToken,
            teslaProvider = teslaProvider,
            teslaClientId = teslaClientId,
            teslaAccessToken = teslaAccessToken,
            teslaRefreshToken = teslaRefreshToken,
            teslaVehicleName = teslaVehicleName,
            teslaVehicleId = teslaVehicleId,
            teslaEnergySiteId = teslaEnergySiteId,
            powerwallGatewayIp = pwGatewayIp,
            powerwallEmail = pwEmail,
            powerwallCustomerPassword = pwCustomerPassword,
            powerwallSiteName = pwSiteName,
            haBaseUrl = haBaseUrl,
            haAccessToken = haAccessToken,
            haSlot1Entity = haSlot1Entity,
            haSlot2Entity = haSlot2Entity,
            haSlot3Entity = haSlot3Entity,
            haSlot4Entity = haSlot4Entity
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            TopAboutSummaryStrip(
                configuredCount = vaultState.configuredProviderCount,
                sleepGuardEnabled = true,
                onOpenAbout = onOpenAbout
            )
        }

        // Vault Status & Show/Hide Toggle Card
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LOCAL ENCRYPTED KEYSTORE VAULT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.7.sp
                        )
                        Text(
                            text = statusBannerMessage,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(onClick = { showSecretsPlain = !showSecretsPlain }) {
                        Icon(
                            imageVector = if (showSecretsPlain) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showSecretsPlain) "Mask" else "Reveal",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 1. RIVIAN ACCOUNT & GRAPHQL SESSION SETUP
        item {
            SectionContainerCard(
                header = "1. RIVIAN ACCOUNT & GRAPHQL SESSION (\$0 CLIENT-ONLY)",
                subtitle = "Sign in directly to rivian.com/api/gql/gateway/graphql or paste an existing session token. Stored 100% locally on-device."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rivianVehicleName,
                        onValueChange = { rivianVehicleName = it },
                        label = { Text("Rivian Display Name", fontSize = 11.5.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = rivianVehicleId,
                        onValueChange = { rivianVehicleId = it },
                        label = { Text("Vehicle ID (Optional)", fontSize = 11.5.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = rivianEmail,
                    onValueChange = { rivianEmail = it },
                    label = { Text("Rivian Account Email", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = rivianPassword,
                    onValueChange = { rivianPassword = it },
                    label = { Text("Rivian Password (saved in local Keystore)", fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = if (showSecretsPlain) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                AnimatedVisibility(visible = rivianOtpPending || vaultState.rivianPendingOtpToken.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rivianOtpCode,
                            onValueChange = { rivianOtpCode = it },
                            label = { Text("6-Digit Rivian MFA / SMS OTP Code", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = { onVerifyRivianOtp(rivianOtpCode) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Verify Rivian MFA Code & Store Session", fontWeight = FontWeight.Black)
                        }
                    }
                }

                TextButton(
                    onClick = { showRivianTokenAdvanced = !showRivianTokenAdvanced },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (showRivianTokenAdvanced) "▲ Hide Direct Rivian Session Token Fields"
                        else "▼ Or Paste Existing Rivian Bearer / U-Sess Tokens Directly",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = showRivianTokenAdvanced) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rivianAccessToken,
                            onValueChange = { rivianAccessToken = it },
                            label = { Text("Rivian Access Token (Bearer)", fontSize = 11.5.sp) },
                            singleLine = true,
                            visualTransformation = if (showSecretsPlain) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = rivianUserSessionToken,
                            onValueChange = { rivianUserSessionToken = it },
                            label = { Text("Rivian User Session Token (U-Sess)", fontSize = 11.5.sp) },
                            singleLine = true,
                            visualTransformation = if (showSecretsPlain) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = rivianRefreshToken,
                            onValueChange = { rivianRefreshToken = it },
                            label = { Text("Rivian Refresh Token (Optional)", fontSize = 11.5.sp) },
                            singleLine = true,
                            visualTransformation = if (showSecretsPlain) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSaveVault(
                                buildCurrentVault(),
                                "Saved Rivian credentials locally in encrypted storage"
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Locally", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onAuthenticateRivian(
                                rivianEmail,
                                rivianPassword,
                                rivianVehicleName,
                                rivianVehicleId
                            )
                        },
                        enabled = !isSyncing,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Login & Save Session", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. TESLA VEHICLE & CLOUD POWERWALL (BYOK FLEET API OR TESSIE / TESLEMETRY)
        item {
            SectionContainerCard(
                header = "2. TESLA VEHICLE & CLOUD ENERGY (BYOK FLEET / PROXY)",
                subtitle = "Use your own Tesla Developer Client ID & Bearer Token (\$10/mo free personal credit) or paste a Tessie / Teslemetry personal API key."
            ) {
                TeslaProviderType.entries.forEach { option ->
                    SelectableOptionRow(
                        title = option.title,
                        badge = option.badge,
                        description = option.description,
                        isSelected = teslaProvider == option,
                        onClick = { teslaProvider = option }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = teslaVehicleName,
                        onValueChange = { teslaVehicleName = it },
                        label = { Text("Tesla Display Name", fontSize = 11.5.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = teslaVehicleId,
                        onValueChange = { teslaVehicleId = it },
                        label = { Text("Vehicle ID / VIN (Auto)", fontSize = 11.5.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                if (teslaProvider == TeslaProviderType.FLEET_NA || teslaProvider == TeslaProviderType.FLEET_EU) {
                    OutlinedTextField(
                        value = teslaClientId,
                        onValueChange = { teslaClientId = it },
                        label = { Text("Your Tesla Developer client_id (BYOK)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = teslaAccessToken,
                    onValueChange = { teslaAccessToken = it },
                    label = {
                        Text(
                            if (teslaProvider == TeslaProviderType.TESSIE || teslaProvider == TeslaProviderType.TESLEMETRY)
                                "${teslaProvider.title.substringBefore(" ")} Personal API Key / Bearer Token"
                            else "Tesla OAuth Bearer Access Token",
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    visualTransformation = if (showSecretsPlain) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = teslaRefreshToken,
                        onValueChange = { teslaRefreshToken = it },
                        label = { Text("Refresh Token (Optional)", fontSize = 11.5.sp) },
                        singleLine = true,
                        visualTransformation = if (showSecretsPlain) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = teslaEnergySiteId,
                        onValueChange = { teslaEnergySiteId = it },
                        label = { Text("Energy Site ID (Powerwall)", fontSize = 11.5.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Button(
                    onClick = {
                        onSaveVault(
                            buildCurrentVault(),
                            "Saved Tesla BYOK credentials locally & triggered sleep-safe sync"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Tesla BYOK Key Locally & Sync", fontWeight = FontWeight.Black)
                }
            }
        }

        // 3. TESLA POWERWALL LOCAL LAN GATEWAY
        item {
            SectionContainerCard(
                header = "3. TESLA POWERWALL LOCAL LAN GATEWAY (\$0 • WI-FI DIRECT)",
                subtitle = "Authenticates directly against https://<gateway-ip>/api/login/Basic & reads /api/system_status/soe on your home network."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pwGatewayIp,
                        onValueChange = { pwGatewayIp = it },
                        label = { Text("Gateway IP (e.g. 192.168.1.50)", fontSize = 11.5.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = pwSiteName,
                        onValueChange = { pwSiteName = it },
                        label = { Text("Powerwall Label", fontSize = 11.5.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = pwEmail,
                    onValueChange = { pwEmail = it },
                    label = { Text("Tesla Email (for Local Gateway Basic Login)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = pwCustomerPassword,
                    onValueChange = { pwCustomerPassword = it },
                    label = { Text("Gateway Customer Password (physical sticker)", fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = if (showSecretsPlain) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    onClick = {
                        onSaveVault(
                            buildCurrentVault(),
                            "Saved Powerwall Local LAN Gateway credentials locally"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Local Gateway & Query SoE", fontWeight = FontWeight.Black)
                }
            }
        }

        // 4. HOME ASSISTANT UNIFIED LONG-LIVED ACCESS TOKEN (OPTIONAL)
        item {
            SectionContainerCard(
                header = "4. HOME ASSISTANT BRIDGE (OPTIONAL SINGLE TOKEN)",
                subtitle = "Already running Rivian, Tesla, or Powerwall in Home Assistant? Enter your HA URL and a single Long-Lived Access Token."
            ) {
                OutlinedTextField(
                    value = haBaseUrl,
                    onValueChange = { haBaseUrl = it },
                    label = { Text("Home Assistant URL (e.g. http://homeassistant.local:8123)", fontSize = 11.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = haAccessToken,
                    onValueChange = { haAccessToken = it },
                    label = { Text("Home Assistant Long-Lived Access Token", fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = if (showSecretsPlain) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = haSlot1Entity,
                        onValueChange = { haSlot1Entity = it },
                        label = { Text("Slot 1 Entity ID", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = haSlot2Entity,
                        onValueChange = { haSlot2Entity = it },
                        label = { Text("Slot 2 Entity ID", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = haSlot3Entity,
                        onValueChange = { haSlot3Entity = it },
                        label = { Text("Slot 3 Entity ID", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = haSlot4Entity,
                        onValueChange = { haSlot4Entity = it },
                        label = { Text("Slot 4 Entity ID", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Button(
                    onClick = {
                        onSaveVault(
                            buildCurrentVault(),
                            "Saved Home Assistant Long-Lived Token & entity mappings locally"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Home Assistant Bridge Locally", fontWeight = FontWeight.Black)
                }
            }
        }

        // 5. 2x2 WIDGET SLOT ASSIGNMENTS (MAX 4 PRODUCTS)
        item {
            SectionContainerCard(
                header = "5. 4x2 WIDGET PRODUCT SLOTS (MAX 4 IN 2x2 GRID)",
                subtitle = "Tap any of the 4 slots below to rename the product, switch its type, or test custom battery percentages on the widget."
            ) {
                slots.forEach { slot ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditSlot(slot) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Slot ${slot.slotIndex + 1}: ${slot.name} (${slot.productType.badge})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${slot.batteryPercent}% • ${slot.statusDetail}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Slot",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 3: SETTINGS (SLEEP / PHANTOM DRAIN GUARD, UNITS, THEME & API CAVEATS)
// ============================================================================
@Composable
private fun SettingsTab(
    isDarkMode: Boolean,
    allowWakeSleepingVehicles: Boolean,
    wifiOnlyLocalGateway: Boolean,
    pollingInterval: PollingInterval,
    distanceUnit: DistanceUnit,
    configuredProviders: Int,
    onOpenAbout: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onAllowWakeChanged: (Boolean) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onPollingIntervalSelected: (PollingInterval) -> Unit,
    onDistanceUnitSelected: (DistanceUnit) -> Unit,
    onResetDemoSlots: () -> Unit,
    onClearAllSecrets: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            TopAboutSummaryStrip(
                configuredCount = configuredProviders,
                sleepGuardEnabled = !allowWakeSleepingVehicles,
                onOpenAbout = onOpenAbout
            )
        }

        // 1. VEHICLE SLEEP & PHANTOM DRAIN PROTECTION
        item {
            SectionContainerCard(
                header = "1. VEHICLE SLEEP & PHANTOM DRAIN PROTECTION",
                subtitle = "Control whether Rivla is allowed to wake sleeping EVs or strictly serve cached battery levels when parked."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "Allow Waking Sleeping Vehicles",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (allowWakeSleepingVehicles)
                                "ENABLED (Caution): Polling may wake sleeping cars, causing 1–3% phantom battery drain per day and higher Tesla Fleet wake costs."
                            else
                                "DISABLED (Recommended): Checks /api/1/vehicles state first. If 'asleep', Rivla displays cached SoC without waking the car.",
                            fontSize = 11.sp,
                            color = if (allowWakeSleepingVehicles) HeadwindRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = allowWakeSleepingVehicles,
                        onCheckedChange = onAllowWakeChanged
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "Local Powerwall Gateway over Wi-Fi / LAN Only",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Only query https://<gateway-ip>/api/system_status/soe when connected to a local private subnet.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = wifiOnlyLocalGateway,
                        onCheckedChange = onWifiOnlyChanged
                    )
                }
            }
        }

        // 2. WIDGET REFRESH INTERVAL & APPEARANCE
        item {
            SectionContainerCard(
                header = "2. WIDGET SYNC INTERVAL, UNITS & DARK THEME",
                subtitle = "Configure background widget update frequency, range units, and Medusko Midnight Navy Dark Mode."
            ) {
                PollingInterval.entries.forEach { interval ->
                    SelectableOptionRow(
                        title = interval.title,
                        badge = interval.badge,
                        description = interval.description,
                        isSelected = pollingInterval == interval,
                        onClick = { onPollingIntervalSelected(interval) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                TogglePairSelectorRow(
                    label = "EV Range Display Units",
                    leftTitle = DistanceUnit.MILES.label,
                    rightTitle = DistanceUnit.KILOMETERS.label,
                    isLeftSelected = distanceUnit == DistanceUnit.MILES,
                    onSelectLeft = { onDistanceUnitSelected(DistanceUnit.MILES) },
                    onSelectRight = { onDistanceUnitSelected(DistanceUnit.KILOMETERS) }
                )

                TogglePairSelectorRow(
                    label = "App Theme Palette",
                    leftTitle = "Dark (Midnight Navy)",
                    rightTitle = "Light (Warm Linen)",
                    isLeftSelected = isDarkMode,
                    onSelectLeft = { if (!isDarkMode) onToggleDarkMode() },
                    onSelectRight = { if (isDarkMode) onToggleDarkMode() }
                )
            }
        }

        // 3. BYOK ARCHITECTURE & CAVEATS GUIDE (FROM RESEARCH CONVERSATION)
        item {
            Text(
                text = "3. BYOK ARCHITECTURE & VEHICLE API CAVEATS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )
        }

        items(ByokCaveatCatalog.items) { caveat ->
            CaveatExplanationCard(caveat = caveat)
        }

        // 4. VAULT MAINTENANCE ACTIONS
        item {
            SectionContainerCard(
                header = "4. LOCAL VAULT & WIDGET MAINTENANCE",
                subtitle = "Restore default 4-slot preview data or wipe all locally stored tokens from Android Keystore."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onResetDemoSlots,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reset 2x2 Preview", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }

                    Button(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HeadwindRed,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Wipe Local Keys", fontWeight = FontWeight.Black, fontSize = 11.5.sp)
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Wipe Local Encrypted Vault?", fontWeight = FontWeight.Black) },
            text = {
                Text("This permanently deletes all saved Rivian sessions, Tesla BYOK tokens, Powerwall LAN passwords, and Home Assistant keys stored on this phone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllSecrets()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HeadwindRed, contentColor = Color.White)
                ) {
                    Text("Yes, Wipe Vault", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun CaveatExplanationCard(caveat: CaveatGuideItem) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = caveat.badge,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = caveat.title,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = caveat.summary,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    caveat.bulletPoints.forEach { bullet ->
                        Text(
                            text = "• $bullet",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProductSlotDialog(
    slot: EnergyProductSlot,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: ProductType,
        pct: Int,
        rangeMi: Int?,
        kw: Double?,
        sleepState: VehicleSleepState,
        detail: String
    ) -> Unit
) {
    var name by remember(slot) { mutableStateOf(slot.name) }
    var productType by remember(slot) { mutableStateOf(slot.productType) }
    var batterySlider by remember(slot) { mutableFloatStateOf(slot.batteryPercent.toFloat()) }
    var rangeText by remember(slot) { mutableStateOf(slot.estimatedRangeMiles?.toString() ?: "") }
    var powerKwText by remember(slot) { mutableStateOf(slot.powerFlowKw?.toString() ?: "") }
    var sleepState by remember(slot) { mutableStateOf(slot.sleepState) }
    var statusDetail by remember(slot) { mutableStateOf(slot.statusDetail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Customize 2x2 Widget Slot ${slot.slotIndex + 1}",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Product Type",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProductType.entries.forEach { pt ->
                        val selected = productType == pt
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { productType = pt },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pt.badge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Energy Level: ${batterySlider.roundToInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = batterySlider,
                    onValueChange = { batterySlider = it },
                    valueRange = 0f..100f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rangeText,
                        onValueChange = { rangeText = it },
                        label = { Text("Range (mi)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = powerKwText,
                        onValueChange = { powerKwText = it },
                        label = { Text("Power (kW)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VehicleSleepState.entries.forEach { st ->
                        val selected = sleepState == st
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    sleepState = st
                                    statusDetail = st.label
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = st.badgeText,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        productType,
                        batterySlider.roundToInt(),
                        rangeText.toIntOrNull(),
                        powerKwText.toDoubleOrNull(),
                        sleepState,
                        statusDetail
                    )
                }
            ) {
                Text("Save & Update Widget", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SectionContainerCard(
    header: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = header,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
private fun SelectableOptionRow(
    title: String,
    badge: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun TogglePairSelectorRow(
    label: String,
    leftTitle: String,
    rightTitle: String,
    isLeftSelected: Boolean,
    onSelectLeft: () -> Unit,
    onSelectRight: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(leftTitle, isLeftSelected, onSelectLeft),
                Triple(rightTitle, !isLeftSelected, onSelectRight)
            ).forEach { (title, selected, onClick) ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onClick() },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
