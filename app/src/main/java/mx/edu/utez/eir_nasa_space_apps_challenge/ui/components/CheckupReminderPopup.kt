package mx.edu.utez.eir_nasa_space_apps_challenge.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*

@Composable
fun CheckupReminderPopup(
    visible: Boolean,
    isDarkMode: Boolean,
    onStartCheckupNow: () -> Unit,
    onSnooze15Min: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF0F9FF)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                color = if (isDarkMode) EirSky.copy(alpha = 0.6f) else Color(0xFF38BDF8)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MascotAvatar(
                            emotion = MascotEmotion.CONCERNED,
                            size = 36.dp,
                            showGlow = true
                        )

                        Column {
                            Text(
                                text = "RECORDATORIO PROGRAMADO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = EirSky
                            )
                            Text(
                                text = "Misión Ares IV • Sol 142",
                                fontSize = 11.sp,
                                color = if (isDarkMode) EirDarkTextSecondary else EirLightTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Posponer",
                            tint = if (isDarkMode) Color.White else Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Message Text
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "⏰ Hora de tu seguimiento de salud de hoy",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkMode) EirDarkTextPrimary else EirLightTextPrimary
                    )
                    Text(
                        text = "Es hora de tu escaneo facial, prueba de voz y registro de signos vitales para sincronizar con la cabina.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = if (isDarkMode) EirDarkTextSecondary else EirLightTextSecondary
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onStartCheckupNow,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EirEmerald)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Iniciar",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Iniciar Chequeo Ahora",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }

                    OutlinedButton(
                        onClick = onSnooze15Min,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDarkMode) EirDarkBorder else EirLightBorder
                        )
                    ) {
                        Text(
                            text = "+15 min",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "CheckupReminderPopup Light Preview")
@Composable
fun CheckupReminderPopupLightPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        Surface(color = EirLightBg) {
            CheckupReminderPopup(
                visible = true,
                isDarkMode = false,
                onStartCheckupNow = {},
                onSnooze15Min = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "CheckupReminderPopup Dark Preview", backgroundColor = 0xFF0B1120)
@Composable
fun CheckupReminderPopupDarkPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = true) {
        Surface(color = EirDarkBg) {
            CheckupReminderPopup(
                visible = true,
                isDarkMode = true,
                onStartCheckupNow = {},
                onSnooze15Min = {},
                onDismiss = {}
            )
        }
    }
}
