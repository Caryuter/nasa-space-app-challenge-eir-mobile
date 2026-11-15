package mx.edu.utez.eir_nasa_space_apps_challenge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.CheckupReminderPopup
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.EirBottomBar
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.EirTopBar
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.MascotCompanionOverlay
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.MascotEmotion
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens.*
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: EirMainViewModel = viewModel()
            val saludViewModel: SaludViewModel = viewModel()
            val habitosViewModel: HabitosViewModel = viewModel()
            val bitacoraViewModel: BitacoraViewModel = viewModel()
            val agenteIaViewModel: AgenteIaViewModel = viewModel()
            val checkupViewModel: CheckupViewModel = viewModel()
            val sosViewModel: SosViewModel = viewModel()

            val mainState by mainViewModel.uiState.collectAsState()
            val saludState by saludViewModel.uiState.collectAsState()
            val habitosState by habitosViewModel.uiState.collectAsState()
            val bitacoraState by bitacoraViewModel.uiState.collectAsState()
            val agenteIaState by agenteIaViewModel.uiState.collectAsState()
            val checkupState by checkupViewModel.uiState.collectAsState()
            val sosState by sosViewModel.uiState.collectAsState()

            EirNASASpaceAppsChallengeTheme(darkTheme = mainState.isDarkMode) {
                EirApp(
                    mainState = mainState,
                    saludState = saludState,
                    habitosState = habitosState,
                    bitacoraState = bitacoraState,
                    agenteIaState = agenteIaState,
                    checkupState = checkupState,
                    sosState = sosState,
                    mainViewModel = mainViewModel,
                    saludViewModel = saludViewModel,
                    habitosViewModel = habitosViewModel,
                    bitacoraViewModel = bitacoraViewModel,
                    agenteIaViewModel = agenteIaViewModel,
                    checkupViewModel = checkupViewModel,
                    sosViewModel = sosViewModel
                )
            }
        }
    }
}

@Composable
fun EirApp(
    mainState: EirMainUiState,
    saludState: SaludUiState,
    habitosState: HabitosUiState,
    bitacoraState: BitacoraUiState,
    agenteIaState: AgenteIaUiState,
    checkupState: CheckupUiState,
    sosState: SosUiState,
    mainViewModel: EirMainViewModel,
    saludViewModel: SaludViewModel,
    habitosViewModel: HabitosViewModel,
    bitacoraViewModel: BitacoraViewModel,
    agenteIaViewModel: AgenteIaViewModel,
    checkupViewModel: CheckupViewModel,
    sosViewModel: SosViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (mainState.isDarkMode) EirDarkBg else EirLightBg)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                EirTopBar(
                    isDarkMode = mainState.isDarkMode,
                    onToggleDarkMode = { mainViewModel.toggleDarkMode() },
                    onOpenSos = { sosViewModel.openSos() },
                    modifier = Modifier.statusBarsPadding()
                )
            },
            bottomBar = {
                EirBottomBar(
                    activeTab = mainState.activeTab,
                    isDarkMode = mainState.isDarkMode,
                    onTabSelected = { mainViewModel.setActiveTab(it) },
                    onOpenCheckup = { checkupViewModel.openCheckup() },
                    modifier = Modifier.navigationBarsPadding()
                )
            },
            containerColor = if (mainState.isDarkMode) EirDarkBg else EirLightBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (mainState.activeTab) {
                    "salud" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 16.dp)
                        ) {
                            SaludScreen(
                                state = saludState,
                                viewModel = saludViewModel,
                                onOpenCheckup = { checkupViewModel.openCheckup() }
                            )
                        }
                    }
                    "habitos" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 16.dp)
                        ) {
                            HabitosScreen(state = habitosState, viewModel = habitosViewModel)
                        }
                    }
                    "bitacora" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 16.dp)
                        ) {
                            BitacoraScreen(state = bitacoraState, viewModel = bitacoraViewModel)
                        }
                    }
                    "agente" -> {
                        AgenteIaScreen(state = agenteIaState, viewModel = agenteIaViewModel)
                    }
                }

                // Top Floating Pop-up Banner ("Mensaje Emergente de Recordatorio")
                CheckupReminderPopup(
                    visible = mainState.showCheckupReminderPopup,
                    isDarkMode = mainState.isDarkMode,
                    onStartCheckupNow = {
                        mainViewModel.dismissReminderPopup()
                        checkupViewModel.openCheckup()
                    },
                    onSnooze15Min = { mainViewModel.snoozeReminder15Min() },
                    onDismiss = { mainViewModel.dismissReminderPopup() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )

                // Toast Notification Overlay
                AnimatedVisibility(
                    visible = mainState.toastMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    mainState.toastMessage?.let { msg ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp)),
                            color = Color(0xFF0F172A).copy(alpha = 0.95f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Éxito",
                                    tint = EirEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Mascot Companion Overlay
        if (!checkupState.checkupOpen && !sosState.sosOpen) {
            val currentGlobalMascotEmotion = when {
                agenteIaState.isAgentLoading -> MascotEmotion.THINKING
                mainState.showCheckupReminderPopup -> MascotEmotion.CONCERNED
                mainState.activeTab == "agente" -> MascotEmotion.HAPPY
                else -> MascotEmotion.IDLE
            }

            MascotCompanionOverlay(
                isDarkMode = mainState.isDarkMode,
                currentEmotion = currentGlobalMascotEmotion,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        // Modal de Chequeo Diario Obligatorio
        CheckupModal(
            state = checkupState,
            viewModel = checkupViewModel,
            onCompleteSuccess = { mainViewModel.showToast(it) },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Emergencia SOS
        SosEmergencyOverlay(
            state = sosState,
            viewModel = sosViewModel,
            onCompleteSuccess = { mainViewModel.showToast(it) },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true, name = "EirApp Full Light Preview")
@Composable
fun EirAppLightPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        Surface(color = EirLightBg) {
            Text("Eir App Preview - Floating Checkup Reminders", modifier = Modifier.padding(20.dp), fontWeight = FontWeight.Bold)
        }
    }
}
