package mx.edu.utez.eir_nasa_space_apps_challenge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*

@Composable
fun EirBottomBar(
    activeTab: String,
    isDarkMode: Boolean,
    onTabSelected: (String) -> Unit,
    onOpenCheckup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkMode || MaterialTheme.colorScheme.isDark
    val containerBg = MaterialTheme.colorScheme.surface
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeColor = MaterialTheme.colorScheme.primary

    // Alturas de diseño para que el botón central sobresalga limpiamente sin cortarse:
    // La barra inferior rectangular mide 64.dp (+ navigationBarsPadding).
    // El botón FAB (56.dp) tiene un margen superior de seguridad (8.dp) para que su sombra
    // y contorno circular nunca se recorten, y sobresale 22.dp por encima de la barra.
    val fabSize = 56.dp
    val barHeight = 64.dp
    val topSafetyMargin = 8.dp
    val fabProminence = 22.dp
    val barTopOffset = topSafetyMargin + fabProminence

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Barra inferior con fondo, elevación y borde
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = barTopOffset),
            color = containerBg,
            tonalElevation = 2.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(barHeight)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Salud
                    NavigationBarItem(
                        selected = activeTab == "salud",
                        onClick = { onTabSelected("salud") },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == "salud") Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(R.string.nav_health)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_health),
                                fontWeight = if (activeTab == "salud") FontWeight.Black else FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = activeColor,
                            selectedTextColor = activeColor,
                            indicatorColor = activeColor.copy(alpha = 0.15f),
                            unselectedIconColor = unselectedColor,
                            unselectedTextColor = unselectedColor
                        )
                    )

                    // 2. Hábitos
                    NavigationBarItem(
                        selected = activeTab == "habitos",
                        onClick = { onTabSelected("habitos") },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == "habitos") Icons.Filled.FitnessCenter else Icons.Outlined.FitnessCenter,
                                contentDescription = stringResource(R.string.nav_habits)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_habits),
                                fontWeight = if (activeTab == "habitos") FontWeight.Black else FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = activeColor,
                            selectedTextColor = activeColor,
                            indicatorColor = activeColor.copy(alpha = 0.15f),
                            unselectedIconColor = unselectedColor,
                            unselectedTextColor = unselectedColor
                        )
                    )

                    // Espacio central reservado para el botón flotante de Chequeo
                    Spacer(modifier = Modifier.weight(1.1f))

                    // 3. Bitácora
                    NavigationBarItem(
                        selected = activeTab == "bitacora",
                        onClick = { onTabSelected("bitacora") },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == "bitacora") Icons.Filled.AutoStories else Icons.Outlined.AutoStories,
                                contentDescription = stringResource(R.string.nav_logbook)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_logbook),
                                fontWeight = if (activeTab == "bitacora") FontWeight.Black else FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = activeColor,
                            selectedTextColor = activeColor,
                            indicatorColor = activeColor.copy(alpha = 0.15f),
                            unselectedIconColor = unselectedColor,
                            unselectedTextColor = unselectedColor
                        )
                    )

                    // 4. Agente IA
                    NavigationBarItem(
                        selected = activeTab == "agente",
                        onClick = { onTabSelected("agente") },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == "agente") Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                contentDescription = stringResource(R.string.nav_ai_agent)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_ai_agent),
                                fontWeight = if (activeTab == "agente") FontWeight.Black else FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = activeColor,
                            selectedTextColor = activeColor,
                            indicatorColor = activeColor.copy(alpha = 0.15f),
                            unselectedIconColor = unselectedColor,
                            unselectedTextColor = unselectedColor
                        )
                    )
                }
            }
        }

        // 2. Botón flotante central de Chequeo (+), sobresaliendo con sombra completa
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topSafetyMargin),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FloatingActionButton(
                onClick = onOpenCheckup,
                shape = CircleShape,
                containerColor = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp,
                    hoveredElevation = 8.dp
                ),
                modifier = Modifier.size(fabSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_start_daily_checkup),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = stringResource(R.string.nav_checkup),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = activeColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "EirBottomBar Light Mode")
@Composable
fun EirBottomBarLightPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        EirBottomBar(
            activeTab = "salud",
            isDarkMode = false,
            onTabSelected = {},
            onOpenCheckup = {}
        )
    }
}

@Preview(showBackground = true, name = "EirBottomBar Dark Mode")
@Composable
fun EirBottomBarDarkPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = true) {
        EirBottomBar(
            activeTab = "salud",
            isDarkMode = true,
            onTabSelected = {},
            onOpenCheckup = {}
        )
    }
}
