package mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.JournalEntry
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.Emotional2DQuadrant
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.BitacoraUiState
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.BitacoraViewModel

data class BreathGuide(val title: String, val sub: String, val scale: Float)

@Composable
fun BitacoraScreen(
    state: BitacoraUiState,
    viewModel: BitacoraViewModel,
    modifier: Modifier = Modifier
) {
    BitacoraScreenContent(
        state = state,
        onToggleBreathing = { viewModel.toggleBreathing() },
        onSetJournalInput = { viewModel.setJournalInput(it) },
        onSaveJournalEntry = { viewModel.saveJournalEntry() },
        onStartVoiceRecording = { viewModel.startVoiceRecording() },
        onStopVoiceRecording = { viewModel.stopVoiceRecording() },
        onSpeakJournalText = { text, entryId -> viewModel.speakJournalText(text, entryId) },
        onStopTts = { viewModel.stopTts() },
        onAppendVoiceText = { viewModel.appendVoiceText(it) },
        onUseSampleSpeech = { viewModel.useSampleSpeechTranscription() },
        onClearSpeechFeedback = { viewModel.clearSpeechFeedback() },
        modifier = modifier
    )
}

@Composable
fun VoiceWaveformIndicator(
    isRecording: Boolean,
    rms: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(0.25f, 0.55f, 0.85f, 1.0f, 0.75f, 0.45f, 0.65f).forEachIndexed { index, baseFactor ->
            val dynamicHeight = if (isRecording) {
                val wave = kotlin.math.sin(phase + index * 0.8f)
                val rmsFactor = (rms / 6f).coerceIn(0.2f, 1.6f)
                (12.dp + (22.dp * ((wave + 1f) / 2f) * baseFactor * rmsFactor)).coerceIn(8.dp, 36.dp)
            } else {
                8.dp
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(dynamicHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isRecording) EirRoseEmergency else EirEmerald)
            )
        }
    }
}

@Composable
fun BitacoraScreenContent(
    state: BitacoraUiState,
    onToggleBreathing: () -> Unit,
    onSetJournalInput: (String) -> Unit,
    onSaveJournalEntry: () -> Unit,
    onStartVoiceRecording: () -> Unit = {},
    onStopVoiceRecording: () -> Unit = {},
    onSpeakJournalText: (String?, Long?) -> Unit = { _, _ -> },
    onStopTts: () -> Unit = {},
    onAppendVoiceText: (String) -> Unit = {},
    onUseSampleSpeech: () -> Unit = {},
    onClearSpeechFeedback: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.isDark
    val cardBg = MaterialTheme.colorScheme.surface
    val cardBorder = MaterialTheme.colorScheme.outline
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            onStartVoiceRecording()
        }
    }

    val speechIntentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onAppendVoiceText(spoken)
            }
        }
    }

    val breathGuides = listOf(
        BreathGuide(stringResource(R.string.breathe_in), stringResource(R.string.breathe_in_sub), 1.25f),
        BreathGuide(stringResource(R.string.breathe_hold), stringResource(R.string.breathe_hold_sub), 1.25f),
        BreathGuide(stringResource(R.string.breathe_out), stringResource(R.string.breathe_out_sub), 0.85f),
        BreathGuide(stringResource(R.string.breathe_rest), stringResource(R.string.breathe_rest_sub), 0.85f)
    )

    val currentGuide = breathGuides[state.breathingPhase]

    val animatedScale by animateFloatAsState(
        targetValue = if (state.breathingActive) currentGuide.scale else 1f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "breathScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SCREEN HEADER
        Column {
            Text(
                text = stringResource(R.string.logbook_title),
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp,
                color = textPrimary
            )
            Text(
                text = stringResource(R.string.logbook_subtitle),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // STREAK & COMMITMENT CARD
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Flame Graphic + Streak Number
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "🔥", fontSize = 48.sp)
                    Text(
                        text = "3",
                        fontWeight = FontWeight.Black,
                        fontSize = 36.sp,
                        color = textPrimary
                    )
                    Text(
                        text = stringResource(R.string.streak_days),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = textPrimary
                    )
                }

                // Weekly Days Tracker Bar (L M X J V S D)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("L", "M", "X", "J", "V", "S", "D").forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textPrimary
                                )
                            }
                        }

                        // Progress Line (3 days completed out of 7)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(3f / 7f)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(EirAmber)
                            )
                        }
                    }
                }

                // Motivational Text
                Text(
                    text = stringResource(R.string.streak_motivational),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )

                // Dark CTA Button "Me comprometo"
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.i_commit),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // BREATHING GUIDED CARD (4-4-4-4)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.breathing_pause_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary
                        )
                        Text(
                            text = stringResource(R.string.breathing_technique_sub),
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }

                    Button(
                        onClick = onToggleBreathing,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.breathingActive) (if (isDark) MaterialTheme.colorScheme.surfaceVariant else EirCharcoal) else MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = stringResource(R.string.breathing_pause_title),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.breathingActive) stringResource(R.string.stop) else stringResource(R.string.start),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (state.breathingActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(animatedScale)
                                .clip(CircleShape)
                                .background(EirEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(EirEmerald.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(EirEmerald)
                                )
                            }
                        }

                        Text(
                            text = currentGuide.title,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = EirEmerald
                        )
                        Text(
                            text = currentGuide.sub,
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }
            }
        }

        // 2D EMOTIONAL QUADRANT CARD
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.emotional_state_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimary
                    )
                    Text(
                        text = stringResource(R.string.today_mood, state.moodLabel),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EirEmerald
                    )
                }

                Emotional2DQuadrant(
                    selectedPoint = state.moodPoint,
                    isInteractive = false,
                    entries = state.journalEntries,
                    isDarkMode = isDark,
                    height = 140.dp
                )
            }
        }

        // PERSONAL JOURNAL ENTRY CARD ("Escribe algo para ti hoy" - Voice First & Text-to-Speech)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header & Recommendation Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.write_for_yourself),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary
                        )
                        Text(
                            text = stringResource(R.string.voice_first_sub),
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EirEmerald.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, EirEmerald.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = EirEmerald,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = stringResource(R.string.voice_first_badge),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EirEmerald
                            )
                        }
                    }
                }

                // HERO VOICE ACTION BOX
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = if (state.isRecordingVoice) EirRoseEmergency.copy(alpha = 0.08f) else (if (isDark) MaterialTheme.colorScheme.surfaceVariant else EirLightCardSubtle),
                    border = BorderStroke(
                        width = if (state.isRecordingVoice) 1.5.dp else 1.dp,
                        color = if (state.isRecordingVoice) EirRoseEmergency else cardBorder
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!state.isRecordingVoice) {
                            // Pulsing / Friendly Mic Button
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                EirEmerald,
                                                EirEmeraldLight
                                            )
                                        )
                                    )
                                    .clickable {
                                        if (!hasAudioPermission) {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            onStartVoiceRecording()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = stringResource(R.string.tap_to_record_voice),
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.tap_to_record_voice),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.dictate_how_you_feel),
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Quick helper chip for demo/testing or simulator
                            TextButton(
                                onClick = onUseSampleSpeech,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = EirSky
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.insert_sample_speech),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EirSky
                                )
                            }
                        } else {
                            // ACTIVE RECORDING STATE
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.18f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(700, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseScale"
                            )

                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(EirRoseEmergency.copy(alpha = 0.25f))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(66.dp)
                                        .clip(CircleShape)
                                        .background(EirRoseEmergency)
                                        .clickable { onStopVoiceRecording() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = stringResource(R.string.stop_recording),
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            // Timer & Status
                            val minutes = state.recordingDurationSec / 60
                            val seconds = state.recordingDurationSec % 60
                            val timerString = String.format("%02d:%02d", minutes, seconds)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(EirRoseEmergency)
                                )
                                Text(
                                    text = stringResource(R.string.recording_reflection, timerString),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = EirRoseEmergency
                                )
                            }

                            VoiceWaveformIndicator(
                                isRecording = true,
                                rms = state.voiceRms,
                                modifier = Modifier.height(36.dp)
                            )

                            Text(
                                text = stringResource(R.string.listening_carefully),
                                fontSize = 11.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = onStopVoiceRecording,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EirRoseEmergency),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.finish_and_transcribe),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Status Banner: Feedback
                if (state.speechSuccessNotice != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = EirEmerald.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, EirEmerald.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EirEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = state.speechSuccessNotice,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EirEmerald,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onClearSpeechFeedback,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = EirEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                if (state.speechError != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = EirAmber.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, EirAmber.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = EirAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = state.speechError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onClearSpeechFeedback,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.close),
                                        tint = textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla tu reflexión para Eir")
                                            }
                                            speechIntentLauncher.launch(intent)
                                        } catch (_: Exception) {
                                            onUseSampleSpeech()
                                        }
                                    }
                                ) {
                                    Text(
                                        text = stringResource(R.string.retry_system_dialog),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EirSky
                                    )
                                }
                            }
                        }
                    }
                }

                // EDITABLE TEXT AREA & TEXT-TO-SPEECH CONTROLS
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.transcribed_text_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )

                    OutlinedTextField(
                        value = state.journalInput,
                        onValueChange = onSetJournalInput,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.transcribed_placeholder),
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EirEmerald,
                            unfocusedBorderColor = cardBorder
                        )
                    )

                    // Action Controls: Text-to-Speech + Clear + Save
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // TEXT-TO-SPEECH ACTION BUTTON
                        if (state.journalInput.isNotBlank()) {
                            Button(
                                onClick = { onSpeakJournalText(state.journalInput, null) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isTtsSpeaking && state.activeTtsEntryId == null) EirRoseEmergency else (if (isDark) MaterialTheme.colorScheme.surfaceVariant else EirSkySoft),
                                    contentColor = if (state.isTtsSpeaking && state.activeTtsEntryId == null) Color.White else (if (isDark) Color.White else EirSky)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isTtsSpeaking && state.activeTtsEntryId == null) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Text-to-Speech",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (state.isTtsSpeaking && state.activeTtsEntryId == null) stringResource(R.string.stop_audio) else stringResource(R.string.listen_tts),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // Right-aligned Buttons: Clear & Save
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.journalInput.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        onSetJournalInput("")
                                        onClearSpeechFeedback()
                                        onStopTts()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = stringResource(R.string.clear_text),
                                        tint = textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = onSaveJournalEntry,
                                enabled = state.journalInput.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    disabledContainerColor = cardBorder.copy(alpha = 0.5f)
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.save_note),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // HISTORICAL JOURNAL ENTRIES ("TUS DÍAS ANTERIORES")
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.previous_days_header),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = textSecondary
            )

            val entries = if (state.journalEntries.isNotEmpty()) state.journalEntries else sampleJournalEntries
            entries.forEach { entry ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = entry.sol,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EirEmerald
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = EirEmerald.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = entry.mood,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EirEmerald,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // TTS listen button for past entries
                            IconButton(
                                onClick = { onSpeakJournalText(entry.text, entry.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isTtsSpeaking && state.activeTtsEntryId == entry.id) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = stringResource(R.string.listen_tts_cd),
                                    tint = if (state.isTtsSpeaking && state.activeTtsEntryId == entry.id) EirRoseEmergency else textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = entry.text,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = textPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

val sampleJournalEntries = listOf(
    JournalEntry(1L, "Sol 142 • Hoy 08:15", "Tranquilo", 74f, 64f, "Dormí mucho mejor después de bajar un poco la temperatura de mi cabina."),
    JournalEntry(2L, "Sol 141 • Ayer 21:30", "Nostálgico", 50f, 44f, "Vi una foto de mi familia antes de dormir. La respiración me relajó."),
    JournalEntry(3L, "Sol 140 • Hace 2 días", "Motivado", 84f, 80f, "Terminamos de calibrar los paneles solares antes de tiempo. Gran equipo.")
)

@Preview(showBackground = true, name = "BitacoraScreen Light Preview", heightDp = 950)
@Composable
fun BitacoraScreenLightPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        Surface(color = EirLightBg) {
            BitacoraScreenContent(
                state = BitacoraUiState(journalEntries = sampleJournalEntries),
                onToggleBreathing = {},
                onSetJournalInput = {},
                onSaveJournalEntry = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "BitacoraScreen Dark Preview", backgroundColor = 0xFF0F172A, heightDp = 950)
@Composable
fun BitacoraScreenDarkPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = true) {
        Surface(color = EirDarkBg) {
            BitacoraScreenContent(
                state = BitacoraUiState(journalEntries = sampleJournalEntries),
                onToggleBreathing = {},
                onSetJournalInput = {},
                onSaveJournalEntry = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Bitacora Breathing Pause Active Preview", heightDp = 950)
@Composable
fun BitacoraBreathingActivePreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        Surface(color = EirLightBg) {
            BitacoraScreenContent(
                state = BitacoraUiState(
                    journalEntries = sampleJournalEntries,
                    breathingActive = true,
                    breathingPhase = 0
                ),
                onToggleBreathing = {},
                onSetJournalInput = {},
                onSaveJournalEntry = {}
            )
        }
    }
}
