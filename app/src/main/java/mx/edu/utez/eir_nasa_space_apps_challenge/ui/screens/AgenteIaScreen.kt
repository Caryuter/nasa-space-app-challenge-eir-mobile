package mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatListNumbered
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
        onSetChatInput = { viewModel.setChatInput(it) },
        onSendChatMessage = { text, simpler, steps -> viewModel.sendChatMessage(text, simpler, steps) },
        modifier = modifier
    )
}

@Composable
fun AgenteIaScreenContent(
    state: AgenteIaUiState,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) EirDarkBg else EirLightBg)
    ) {
        // Synchronized Context Pill
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFEEF2FF),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Contexto Sincronizado",
                        tint = EirIndigo,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Al tanto de tu último chequeo: 68 lat/min • 7.5h sueño",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) EirDarkTextSecondary else Color(0xFF4338CA),
                        maxLines = 1
                    )
                }

                Text(
                    text = "Modo Simple",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EirEmerald,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Full Screen Messages List
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
                    Text(
                        text = "EIR está preparando una explicación clara para ti...",
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Bottom Controls: Quick Chips + Input Field
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
                // Suggested chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "¿Por qué siento la cabeza pesada?",
                        "¿Para qué sirve la caminadora?",
                        "Me siento cansado hoy",
                        "¿Qué pasa con mis huesos?"
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

                // Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.chatInput,
                        onValueChange = onSetChatInput,
                        placeholder = {
                            Text(
                                text = "Pregunta cualquier duda con tus palabras...",
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EirSky,
                            unfocusedBorderColor = if (isDark) EirDarkBorder else EirLightBorder
                        )
                    )

                    IconButton(
                        onClick = { onSendChatMessage(null, false, false) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(EirSky)
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
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = if (isUser) Color.White else {
                    if (isDark) EirDarkTextPrimary else EirLightTextPrimary
                },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        // Quick action buttons under assistant reply
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
                        text = "Explícamelo aún más simple",
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
                        text = "Dame los pasos a seguir",
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
    ChatMessage(1L, "eir", "Hola. Vi tus datos de hoy: dormiste 7.5 horas, tus latidos están tranquilos (68) y tu batería física está al 84%. ¿Cómo sientes tu cuerpo ahorita?"),
    ChatMessage(2L, "user", "¿Por qué siento la cabeza pesada al despertar?"),
    ChatMessage(3L, "eir", "En el espacio los líquidos suben hacia la cabeza porque no hay gravedad que los jale hacia los pies. Toma agua despacio y la sensación se aliviará.")
)

@Preview(showBackground = true, name = "AgenteIaScreen Light Preview", heightDp = 600)
@Composable
fun AgenteIaScreenLightPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        Surface(color = EirLightBg) {
            AgenteIaScreenContent(
                state = AgenteIaUiState(chatMessages = sampleChatMessages),
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
                state = AgenteIaUiState(chatMessages = sampleChatMessages),
                onSetChatInput = {},
                onSendChatMessage = { _, _, _ -> }
            )
        }
    }
}
