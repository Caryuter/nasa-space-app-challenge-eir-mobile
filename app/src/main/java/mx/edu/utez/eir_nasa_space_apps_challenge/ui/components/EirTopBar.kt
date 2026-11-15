package mx.edu.utez.eir_nasa_space_apps_challenge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Warning
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
fun EirTopBar(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onOpenSos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkMode || MaterialTheme.colorScheme.isDark
    val bg = MaterialTheme.colorScheme.surface
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val border = MaterialTheme.colorScheme.outline

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bg,
        shadowElevation = 0.5.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "EIR",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Misión Ares IV • Sol 142",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dark / Light toggle button
                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Cambiar Tema",
                        tint = if (isDark) EirAmber else textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // SOS Emergency Button
                Button(
                    onClick = onOpenSos,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EirRoseEmergency,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SOS",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "EirTopBar Light Mode")
@Composable
fun EirTopBarLightPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        EirTopBar(
            isDarkMode = false,
            onToggleDarkMode = {},
            onOpenSos = {}
        )
    }
}

@Preview(showBackground = true, name = "EirTopBar Dark Mode")
@Composable
fun EirTopBarDarkPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = true) {
        EirTopBar(
            isDarkMode = true,
            onToggleDarkMode = {},
            onOpenSos = {}
        )
    }
}
