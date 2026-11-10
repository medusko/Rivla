package com.medusko.rivla.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.medusko.rivla.BuildConfig
import com.medusko.rivla.R
import com.medusko.rivla.model.DistanceUnit
import com.medusko.rivla.model.PollingInterval
import com.medusko.rivla.ui.theme.TailwindGreen

@Composable
fun AboutRivlaDialog(
    configuredProviders: Int,
    sleepGuardEnabled: Boolean,
    pollingInterval: PollingInterval,
    distanceUnit: DistanceUnit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "About Rivla",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. HERO IDENTITY CARD
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.mipmap.ic_launcher),
                                    contentDescription = "App Icon",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Rivla",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Android",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "BYOK EV & Home Energy Telemetry Widget Hub",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "v${BuildConfig.VERSION_NAME}",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = "Build ${BuildConfig.VERSION_CODE}",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(TailwindGreen)
                                    )
                                    Text(
                                        text = if (sleepGuardEnabled) "Sleep Guard Active" else "Wake Allowed",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TailwindGreen
                                    )
                                }

                                Text(
                                    text = "$configuredProviders/4 BYOK Vaults",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "${pollingInterval.badge} • ${distanceUnit.shortUnit}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 2. ARCHITECTURE & BYOK TELEMETRY ENGINE
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "ZERO-SERVER BYOK EV & ENERGY TELEMETRY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Rivla aggregates State of Charge (SoC), range, and solar/backup power across Rivian vehicles, Tesla vehicles, and Tesla Powerwalls into a unified 4x2 grid widget for your Android Home Screen and Lock Screen.\n\nBuilt on a strict Bring Your Own Key (BYOK) & client-only architecture, your authentication sessions and API tokens are encrypted locally in Android Keystore—never transmitted to any intermediate server.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 17.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AboutFeatureItem(
                                title = "Rivian Direct GraphQL & MFA Handshake ($0 Cost)",
                                description = "Authenticates directly with Rivian's GraphQL gateway using your account credentials or session tokens to read live batteryLevel & distanceToEmpty."
                            )
                            AboutFeatureItem(
                                title = "Tesla Fleet BYOK & Tessie / Teslemetry Support",
                                description = "Use your own Tesla Developer Client ID & Bearer token (covered by Tesla's \$10/mo personal credit) or paste a Tessie / Teslemetry personal key."
                            )
                            AboutFeatureItem(
                                title = "Powerwall Local LAN Gateway & Home Assistant",
                                description = "Connect directly over Wi-Fi to https://<gateway-ip>/api/system_status/soe or bridge all sensors through a single Home Assistant Long-Lived Token."
                            )
                            AboutFeatureItem(
                                title = "Phantom-Drain Safe Polling & 4x2 Lock/Home Widget",
                                description = "Checks vehicle sleep state before querying telemetry so parked EVs stay in deep sleep while displaying up to 4 products in a 2x2 widget grid."
                            )
                        }
                    }

                    // 3. LINKS & RESOURCES
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "LINKS & RESOURCES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AboutResourceLink(text = "Changelog", modifier = Modifier.weight(1f)) {
                                uriHandler.openUri("https://github.com/medusko/Rivla/blob/main/CHANGELOG.md")
                            }
                            AboutResourceLink(text = "Releases", modifier = Modifier.weight(1f)) {
                                uriHandler.openUri("https://github.com/medusko/Rivla/releases")
                            }
                            AboutResourceLink(text = "Source", modifier = Modifier.weight(1f)) {
                                uriHandler.openUri("https://github.com/medusko/Rivla")
                            }
                            AboutResourceLink(text = "Sponsor", modifier = Modifier.weight(1f)) {
                                uriHandler.openUri("https://github.com/sponsors/Zlatty")
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = Color(0xFF131B2E), shape = RoundedCornerShape(10.dp))
                                .border(width = 1.dp, color = Color(0xFF22304E), shape = RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Website:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.width(64.dp))
                                Text(
                                    text = "medusko.com",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.clickable { uriHandler.openUri("https://medusko.com") }
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Support:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.width(64.dp))
                                Text(
                                    text = "support@medusko.com",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.clickable { uriHandler.openUri("mailto:support@medusko.com") }
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Social:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.width(64.dp))
                                Text(
                                    text = "@meduskodev",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.clickable { uriHandler.openUri("https://twitter.com/meduskodev") }
                                )
                                Text(" (on Twitter)", fontSize = 11.5.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }

                    Text(
                        text = "Rivla v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • Built for Android • Designed & Engineered by Medusko",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun AboutFeatureItem(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "• $title",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 14.5.sp
        )
    }
}

@Composable
private fun AboutResourceLink(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}
