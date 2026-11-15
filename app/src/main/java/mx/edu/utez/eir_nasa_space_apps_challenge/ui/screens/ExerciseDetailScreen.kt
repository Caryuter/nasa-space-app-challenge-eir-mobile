package mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.Exercise
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exercise: Exercise,
    restTimerSec: Int,
    isRestRunning: Boolean,
    onBack: () -> Unit,
    onToggleDone: (Long) -> Unit,
    onToggleRestTimer: (Long) -> Unit,
    onUpdateParameters: (Long, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val cardBg = MaterialTheme.colorScheme.surface
    val cardBorder = MaterialTheme.colorScheme.outline
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    var showEditDialog by remember { mutableStateOf(false) }
    var editSetsReps by remember(exercise) { mutableStateOf(exercise.setsReps) }
    var editLoadKg by remember(exercise) { mutableStateOf(exercise.loadKg) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP APP BAR (Smart Fit screenshot 2 header)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = textPrimary
                )
            }
            Text(
                text = exercise.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textPrimary,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LARGE EXERCISE VISUAL CONTAINER (Screenshot 2 Replica)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                val imageRes = exercise.imageRes ?: R.drawable.ex_space_squats
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = exercise.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            // EXERCISE TITLE
            Text(
                text = exercise.name,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = textPrimary,
                letterSpacing = (-0.5).sp
            )

            // TWO PARAMETER CARDS (Series y Repeticiones & Carga kg - Screenshot 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sets & Reps Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showEditDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
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
                                text = "Series y\nRepeticiones",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = textSecondary,
                                lineHeight = 16.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar series",
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = exercise.setsReps,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary
                        )
                    }
                }

                // Load (kg) Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showEditDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
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
                                text = "Carga (kg)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = textSecondary
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar carga",
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = exercise.loadKg,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary
                        )
                    }
                }
            }

            // SPACE COUNTERMEASURES / BIOMECHANICS GUIDANCE
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EirAmber.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "ARED PROTOCOL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = EirAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Preservación Ósea",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    Text(
                        text = exercise.why,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )

                    Text(
                        text = "Instrucciones de postura:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = exercise.instructions,
                        fontSize = 12.sp,
                        color = textSecondary,
                        lineHeight = 17.sp
                    )
                }
            }

            // REST TIMER CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (isRestRunning) EirEmerald.copy(alpha = 0.1f) else cardBg,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isRestRunning) EirEmerald else cardBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Temporizador",
                            tint = if (isRestRunning) EirEmerald else textSecondary
                        )
                        Column {
                            val formatted = String.format(java.util.Locale.US, "%02d:%02d", restTimerSec / 60, restTimerSec % 60)
                            Text(
                                text = if (isRestRunning) "$formatted Restante" else "00:45",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isRestRunning) EirEmerald else textPrimary
                            )
                            Text(
                                text = "Descanso entre series",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = { onToggleRestTimer(exercise.id) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isRestRunning) EirEmerald else EirCharcoal)
                    ) {
                        Icon(
                            imageVector = if (isRestRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Iniciar descanso",
                            tint = Color.White
                        )
                    }
                }
            }

            // MARK AS DONE TOGGLE BUTTON
            Button(
                onClick = { onToggleDone(exercise.id) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (exercise.done) EirEmerald else EirAmber,
                    contentColor = if (exercise.done) Color.White else EirCharcoal
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (exercise.done) "Ejercicio completado" else "Marcar ejercicio como realizado",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // EDIT PARAMETERS DIALOG
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Ajustar Parámetros",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editSetsReps,
                        onValueChange = { editSetsReps = it },
                        label = { Text("Series y Repeticiones") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editLoadKg,
                        onValueChange = { editLoadKg = it },
                        label = { Text("Carga (kg o nivel ARED)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateParameters(exercise.id, editSetsReps, editLoadKg)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EirCharcoal)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private val sampleDetailExercise: Exercise
    get() = Exercise(
        id = 1L,
        name = "Sentadillas con ligas de resistencia",
        setsReps = "4x 15 a 20",
        loadKg = "35",
        restSec = 45,
        why = "Mantiene fuertes los huesos de cadera y piernas sin gravedad",
        duration = "20 min",
        done = false,
        imageRes = R.drawable.ex_space_squats,
        thumbRes = R.drawable.ex_space_squats,
        category = "Piernas y Cadera",
        instructions = "Fija las bandas elásticas y arnés ARED a la plataforma. Desciende flexionando rodillas a 90° controlando la tracción elástica para comprimir los huesos largos del tren inferior."
    )

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "ExerciseDetail Light", heightDp = 850)
@Composable
fun ExerciseDetailLightPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        Surface(color = EirLightBg) {
            ExerciseDetailScreen(
                exercise = sampleDetailExercise,
                restTimerSec = 45,
                isRestRunning = false,
                onBack = {},
                onToggleDone = {},
                onToggleRestTimer = {},
                onUpdateParameters = { _, _, _ -> }
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "ExerciseDetail Dark", backgroundColor = 0xFF0F172A, heightDp = 850)
@Composable
fun ExerciseDetailDarkPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = true) {
        Surface(color = EirDarkBg) {
            ExerciseDetailScreen(
                exercise = sampleDetailExercise,
                restTimerSec = 45,
                isRestRunning = false,
                onBack = {},
                onToggleDone = {},
                onToggleRestTimer = {},
                onUpdateParameters = { _, _, _ -> }
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "ExerciseDetail Resting Countdown", backgroundColor = 0xFF0F172A, heightDp = 850)
@Composable
fun ExerciseDetailRestingPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = true) {
        Surface(color = EirDarkBg) {
            ExerciseDetailScreen(
                exercise = sampleDetailExercise.copy(done = true),
                restTimerSec = 32,
                isRestRunning = true,
                onBack = {},
                onToggleDone = {},
                onToggleRestTimer = {},
                onUpdateParameters = { _, _, _ -> }
            )
        }
    }
}
