package mx.edu.utez.eir_nasa_space_apps_challenge.ui.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*

@Composable
fun EirBottomBar(
    activeTab: String,
    isDarkMode: Boolean,
    onTabSelected: (String) -> Unit,
    onOpenCheckup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerBg = if (isDarkMode) EirDarkHeader else EirLightHeader
    val unselectedColor = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
    val activeColor = EirEmerald

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerBg,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(82.dp)
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(68.dp),
                color = containerBg,
                tonalElevation = 0.dp
            ) {
                NavigationBar(
                    containerColor = containerBg,
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
                            contentDescription = "Salud"
                        )
                    },
                    label = {
                        Text(
                            text = "Salud",
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
                            contentDescription = "Hábitos"
                        )
                    },
                    label = {
                        Text(
                            text = "Hábitos",
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

                // Spacer placeholder for central elevated Checkup button
                Spacer(modifier = Modifier.weight(1f))

                // 3. Bitácora
                NavigationBarItem(
                    selected = activeTab == "bitacora",
                    onClick = { onTabSelected("bitacora") },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == "bitacora") Icons.Filled.AutoStories else Icons.Outlined.AutoStories,
                            contentDescription = "Bitácora"
                        )
                    },
                    label = {
                        Text(
                            text = "Bitácora",
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
                            contentDescription = "Agente IA"
                        )
                    },
                    label = {
                        Text(
                            text = "Agente IA",
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

        // Elevated Center Button: + Chequeo
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-6).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FloatingActionButton(
                onClick = onOpenCheckup,
                shape = CircleShape,
                containerColor = EirCharcoal,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp
                ),
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Iniciar Chequeo Diario Obligatorio",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "Chequeo",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = activeColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
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
