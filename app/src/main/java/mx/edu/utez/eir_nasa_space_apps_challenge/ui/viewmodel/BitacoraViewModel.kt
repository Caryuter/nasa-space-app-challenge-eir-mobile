package mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.JournalEntry
import mx.edu.utez.eir_nasa_space_apps_challenge.data.repository.EirHealthRepository
import java.util.Locale

data class BitacoraUiState(
    val moodLabel: String = "Tranquilo",
    val moodPoint: Pair<Float, Float> = Pair(74f, 64f),
    val journalInput: String = "",
    val journalEntries: List<JournalEntry> = emptyList(),
    val breathingActive: Boolean = false,
    val breathingPhase: Int = 0, // 0..3
    val isRecordingVoice: Boolean = false,
    val voiceRms: Float = 0f,
    val recordingDurationSec: Int = 0,
    val isTtsSpeaking: Boolean = false,
    val activeTtsEntryId: Long? = null,
    val speechError: String? = null,
    val speechSuccessNotice: String? = null
)

class BitacoraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EirHealthRepository.getInstance(application)

    private val initialJournal = listOf(
        JournalEntry(
            id = 1L,
            sol = "Sol 142 • Hoy 08:15",
            mood = "Tranquilo",
            x = 74f,
            y = 64f,
            text = "Dormí mucho mejor después de bajar un poco la temperatura de mi cabina. Me siento listo para las pruebas de hoy."
        ),
        JournalEntry(
            id = 2L,
            sol = "Sol 141 • Ayer 21:30",
            mood = "Nostálgico",
            x = 50f,
            y = 44f,
            text = "Vi una foto de mi familia antes de dormir. Se siente la distancia de casa, pero hacer los ejercicios de respiración me relajó."
        ),
        JournalEntry(
            id = 3L,
            sol = "Sol 140 • Hace 2 días",
            mood = "Motivado",
            x = 84f,
            y = 80f,
            text = "Terminamos de calibrar los paneles solares antes de tiempo. Gran equipo a bordo."
        )
    )

    private val _uiState = MutableStateFlow(BitacoraUiState(journalEntries = initialJournal))
    val uiState: StateFlow<BitacoraUiState> = _uiState.asStateFlow()

    private var breathingJob: Job? = null
    private var recordingTimerJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        // Inicializar Text-to-Speech (TTS)
        tts = TextToSpeech(application.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale("es", "MX")
                val res = tts?.setLanguage(locale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale("es", "ES"))
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _uiState.update { it.copy(isTtsSpeaking = true) }
                    }

                    override fun onDone(utteranceId: String?) {
                        _uiState.update { it.copy(isTtsSpeaking = false, activeTtsEntryId = null) }
                    }

                    override fun onError(utteranceId: String?) {
                        _uiState.update { it.copy(isTtsSpeaking = false, activeTtsEntryId = null) }
                    }
                })
                isTtsInitialized = true
            }
        }

        viewModelScope.launch {
            repository.wellbeingCheckinsFlow.collect { checkins ->
                if (checkins.isNotEmpty()) {
                    val newEntries = checkins.map { ch ->
                        JournalEntry(
                            id = ch.recordId.hashCode().toLong(),
                            sol = "Sol 142 • " + ch.recordedAt.take(10),
                            mood = when (ch.mood) {
                                5 -> "Motivado"
                                4 -> "Tranquilo"
                                3 -> "Enfocado"
                                2 -> "Cansado"
                                else -> "Nostálgico"
                            },
                            x = (ch.mood * 18f).coerceIn(10f, 90f),
                            y = (ch.energy * 18f).coerceIn(10f, 90f),
                            text = ch.note ?: "Registro guardado en Eir."
                        )
                    }
                    _uiState.update { current ->
                        val merged = (newEntries + current.journalEntries).distinctBy { it.id }
                        current.copy(journalEntries = merged)
                    }
                }
            }
        }
    }

    // --- RESPIRACIÓN ---
    fun toggleBreathing() {
        val active = !_uiState.value.breathingActive
        _uiState.update { it.copy(breathingActive = active, breathingPhase = 0) }
        if (active) {
            breathingJob?.cancel()
            breathingJob = viewModelScope.launch {
                while (true) {
                    delay(4000)
                    _uiState.update { it.copy(breathingPhase = (it.breathingPhase + 1) % 4) }
                }
            }
        } else {
            breathingJob?.cancel()
        }
    }

    // --- ENTRADA DE TEXTO ---
    fun setJournalInput(text: String) {
        _uiState.update { it.copy(journalInput = text) }
    }

    fun clearSpeechFeedback() {
        _uiState.update { it.copy(speechError = null, speechSuccessNotice = null) }
    }

    // --- GRABACIÓN POR VOZ (SPEECH-TO-TEXT) ---
    fun startVoiceRecording() {
        clearSpeechFeedback()
        viewModelScope.launch(Dispatchers.Main) {
            val context = getApplication<Application>().applicationContext
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _uiState.update {
                    it.copy(
                        speechError = "El reconocimiento de voz del sistema no está habilitado en este entorno. Puedes usar la opción de dictado rápido o escribir.",
                        isRecordingVoice = false
                    )
                }
                return@launch
            }

            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _uiState.update {
                                it.copy(
                                    isRecordingVoice = true,
                                    speechError = null,
                                    speechSuccessNotice = null,
                                    recordingDurationSec = 0
                                )
                            }
                        }

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {
                            _uiState.update { it.copy(voiceRms = rmsdB.coerceIn(0f, 10f)) }
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            val errorMsg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No se detectó voz clara. Acércate más al micrófono e intenta de nuevo."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo agotado sin detectar voz."
                                SpeechRecognizer.ERROR_AUDIO -> "Error al capturar audio del micrófono."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Se requiere permiso de grabación de audio."
                                else -> "No se pudo completar el reconocimiento de voz (código $error)."
                            }
                            stopRecordingInternal()
                            _uiState.update { it.copy(speechError = errorMsg, isRecordingVoice = false) }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognized = matches?.firstOrNull()?.trim()
                            if (!recognized.isNullOrEmpty()) {
                                appendVoiceText(recognized)
                            } else {
                                _uiState.update { it.copy(isRecordingVoice = false) }
                            }
                            stopRecordingInternal()
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                            if (!partial.isNullOrEmpty()) {
                                // Opcional: mostrar lo que va capturando
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-MX")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.startListening(intent)
                _uiState.update {
                    it.copy(
                        isRecordingVoice = true,
                        speechError = null,
                        speechSuccessNotice = null,
                        recordingDurationSec = 0
                    )
                }

                recordingTimerJob?.cancel()
                recordingTimerJob = viewModelScope.launch {
                    while (true) {
                        delay(1000)
                        _uiState.update { it.copy(recordingDurationSec = it.recordingDurationSec + 1) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        speechError = "Error al inicializar micrófono: ${e.localizedMessage}",
                        isRecordingVoice = false
                    )
                }
            }
        }
    }

    fun stopVoiceRecording() {
        viewModelScope.launch(Dispatchers.Main) {
            stopRecordingInternal()
        }
    }

    private fun stopRecordingInternal() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _uiState.update { it.copy(isRecordingVoice = false, voiceRms = 0f) }
    }

    fun appendVoiceText(text: String) {
        val current = _uiState.value.journalInput.trim()
        val updated = if (current.isEmpty()) text else "$current $text"
        _uiState.update {
            it.copy(
                journalInput = updated,
                isRecordingVoice = false,
                speechSuccessNotice = "Voz convertida a texto con éxito"
            )
        }
    }

    fun useSampleSpeechTranscription() {
        val sampleThoughts = listOf(
            "Hoy en la cabina logré mantener el ritmo de trabajo sin tensión. El ejercicio me ayudó a despejar la mente y me siento en calma.",
            "Fue un día demandante calibrando los subsistemas, pero me tomé unos minutos de respiración y ahora me siento renovado y enfocado.",
            "Dormí profundamente anoche y los niveles de energía están al máximo. Listo para la siguiente jornada de exploración."
        )
        val selected = sampleThoughts.random()
        appendVoiceText(selected)
    }

    // --- REPRODUCCIÓN POR VOZ (TEXT-TO-SPEECH) ---
    fun speakJournalText(customText: String? = null, entryId: Long? = null) {
        val textToSpeak = customText ?: _uiState.value.journalInput.ifBlank {
            _uiState.value.journalEntries.firstOrNull()?.text ?: "No hay texto registrado en tu bitácora para reproducir."
        }

        // Si ya está reproduciendo este mismo contenido, pausar/detener
        if (_uiState.value.isTtsSpeaking && _uiState.value.activeTtsEntryId == entryId) {
            stopTts()
            return
        }

        if (isTtsInitialized) {
            _uiState.update { it.copy(isTtsSpeaking = true, activeTtsEntryId = entryId) }
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "journal_${entryId ?: "draft"}")
        } else {
            // Si el motor TTS no está disponible de inmediato, simulación suave de feedback
            _uiState.update { it.copy(isTtsSpeaking = true, activeTtsEntryId = entryId) }
            viewModelScope.launch {
                delay(2500)
                _uiState.update { it.copy(isTtsSpeaking = false, activeTtsEntryId = null) }
            }
        }
    }

    fun stopTts() {
        tts?.stop()
        _uiState.update { it.copy(isTtsSpeaking = false, activeTtsEntryId = null) }
    }

    // --- GUARDAR ENTRADA ---
    fun saveJournalEntry() {
        val text = _uiState.value.journalInput.trim()
        if (text.isBlank()) return

        val moodRating = when (_uiState.value.moodLabel) {
            "Motivado" -> 5
            "Tranquilo" -> 4
            "Enfocado" -> 3
            "Cansado" -> 2
            else -> 1
        }

        viewModelScope.launch {
            repository.saveWellbeingCheckin(
                mood = moodRating,
                connection = 4,
                energy = (_uiState.value.moodPoint.second / 20f).toInt().coerceIn(1, 5),
                purpose = 5,
                sleep = 4,
                note = text
            )

            setJournalInput("")
            clearSpeechFeedback()
            stopTts()
        }
    }

    override fun onCleared() {
        super.onCleared()
        breathingJob?.cancel()
        recordingTimerJob?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
