package mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .clickable(enabled = false) {}
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // SOS Top Header
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .border(
                            width = 0.5.dp,
                            color = EirRoseEmergency.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(0.dp)
                        )
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EirRoseEmergency),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Emergencia",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "AYUDA DE EMERGENCIA",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = Color(0xFFFB7185)
                            )
                            Text(
                                text = "Paso a paso simple • Sin conexión",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Salir",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Scenario Selector (4 Everyday Emergency Scenarios)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    scenarios.take(2).forEach { sc ->
                        val isSelected = sc.id == currentScenario.id
                        ScenarioCard(
                            scenario = sc,
                            isSelected = isSelected,
                            onClick = { onSelectScenario(sc.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    scenarios.drop(2).forEach { sc ->
                        val isSelected = sc.id == currentScenario.id
                        ScenarioCard(
                            scenario = sc,
                            isSelected = isSelected,
                            onClick = { onSelectScenario(sc.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TARJETA CENTRAL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF0F172A))
                    .border(2.dp, EirRoseEmergency.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(EirRoseEmergency)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "PASO ${state.sosStepIndex + 1} DE ${currentScenario.steps.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (state.isVoiceGuidePlaying) EirEmerald else Color(0xFF1E293B)
                            )
                            .clickable { onPlayVoiceGuide() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Escuchar",
                            tint = if (state.isVoiceGuidePlaying) Color.Black else Color(0xFFFDA4AF),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (state.isVoiceGuidePlaying) "Leyendo..." else "Escuchar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isVoiceGuidePlaying) Color.Black else Color(0xFFFDA4AF)
                        )
                    }
                }

                // Giant Instruction
                Text(
                    text = currentStep.instruction,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 26.sp,
                    color = Color.White
                )

                // Why explanation
                Text(
                    text = currentStep.why,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFFCBD5E1)
                )

                // Visual Kit Action Guide
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0C4A6E).copy(alpha = 0.6f))
                        .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = "Botiquín",
                        tint = EirSkyLight,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "QUÉ USAR DEL BOTIQUÍN:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = EirSkyLight
                        )
                        Text(
                            text = currentStep.kitAction,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.sosStepIndex > 0) {
                    Button(
                        onClick = onPrevStep,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Anterior", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Anterior", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                if (state.sosStepIndex < currentScenario.steps.size - 1) {
                    Button(
                        onClick = onNextStep,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EirEmerald)
                    ) {
                        Text(
                            text = "Listo, siguiente paso",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = "Siguiente", tint = Color.Black)
                    }
                } else {
                    Button(
                        onClick = {
                            onClose()
                            onCompleteSuccess("Protocolo de emergencia completado")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EirSky)
                    ) {
                        Text(
                            text = "Finalizar asistencia ✓",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioCard(
    scenario: SosScenario,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) EirRoseEmergency.copy(alpha = 0.25f) else Color(0xFF0F172A)
            )
            .border(
                1.dp,
                if (isSelected) EirRoseEmergency else Color(0xFF1E293B),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = scenario.tag,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFB7185)
        )
        Text(
            text = scenario.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 2
        )
    }
}

val sampleSosScenario = SosScenario(
    id = "faint",
    title = "Alguien se mareó o desmayó",
    tag = "MAREO / DESMAYO",
    kitItem = "Estuche Azul",
    steps = listOf(
        SosStep(
            instruction = "Sujétalo al asiento con el cinturón para que no flote ni se golpee.",
            why = "En gravedad cero un cuerpo inconsciente puede chocar contra los paneles.",
            kitAction = "Asegura primero el arnés de pecho y cintura."
        ),
        SosStep(
            instruction = "Saca del estuche azul la pinza blanca y ponla en su dedo índice.",
            why = "Nos dirá en 5 segundos si le está llegando suficiente aire y sangre.",
            kitAction = "Usa la pinza del bolsillo superior del Estuche Azul."
        )
    )
)

@Preview(showBackground = true, name = "SosEmergencyOverlay Light Preview", heightDp = 600)
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
