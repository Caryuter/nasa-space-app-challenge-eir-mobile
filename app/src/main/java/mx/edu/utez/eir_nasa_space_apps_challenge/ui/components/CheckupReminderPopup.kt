package mx.edu.utez.eir_nasa_space_apps_challenge.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Encabezado: Título y botón cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chequeo diario pendiente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Mensaje conciso sin sobrecarga de texto
                Text(
                    text = "Completa tu registro de hoy para mantener tu seguimiento al día.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Botones de acción Material 3 sin iconos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onSnooze15Min,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Posponer",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onStartCheckupNow,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Iniciar chequeo",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
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
