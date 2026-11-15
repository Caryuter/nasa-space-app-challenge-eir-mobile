package mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.SosScenario
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.SosStep
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.SosUiState
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.SosViewModel

@Composable
fun SosEmergencyOverlay(
    state: SosUiState,
    viewModel: SosViewModel,
    onCompleteSuccess: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    SosEmergencyOverlayContent(
        state = state,
        onClose = { viewModel.closeSos() },
        onSelectScenario = { viewModel.selectSosScenario(it) },
        onPlayVoiceGuide = { viewModel.playVoiceGuide() },
        onPrevStep = { viewModel.prevSosStep() },
        onNextStep = { viewModel.nextSosStep() },
        scenarios = viewModel.getSosScenarios(),
        onCompleteSuccess = onCompleteSuccess,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosEmergencyOverlayContent(
    state: SosUiState,
    onClose: () -> Unit,
    onSelectScenario: (String) -> Unit,
    onPlayVoiceGuide: () -> Unit,
    onPrevStep: () -> Unit,
    onNextStep: () -> Unit,
    scenarios: List<SosScenario>,
    onCompleteSuccess: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!state.sosOpen) return

    val currentScenario = state.activeScenario
    val currentStep = currentScenario.steps.getOrNull(state.sosStepIndex) ?: return
    var showScenarioPicker by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.isDark
    val redBannerColor = EirRoseEmergency // Franja roja de emergencia
    val surfaceBgColor = MaterialTheme.colorScheme.background
    val textPrimaryColor = MaterialTheme.colorScheme.onBackground
    val textSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textBodyColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceBgColor)
            .clickable(enabled = false) {}
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ==========================================
            // 1. FRANJA ROJA SUPERIOR (Única señal roja)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(redBannerColor)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Cerrar",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onClose() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // ==========================================
            // 2. CUERPO PRINCIPAL CON ALTO CONTRASTE
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(18.dp))

                    // Encabezado: Título SOS y botón Audio
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Título de emergencia con selector desplegable sutil
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showScenarioPicker = true }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = "SOS · ${currentScenario.tag}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimaryColor,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Cambiar emergencia",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Botón de audio tipo píldora
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (state.isVoiceGuidePlaying) (if (isDark) redBannerColor.copy(alpha = 0.25f) else Color(0xFFFEE2E2)) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (state.isVoiceGuidePlaying) redBannerColor else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.clickable { onPlayVoiceGuide() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Audio",
                                    tint = if (state.isVoiceGuidePlaying) redBannerColor else textPrimaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (state.isVoiceGuidePlaying) "Leyendo..." else "Audio",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isVoiceGuidePlaying) redBannerColor else textPrimaryColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Indicador de paso
                    Text(
                        text = "PASO ${state.sosStepIndex + 1} DE ${currentScenario.steps.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = textSecondaryColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Instrucción principal grande y clara
                    Text(
                        text = currentStep.instruction,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                        color = textPrimaryColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Explicación de apoyo concisa
                    Text(
                        text = currentStep.why,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 22.sp,
                        color = textBodyColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ==========================================
                    // ILUSTRACIÓN TÉCNICA / IMAGEN DEL PASO
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageRes = currentStep.imageRes
                        if (imageRes != null) {
                            Image(
                                painter = painterResource(id = imageRes),
                                contentDescription = currentStep.instruction,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Fallback técnico para pasos de respiración o sin imagen específica
                            SosBreathingIllustration()
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Detalle del equipo a utilizar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Equipo: ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimaryColor
                        )
                        Text(
                            text = currentStep.kitAction,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = textBodyColor
                        )
                    }
                }

                // ==========================================
                // 3. BOTONES DE NAVEGACIÓN INFERIORES
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 16.dp, bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.sosStepIndex > 0) {
                            OutlinedButton(
                                onClick = onPrevStep,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = textPrimaryColor
                                ),
                                modifier = Modifier
                                    .weight(0.38f)
                                    .height(54.dp)
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "Anterior",
                                    tint = textPrimaryColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Anterior",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimaryColor
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (state.sosStepIndex < currentScenario.steps.size - 1) {
                                    onNextStep()
                                } else {
                                    onClose()
                                    onCompleteSuccess("Protocolo de emergencia completado")
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) redBannerColor else textPrimaryColor
                            ),
                            modifier = Modifier
                                .weight(if (state.sosStepIndex > 0) 0.62f else 1f)
                                .height(54.dp)
                        ) {
                            Text(
                                text = if (state.sosStepIndex < currentScenario.steps.size - 1) {
                                    "Siguiente paso"
                                } else {
                                    "Finalizar asistencia ✓"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // SELECTOR DE ESCENARIOS (Hoja limpia y rápida)
        // ==========================================
        if (showScenarioPicker) {
            ModalBottomSheet(
                onDismissRequest = { showScenarioPicker = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "Seleccionar Emergencia",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimaryColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    scenarios.forEach { sc ->
                        val isSelected = sc.id == currentScenario.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                                )
                                .clickable {
                                    onSelectScenario(sc.id)
                                    showScenarioPicker = false
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = sc.tag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) redBannerColor else textSecondaryColor
                                )
                                Text(
                                    text = sc.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimaryColor
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = redBannerColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Gráfico técnico minimalista para guía de respiración rítmica en microgravedad.
 */
@Composable
private fun SosBreathingIllustration() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Air,
                contentDescription = "Respiración",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(54.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "4 seg. Inhala  •  6 seg. Exhala",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Mantén la respiración lenta y constante",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

val sampleSosScenario: SosScenario
    get() = SosScenario(
    id = "faint",
    title = "Alguien se mareó o desmayó",
    tag = "MAREO / DESMAYO",
    kitItem = "Estuche Azul",
    steps = listOf(
        SosStep(
            instruction = "Sujétalo al asiento con el cinturón",
            why = "Evita que flote y golpee los paneles en gravedad cero.",
            kitAction = "Arnés de pecho y cintura",
            imageRes = R.drawable.sos_seatbelt
        ),
        SosStep(
            instruction = "Coloca la pinza blanca en su dedo",
            why = "Nos dirá en 5 segundos si le está llegando suficiente oxígeno.",
            kitAction = "Pinza de oximetría (Estuche Azul)",
            imageRes = R.drawable.sos_oximeter
        )
    )
)

@Preview(showBackground = true, name = "SosEmergencyOverlay Light High Contrast Preview", heightDp = 700)
@Composable
fun SosEmergencyOverlayLightPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        SosEmergencyOverlayContent(
            state = SosUiState(sosOpen = true, activeScenario = sampleSosScenario),
            onClose = {},
            onSelectScenario = {},
            onPlayVoiceGuide = {},
            onPrevStep = {},
            onNextStep = {},
            scenarios = listOf(sampleSosScenario)
        )
    }
}
