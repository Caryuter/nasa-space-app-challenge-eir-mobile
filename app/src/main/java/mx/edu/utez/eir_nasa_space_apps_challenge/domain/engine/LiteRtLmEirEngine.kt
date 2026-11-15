package mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds

sealed class LiteRtModelStatus {
    data class NotDownloaded(
        val sizeLabel: String = "~614 MB",
        val modelName: String = "Qwen3-0.6B (LiteRT-LM)"
    ) : LiteRtModelStatus()

    data class Downloading(
        val progressPercent: Int,
        val downloadedMb: String,
        val totalMb: String,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L
    ) : LiteRtModelStatus()

    object Initializing : LiteRtModelStatus()

    data class Ready(val modelName: String) : LiteRtModelStatus()

    data class Error(val message: String) : LiteRtModelStatus()
}

data class LiteRtLmState(
    val isInitialized: Boolean = false,
    val isBusy: Boolean = false,
    val activeModelName: String = "Mindfulness Eir (LiteRT-LM Ready)",
    val lastError: String? = null
)

class LiteRtLmEirEngine(private val context: Context) : EirAiEngine {

    private val executor = Executors.newSingleThreadExecutor()
    private val dispatcher = executor.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutex = Mutex()

    private var engine: Engine? = null
    private var conversation: Conversation? = null

    private val downloader = ModelDownloader(context)
    private var downloadJob: Job? = null

    private val fallbackEngine = LocalDeterministicEirEngine()

    private val _modelStatus = MutableStateFlow<LiteRtModelStatus>(LiteRtModelStatus.NotDownloaded())
    val modelStatus: StateFlow<LiteRtModelStatus> = _modelStatus.asStateFlow()

    private val _state = MutableStateFlow(LiteRtLmState())
    val state: StateFlow<LiteRtLmState> = _state.asStateFlow()

    private val DEFAULT_MODEL_URL = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm"
    private val DEFAULT_MODEL_NAME = "Qwen3-0.6B.litertlm"

    init {
        scope.launch {
            checkExistingModelOrSetNotDownloaded()
        }
    }

    private suspend fun checkExistingModelOrSetNotDownloaded() {
        mutex.withLock {
            val possibleModelFiles = listOf(
                File(context.filesDir, DEFAULT_MODEL_NAME),
                File(context.filesDir, "gemma-4-E2B-it.litertlm"),
                File(context.filesDir, "model.litertlm")
            )

            val existingModel = possibleModelFiles.firstOrNull { it.exists() && it.length() > 500_000_000L }

            if (existingModel != null) {
                initLiteRtLmModel(existingModel)
            } else {
                _modelStatus.value = LiteRtModelStatus.NotDownloaded(
                    sizeLabel = "~614 MB",
                    modelName = "Qwen3-0.6B (LiteRT-LM)"
                )
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = bytes / 1024.0
        if (kb < 1000.0) {
            return String.format(java.util.Locale.US, "%.1f KB", kb)
        }
        val mb = kb / 1024.0
        if (mb < 1000.0) {
            return String.format(java.util.Locale.US, "%.1f MB", mb)
        }
        val gb = mb / 1024.0
        return String.format(java.util.Locale.US, "%.2f GB", gb)
    }

    fun startModelDownload(customUrl: String? = null) {
        if (_modelStatus.value is LiteRtModelStatus.Downloading || _modelStatus.value is LiteRtModelStatus.Initializing) {
            return
        }

        // Set status to Downloading immediately so UI transitions and disables repeated clicks
        _modelStatus.value = LiteRtModelStatus.Downloading(
            progressPercent = 0,
            downloadedMb = "0 KB",
            totalMb = formatFileSize(ModelDownloader.EXPECTED_MODEL_BYTES),
            downloadedBytes = 0L,
            totalBytes = ModelDownloader.EXPECTED_MODEL_BYTES
        )

        downloadJob?.cancel()
        downloadJob = scope.launch {
            val url = customUrl ?: DEFAULT_MODEL_URL
            downloader.downloadModel(url, DEFAULT_MODEL_NAME).collect { state ->
                when (state) {
                    is ModelDownloadState.Progress -> {
                        val downloadedStr = formatFileSize(state.downloadedBytes)
                        val totalStr = formatFileSize(state.totalBytes)

                        _modelStatus.value = LiteRtModelStatus.Downloading(
                            progressPercent = state.percentage,
                            downloadedMb = downloadedStr,
                            totalMb = totalStr,
                            downloadedBytes = state.downloadedBytes,
                            totalBytes = state.totalBytes
                        )
                    }
                    is ModelDownloadState.Success -> {
                        mutex.withLock {
                            _modelStatus.value = LiteRtModelStatus.Initializing
                            initLiteRtLmModel(state.file)
                        }
                    }
                    is ModelDownloadState.Error -> {
                        _modelStatus.value = LiteRtModelStatus.Error(state.message)
                    }
                    is ModelDownloadState.Canceled -> {
                        _modelStatus.value = LiteRtModelStatus.NotDownloaded()
                    }
                    is ModelDownloadState.Idle -> {}
                }
            }
        }
    }

    fun cancelModelDownload() {
        downloadJob?.cancel()
        downloadJob = null

        val targetFile = File(context.filesDir, DEFAULT_MODEL_NAME)
        if (targetFile.exists()) {
            try { targetFile.delete() } catch (_: Exception) {}
        }
        val tempFile = File(context.filesDir, "$DEFAULT_MODEL_NAME.tmp")
        if (tempFile.exists()) {
            try { tempFile.delete() } catch (_: Exception) {}
        }
        _modelStatus.value = LiteRtModelStatus.NotDownloaded()
    }

    private fun initLiteRtLmModel(modelFile: File) {
        val backends = listOf(Backend.GPU(), Backend.CPU())
        var initializedEngine: Engine? = null
        var lastErr: Throwable? = null

        for (b in backends) {
            try {
                val config = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = b,
                    cacheDir = context.cacheDir.path
                )
                initializedEngine = Engine(config).also { it.initialize() }
                break
            } catch (t: Throwable) {
                lastErr = t
            }
        }

        if (initializedEngine == null) {
            _state.value = LiteRtLmState(
                isInitialized = false,
                activeModelName = "Error de Inicialización",
                lastError = lastErr?.message
            )
            _modelStatus.value = LiteRtModelStatus.Error("Error al iniciar motor IA: ${lastErr?.message}")
            return
        }

        try {
            engine = initializedEngine

            val systemPromptText = """
                Eres EIR, médica y asistente de salud de la tripulación en misiones espaciales.
                Responde en español de forma empática, directa y breve (máximo 2 a 3 oraciones).
                Si reportan dolor de pecho, falta de aire o emergencia grave: ordena reposo inmediato y activar botón SOS.
                Para preguntas o saludos: sé cordial y ten en cuenta sus signos vitales.
            """.trimIndent()

            conversation = initializedEngine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(systemPromptText),
                    samplerConfig = SamplerConfig(10, 0.95, 0.7, 42),
                    maxOutputToken = 200
                )
            )
            _state.value = LiteRtLmState(
                isInitialized = true,
                activeModelName = "LiteRT-LM ${modelFile.nameWithoutExtension} On-Device"
            )
            _modelStatus.value = LiteRtModelStatus.Ready(
                modelName = "LiteRT-LM ${modelFile.nameWithoutExtension} On-Device"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _state.value = LiteRtLmState(
                isInitialized = false,
                activeModelName = "Error de Inicialización",
                lastError = e.message
            )
            _modelStatus.value = LiteRtModelStatus.Error("Error al iniciar el motor de IA: ${e.message}")
        }
    }

    override fun queryStream(
        userMessage: String,
        biometricContext: String,
        forceSimpler: Boolean,
        giveSteps: Boolean
    ): Flow<String> = channelFlow {
        if (!_state.value.isInitialized || conversation == null) {
            send(fallbackEngine.query(userMessage, biometricContext, forceSimpler, giveSteps))
            return@channelFlow
        }

        val promptWithContext = buildString {
            append("Signos vitales actuales: ").append(biometricContext)
            append("\nMensaje del astronauta: ").append(userMessage)
            if (forceSimpler) append(" (Explícamelo aún más simple)")
            if (giveSteps) append(" (Dame los pasos exactos a seguir)")
        }

        var emittedAny = false
        try {
            withTimeout(20_000L) {
                withContext(dispatcher) {
                    _state.value = _state.value.copy(isBusy = true)
                    val replies = checkNotNull(conversation).sendMessageAsync(promptWithContext)
                    replies.collect { message ->
                        val text = message.toString()
                        if (text.isNotEmpty()) {
                            emittedAny = true
                            send(text)
                        }
                    }
                    _state.value = _state.value.copy(isBusy = false)
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            _state.value = _state.value.copy(isBusy = false, lastError = t.message)
        }

        if (!emittedAny) {
            val fallback = fallbackEngine.query(userMessage, biometricContext, forceSimpler, giveSteps)
            send(fallback)
        }
    }

    override suspend fun query(
        userMessage: String,
        biometricContext: String,
        forceSimpler: Boolean,
        giveSteps: Boolean
    ): String {
        val builder = StringBuilder()
        queryStream(userMessage, biometricContext, forceSimpler, giveSteps).collect { chunk ->
            builder.append(chunk)
        }
        val result = builder.toString()
        return if (result.isNotBlank()) result else fallbackEngine.query(userMessage, biometricContext, forceSimpler, giveSteps)
    }

    fun close() {
        scope.launch {
            mutex.withLock {
                try {
                    conversation?.cancelProcess()
                    kotlinx.coroutines.delay(100.milliseconds)
                    conversation?.close()
                    conversation = null
                    engine?.close()
                    engine = null
                } catch (_: Exception) {}
            }
        }
    }
}
