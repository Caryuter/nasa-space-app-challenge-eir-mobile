package mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.JournalEntry
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.Emotional2DQuadrant
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.BitacoraUiState
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.BitacoraViewModel

data class BreathGuide(val title: String, val sub: String, val scale: Float)

@Composable
fun BitacoraScreen(
    state: BitacoraUiState,
    viewModel: BitacoraViewModel,
    modifier: Modifier = Modifier
) {
    BitacoraScreenContent(
        state = state,
        onToggleBreathing = { viewModel.toggleBreathing() },
        onSetJournalInput = { viewModel.setJournalInput(it) },
        onSaveJournalEntry = { viewModel.saveJournalEntry() },
        modifier = modifier
    )
}

@Composable
fun BitacoraScreenContent(
    state: BitacoraUiState,
    onToggleBreathing: () -> Unit,
    onSetJournalInput: (String) -> Unit,
    onSaveJournalEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = false
    val cardBg = if (isDark) EirDarkSurface else EirLightSurface

    val breathGuides = listOf(
        BreathGuide("Toma aire despacio", "Llena tus pulmones sin prisa • 4 seg", 1.25f),
        BreathGuide("Sostén el aire", "Quédate quieto un momento • 4 seg", 1.25f),
        BreathGuide("Suelta el aire suave", "Deja caer los hombros • 4 seg", 0.85f),
        BreathGuide("Descansa antes de seguir", "Siente la calma de la cabina • 4 seg", 0.85f)
    )

    val currentGuide = breathGuides[state.breathingPhase]

    val animatedScale by animateFloatAsState(
        targetValue = if (state.breathingActive) currentGuide.scale else 1f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "breathScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Atmospheric Emotional Banner (Card)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isDark) {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF1E1B4B).copy(alpha = 0.7f),
                                    Color(0xFF0F172A),
                                    Color(0xFF3B0764).copy(alpha = 0.4f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFEEF2FF),
                                    Color.White,
                                    Color(0xFFF0FDF4)
                                )
                            )
                        }
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tu espacio tranquilo • 100% Privado",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EirIndigo
                        )
                        Text(
                            text = "Bitácora de Eir",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Button(
                        onClick = onToggleBreathing,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.breathingActive) EirIndigo else {
                                if (isDark) EirIndigo.copy(alpha = 0.2f) else Color.White
                            },
                            contentColor = if (state.breathingActive) Color.White else EirIndigo
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = "Respirar",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.breathingActive) "Detener" else "Respirar 4-4-4-4",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Aquí no hay números clínicos. Solo un rincón para soltar lo que piensas y recargar calma.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                )

                if (state.breathingActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .scale(animatedScale)
                                .clip(CircleShape)
                                .background(EirIndigo.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(EirIndigo.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(EirIndigo)
                                )
                            }
                        }

                        Text(
                            text = currentGuide.title,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = EirIndigo
                        )
                        Text(
                            text = currentGuide.sub,
                            fontSize = 11.sp,
                            color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                        )
                    }
                }
            }
        }

        // 2D Emotional Quadrant
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = cardBg)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cómo te has sentido esta semana",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Hoy: ${state.moodLabel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EirIndigo
                    )
                }

                Emotional2DQuadrant(
                    selectedPoint = state.moodPoint,
                    isInteractive = false,
                    entries = state.journalEntries,
                    isDarkMode = isDark,
                    height = 140.dp
                )
            }
        }

        // New Personal Journal Entry
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = cardBg)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Escribe algo para ti hoy",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = state.journalInput,
                    onValueChange = onSetJournalInput,
                    placeholder = {
                        Text(
                            text = "¿Cómo estuvo tu día en la cabina? Escribe libremente...",
                            fontSize = 12.sp
                        )
                    },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onSaveJournalEntry,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EirIndigo)
                    ) {
                        Text(
                            text = "Guardar Nota",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Historical Journal Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "TUS DÍAS ANTERIORES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
            )

            val entries = if (state.journalEntries.isNotEmpty()) state.journalEntries else sampleJournalEntries
            entries.forEach { entry ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.sol,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EirIndigo
                            )

                            Surface(
                                shape = CircleShape,
                                color = EirIndigo.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = entry.mood,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EirIndigo,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = entry.text,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = if (isDark) EirDarkTextPrimary else EirLightTextPrimary
                        )
                    }
                }
            }
        }
    }
}

val sampleJournalEntries = listOf(
    JournalEntry(1L, "Sol 142 • Hoy 08:15", "Tranquilo", 74f, 64f, "Dormí mucho mejor después de bajar un poco la temperatura de mi cabina."),
    JournalEntry(2L, "Sol 141 • Ayer 21:30", "Nostálgico", 50f, 44f, "Vi una foto de mi familia antes de dormir. La respiración me relajó."),
    JournalEntry(3L, "Sol 140 • Hace 2 días", "Motivado", 84f, 80f, "Terminamos de calibrar los paneles solares antes de tiempo. Gran equipo.")
)

@Preview(showBackground = true, name = "BitacoraScreen Light Preview", heightDp = 900)
@Composable
fun BitacoraScreenLightPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        Surface(color = EirLightBg) {
            BitacoraScreenContent(
                state = BitacoraUiState(journalEntries = sampleJournalEntries),
                onToggleBreathing = {},
                onSetJournalInput = {},
                onSaveJournalEntry = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "BitacoraScreen Dark Preview", backgroundColor = 0xFF0B1120, heightDp = 900)
@Composable
fun BitacoraScreenDarkPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = true) {
        Surface(color = EirDarkBg) {
            BitacoraScreenContent(
                state = BitacoraUiState(journalEntries = sampleJournalEntries),
                onToggleBreathing = {},
                onSetJournalInput = {},
                onSaveJournalEntry = {}
            )
        }
    }
}
