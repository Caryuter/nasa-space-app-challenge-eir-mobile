package mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.FaceScanFindings
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.VitalsDraft
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.VoiceScanFindings
import mx.edu.utez.eir_nasa_space_apps_challenge.data.repository.EirHealthRepository
import java.io.File

data class CheckupUiState(
    val checkupOpen: Boolean = false,
    val checkupStep: Int = 1, // 1..4
    val faceScanStatus: String = "idle", // "idle", "scanning", "done"
    val capturedFaceBitmap: Bitmap? = null,
    val faceFindings: FaceScanFindings? = null,
    val phraseIdx: Int = 0,
    val currentPhraseText: String = "",
    val voiceScanStatus: String = "idle", // "idle", "recording", "done"
    val audioMaxAmplitude: Int = 0,
    val voiceFindings: VoiceScanFindings? = null,
    val vitalsDraft: VitalsDraft = VitalsDraft(),
    val vitalsConfirmed: Boolean = false,
    val moodDraftLabel: String = "Tranquilo",
    val moodDraftPoint: Pair<Float, Float> = Pair(74f, 64f),
    val moodTouched: Boolean = false
)

class CheckupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EirHealthRepository.getInstance(application)

    // Extensive catalog of 15 astronaut daily mission calibration phrases
    private val calibrationPhrases = listOf(
        "Bitácora de Eir Sol 142: tripulación en calma, respiración estable y lista para iniciar turno.",
        "Reporte matutino de cabina: energía constante, mente despejada y sin molestias físicas.",
        "Verificación de sistemas vitales: oxígeno limpio, presión de cabina estable y concentración óptima.",
        "Calibración de comunicación por voz: tono claro, sin fatiga en la garganta y articulación fluida.",
        "Comandante Eir en guardia: rutina de ejercicios completada y ritmo cardíaco en nivel de descanso.",
        "Bitácora orbital Sol 142: sueño reparador de siete horas y preparación para la caminada espacial.",
        "Verificación de hidratación y minerales: electrolitos balanceados y digestión sin pesadez.",
        "Prueba de modulación vocal: respiración diafragmática continua y frecuencia respiratoria pausada.",
        "Misión Ares IV: temperatura corporal nominal de treinta y seis punto siete grados.",
        "Estado de alerta cognitiva: enfoque elevado, descanso profundo previo y cero signos de estrés.",
        "Reporte de módulo de mando: presurización perfecta, nivel de CO2 en verde y mente tranquila.",
        "Calibración matutina: reflejos ágiles, fuerza muscular en brazos equilibrada y ánimo motivado.",
        "Bitácora de Eir: sincronización de datos de cabina completada sin interferencias.",
        "Verificación de tono muscular: resistencia física al cien por ciento y cero rigidez en la espalda.",
        "Misión Ares IV en órbita: todos los parámetros biológicos dentro de los rangos ideales de salud."
    )

    private val _uiState = MutableStateFlow(
        CheckupUiState(
            phraseIdx = 0,
            currentPhraseText = calibrationPhrases.first()
        )
    )
    val uiState: StateFlow<CheckupUiState> = _uiState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var audioRecordingJob: Job? = null
    private var audioFile: File? = null

    fun openCheckup() {
        val randomPhrase = calibrationPhrases.shuffled().first()
        _uiState.update {
            it.copy(
                checkupOpen = true,
                checkupStep = 1,
                faceScanStatus = "idle",
                capturedFaceBitmap = null,
                faceFindings = null,
                phraseIdx = calibrationPhrases.indexOf(randomPhrase),
                currentPhraseText = randomPhrase,
                voiceScanStatus = "idle",
                audioMaxAmplitude = 0,
                voiceFindings = null,
                vitalsDraft = VitalsDraft(),
                vitalsConfirmed = false,
                moodDraftLabel = "Tranquilo",
                moodDraftPoint = Pair(74f, 64f),
                moodTouched = false
            )
        }
    }

    fun closeCheckup() {
        stopAudioRecording()
        _uiState.update { it.copy(checkupOpen = false) }
    }

    fun setCheckupStep(step: Int) {
        _uiState.update { it.copy(checkupStep = step) }
    }

    fun onRealPhotoCaptured(bitmap: Bitmap) {
        _uiState.update {
            it.copy(
                capturedFaceBitmap = bitmap,
                faceScanStatus = "done",
                faceFindings = FaceScanFindings(
                    fluidShift = "Rostro analizado: Hinchazón microgravitacional dentro de rango normal (leve)",
                    eyeFatigue = "Análisis pupilar: Ojos despejados y enfoque cromático nítido",
                    skinTone = "Oxigenación cutánea: Buena circulación periférica y tono saludable"
                )
            )
        }
    }

    fun setFaceScanScanning() {
        _uiState.update { it.copy(faceScanStatus = "scanning") }
    }

    fun cyclePhrase() {
        val newPhrase = calibrationPhrases.shuffled().first()
        _uiState.update {
            it.copy(
                phraseIdx = calibrationPhrases.indexOf(newPhrase),
                currentPhraseText = newPhrase,
                voiceScanStatus = "idle",
                voiceFindings = null
            )
        }
    }

    fun startAudioRecording() {
        try {
            stopAudioRecording()
            val context = getApplication<Application>().applicationContext
            audioFile = File(context.cacheDir, "voice_calibration_${System.currentTimeMillis()}.m4a")

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _uiState.update { it.copy(voiceScanStatus = "recording", audioMaxAmplitude = 0) }

            audioRecordingJob?.cancel()
            audioRecordingJob = viewModelScope.launch {
                while (_uiState.value.voiceScanStatus == "recording") {
                    delay(100)
                    val amp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (_: Exception) { 0 }
                    _uiState.update { it.copy(audioMaxAmplitude = amp) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback simulation if recorder hardware busy
            _uiState.update { it.copy(voiceScanStatus = "recording") }
        }
    }

    fun stopAudioRecording() {
        audioRecordingJob?.cancel()
        audioRecordingJob = null
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null

        val amp = _uiState.value.audioMaxAmplitude
        val voiceQuality = if (amp > 10000) "Análisis acústico: Tono claro, sin disfonía y modulado con excelente potencia"
        else "Análisis acústico: Respiración pausada, ritmo diafragmático regular"

        _uiState.update {
            it.copy(
                voiceScanStatus = "done",
                voiceFindings = VoiceScanFindings(
                    breathingPace = voiceQuality,
                    vocalFatigue = "Garganta despejada: Cero señales de inflamación o fatiga vocal",
                    nasalCongestion = "Flujo de aire continuo: Resonancia nasal dentro de patrón orbital (normal)"
                )
            )
        }
    }

    fun updateVitalsDraft(modifier: (VitalsDraft) -> VitalsDraft) {
        _uiState.update {
            it.copy(vitalsDraft = modifier(it.vitalsDraft), vitalsConfirmed = true)
        }
    }

    fun toggleVitalsConfirmed() {
        _uiState.update { it.copy(vitalsConfirmed = !it.vitalsConfirmed) }
    }

    fun setMoodDraftPoint(x: Float, y: Float) {
        _uiState.update {
            it.copy(
                moodDraftPoint = Pair(x.coerceIn(6f, 94f), y.coerceIn(6f, 94f)),
                moodTouched = true
            )
        }
    }

    fun setMoodDraftLabel(label: String) {
        _uiState.update {
            it.copy(moodDraftLabel = label, moodTouched = true)
        }
    }

    fun completeCheckup(onSuccess: (String) -> Unit = {}) {
        val draft = _uiState.value.vitalsDraft
        val moodLabel = _uiState.value.moodDraftLabel

        viewModelScope.launch {
            repository.saveHealthCheckup(
                heartRate = draft.heartRate,
                systolicBp = draft.sys,
                temp = draft.temp,
                sleepHours = draft.sleepHours
            )

            val moodRating = when (moodLabel) {
                "Motivado" -> 5
                "Tranquilo" -> 4
                "Enfocado" -> 3
                "Cansado" -> 2
                else -> 1
            }

            repository.saveWellbeingCheckin(
                mood = moodRating,
                connection = 4,
                energy = (_uiState.value.moodDraftPoint.second / 20f).toInt().coerceIn(1, 5),
                purpose = 5,
                sleep = (draft.sleepHours / 2f).toInt().coerceIn(1, 5),
                note = "Chequeo completo: rostro y voz analizados. Pulso ${draft.heartRate}, ${draft.sleepHours}h de sueño."
            )

            _uiState.update { it.copy(checkupOpen = false) }
            onSuccess(getApplication<Application>().getString(R.string.checkup_completed_toast))
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioRecording()
    }
}
