package mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
    val cardBorder = if (isDark) EirDarkBorder else EirLightBorder
    val textPrimary = if (isDark) EirDarkTextPrimary else EirLightTextPrimary
    val textSecondary = if (isDark) EirDarkTextSecondary else EirLightTextSecondary

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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SCREEN HEADER
        Column {
            Text(
                text = "Bitácora de Eir",
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp,
                color = textPrimary
            )
            Text(
                text = "Tu rincón de calma, reflexión y seguimiento de racha",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // STREAK & COMMITMENT CARD (Screenshot 2 Replica)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Flame Graphic + Streak Number
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "🔥", fontSize = 48.sp)
                    Text(
                        text = "3",
                        fontWeight = FontWeight.Black,
                        fontSize = 36.sp,
                        color = textPrimary
                    )
                    Text(
                        text = "Días de racha",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = textPrimary
                    )
                }

                // Weekly Days Tracker Bar (L M X J V S D - Screenshot 2)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF0F172A) else Color(0xFFFAFAFA),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Day Labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("L", "M", "X", "J", "V", "S", "D").forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textPrimary
                                )
                            }
                        }

                        // Progress Line (3 days completed out of 7)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(3f / 7f)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(EirAmber)
                            )
                        }
                    }
                }

                // Motivational Text
                Text(
                    text = "¡Increíble! Ahora, ¡a por el hito de 7 días!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )

                // Secondary Link "Compartir hito"
                Text(
                    text = "Compartir hito",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EirEmerald
                )

                // Dark CTA Button "Me comprometo" (Screenshot 2)
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EirCharcoal,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Me comprometo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // BREATHING GUIDED CARD (4-4-4-4)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pausa de Respiración",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary
                        )
                        Text(
                            text = "Técnica 4-4-4-4 para desacelerar el ritmo cardiaco",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }

                    Button(
                        onClick = onToggleBreathing,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.breathingActive) EirCharcoal else EirEmerald,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = "Respirar",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.breathingActive) "Detener" else "Iniciar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (state.breathingActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(animatedScale)
                                .clip(CircleShape)
                                .background(EirEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(EirEmerald.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(EirEmerald)
                                )
                            }
                        }

                        Text(
                            text = currentGuide.title,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = EirEmerald
                        )
                        Text(
                            text = currentGuide.sub,
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }
            }
        }

        // 2D EMOTIONAL QUADRANT CARD
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estado Emocional",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimary
                    )
                    Text(
                        text = "Hoy: ${state.moodLabel}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EirEmerald
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

        // PERSONAL JOURNAL ENTRY CARD ("Escribe algo para ti hoy")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Escribe algo para ti hoy",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = textPrimary
                )

                OutlinedTextField(
                    value = state.journalInput,
                    onValueChange = onSetJournalInput,
                    placeholder = {
                        Text(
                            text = "¿Cómo estuvo tu día en la cabina? Escribe libremente...",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EirEmerald,
                        unfocusedBorderColor = cardBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onSaveJournalEntry,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EirCharcoal)
                    ) {
                        Text(
                            text = "Guardar Nota",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // HISTORICAL JOURNAL ENTRIES
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "TUS DÍAS ANTERIORES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = textSecondary
            )

            val entries = if (state.journalEntries.isNotEmpty()) state.journalEntries else sampleJournalEntries
            entries.forEach { entry ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.sol,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EirEmerald
                            )

                            Surface(
                                shape = CircleShape,
                                color = EirEmerald.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = entry.mood,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EirEmerald,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = entry.text,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = textPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

val sampleJournalEntries = listOf(
    JournalEntry(1L, "Sol 142 • Hoy 08:15", "Tranquilo", 74f, 64f, "Dormí mucho mejor después de bajar un poco la temperatura de mi cabina."),
    JournalEntry(2L, "Sol 141 • Ayer 21:30", "Nostálgico", 50f, 44f, "Vi una foto de mi familia antes de dormir. La respiración me relajó."),
    JournalEntry(3L, "Sol 140 • Hace 2 días", "Motivado", 84f, 80f, "Terminamos de calibrar los paneles solares antes de tiempo. Gran equipo.")
)

@Preview(showBackground = true, name = "BitacoraScreen Light Preview", heightDp = 950)
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

@Preview(showBackground = true, name = "BitacoraScreen Dark Preview", backgroundColor = 0xFF0F172A, heightDp = 950)
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
