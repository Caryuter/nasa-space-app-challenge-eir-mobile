package mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.ChatMessage
import mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine.LiteRtModelStatus
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.MascotAvatar
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.MascotEmotion
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.AgenteIaUiState
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.AgenteIaViewModel

@Composable
fun AgenteIaScreen(
    state: AgenteIaUiState,
    viewModel: AgenteIaViewModel,
    modifier: Modifier = Modifier
) {
    AgenteIaScreenContent(
        state = state,
        onStartDownload = { viewModel.startDownloadModel() },
        onCancelDownload = { viewModel.cancelDownloadModel() },
        onSetChatInput = { viewModel.setChatInput(it) },
        onSendChatMessage = { text, simpler, steps -> viewModel.sendChatMessage(text, simpler, steps) },
        modifier = modifier
    )
}

@Composable
fun AgenteIaScreenContent(
    state: AgenteIaUiState,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onSetChatInput: (String) -> Unit,
    onSendChatMessage: (String?, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = false
    val cardBg = if (isDark) EirDarkSurface else EirLightSurface
    val cardBorder = if (isDark) EirDarkBorder else EirLightBorder
    val listState = rememberLazyListState()

    LaunchedEffect(state.chatMessages.size, state.isAgentLoading) {
        if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.chatMessages.size)
        }
    }

    val currentHeaderEmotion = when {
        state.isAgentLoading || state.modelStatus is LiteRtModelStatus.Downloading || state.modelStatus is LiteRtModelStatus.Initializing -> MascotEmotion.THINKING
        state.modelStatus is LiteRtModelStatus.Error -> MascotEmotion.CONCERNED
        else -> MascotEmotion.IDLE
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) EirDarkBg else EirLightBg)
    ) {
        // Chat Mascot Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDark) EirDarkHeader else EirLightHeader,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "EIR • Asistente Médico",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) EirDarkTextPrimary else EirLightTextPrimary
                        )
                    }
                }
            }
        }

        // Banner discreto de estado/descarga (desaparece cuando está listo)
        AnimatedVisibility(
            visible = !state.isChatUnlocked,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            SimpleDownloadBanner(
                status = state.modelStatus,
                isDownloadingOrConnecting = state.isDownloadingOrConnecting,
                onStartDownload = onStartDownload,
                onCancelDownload = onCancelDownload,
                isDark = isDark,
                cardBg = cardBg,
                cardBorder = cardBorder
            )
        }

        // Lista de mensajes
        val messages = if (state.chatMessages.isNotEmpty()) state.chatMessages else sampleChatMessages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                ChatMessageItem(
                    message = msg,
                    isDark = isDark,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    onSimplify = {
                        onSendChatMessage(
                            "Explícame esto aún más simple: \"${msg.text}\"",
                            true,
                            false
                        )
                    },
                    onGiveSteps = {
                        onSendChatMessage(
                            "Dame los pasos a seguir para esto: \"${msg.text}\"",
                            false,
                            true
                        )
                    }
                )
            }

            if (state.isAgentLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        MascotAvatar(
                            emotion = MascotEmotion.THINKING,
                            size = 28.dp,
                            showGlow = true
                        )
                        Text(
                            text = "EIR está analizando los datos...",
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                        )
                    }
                }
            }
        }

        // Barra inferior: chips sugeridos + input
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDark) EirDarkHeader else EirLightHeader,
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.isChatUnlocked) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "¿Por qué siento la cabeza pesada?",
                            "Siento un poco de ansiedad hoy",
                            "¿Cómo van mis signos vitales?",
                            "Dame un consejo para descansar mejor"
                        ).forEach { chip ->
                            SuggestionChip(
                                onClick = { onSendChatMessage(chip, false, false) },
                                label = {
                                    Text(
                                        text = chip,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.chatInput,
                        onValueChange = onSetChatInput,
                        enabled = state.isChatUnlocked,
                        placeholder = {
                            Text(
                                text = if (state.isChatUnlocked) {
                                    "Escribe un mensaje..."
                                } else {
                                    "Descarga el asistente arriba para chatear"
                                },
                                fontSize = 13.sp
                            )
                        },
                        trailingIcon = {
                            if (!state.isChatUnlocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Bloqueado",
                                    tint = (if (isDark) EirDarkTextSecondary else EirLightTextSecondary).copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EirSky,
                            unfocusedBorderColor = if (isDark) EirDarkBorder else EirLightBorder,
                            disabledBorderColor = if (isDark) EirDarkBorder.copy(alpha = 0.5f) else EirLightBorder.copy(alpha = 0.5f)
                        )
                    )

                    IconButton(
                        onClick = { onSendChatMessage(null, false, false) },
                        enabled = state.isChatUnlocked && state.chatInput.isNotBlank(),
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.isChatUnlocked && state.chatInput.isNotBlank()) EirSky else Color.Gray.copy(alpha = 0.3f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleDownloadBanner(
    status: LiteRtModelStatus,
    isDownloadingOrConnecting: Boolean,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        tonalElevation = 1.dp
    ) {
        when (status) {
            is LiteRtModelStatus.NotDownloaded -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Asistente inteligente",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) EirDarkTextPrimary else EirLightTextPrimary
                        )
                        Text(
                            text = "Descárgalo para usarlo sin conexión (${status.sizeLabel})",
                            fontSize = 11.sp,
                            color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onStartDownload,
                        enabled = !isDownloadingOrConnecting,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EirSky,
                            disabledContainerColor = EirSky.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isDownloadingOrConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Iniciando...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Descargar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            is LiteRtModelStatus.Downloading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = EirSky
                            )
                            Text(
                                text = "Descargando modelo local...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) EirDarkTextPrimary else EirLightTextPrimary
                            )
                        }
                        IconButton(
                            onClick = onCancelDownload,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar descarga",
                                tint = Color.Red.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (status.progressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = EirSky,
                        trackColor = EirSky.copy(alpha = 0.15f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Descargado:",
                                fontSize = 11.sp,
                                color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                            )
                            Text(
                                text = "${status.downloadedMb} / ${status.totalMb}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EirSky
                            )
                        }
                        Text(
                            text = "${status.progressPercent}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) EirDarkTextPrimary else EirLightTextPrimary
                        )
                    }
                }
            }

            is LiteRtModelStatus.Initializing -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = EirSky
                    )
                    Text(
                        text = "Preparando asistente...",
                        fontSize = 12.sp,
                        color = if (isDark) EirDarkTextPrimary else EirLightTextPrimary
                    )
                }
            }

            is LiteRtModelStatus.Error -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Error al descargar el modelo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                        Text(
                            text = status.message,
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                        )
                    }
                    IconButton(
                        onClick = onStartDownload,
                        enabled = !isDownloadingOrConnecting,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reintentar",
                            tint = EirSky,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            is LiteRtModelStatus.Ready -> {
                // No se muestra nada al completarse; se oculta automáticamente
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    onSimplify: () -> Unit,
    onGiveSteps: () -> Unit
) {
    val isUser = message.sender == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(if (isUser) 1f else 0.9f)
        ) {
            if (!isUser) {
                MascotAvatar(
                    emotion = MascotEmotion.HAPPY,
                    size = 32.dp,
                    showGlow = false
                )
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) EirSky else cardBg,
                tonalElevation = if (isUser) 0.dp else 1.dp,
                border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = if (isUser) Color.White else {
                        if (isDark) EirDarkTextPrimary else EirLightTextPrimary
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        if (!isUser && message.canSimplify) {
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { onSimplify() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Simplificar",
                        tint = EirSky,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Explicar más simple",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EirSky
                    )
                }

                Row(
                    modifier = Modifier.clickable { onGiveSteps() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListNumbered,
                        contentDescription = "Pasos",
                        tint = EirEmerald,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Pasos a seguir",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EirEmerald
                    )
                }
            }
        }
    }
}

val sampleChatMessages = listOf(
    ChatMessage(1L, "eir", "Hola, comandante. Observo tus datos de hoy en orden. Descarga el asistente para empezar a chatear.")
)

@Preview(showBackground = true, name = "AgenteIaScreen Light Preview", heightDp = 600)
@Composable
fun AgenteIaScreenLightPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        Surface(color = EirLightBg) {
            AgenteIaScreenContent(
                state = AgenteIaUiState(
                    chatMessages = sampleChatMessages,
                    modelStatus = LiteRtModelStatus.NotDownloaded()
                ),
                onStartDownload = {},
                onCancelDownload = {},
                onSetChatInput = {},
                onSendChatMessage = { _, _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, name = "AgenteIaScreen Dark Preview", backgroundColor = 0xFF0B1120, heightDp = 600)
@Composable
fun AgenteIaScreenDarkPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = true) {
        Surface(color = EirDarkBg) {
            AgenteIaScreenContent(
                state = AgenteIaUiState(
                    chatMessages = sampleChatMessages,
                    modelStatus = LiteRtModelStatus.Downloading(
                        progressPercent = 45,
                        downloadedMb = "270.0 MB",
                        totalMb = "600.0 MB"
                    )
                ),
                onStartDownload = {},
                onCancelDownload = {},
                onSetChatInput = {},
                onSendChatMessage = { _, _, _ -> }
            )
        }
    }
}
