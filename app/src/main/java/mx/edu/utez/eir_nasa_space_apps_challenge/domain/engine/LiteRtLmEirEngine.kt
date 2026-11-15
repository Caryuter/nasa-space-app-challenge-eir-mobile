package mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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

    private val _modelStatus = MutableStateFlow<LiteRtModelStatus>(LiteRtModelStatus.NotDownloaded())
    val modelStatus: StateFlow<LiteRtModelStatus> = _modelStatus.asStateFlow()

    private val _state = MutableStateFlow(LiteRtLmState())
    val state: StateFlow<LiteRtLmState> = _state.asStateFlow()

    private val DEFAULT_MODEL_URL = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm"
    private val DEFAULT_MODEL_NAME = "Qwen3-0.6B.litertlm"

    private val systemPrompt = """
        Eres EIR, asistente médica y de salud de la tripulación en la misión espacial Ares IV en Marte.
        Responde en español de forma empática, profesional, directa y breve (máximo 2 a 3 oraciones).
        Analiza los síntomas considerando las particularidades de microgravedad y vida en el espacio.
        Si reportan dolor de pecho opresivo, falta grave de aire o pérdida de conocimiento: ordena reposo inmediato y activar el botón rojo SOS.
    """.trimIndent()

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
                _modelStatus.value = LiteRtModelStatus.Initializing
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

    private suspend fun initLiteRtLmModel(modelFile: File) {
        release()
        try {
            Log.i("LiteRtLm", "Inicializando LiteRT-LM con Backend.CPU...")
            val config = EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(),
                cacheDir = context.cacheDir.path
            )
            val eng = Engine(config).also { it.initialize() }
            val conv = eng.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(systemPrompt),
                    samplerConfig = SamplerConfig(10, 0.95, 0.8, 42),
                    maxOutputToken = 200
                )
            )

            engine = eng
            conversation = conv
            _state.value = LiteRtLmState(
                isInitialized = true,
                activeModelName = "LiteRT-LM ${modelFile.nameWithoutExtension} (CPU On-Device)"
            )
            _modelStatus.value = LiteRtModelStatus.Ready(
                modelName = "LiteRT-LM ${modelFile.nameWithoutExtension} (CPU On-Device)"
            )
            Log.i("LiteRtLm", "¡LiteRT-LM inicializado exitosamente en CPU!")
        } catch (e: Exception) {
            Log.e("LiteRtLm", "Error al inicializar LiteRT-LM en CPU: ${e.message}", e)
            release()
            _state.value = LiteRtLmState(
                isInitialized = false,
                activeModelName = "Error de Inicialización",
                lastError = e.message
            )
            _modelStatus.value = LiteRtModelStatus.Error("Error al iniciar motor IA: ${e.message}")
        }
    }

    override fun queryStream(
        userMessage: String,
        biometricContext: String,
        forceSimpler: Boolean,
        giveSteps: Boolean
    ): Flow<String> = channelFlow {
        if (!_state.value.isInitialized || conversation == null) {
            send("⚠️ El motor de IA local no está inicializado o el modelo no está listo.")
            return@channelFlow
        }

        val prompt = buildString {
            if (biometricContext.isNotBlank()) {
                append("Signos vitales de cabina Sol 142: ").append(biometricContext).append("\n")
            }
            append("Consulta del astronauta: ").append(userMessage)
            if (forceSimpler) append(" (Por favor responde de manera más sencilla y comprensible)")
            if (giveSteps) append(" (Proporciona los pasos claros y ordenados a seguir)")
        }

        withContext(dispatcher) {
            val conv = conversation
            if (conv == null) {
                send("⚠️ La sesión de conversación no está disponible.")
                return@withContext
            }

            _state.update { it.copy(isBusy = true) }
            try {
                val replies = conv.sendMessageAsync(prompt)
                replies.collect { message ->
                    val text = message.toString()
                    if (text.isNotEmpty()) {
                        send(text)
                    }
                }
            } catch (c: CancellationException) {
                try {
                    conv.cancelProcess()
                } catch (_: Exception) {}
                throw c
            } catch (t: Throwable) {
                Log.e("LiteRtLm", "Error durante inferencia con LiteRT-LM: ${t.message}", t)
                _state.update { it.copy(lastError = t.message) }
                send("⚠️ Error en el motor de IA local: ${t.localizedMessage ?: t.message}")
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
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
        return if (result.isNotBlank()) result else "⚠️ Sin respuesta del motor de IA."
    }

    private suspend fun release() {
        _state.update { it.copy(isInitialized = false) }
        while (_state.value.isBusy) {
            try {
                conversation?.cancelProcess()
            } catch (_: Exception) {}
            delay(100.milliseconds)
        }
        try {
            conversation?.close()
        } catch (_: Exception) {}
        conversation = null
        try {
            engine?.close()
        } catch (_: Exception) {}
        engine = null
    }

    fun close() {
        scope.launch {
            mutex.withLock {
                release()
            }
            executor.shutdown()
        }.invokeOnCompletion {
            scope.cancel()
        }
    }
}
