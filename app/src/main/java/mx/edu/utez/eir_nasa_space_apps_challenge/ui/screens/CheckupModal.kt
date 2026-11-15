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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.VitalsDraft
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.Emotional2DQuadrant
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
    val isDark = false
    val modalBg = if (isDark) Color(0xFF111827) else Color.White
    val modalBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header & 4-Step Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PASO ${state.checkupStep} DE 4 • OBLIGATORIO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = EirSky
                        )
                        Text(
                            text = when (state.checkupStep) {
                                1 -> "1. Escaneo Facial Rápido"
                                2 -> "2. Prueba de Voz Guiada"
                                3 -> "3. Signos Básicos del Kit"
                                else -> "4. Ánimo y Energía Hoy"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(18.dp))
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
                                    else if (isDark) Color(0xFF1E293B)
                                    else Color(0xFFE2E8F0)
                                )
                        )
                    }
                }
            }

            // STEP 1: REAL CAMERA FACE SCAN
            if (state.checkupStep == 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Toma una foto real de tu rostro. Usamos la cámara del dispositivo para verificar hinchazón microgravitacional o cansancio en los ojos.",
                        fontSize = 12.sp,
                        color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary,
                        lineHeight = 16.sp
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0F172A)
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
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasCameraPermission) {
                                    if (state.capturedFaceBitmap != null) {
                                        // Show real captured photo
                                        Image(
                                            bitmap = state.capturedFaceBitmap.asImageBitmap(),
                                            contentDescription = "Foto capturada",
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
                                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = EirSky, modifier = Modifier.size(32.dp))
                                        Text(text = "Se requiere acceso a la Cámara", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(top = 4.dp))
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

                            if (!hasCameraPermission) {
                                Button(
                                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EirSky)
                                ) {
                                    Text(text = "Permitir uso de Cámara", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (state.capturedFaceBitmap != null) {
                                            onSetFaceScanning()
                                        } else {
                                            onSetFaceScanning()
                                            takeRealPhoto(context, imageCapture) { bmp ->
                                                onFacePhotoCaptured(bmp)
                                            }
                                        }
                                    },
                                    enabled = state.faceScanStatus != "scanning",
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EirSky)
                                ) {
                                    Text(
                                        text = if (state.faceScanStatus == "scanning") "Escaneando rostro..."
                                        else if (state.capturedFaceBitmap != null) "Repetir Foto"
                                        else "Tomar Foto del Rostro",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }

                    if (state.faceFindings != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = EirEmerald.copy(alpha = 0.15f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Análisis óptico facial completado:", fontWeight = FontWeight.Black, fontSize = 11.sp, color = EirEmerald)
                                Text(text = "• ${state.faceFindings.fluidShift}", fontSize = 11.sp)
                                Text(text = "• ${state.faceFindings.eyeFatigue}", fontSize = 11.sp)
                                Text(text = "• ${state.faceFindings.skinTone}", fontSize = 11.sp)
                            }
                        }
                    }

                    Button(
                        onClick = { onSetStep(2) },
                        enabled = state.faceScanStatus == "done",
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EirSky)
                    ) {
                        Text(text = "Siguiente: Prueba de Voz →", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            // STEP 2: REAL VOICE RECORDING & RANDOM PHRASE CATALOG
            if (state.checkupStep == 2) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Lee en voz alta la frase aleatoria asignada. Grabamos el audio del micrófono real para analizar tono, ritmo de respiración y modulación.",
                        fontSize = 12.sp,
                        color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary,
                        lineHeight = 16.sp
                    )

                    OutlinedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "FRASE ALEATORIA DEL CATÁLOGO:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = EirIndigo)
                                Text(
                                    text = "🎲 Otra frase",
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
                            Button(
                                onClick = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EirSky)
                            ) {
                                Text(text = "Permitir uso del Micrófono", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            }
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
                                    contentDescription = "Micrófono Real",
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
                                    "idle" -> "Toca el micrófono para grabar la frase"
                                    "recording" -> "Grabando micrófono real... Toca para finalizar"
                                    else -> "Prueba de voz grabada y analizada"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (state.voiceFindings != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = EirEmerald.copy(alpha = 0.15f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Resultado de análisis acústico de voz:", fontWeight = FontWeight.Black, fontSize = 11.sp, color = EirEmerald)
                                Text(text = "• ${state.voiceFindings.breathingPace}", fontSize = 11.sp)
                                Text(text = "• ${state.voiceFindings.vocalFatigue}", fontSize = 11.sp)
                                Text(text = "• ${state.voiceFindings.nasalCongestion}", fontSize = 11.sp)
                            }
                        }
                    }

                    Button(
                        onClick = { onSetStep(3) },
                        enabled = state.voiceScanStatus == "done",
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EirSky)
                    ) {
                        Text(text = "Siguiente: Signos Básicos →", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            // STEP 3: VITALS
            if (state.checkupStep == 3) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Ajusta los 4 datos básicos de tu kit de bolsillo y confirma que los revisaste hoy.", fontSize = 12.sp)

                    VitalRow(label = "Latidos por minuto", hint = "Normal: 60-85", value = "${state.vitalsDraft.heartRate}", onMinus = { onUpdateVitalsDraft { it.copy(heartRate = (it.heartRate - 1).coerceAtLeast(40)) } }, onPlus = { onUpdateVitalsDraft { it.copy(heartRate = (it.heartRate + 1).coerceAtMost(160)) } }, isDark = isDark)
                    VitalRow(label = "Presión arterial alta", hint = "Normal: 115-125", value = "${state.vitalsDraft.sys}", onMinus = { onUpdateVitalsDraft { it.copy(sys = (it.sys - 2).coerceAtLeast(80)) } }, onPlus = { onUpdateVitalsDraft { it.copy(sys = (it.sys + 2).coerceAtMost(180)) } }, isDark = isDark)
                    VitalRow(label = "Calor corporal (°C)", hint = "Normal: 36.2°-37.2°", value = "${state.vitalsDraft.temp}°", onMinus = { onUpdateVitalsDraft { it.copy(temp = (it.temp - 0.1f).coerceAtLeast(35f)) } }, onPlus = { onUpdateVitalsDraft { it.copy(temp = (it.temp + 0.1f).coerceAtMost(41f)) } }, isDark = isDark)
                    VitalRow(label = "Horas que dormiste", hint = "Ideal: 7-8 horas", value = "${state.vitalsDraft.sleepHours}h", onMinus = { onUpdateVitalsDraft { it.copy(sleepHours = (it.sleepHours - 0.5f).coerceAtLeast(2f)) } }, onPlus = { onUpdateVitalsDraft { it.copy(sleepHours = (it.sleepHours + 0.5f).coerceAtMost(12f)) } }, isDark = isDark)

                    OutlinedButton(
                        onClick = onToggleVitalsConfirmed,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = if (state.vitalsConfirmed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = "Confirmar", tint = if (state.vitalsConfirmed) EirEmerald else Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Confirmo que estos son mis valores de hoy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onSetStep(4) },
                        enabled = state.vitalsConfirmed,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EirSky)
                    ) {
                        Text(text = "Siguiente: Ánimo y Energía →", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            // STEP 4: MOOD & ENERGY
            if (state.checkupStep == 4) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Por último, toca en el cuadro cómo está tu ánimo y cuánta energía sientes hoy.", fontSize = 12.sp)

                    Emotional2DQuadrant(
                        selectedPoint = state.moodDraftPoint,
                        isInteractive = true,
                        isDarkMode = isDark,
                        height = 150.dp,
                        onPointChanged = { x, y -> onSetMoodDraftPoint(x, y) }
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Tranquilo", "Enfocado", "Motivado", "Cansado").forEach { tag ->
                            FilterChip(
                                selected = state.moodDraftLabel == tag,
                                onClick = { onSetMoodDraftLabel(tag) },
                                label = { Text(text = tag, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Button(
                        onClick = onCompleteCheckup,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EirEmerald)
                    ) {
                        Text(text = "Finalizar Chequeo Diario ✓", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.Black)
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
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    isDark: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = hint, fontSize = 10.sp, color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onMinus, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE2E8F0))) {
                    Text(text = "-", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                Text(text = value, fontWeight = FontWeight.Black, fontSize = 14.sp)
                IconButton(onClick = onPlus, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(EirSky.copy(alpha = 0.2f))) {
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
