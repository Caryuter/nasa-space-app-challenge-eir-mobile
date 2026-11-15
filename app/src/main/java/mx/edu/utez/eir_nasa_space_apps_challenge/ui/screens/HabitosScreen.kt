package mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.Exercise
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.Meal
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.HabitosUiState
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.HabitosViewModel

@Composable
fun HabitosScreen(
    state: HabitosUiState,
    viewModel: HabitosViewModel,
    modifier: Modifier = Modifier
) {
    HabitosScreenContent(
        state = state,
        onSetHabitFilter = { viewModel.setHabitFilter(it) },
        onAddWaterGlass = { viewModel.addWaterGlass() },
        onToggleMeal = { viewModel.toggleMeal(it) },
        onToggleTreadmill = { viewModel.toggleTreadmill() },
        onResetTreadmill = { viewModel.resetTreadmill() },
        onToggleExercise = { viewModel.toggleExercise(it) },
        modifier = modifier
    )
}

@Composable
fun HabitosScreenContent(
    state: HabitosUiState,
    onSetHabitFilter: (String) -> Unit,
    onAddWaterGlass: () -> Unit,
    onToggleMeal: (Long) -> Unit,
    onToggleTreadmill: () -> Unit,
    onResetTreadmill: () -> Unit,
    onToggleExercise: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = false
    val cardBg = if (isDark) EirDarkSurface else EirLightSurface

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        Column {
            Text(
                text = "Cuidado Diario en Órbita",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
            Text(
                text = "Mantén tu cuerpo fuerte mientras no hay gravedad",
                fontSize = 12.sp,
                color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "all" to "Todo",
                "food" to "Comida y Agua",
                "exercise" to "Ejercicio"
            ).forEach { (id, label) ->
                FilterChip(
                    selected = state.habitFilter == id,
                    onClick = { onSetHabitFilter(id) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (state.habitFilter == id) FontWeight.Black else FontWeight.Bold
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EirSky,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // FOOD & WATER SECTION
        if (state.habitFilter == "all" || state.habitFilter == "food") {
            // Energy Intake Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0284C7), Color(0xFF4338CA))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Energía consumida hoy",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE0F2FE)
                    )

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${state.energyIntake}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Default,
                            color = Color.White
                        )
                        Text(
                            text = "/ ${state.energyGoal} kcal diarias",
                            fontSize = 12.sp,
                            color = Color(0xFFBAE6FD),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    val progress = (state.energyIntake.toFloat() / state.energyGoal).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = EirEmerald,
                        trackColor = Color.Black.copy(alpha = 0.2f)
                    )

                    Text(
                        text = "En el espacio es común perder el apetito; cumple tus 4 comidas para no perder músculo.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Color(0xFFE0F2FE),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Water Intake Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = cardBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Agua",
                                tint = EirSky,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Vasos de Agua Hoy",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = "${state.waterGlasses} de ${state.waterGoal} vasos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = EirSky
                        )
                    }

                    // 6 Water Glasses Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 0 until state.waterGoal) {
                            val isFilled = i < state.waterGlasses
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 6.dp,
                                            topEnd = 6.dp,
                                            bottomStart = 12.dp,
                                            bottomEnd = 12.dp
                                        )
                                    )
                                    .background(
                                        if (isFilled) EirSky
                                        else if (isDark) Color(0xFF0F172A)
                                        else Color(0xFFF1F5F9)
                                    )
                                    .border(
                                        1.dp,
                                        if (isFilled) EirSkyLight
                                        else if (isDark) EirDarkBorder
                                        else Color(0xFFCBD5E1),
                                        RoundedCornerShape(
                                            topStart = 6.dp,
                                            topEnd = 6.dp,
                                            bottomStart = 12.dp,
                                            bottomEnd = 12.dp
                                        )
                                    )
                                    .clickable { onAddWaterGlass() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Vaso ${i + 1}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFilled) Color.White else {
                                        if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onAddWaterGlass,
                        enabled = state.waterGlasses < state.waterGoal,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EirSky.copy(alpha = 0.15f),
                            contentColor = EirSky
                        )
                    ) {
                        Text(text = "+ Tomé 1 vaso de agua", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Daily Meals Checklist
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
                        text = "COMIDAS Y VITAMINAS DEL DÍA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                    )

                    state.dailyMeals.forEach { meal ->
                        OutlinedCard(
                            onClick = { onToggleMeal(meal.id) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (meal.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Estado",
                                        tint = if (meal.done) EirEmerald else if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Text(
                                        text = meal.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = if (meal.done) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (meal.done) {
                                            if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                                        } else {
                                            if (isDark) EirDarkTextPrimary else EirLightTextPrimary
                                        }
                                    )
                                }

                                Text(
                                    text = "${meal.kcal} kcal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // EXERCISE SECTION
        if (state.habitFilter == "all" || state.habitFilter == "exercise") {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = cardBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PROTECCIÓN DE HUESOS Y MÚSCULOS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = EirTeal
                            )
                            Text(
                                text = "Caminadora con Arnés",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        val mins = (state.treadmillTimerSec / 60).toString().padStart(2, '0')
                        val secs = (state.treadmillTimerSec % 60).toString().padStart(2, '0')
                        Text(
                            text = "$mins:$secs",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Space Treadmill Graphic Container
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF0FDF4)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Canvas(modifier = Modifier.size(width = 110.dp, height = 60.dp)) {
                                val strokeW = 3.dp.toPx()
                                // Treadmill band
                                drawLine(
                                    color = Color(0xFF0D9488),
                                    start = Offset(10f, size.height - 10f),
                                    end = Offset(size.width - 10f, size.height - 20f),
                                    strokeWidth = strokeW,
                                    cap = StrokeCap.Round
                                )
                                // Elastic Harness Lines
                                drawLine(
                                    color = Color(0xFF38BDF8),
                                    start = Offset(30f, size.height - 15f),
                                    end = Offset(50f, size.height / 2f),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )
                                drawLine(
                                    color = Color(0xFF38BDF8),
                                    start = Offset(80f, size.height - 20f),
                                    end = Offset(60f, size.height / 2f),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )
                                // Running Astronaut Body
                                drawCircle(color = Color(0xFF0EA5E9), radius = 6.dp.toPx(), center = Offset(55f, 15f))
                                drawLine(color = Color(0xFF0EA5E9), start = Offset(55f, 20f), end = Offset(55f, 38f), strokeWidth = 3.dp.toPx())
                                drawLine(color = Color(0xFF0EA5E9), start = Offset(55f, 38f), end = Offset(40f, 52f), strokeWidth = 2.5.dp.toPx())
                                drawLine(color = Color(0xFF0EA5E9), start = Offset(55f, 38f), end = Offset(68f, 50f), strokeWidth = 2.5.dp.toPx())
                            }

                            Text(
                                text = "Las ligas te sujetan a la banda para simular el peso de la Tierra y mantener tus huesos firmes.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.weight(1f).padding(start = 10.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onToggleTreadmill,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EirTeal)
                        ) {
                            Icon(
                                imageVector = if (state.isTreadmillRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Timer",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.isTreadmillRunning) "Pausar" else "Iniciar Caminadora",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }

                        IconButton(
                            onClick = onResetTreadmill,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reiniciar",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Exercises Checklist
                    state.exercises.forEach { ex ->
                        OutlinedCard(
                            onClick = { onToggleExercise(ex.id) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (ex.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Estado",
                                        tint = if (ex.done) EirTeal else if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Column {
                                        Text(
                                            text = ex.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (ex.done) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                        Text(
                                            text = ex.why,
                                            fontSize = 10.sp,
                                            color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = ex.duration,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

val sampleMeals = listOf(
    Meal(1L, "Desayuno energético + Vitamina D (para huesos)", 550, true),
    Meal(2L, "Comida principal rica en proteína", 700, true),
    Meal(3L, "Colación de frutos secos y calcio", 500, true),
    Meal(4L, "Cena ligera + Sobres de sales minerales", 650, false)
)

val sampleExercises = listOf(
    Exercise(1L, "Sentadillas con ligas de resistencia", "Mantiene fuertes los huesos de cadera y piernas", "20 min", true),
    Exercise(2L, "Caminadora espacial con arnés", "Cuida tu corazón y evita que pierdas condición", "15 min", false),
    Exercise(3L, "Remo de espalda y abdomen", "Evita dolor de espalda baja por flotar", "10 min", false)
)

@Preview(showBackground = true, name = "HabitosScreen Light Preview", heightDp = 900)
@Composable
fun HabitosScreenLightPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        Surface(color = EirLightBg) {
            HabitosScreenContent(
                state = HabitosUiState(dailyMeals = sampleMeals, exercises = sampleExercises),
                onSetHabitFilter = {},
                onAddWaterGlass = {},
                onToggleMeal = {},
                onToggleTreadmill = {},
                onResetTreadmill = {},
                onToggleExercise = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "HabitosScreen Dark Preview", backgroundColor = 0xFF0B1120, heightDp = 900)
@Composable
fun HabitosScreenDarkPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = true) {
        Surface(color = EirDarkBg) {
            HabitosScreenContent(
                state = HabitosUiState(dailyMeals = sampleMeals, exercises = sampleExercises),
                onSetHabitFilter = {},
                onAddWaterGlass = {},
                onToggleMeal = {},
                onToggleTreadmill = {},
                onResetTreadmill = {},
                onToggleExercise = {}
            )
        }
    }
}
