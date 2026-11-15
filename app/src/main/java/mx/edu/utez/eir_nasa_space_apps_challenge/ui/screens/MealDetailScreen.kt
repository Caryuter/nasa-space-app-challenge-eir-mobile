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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.Meal
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.MealFoodItem
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*

@Composable
fun MealDetailScreen(
    meal: Meal,
    allMeals: List<Meal>,
    onSelectMeal: (Long) -> Unit,
    onToggleDone: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val bg = MaterialTheme.colorScheme.background
    val cardBg = if (isDark) EirDarkSurface else EirLightSurface
    val cardBorder = if (isDark) EirDarkBorder else EirLightBorder
    val textPrimary = if (isDark) EirDarkTextPrimary else EirLightTextPrimary
    val textSecondary = if (isDark) EirDarkTextSecondary else EirLightTextSecondary

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = textPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.nutrition_detail),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textPrimary
                )
                Text(
                    text = stringResource(R.string.mission_subtitle),
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            // Quick Status Chip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (meal.done) EirEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (meal.done) EirEmerald.copy(alpha = 0.4f) else cardBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (meal.done) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.consumed),
                            tint = EirEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = if (meal.done) stringResource(R.string.consumed) else stringResource(R.string.pending),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (meal.done) EirEmerald else textSecondary
                    )
                }
            }
        }

        // MEAL SELECTOR TABS / CHIPS (Real nutrition app meal carousel: Desayuno, Almuerzo, Colación, Cena)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allMeals.forEach { item ->
                val isSelected = item.id == meal.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectMeal(item.id) },
                    label = {
                        Text(
                            text = item.mealType,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = if (item.done) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.done),
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) Color.White else EirEmerald
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EirEmerald,
                        selectedLabelColor = Color.White,
                        containerColor = cardBg,
                        labelColor = textSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = cardBorder,
                        selectedBorderColor = EirEmerald
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // MAIN SCROLLABLE CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HERO IMAGE CONTAINER
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                shadowElevation = 2.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val imgRes = meal.imageRes ?: R.drawable.meal_breakfast
                    Image(
                        painter = painterResource(id = imgRes),
                        contentDescription = meal.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient overlay at bottom for title legibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.75f)
                                    ),
                                    startY = 100f
                                )
                            )
                    )

                    // Time and Calorie pill overlay on bottom left
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = meal.time,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EirEmerald.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "${meal.kcal} kcal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // MEAL NAME & SUBTITLE
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = meal.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = textPrimary,
                    lineHeight = 28.sp
                )
                Text(
                    text = stringResource(R.string.menu_calculated_subtitle, meal.mealType),
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }

            // MACRONUTRIENT DASHBOARD (Real nutrition apps style: Lifesum / MyFitnessPal)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.energy_and_macros),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = stringResource(R.string.kcal_per_day, meal.kcal),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EirEmerald
                        )
                    }

                    // Multi-segmented colored macro bar
                    val totalMacroGrams = (meal.proteinGrams + meal.carbsGrams + meal.fatGrams).coerceAtLeast(1)
                    val proteinFraction = meal.proteinGrams.toFloat() / totalMacroGrams
                    val carbsFraction = meal.carbsGrams.toFloat() / totalMacroGrams
                    val fatFraction = meal.fatGrams.toFloat() / totalMacroGrams

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(proteinFraction.coerceAtLeast(0.05f))
                                .fillMaxHeight()
                                .background(Color(0xFF38BDF8))
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .weight(carbsFraction.coerceAtLeast(0.05f))
                                .fillMaxHeight()
                                .background(Color(0xFFFBBF24))
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .weight(fatFraction.coerceAtLeast(0.05f))
                                .fillMaxHeight()
                                .background(Color(0xFFF43F5E))
                        )
                    }

                    // 4 Macro Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MacroBadgeItem(
                            label = stringResource(R.string.macro_protein),
                            grams = "${meal.proteinGrams}g",
                            percentage = "${(proteinFraction * 100).toInt()}%",
                            color = Color(0xFF38BDF8),
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        MacroBadgeItem(
                            label = stringResource(R.string.macro_carbs),
                            grams = "${meal.carbsGrams}g",
                            percentage = "${(carbsFraction * 100).toInt()}%",
                            color = Color(0xFFFBBF24),
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        MacroBadgeItem(
                            label = stringResource(R.string.macro_fat),
                            grams = "${meal.fatGrams}g",
                            percentage = "${(fatFraction * 100).toInt()}%",
                            color = Color(0xFFF43F5E),
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        MacroBadgeItem(
                            label = stringResource(R.string.macro_fiber),
                            grams = "${meal.fiberGrams}g",
                            percentage = stringResource(R.string.optimal),
                            color = EirEmerald,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
            }

            // SPACE HEALTH & HUMAN TRANSLATION ("Cero Tecnicismos" - SOLIS UI Standard)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF0F1E2E) else Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(1.dp, EirEmerald.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🛡️", fontSize = 16.sp)
                        Text(
                            text = stringResource(R.string.why_body_needs_food),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) EirEmerald else Color(0xFF166534)
                        )
                    }
                    Text(
                        text = meal.whySpace,
                        fontSize = 12.sp,
                        color = textPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            // PREPARATION METHOD IN STATION (Microgravity Station protocol)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "💧", fontSize = 18.sp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.prep_method_station),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = meal.prepInfo,
                            fontSize = 12.sp,
                            color = textSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // FOOD ITEMS / INGREDIENTS LIST
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.portions_and_food_included),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textPrimary
                    )
                    Text(
                        text = stringResource(R.string.portions_count, meal.items.size),
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (meal.items.isEmpty()) {
                            Text(
                                text = stringResource(R.string.meal_empty_ration),
                                fontSize = 12.sp,
                                color = textSecondary,
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            meal.items.forEachIndexed { index, food ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = food.icon, fontSize = 16.sp)
                                        }

                                        Column {
                                            Text(
                                                text = food.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textPrimary
                                            )
                                            Text(
                                                text = food.portion,
                                                fontSize = 11.sp,
                                                color = textSecondary
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${food.kcal} cal",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (food.kcal > 0) textPrimary else textSecondary
                                    )
                                }

                                if (index < meal.items.lastIndex) {
                                    HorizontalDivider(
                                        color = cardBorder.copy(alpha = 0.5f),
                                        thickness = 0.8.dp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ACTION CTA: TOGGLE MEAL CONSUMPTION
            Button(
                onClick = { onToggleDone(meal.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (meal.done) MaterialTheme.colorScheme.surfaceVariant else EirEmerald,
                    contentColor = if (meal.done) textPrimary else Color.White
                ),
                border = if (meal.done) androidx.compose.foundation.BorderStroke(1.5.dp, EirEmerald) else null
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (meal.done) Icons.Default.CheckCircle else Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = if (meal.done) EirEmerald else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (meal.done) stringResource(R.string.meal_registered_unmark) else stringResource(R.string.mark_as_consumed),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MacroBadgeItem(
    label: String,
    grams: String,
    percentage: String,
    color: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary
            )
        }
        Text(
            text = grams,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = textPrimary
        )
        Text(
            text = percentage,
            fontSize = 10.sp,
            color = textSecondary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MealDetailScreenPreview() {
    val sample = Meal(
        id = 1L,
        name = "Desayuno energético + Vitamina D",
        kcal = 550,
        done = true,
        mealType = "Desayuno",
        time = "07:30",
        proteinGrams = 28,
        carbsGrams = 68,
        fatGrams = 16,
        fiberGrams = 9,
        imageRes = R.drawable.meal_breakfast,
        whySpace = "Protege tus huesos de la pérdida acelerada de calcio en microgravedad.",
        prepInfo = "Inyecta 150 ml de agua caliente a 70°C en la boquilla A. Deja reposar 4 minutos.",
        items = listOf(
            MealFoodItem("Avena liofilizada con chía", "1 paquete (60g)", 240, "🥣"),
            MealFoodItem("Frutas del bosque deshidratadas", "1 porción (45g)", 140, "🫐")
        )
    )
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        MealDetailScreen(
            meal = sample,
            allMeals = listOf(sample),
            onSelectMeal = {},
            onToggleDone = {},
            onBack = {}
        )
    }
}
