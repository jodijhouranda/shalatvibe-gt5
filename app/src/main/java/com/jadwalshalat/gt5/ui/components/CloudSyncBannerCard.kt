package com.jadwalshalat.gt5.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.jadwalshalat.gt5.cloud.CloudSyncState
import com.jadwalshalat.gt5.cloud.onGoogleSignInClicked
import com.jadwalshalat.gt5.cloud.signOutGoogle
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette

@Composable
fun CloudSyncBannerCard(
    syncState: CloudSyncState,
    onBeforeSignOut: () -> Unit,
    onAuthFeedback: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val palette = LocalShalatVibePalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isSigningIn by remember { mutableStateOf(false) }

    when (syncState) {
        CloudSyncState.SignedOut -> {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.cardSurface,
                border = BorderStroke(1.dp, palette.emeraldPrimary.copy(alpha = 0.25f)),
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("cloud_sync_signed_out_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = if (compact) 11.dp else 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = palette.emeraldSoft,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CloudSync,
                                contentDescription = "Sinkronisasi Cloud",
                                tint = palette.emeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sinkronkan Kamus ke Web",
                            color = palette.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Masuk Google agar kata tersimpan & progres otomatis terhubung antar perangkat.",
                            color = palette.textSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            isSigningIn = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    onAuthFeedback("Berhasil masuk Google. Sinkronisasi cloud aktif.")
                                },
                                onAuthError = { msg ->
                                    isSigningIn = false
                                    onAuthFeedback(msg)
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isSigningIn = false
                                }
                            )
                        },
                        enabled = !isSigningIn,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.emeraldPrimary,
                            contentColor = if (palette.isDark) Color(0xFF03231D) else Color.White
                        ),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                color = if (palette.isDark) Color(0xFF03231D) else Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Login,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Masuk",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        is CloudSyncState.Syncing -> {
            CloudConnectedRow(
                icon = Icons.Rounded.CloudSync,
                iconTint = palette.emeraldPrimary,
                badgeBg = palette.emeraldSoft,
                title = "Menyinkronkan cloud...",
                subtitle = syncState.email.ifBlank { syncState.displayName.ifBlank { "Akun Google" } },
                showSpinner = true,
                onSignOut = {
                    signOutGoogle(
                        credentialManager = credentialManager,
                        scope = scope,
                        onBeforeSignOut = onBeforeSignOut,
                        onSignOutComplete = { onAuthFeedback("Keluar dari sinkronisasi cloud.") }
                    )
                },
                modifier = modifier
            )
        }

        is CloudSyncState.Synced -> {
            CloudConnectedRow(
                icon = Icons.Rounded.CloudDone,
                iconTint = palette.emeraldPrimary,
                badgeBg = palette.emeraldSoft,
                title = "Cloud Aktif • ${syncState.syncedWordCount} kata",
                subtitle = syncState.email.ifBlank { syncState.displayName.ifBlank { "Akun Google" } },
                showSpinner = false,
                onSignOut = {
                    signOutGoogle(
                        credentialManager = credentialManager,
                        scope = scope,
                        onBeforeSignOut = onBeforeSignOut,
                        onSignOutComplete = { onAuthFeedback("Keluar dari sinkronisasi cloud.") }
                    )
                },
                modifier = modifier
            )
        }

        is CloudSyncState.Error -> {
            CloudConnectedRow(
                icon = Icons.Rounded.CloudOff,
                iconTint = palette.goldAccent,
                badgeBg = palette.goldSoft,
                title = "Sinkronisasi tertunda",
                subtitle = syncState.message,
                showSpinner = false,
                onSignOut = {
                    signOutGoogle(
                        credentialManager = credentialManager,
                        scope = scope,
                        onBeforeSignOut = onBeforeSignOut,
                        onSignOutComplete = { onAuthFeedback("Keluar dari sinkronisasi cloud.") }
                    )
                },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun CloudConnectedRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    badgeBg: Color,
    title: String,
    subtitle: String,
    showSpinner: Boolean,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.cardSurface,
        border = BorderStroke(1.dp, palette.hairline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("cloud_sync_connected_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = badgeBg,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (showSpinner) {
                        CircularProgressIndicator(
                            color = iconTint,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.textPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                border = BorderStroke(1.dp, palette.hairline),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("google_sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = "Keluar akun Google",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Keluar",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
