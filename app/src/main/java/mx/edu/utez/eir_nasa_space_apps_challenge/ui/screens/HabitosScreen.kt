package mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val cardBorder = if (isDark) EirDarkBorder else EirLightBorder
    val textPrimary = if (isDark) EirDarkTextPrimary else EirLightTextPrimary
    val textSecondary = if (isDark) EirDarkTextSecondary else EirLightTextSecondary

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP HEADER: STREAK BADGES & TITLE
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hoy",
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        letterSpacing = (-0.5).sp,
                        color = textPrimary
                    )
                    Text(
                        text = "Semana 175 • Misión Ares IV",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }

                // Top Streak & Stats Badges (as seen in screenshot 3)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "💎", fontSize = 12.sp)
                            Text(
                                text = "2000",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🔥", fontSize = 12.sp)
                            Text(
                                text = "135",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "💼", fontSize = 12.sp)
                        }
                    }
                }
            }

            // FILTER CHIPS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "all" to "Todo",
                    "food" to "Nutrición y Agua",
                    "exercise" to "Actividades"
                ).forEach { (id, label) ->
                    val selected = state.habitFilter == id
                    FilterChip(
                        selected = selected,
                        onClick = { onSetHabitFilter(id) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EirCharcoal,
                            selectedLabelColor = Color.White,
                            containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            labelColor = textSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // FOOD & WATER SECTION
        if (state.habitFilter == "all" || state.habitFilter == "food") {

            // SUMMARY CARD (Kcal & Macros Ring - Screenshot 3 Style)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Summary",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = textPrimary
                        )

                        Text(
                            text = "Details",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EirEmerald,
                            modifier = Modifier.clickable { }
                        )
                    }

                    // Progress Ring + Eaten / Remaining / Burned
                    val remainingKcal = (state.energyGoal - state.energyIntake).coerceAtLeast(0)
                    val progressRatio = (state.energyIntake.toFloat() / state.energyGoal).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Eaten
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.energyIntake}",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = textPrimary
                            )
                            Text(
                                text = "Eaten",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = textSecondary
                            )
                        }

                        // Center: Semi-circular/Circular Progress Ring
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val ringTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                            val ringFillColor = EirEmerald

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 10.dp.toPx()
                                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                                // Background Arc (240 degrees)
                                drawArc(
                                    color = ringTrackColor,
                                    startAngle = 150f,
                                    sweepAngle = 240f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                // Progress Arc
                                drawArc(
                                    color = ringFillColor,
                                    startAngle = 150f,
                                    sweepAngle = 240f * progressRatio,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$remainingKcal",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Remaining",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textSecondary
                                )
                            }
                        }

                        // Right: Burned
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "142",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = textPrimary
                            )
                            Text(
                                text = "Burned",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = textSecondary
                            )
                        }
                    }

                    // Macronutrients Progress Bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MacroProgressBar(
                            name = "Carbs",
                            current = 97,
                            target = 207,
                            color = EirEmerald,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        MacroProgressBar(
                            name = "Protein",
                            current = 46,
                            target = 115,
                            color = EirEmerald,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        MacroProgressBar(
                            name = "Fat",
                            current = 50,
                            target = 61,
                            color = EirEmerald,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // WATER TRACKER CARD (Registro de Agua - Screenshot 1 Replica)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Registro de agua",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = textPrimary,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Agua",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "Objetivo: 2,20 litros",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        val litersVal = String.format(java.util.Locale.US, "%.2f L", state.waterGlasses * 0.355f)
                        Text(
                            text = litersVal,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = textPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Grid of 8 Water Glasses (as in screenshot 1)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Row 1 (4 glasses)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (i in 0 until 4) {
                                WaterGlassIcon(
                                    isFilled = i < state.waterGlasses,
                                    isLastFilled = (i == state.waterGlasses - 1 && i < state.waterGoal),
                                    isNextToAdd = (i == state.waterGlasses),
                                    onClick = onAddWaterGlass
                                )
                            }
                        }
                        // Row 2 (4 glasses)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (i in 4 until 8) {
                                WaterGlassIcon(
                                    isFilled = i < state.waterGlasses,
                                    isLastFilled = (i == state.waterGlasses - 1 && i < state.waterGoal),
                                    isNextToAdd = (i == state.waterGlasses),
                                    onClick = onAddWaterGlass
                                )
                            }
                        }
                    }

                    Text(
                        text = "+ Agua de los alimentos: 0 mL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }

            // NUTRITION / DAILY MEALS CHECKLIST (Screenshot 3 Style)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nutrition",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = textPrimary
                        )

                        Text(
                            text = "More",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EirEmerald,
                            modifier = Modifier.clickable { }
                        )
                    }

                    state.dailyMeals.forEach { meal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleMeal(meal.id) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Circular Progress Icon for Meal
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (meal.done) EirEmerald.copy(alpha = 0.15f) else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (meal.done) Icons.Default.Check else Icons.Default.Restaurant,
                                        contentDescription = meal.name,
                                        tint = if (meal.done) EirEmerald else textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = meal.name.take(24),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (meal.done) textSecondary else textPrimary,
                                        textDecoration = if (meal.done) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    Text(
                                        text = "${meal.kcal} / ${meal.kcal + 100} Cal",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFEDE9FE)
                                        ) {
                                            Text(
                                                text = "AI",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF7C3AED),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Text(
                                            text = meal.name,
                                            fontSize = 10.sp,
                                            color = textSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Black Plus Action Button
                            IconButton(
                                onClick = { onToggleMeal(meal.id) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EirCharcoal)
                            ) {
                                Icon(
                                    imageVector = if (meal.done) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = "Toggle",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // EXERCISE & ACTIVITIES SECTION
        if (state.habitFilter == "all" || state.habitFilter == "exercise") {

            // ACTIVITIES CARD (Actividades - Screenshot 1 Replica)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Actividades",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = textPrimary
                        )

                        Text(
                            text = "Más",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EirEmerald,
                            modifier = Modifier.clickable { }
                        )
                    }

                    // Steps / Treadmill Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFFAFAFA),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "👟",
                                    fontSize = 28.sp
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Pasos",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Registro automático",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textSecondary
                                    )
                                }

                                Text(
                                    text = "👾",
                                    fontSize = 28.sp
                                )
                            }

                            // Dark Primary CTA Button "Conectar"
                            Button(
                                onClick = onToggleTreadmill,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EirCharcoal,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text(
                                    text = if (state.isTreadmillRunning) "Pausar" else "Conectar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }

                            Text(
                                text = "Registrar pasos manualmente",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EirEmerald,
                                modifier = Modifier.clickable { }
                            )
                        }
                    }

                    // Quick Add Row (Añadir + 361 kcal)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Add Button Card
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = cardBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = "Añadir",
                                        tint = textPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Añadir",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                        }

                        // Burned Kcal Card
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = cardBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "🏃", fontSize = 24.sp)
                                }
                            }
                            Text(
                                text = "361 kcal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                        }
                    }

                    // EXERCISES LIST CHECKLIST
                    state.exercises.forEach { ex ->
                        OutlinedCard(
                            onClick = { onToggleExercise(ex.id) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
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
                                        tint = if (ex.done) EirEmerald else textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Column {
                                        Text(
                                            text = ex.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            textDecoration = if (ex.done) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                        Text(
                                            text = ex.why,
                                            fontSize = 10.sp,
                                            color = textSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = ex.duration,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EirEmerald
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroProgressBar(
    name: String,
    current: Int,
    target: Int,
    color: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = (current.toFloat() / target).coerceIn(0f, 1f)
    val trackBg = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = trackBg
        )

        Text(
            text = "$current / $target g",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) EirDarkTextPrimary else EirLightTextPrimary
        )
    }
}

@Composable
private fun WaterGlassIcon(
    isFilled: Boolean,
    isLastFilled: Boolean,
    isNextToAdd: Boolean,
    onClick: () -> Unit
) {
    val glassColor = if (isFilled) Color(0xFF38BDF8) else Color(0xFFE0F2FE)
    val borderColor = if (isFilled) Color(0xFF0284C7) else Color(0xFFBAE6FD)

    Box(
        modifier = Modifier
            .width(36.dp)
            .height(52.dp)
            .clip(
                RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 4.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                )
            )
            .background(glassColor)
            .border(
                1.dp,
                borderColor,
                RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 4.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLastFilled) {
            // Green Checkmark badge on the last filled glass (Screenshot 1)
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(EirEmerald),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Complete",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        } else if (isNextToAdd) {
            // Plus symbol for next glass to add
            Text(
                text = "+",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = EirCharcoal
            )
        }
    }
}

val sampleMeals = listOf(
    Meal(1L, "Desayuno energético + Vitamina D", 550, true),
    Meal(2L, "Comida principal rica en proteína", 700, true),
    Meal(3L, "Colación de frutos secos y calcio", 500, true),
    Meal(4L, "Cena ligera + Sobres de minerales", 650, false)
)

val sampleExercises = listOf(
    Exercise(1L, "Sentadillas con ligas de resistencia", "Mantiene fuertes los huesos", "20 min", true),
    Exercise(2L, "Caminadora espacial con arnés", "Cuida tu corazón y condición", "15 min", false),
    Exercise(3L, "Remo de espalda y abdomen", "Evita dolor de espalda baja", "10 min", false)
)

@Preview(showBackground = true, name = "HabitosScreen Light Preview", heightDp = 950)
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

@Preview(showBackground = true, name = "HabitosScreen Dark Preview", backgroundColor = 0xFF0F172A, heightDp = 950)
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
