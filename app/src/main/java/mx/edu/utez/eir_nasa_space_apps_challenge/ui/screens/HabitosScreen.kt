package mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.utez.eir_nasa_space_apps_challenge.R
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
    val selectedEx = state.exercises.find { it.id == state.selectedExerciseId }
    val selectedMeal = state.dailyMeals.find { it.id == state.selectedMealId }

    if (selectedEx != null) {
        ExerciseDetailScreen(
            exercise = selectedEx,
            restTimerSec = if (state.activeRestExerciseId == selectedEx.id) state.restTimerSec else selectedEx.restSec,
            isRestRunning = state.isRestTimerRunning && state.activeRestExerciseId == selectedEx.id,
            onBack = { viewModel.selectExercise(null) },
            onToggleDone = { viewModel.toggleExercise(it) },
            onToggleRestTimer = { viewModel.toggleRestTimer(it) },
            onUpdateParameters = { id, sets, load -> viewModel.updateExerciseParameters(id, sets, load) },
            modifier = modifier
        )
    } else if (selectedMeal != null) {
        MealDetailScreen(
            meal = selectedMeal,
            allMeals = state.dailyMeals,
            onSelectMeal = { viewModel.selectMeal(it) },
            onToggleDone = { viewModel.toggleMeal(it) },
            onBack = { viewModel.selectMeal(null) },
            modifier = modifier
        )
    } else {
        HabitosScreenContent(
            state = state,
            onSetHabitFilter = { viewModel.setHabitFilter(it) },
            onAddWaterGlass = { viewModel.addWaterGlass() },
            onSetWaterGlasses = { viewModel.setWaterGlasses(it) },
            onToggleMeal = { viewModel.toggleMeal(it) },
            onSelectMeal = { viewModel.selectMeal(it) },
            onToggleWorkoutTimer = { viewModel.toggleWorkoutTimer() },
            onResetWorkoutTimer = { viewModel.resetWorkoutTimer() },
            onFinishWorkout = { viewModel.finishWorkout() },
            onToggleExercise = { viewModel.toggleExercise(it) },
            onSelectExercise = { viewModel.selectExercise(it) },
            onToggleRestTimer = { viewModel.toggleRestTimer(it) },
            modifier = modifier
        )
    }
}

@Composable
fun HabitosScreenContent(
    state: HabitosUiState,
    onSetHabitFilter: (String) -> Unit,
    onAddWaterGlass: () -> Unit,
    onSetWaterGlasses: (Int) -> Unit = {},
    onToggleMeal: (Long) -> Unit,
    onSelectMeal: (Long) -> Unit = {},
    onToggleWorkoutTimer: () -> Unit,
    onResetWorkoutTimer: () -> Unit,
    onFinishWorkout: () -> Unit,
    onToggleExercise: (Long) -> Unit,
    onSelectExercise: (Long) -> Unit,
    onToggleRestTimer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val cardBg = if (isDark) EirDarkSurface else EirLightSurface
    val cardBorder = if (isDark) EirDarkBorder else EirLightBorder
    val textPrimary = if (isDark) EirDarkTextPrimary else EirLightTextPrimary
    val textSecondary = if (isDark) EirDarkTextSecondary else EirLightTextSecondary

    var showRoutineDialog by remember { mutableStateOf(false) }

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
                        text = stringResource(R.string.habits_today),
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        letterSpacing = (-0.5).sp,
                        color = textPrimary
                    )
                    Text(
                        text = stringResource(R.string.habits_week_mission),
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
                        color = MaterialTheme.colorScheme.surfaceVariant,
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
                        color = MaterialTheme.colorScheme.surfaceVariant,
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
                        color = MaterialTheme.colorScheme.surfaceVariant,
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
                    "all" to stringResource(R.string.filter_all),
                    "food" to stringResource(R.string.filter_nutrition),
                    "exercise" to stringResource(R.string.filter_training)
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
                            selectedContainerColor = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
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
                    Text(
                        text = stringResource(R.string.summary_title),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = textPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Circular Progress Indicator (Donut Chart)
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val ratio = (state.energyIntake.toFloat() / state.energyGoal.toFloat()).coerceIn(0f, 1f)
                            val trackColor = MaterialTheme.colorScheme.outlineVariant
                            val ringColor = EirAmber

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = trackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = ringColor,
                                    startAngle = -90f,
                                    sweepAngle = ratio * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${state.energyIntake}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.summary_eaten),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                            }
                        }

                        // Remaining & Burned details column
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "${(state.energyGoal - state.energyIntake).coerceAtLeast(0)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.kcal_remaining),
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }

                            Column {
                                Text(
                                    text = "361",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.kcal_burned),
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }

                    // 3 Macros Linear Progress Bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MacroProgressBar(
                            name = stringResource(R.string.macro_carbs),
                            current = 145,
                            target = 220,
                            color = EirAmber,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        MacroProgressBar(
                            name = stringResource(R.string.macro_protein),
                            current = 98,
                            target = 130,
                            color = Color(0xFFEF4444),
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        MacroProgressBar(
                            name = stringResource(R.string.macro_fat),
                            current = 42,
                            target = 70,
                            color = EirSky,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // WATER TRACKER CARD (Registro de Agua)
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
                        text = stringResource(R.string.water_tracker_title),
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = textPrimary,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.water_label),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textPrimary
                        )
                        Text(
                            text = stringResource(R.string.water_target),
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

                    // Grid of 8 Water Glasses
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (i in 0 until 4) {
                                WaterGlassIcon(
                                    isFilled = i < state.waterGlasses,
                                    isLastFilled = (i == state.waterGlasses - 1 && i < state.waterGoal),
                                    isNextToAdd = (i == state.waterGlasses),
                                    onClick = {
                                        if (i == state.waterGlasses - 1) {
                                            onSetWaterGlasses(state.waterGlasses - 1)
                                        } else {
                                            onSetWaterGlasses(i + 1)
                                        }
                                    }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (i in 4 until 8) {
                                WaterGlassIcon(
                                    isFilled = i < state.waterGlasses,
                                    isLastFilled = (i == state.waterGlasses - 1 && i < state.waterGoal),
                                    isNextToAdd = (i == state.waterGlasses),
                                    onClick = {
                                        if (i == state.waterGlasses - 1) {
                                            onSetWaterGlasses(state.waterGlasses - 1)
                                        } else {
                                            onSetWaterGlasses(i + 1)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.water_from_food),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }

            // NUTRITION / DAILY MEALS CHECKLIST
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
                            text = stringResource(R.string.space_nutrition_title),
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = textPrimary
                        )

                        Text(
                            text = stringResource(R.string.view_detail),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EirEmerald,
                            modifier = Modifier.clickable {
                                val targetMeal = state.dailyMeals.firstOrNull { !it.done }
                                    ?: state.dailyMeals.firstOrNull()
                                targetMeal?.let { onSelectMeal(it.id) }
                            }
                        )
                    }

                    state.dailyMeals.forEach { meal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectMeal(meal.id) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (meal.done) EirEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
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
                                        text = "${meal.kcal} Cal • ${meal.mealType} ${meal.time}",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { onToggleMeal(meal.id) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (meal.done) EirEmerald else EirCharcoal)
                                ) {
                                    Icon(
                                        imageVector = if (meal.done) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = "Toggle",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.view_detail),
                                    tint = textSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SMART FIT STYLE WORKOUT ROUTINE SECTION (Screenshots 1 & 2 Replica)
        // =========================================================================
        if (state.habitFilter == "all" || state.habitFilter == "exercise") {

            // CHRONOMETER AT THE TOP (Exact Smart Fit Header: "00:00:02")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val hrs = state.workoutTimerSec / 3600
                    val mins = (state.workoutTimerSec % 3600) / 60
                    val secs = state.workoutTimerSec % 60
                    val timerDisplay = String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs, mins, secs)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timerDisplay,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            letterSpacing = 1.sp,
                            color = textPrimary,
                            modifier = Modifier.clickable { onToggleWorkoutTimer() }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (state.isWorkoutRunning) EirEmerald.copy(alpha = 0.2f) else textSecondary.copy(alpha = 0.2f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (state.isWorkoutRunning) EirEmerald else textSecondary, CircleShape)
                            )
                        }
                        Text(
                            text = if (state.isWorkoutRunning) stringResource(R.string.chronometer_active) else stringResource(R.string.chronometer_paused),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (state.isWorkoutRunning) EirEmerald else textSecondary
                        )
                    }
                }
            }

            // ROUTINE TITLE AND "Editar rutina" LINK
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.routineName,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    color = textPrimary,
                    letterSpacing = (-0.5).sp
                )

                Text(
                    text = stringResource(R.string.edit_routine),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EirAmber,
                    modifier = Modifier.clickable { showRoutineDialog = true }
                )
            }

            // LIST OF EXERCISE CARDS (Smart Fit Replica)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                state.exercises.forEach { ex ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = cardBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (ex.done) EirEmerald.copy(alpha = 0.4f) else cardBorder
                        ),
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // TOP ROW: Checkbox | Thumbnail | Title & Sets/Reps | Chevron >
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Rounded square checkbox (Smart Fit style)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (ex.done) EirAmber else Color.Transparent
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = if (ex.done) EirAmber else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onToggleExercise(ex.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (ex.done) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.completed),
                                            tint = EirCharcoal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Exercise Thumbnail Image
                                Surface(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onSelectExercise(ex.id) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                                ) {
                                    val thumbRes = ex.thumbRes ?: R.drawable.ex_space_squats
                                    Image(
                                        painter = painterResource(id = thumbRes),
                                        contentDescription = ex.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Title and Sets & Reps
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectExercise(ex.id) },
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = ex.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        maxLines = 2,
                                        lineHeight = 18.sp,
                                        textDecoration = if (ex.done) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    Text(
                                        text = ex.setsReps,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textSecondary
                                    )
                                }

                                // Right Chevron '>' button to open Detail
                                IconButton(
                                    onClick = { onSelectExercise(ex.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = stringResource(R.string.view_detail),
                                        tint = textSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Thin Divider
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp
                            )

                            // BOTTOM ROW: Rest Timer "00:45 (Descanso entre series)" + Play/Pause ▶
                            val isThisExerciseResting = state.isRestTimerRunning && state.activeRestExerciseId == ex.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val restDisplay = if (isThisExerciseResting) {
                                    String.format(java.util.Locale.US, "%02d:%02d", state.restTimerSec / 60, state.restTimerSec % 60)
                                } else {
                                    String.format(java.util.Locale.US, "%02d:%02d", ex.restSec / 60, ex.restSec % 60)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.rest_between_sets, restDisplay),
                                        fontSize = 12.sp,
                                        fontWeight = if (isThisExerciseResting) FontWeight.Black else FontWeight.SemiBold,
                                        color = if (isThisExerciseResting) EirEmerald else textSecondary
                                    )
                                }

                                IconButton(
                                    onClick = { onToggleRestTimer(ex.id) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isThisExerciseResting) EirEmerald else MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = if (isThisExerciseResting) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.rest_label),
                                        tint = if (isThisExerciseResting) Color.White else textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // BIG YELLOW/AMBER BOTTOM BUTTON ("Finalizar entrenamiento" - Screenshot 1)
            Button(
                onClick = onFinishWorkout,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.workoutCompleted) EirEmerald else EirAmber,
                    contentColor = if (state.workoutCompleted) Color.White else EirCharcoal
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = if (state.workoutCompleted) stringResource(R.string.workout_finished) else stringResource(R.string.finish_workout),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 0.2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ROUTINE SELECTOR DIALOG
    if (showRoutineDialog) {
        AlertDialog(
            onDismissRequest = { showRoutineDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.select_routine_dialog_title),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        stringResource(R.string.routine_legs_title) to stringResource(R.string.routine_legs_desc),
                        stringResource(R.string.routine_upper_title) to stringResource(R.string.routine_upper_desc),
                        stringResource(R.string.routine_cardio_title) to stringResource(R.string.routine_cardio_desc)
                    ).forEach { (name, desc) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showRoutineDialog = false
                                },
                            color = if (state.routineName == name) EirAmber.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = textPrimary
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoutineDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
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
    val trackBg = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            color = MaterialTheme.colorScheme.onSurface
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
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(
                id = if (isFilled) R.drawable.ic_glass_water_full else R.drawable.ic_glass_water_empty
            ),
            contentDescription = if (isFilled) stringResource(R.string.glass_full) else stringResource(R.string.glass_empty),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        if (isLastFilled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(EirEmerald),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.completed),
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        } else if (isNextToAdd) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(EirEmerald.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_glass),
                    tint = EirEmerald,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

val sampleMeals: List<Meal>
    get() = listOf(
    Meal(1L, "Desayuno energético + Vitamina D", 550, true, "Desayuno", "07:30", 28, 68, 16, 9, R.drawable.meal_breakfast),
    Meal(2L, "Comida principal rica en proteína", 700, true, "Almuerzo", "13:00", 46, 72, 22, 11, R.drawable.meal_lunch),
    Meal(3L, "Colación de frutos secos y calcio", 500, true, "Colación", "17:00", 18, 44, 26, 7, R.drawable.meal_snack),
    Meal(4L, "Cena ligera + Sobres de minerales", 650, false, "Cena", "20:30", 30, 62, 20, 8, R.drawable.meal_dinner)
)

val sampleExercises: List<Exercise>
    get() = listOf(
    Exercise(
        id = 1L,
        name = "Sentadillas con ligas de resistencia",
        setsReps = "4x 15 a 20",
        loadKg = "35",
        restSec = 45,
        why = "Mantiene fuertes los huesos de cadera y piernas sin gravedad",
        duration = "20 min",
        done = true,
        imageRes = R.drawable.ex_space_squats,
        thumbRes = R.drawable.ex_space_squats,
        category = "Piernas y Cadera"
    ),
    Exercise(
        id = 2L,
        name = "Caminadora espacial con arnés",
        setsReps = "Intervalos 15 min",
        loadKg = "65% masa",
        restSec = 60,
        why = "Cuida tu corazón y evita que pierdas condición física",
        duration = "15 min",
        done = false,
        imageRes = R.drawable.ex_space_treadmill,
        thumbRes = R.drawable.ex_space_treadmill,
        category = "Cardio y Resistencia"
    ),
    Exercise(
        id = 3L,
        name = "Remo de espalda y abdomen",
        setsReps = "3x 12 a 15",
        loadKg = "25",
        restSec = 45,
        why = "Evita dolor de espalda baja por flotar todo el día",
        duration = "10 min",
        done = false,
        imageRes = R.drawable.ex_space_rower,
        thumbRes = R.drawable.ex_space_rower,
        category = "Espalda y Core"
    )
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
                onSelectMeal = {},
                onToggleWorkoutTimer = {},
                onResetWorkoutTimer = {},
                onFinishWorkout = {},
                onToggleExercise = {},
                onSelectExercise = {},
                onToggleRestTimer = {}
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
                onSelectMeal = {},
                onToggleWorkoutTimer = {},
                onResetWorkoutTimer = {},
                onFinishWorkout = {},
                onToggleExercise = {},
                onSelectExercise = {},
                onToggleRestTimer = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Smart Fit Workout Routine Light Preview", heightDp = 750)
@Composable
fun HabitosWorkoutSmartFitLightPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        Surface(color = EirLightBg) {
            HabitosScreenContent(
                state = HabitosUiState(
                    habitFilter = "exercise",
                    workoutTimerSec = 2,
                    isWorkoutRunning = true,
                    routineName = "Piernas",
                    exercises = sampleExercises,
                    activeRestExerciseId = 1L,
                    isRestTimerRunning = true,
                    restTimerSec = 45
                ),
                onSetHabitFilter = {},
                onAddWaterGlass = {},
                onToggleMeal = {},
                onSelectMeal = {},
                onToggleWorkoutTimer = {},
                onResetWorkoutTimer = {},
                onFinishWorkout = {},
                onToggleExercise = {},
                onSelectExercise = {},
                onToggleRestTimer = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Smart Fit Workout Routine Dark Preview", backgroundColor = 0xFF0F172A, heightDp = 750)
@Composable
fun HabitosWorkoutSmartFitDarkPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = true) {
        Surface(color = EirDarkBg) {
            HabitosScreenContent(
                state = HabitosUiState(
                    habitFilter = "exercise",
                    workoutTimerSec = 2,
                    isWorkoutRunning = true,
                    routineName = "Piernas",
                    exercises = sampleExercises,
                    activeRestExerciseId = 2L,
                    isRestTimerRunning = true,
                    restTimerSec = 38
                ),
                onSetHabitFilter = {},
                onAddWaterGlass = {},
                onToggleMeal = {},
                onSelectMeal = {},
                onToggleWorkoutTimer = {},
                onResetWorkoutTimer = {},
                onFinishWorkout = {},
                onToggleExercise = {},
                onSelectExercise = {},
                onToggleRestTimer = {}
            )
        }
    }
}
