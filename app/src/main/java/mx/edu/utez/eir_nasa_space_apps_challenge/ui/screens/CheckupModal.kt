@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.VitalsDraft
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.Emotional2DQuadrant
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.MascotAvatar
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.MascotEmotion
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.CheckupUiState
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.CheckupViewModel

@Composable
fun CheckupModal(
    state: CheckupUiState,
    viewModel: CheckupViewModel,
    onCompleteSuccess: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    CheckupModalContent(
        state = state,
        onClose = { viewModel.closeCheckup() },
        onSetStep = { viewModel.setCheckupStep(it) },
        onFacePhotoCaptured = { viewModel.onRealPhotoCaptured(it) },
        onSetFaceScanning = { viewModel.setFaceScanScanning() },
        onCyclePhrase = { viewModel.cyclePhrase() },
        onStartAudioRecording = { viewModel.startAudioRecording() },
        onStopAudioRecording = { viewModel.stopAudioRecording() },
        onUpdateVitalsDraft = { viewModel.updateVitalsDraft(it) },
        onToggleVitalsConfirmed = { viewModel.toggleVitalsConfirmed() },
        onSetMoodDraftPoint = { x, y -> viewModel.setMoodDraftPoint(x, y) },
        onSetMoodDraftLabel = { viewModel.setMoodDraftLabel(it) },
        onCompleteCheckup = { viewModel.completeCheckup(onCompleteSuccess) },
        modifier = modifier
    )
}

@Composable
fun CheckupModalContent(
    state: CheckupUiState,
    onClose: () -> Unit,
    onSetStep: (Int) -> Unit,
    onFacePhotoCaptured: (Bitmap) -> Unit,
    onSetFaceScanning: () -> Unit,
    onCyclePhrase: () -> Unit,
    onStartAudioRecording: () -> Unit,
    onStopAudioRecording: () -> Unit,
    onUpdateVitalsDraft: ((VitalsDraft) -> VitalsDraft) -> Unit,
    onToggleVitalsConfirmed: () -> Unit,
    onSetMoodDraftPoint: (Float, Float) -> Unit,
    onSetMoodDraftLabel: (String) -> Unit,
    onCompleteCheckup: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.checkupOpen) return

    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.isDark
    val modalBg = MaterialTheme.colorScheme.surface
    val modalBorder = MaterialTheme.colorScheme.outline

    // CameraX ImageCapture Instance
    val imageCapture = remember { ImageCapture.Builder().build() }

    // Camera & Audio Runtime Permissions Launcher
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            onStartAudioRecording()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(modalBg)
                .border(1.dp, modalBorder, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header & 4-Step Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MascotAvatar(
                            emotion = when (state.checkupStep) {
                                1 -> MascotEmotion.IDLE
                                2 -> MascotEmotion.THINKING
                                3 -> MascotEmotion.CONCERNED
                                else -> MascotEmotion.HAPPY
                            },
                            size = 40.dp,
                            showGlow = true
                        )

                        Column {
                            Text(
                                text = when (state.checkupStep) {
                                    1 -> stringResource(R.string.step_1_title)
                                    2 -> stringResource(R.string.step_2_title)
                                    3 -> stringResource(R.string.step_3_title)
                                    else -> stringResource(R.string.step_4_title)
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), modifier = Modifier.size(18.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (step in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (step <= state.checkupStep) EirSky
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }
            }

            // Scrollable Content Area for current step
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imeNestedScroll()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // STEP 1: REAL CAMERA FACE SCAN
                if (state.checkupStep == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.step_1_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 160.dp, height = 200.dp)
                                        .clip(RoundedCornerShape(70.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasCameraPermission) {
                                        if (state.capturedFaceBitmap != null) {
                                            // Show real captured photo
                                            Image(
                                                bitmap = state.capturedFaceBitmap.asImageBitmap(),
                                                contentDescription = stringResource(R.string.captured_photo),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            // Live CameraX Preview Feed inside Oval Frame
                                            CameraPreviewView(
                                                imageCapture = imageCapture,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = EirSky, modifier = Modifier.size(32.dp))
                                            Text(text = stringResource(R.string.camera_permission_required), fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(top = 4.dp))
                                        }
                                    }

                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawCircle(
                                            color = EirSky,
                                            radius = size.width * 0.45f,
                                            style = Stroke(
                                                width = 2.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                            )
                                        )
                                    }
                                }

                                if (hasCameraPermission && (state.capturedFaceBitmap != null || state.faceScanStatus == "done")) {
                                    OutlinedButton(
                                        onClick = {
                                            onSetFaceScanning()
                                            takeRealPhoto(context, imageCapture) { bmp ->
                                                onFacePhotoCaptured(bmp)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = stringResource(R.string.retake_photo), fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // STEP 2: REAL VOICE RECORDING & RANDOM PHRASE CATALOG
                if (state.checkupStep == 2) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.step_2_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        OutlinedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = stringResource(R.string.random_phrase_catalog), fontSize = 10.sp, fontWeight = FontWeight.Black, color = EirIndigo)
                                    Text(
                                        text = stringResource(R.string.another_phrase),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EirIndigo,
                                        modifier = Modifier.clickable { onCyclePhrase() }
                                    )
                                }
                                Text(text = "“${state.currentPhraseText}”", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 18.sp)
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!hasAudioPermission) {
                                Text(
                                    text = stringResource(R.string.mic_permission_required),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        if (state.voiceScanStatus == "recording") {
                                            onStopAudioRecording()
                                        } else {
                                            onStartAudioRecording()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (state.voiceScanStatus == "recording") EirRoseEmergency
                                             else if (state.voiceScanStatus == "done") EirEmerald
                                            else EirSky
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (state.voiceScanStatus == "done") Icons.Default.Check
                                        else if (state.voiceScanStatus == "recording") Icons.Default.Stop
                                        else Icons.Default.Mic,
                                        contentDescription = stringResource(R.string.real_mic),
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                // Live audio waveform amplitude visualization
                                if (state.voiceScanStatus == "recording") {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (i in 0..7) {
                                            val barHeight = ((state.audioMaxAmplitude % (i + 5)) * 3 + 10).coerceIn(6, 28)
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(barHeight.dp)
                                                    .clip(CircleShape)
                                                    .background(EirRoseEmergency)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = when (state.voiceScanStatus) {
                                        "idle" -> stringResource(R.string.tap_mic_to_record)
                                        "recording" -> stringResource(R.string.recording_real_mic)
                                        else -> stringResource(R.string.voice_test_recorded)
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // STEP 3: VITALS
                if (state.checkupStep == 3) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = stringResource(R.string.step_3_desc), fontSize = 12.sp)

                        VitalRow(
                            label = stringResource(R.string.vital_heart_rate),
                            hint = stringResource(R.string.vital_heart_rate_hint),
                            placeholder = "70",
                            value = state.vitalsDraft.heartRateInput,
                            unit = "bpm",
                            onValueChange = { newValue ->
                                onUpdateVitalsDraft { it.copy(heartRateInput = newValue) }
                            },
                            onMinus = {
                                val current = state.vitalsDraft.heartRateInput.toIntOrNull() ?: 68
                                val updated = (current - 1).coerceAtLeast(40)
                                onUpdateVitalsDraft { it.copy(heartRateInput = updated.toString()) }
                            },
                            onPlus = {
                                val current = state.vitalsDraft.heartRateInput.toIntOrNull() ?: 68
                                val updated = (current + 1).coerceAtMost(160)
                                onUpdateVitalsDraft { it.copy(heartRateInput = updated.toString()) }
                            },
                            keyboardType = KeyboardType.Number,
                            isDark = isDark
                        )

                        VitalRow(
                            label = stringResource(R.string.vital_sys),
                            hint = stringResource(R.string.vital_sys_hint),
                            placeholder = "120",
                            value = state.vitalsDraft.sysInput,
                            unit = "mmHg",
                            onValueChange = { newValue ->
                                onUpdateVitalsDraft { it.copy(sysInput = newValue) }
                            },
                            onMinus = {
                                val current = state.vitalsDraft.sysInput.toIntOrNull() ?: 118
                                val updated = (current - 2).coerceAtLeast(80)
                                onUpdateVitalsDraft { it.copy(sysInput = updated.toString()) }
                            },
                            onPlus = {
                                val current = state.vitalsDraft.sysInput.toIntOrNull() ?: 118
                                val updated = (current + 2).coerceAtMost(180)
                                onUpdateVitalsDraft { it.copy(sysInput = updated.toString()) }
                            },
                            keyboardType = KeyboardType.Number,
                            isDark = isDark
                        )

                        VitalRow(
                            label = stringResource(R.string.vital_temp),
                            hint = stringResource(R.string.vital_temp_hint),
                            placeholder = "36.7",
                            value = state.vitalsDraft.tempInput,
                            unit = "°C",
                            onValueChange = { newValue ->
                                onUpdateVitalsDraft { it.copy(tempInput = newValue) }
                            },
                            onMinus = {
                                val current = state.vitalsDraft.tempInput.toFloatOrNull() ?: 36.7f
                                val updated = (current - 0.1f).coerceAtLeast(35f)
                                onUpdateVitalsDraft { it.copy(tempInput = String.format(java.util.Locale.US, "%.1f", updated)) }
                            },
                            onPlus = {
                                val current = state.vitalsDraft.tempInput.toFloatOrNull() ?: 36.7f
                                val updated = (current + 0.1f).coerceAtMost(41f)
                                onUpdateVitalsDraft { it.copy(tempInput = String.format(java.util.Locale.US, "%.1f", updated)) }
                            },
                            keyboardType = KeyboardType.Decimal,
                            isDark = isDark
                        )

                        VitalRow(
                            label = stringResource(R.string.vital_sleep),
                            hint = stringResource(R.string.vital_sleep_hint),
                            placeholder = "7.5",
                            value = state.vitalsDraft.sleepHoursInput,
                            unit = "hrs",
                            onValueChange = { newValue ->
                                onUpdateVitalsDraft { it.copy(sleepHoursInput = newValue) }
                            },
                            onMinus = {
                                val current = state.vitalsDraft.sleepHoursInput.toFloatOrNull() ?: 7.5f
                                val updated = (current - 0.5f).coerceAtLeast(1f)
                                onUpdateVitalsDraft { it.copy(sleepHoursInput = String.format(java.util.Locale.US, "%.1f", updated)) }
                            },
                            onPlus = {
                                val current = state.vitalsDraft.sleepHoursInput.toFloatOrNull() ?: 7.5f
                                val updated = (current + 0.5f).coerceAtMost(16f)
                                onUpdateVitalsDraft { it.copy(sleepHoursInput = String.format(java.util.Locale.US, "%.1f", updated)) }
                            },
                            keyboardType = KeyboardType.Decimal,
                            isDark = isDark
                        )

                        OutlinedButton(
                            onClick = onToggleVitalsConfirmed,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = if (state.vitalsConfirmed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = stringResource(R.string.confirm), tint = if (state.vitalsConfirmed) EirEmerald else Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.confirm_vitals_text), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // STEP 4: MOOD & ENERGY
                if (state.checkupStep == 4) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = stringResource(R.string.step_4_desc), fontSize = 12.sp)

                        Emotional2DQuadrant(
                            selectedPoint = state.moodDraftPoint,
                            isInteractive = true,
                            isDarkMode = isDark,
                            height = 150.dp,
                            onPointChanged = { x, y -> onSetMoodDraftPoint(x, y) }
                        )

                        val moodOptions = listOf(
                            "Tranquilo" to R.string.mood_tag_calm,
                            "Enfocado" to R.string.mood_tag_focused,
                            "Motivado" to R.string.mood_tag_motivated,
                            "Cansado" to R.string.mood_tag_tired
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            moodOptions.forEach { (tagKey, stringResId) ->
                                FilterChip(
                                    selected = state.moodDraftLabel == tagKey,
                                    onClick = { onSetMoodDraftLabel(tagKey) },
                                    label = { Text(text = stringResource(stringResId), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Fixed Primary Action Button at bottom of screen modal
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                when (state.checkupStep) {
                    1 -> {
                        if (!hasCameraPermission) {
                            Button(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EirSky)
                            ) {
                                Text(text = stringResource(R.string.allow_camera), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            }
                        } else if (state.faceScanStatus == "done") {
                            Button(
                                onClick = { onSetStep(2) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EirSky)
                            ) {
                                Text(text = stringResource(R.string.next_voice_test), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = {
                                    onSetFaceScanning()
                                    takeRealPhoto(context, imageCapture) { bmp ->
                                        onFacePhotoCaptured(bmp)
                                    }
                                },
                                enabled = state.faceScanStatus != "scanning",
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EirSky)
                            ) {
                                Text(
                                    text = if (state.faceScanStatus == "scanning") stringResource(R.string.scanning_face) else stringResource(R.string.take_face_photo),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                    2 -> {
                        if (!hasAudioPermission) {
                            Button(
                                onClick = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EirSky)
                            ) {
                                Text(text = stringResource(R.string.allow_mic), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            }
                        } else if (state.voiceScanStatus == "recording") {
                            Button(
                                onClick = onStopAudioRecording,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EirRoseEmergency)
                            ) {
                                Text(text = stringResource(R.string.stop_recording_btn), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = { onSetStep(3) },
                                enabled = state.voiceScanStatus == "done",
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EirSky)
                            ) {
                                Text(text = stringResource(R.string.next_basic_signs), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                    3 -> {
                        Button(
                            onClick = { onSetStep(4) },
                            enabled = state.vitalsConfirmed,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EirSky)
                        ) {
                            Text(text = stringResource(R.string.next_mood_energy), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                    4 -> {
                        Button(
                            onClick = onCompleteCheckup,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EirEmerald)
                        ) {
                            Text(text = stringResource(R.string.finish_daily_checkup), fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

// CameraX Live Preview View
@Composable
private fun CameraPreviewView(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = CameraXPreview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

// Helper function to capture real photo from CameraX
private fun takeRealPhoto(
    context: Context,
    imageCapture: ImageCapture,
    onCaptured: (Bitmap) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()
                onCaptured(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

@Composable
private fun VitalRow(
    label: String,
    hint: String,
    placeholder: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Number,
    isDark: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = hint, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onMinus,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Text(text = "-", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        val filtered = if (keyboardType == KeyboardType.Decimal) {
                            newValue.filter { it.isDigit() || it == '.' }
                        } else {
                            newValue.filter { it.isDigit() }
                        }
                        onValueChange(filtered)
                    },
                    placeholder = {
                        Text(text = placeholder, fontSize = 11.sp, color = Color.Gray)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.width(96.dp),
                    shape = RoundedCornerShape(10.dp),
                    suffix = if (unit.isNotEmpty()) {
                        { Text(text = unit, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EirSky) }
                    } else null,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EirSky,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                IconButton(
                    onClick = onPlus,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EirSky.copy(alpha = 0.2f))
                ) {
                    Text(text = "+", fontWeight = FontWeight.Black, fontSize = 16.sp, color = EirSky)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "CheckupModal Step 1 Light Preview", heightDp = 600)
@Composable
fun CheckupModalStep1Preview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        CheckupModalContent(
            state = CheckupUiState(checkupOpen = true, checkupStep = 1),
            onClose = {},
            onSetStep = {},
            onFacePhotoCaptured = {},
            onSetFaceScanning = {},
            onCyclePhrase = {},
            onStartAudioRecording = {},
            onStopAudioRecording = {},
            onUpdateVitalsDraft = {},
            onToggleVitalsConfirmed = {},
            onSetMoodDraftPoint = { _, _ -> },
            onSetMoodDraftLabel = {},
            onCompleteCheckup = {}
        )
    }
}

@Preview(showBackground = true, name = "CheckupModal Step 2 Light Preview", heightDp = 600)
@Composable
fun CheckupModalStep2Preview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        CheckupModalContent(
            state = CheckupUiState(checkupOpen = true, checkupStep = 2, currentPhraseText = "Bitácora de Eir Sol 142..."),
            onClose = {},
            onSetStep = {},
            onFacePhotoCaptured = {},
            onSetFaceScanning = {},
            onCyclePhrase = {},
            onStartAudioRecording = {},
            onStopAudioRecording = {},
            onUpdateVitalsDraft = {},
            onToggleVitalsConfirmed = {},
            onSetMoodDraftPoint = { _, _ -> },
            onSetMoodDraftLabel = {},
            onCompleteCheckup = {}
        )
    }
}
