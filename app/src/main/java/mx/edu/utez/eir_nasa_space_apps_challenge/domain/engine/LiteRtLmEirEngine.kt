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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
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

    private val _modelStatus = MutableStateFlow<LiteRtModelStatus>(LiteRtModelStatus.NotDownloaded())
    val modelStatus: StateFlow<LiteRtModelStatus> = _modelStatus.asStateFlow()

    private val _state = MutableStateFlow(LiteRtLmState())
    val state: StateFlow<LiteRtLmState> = _state.asStateFlow()

    private val DEFAULT_MODEL_URL = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm"
    private val DEFAULT_MODEL_NAME = "Qwen3-0.6B.litertlm"

    private val systemPrompt = """
        Eres EIR, asistente médica y de salud de la tripulación en la misión Ares IV en Marte.
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

    private suspend fun initLiteRtLmModel(modelFile: File) {
        val cpuThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        val backendsToTry = listOf<Pair<String, () -> Backend>>(
            "GPU" to { Backend.GPU() },
            "CPU" to { Backend.CPU(cpuThreads, cpuThreads) }
        )

        var validatedEngine: Engine? = null
        var validatedConversation: Conversation? = null
        var selectedBackendName = ""
        var lastErr: Throwable? = null

        for ((name, backendFactory) in backendsToTry) {
            var candidateEngine: Engine? = null
            var testConv: Conversation? = null
            try {
                Log.i("LiteRtLm", "Inicializando LiteRT-LM con backend $name...")
                val config = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = backendFactory(),
                    cacheDir = context.cacheDir.path
                )
                val eng = Engine(config)
                eng.initialize()
                candidateEngine = eng

                // Warm-up de inferencia para comprobar compatibilidad real de drivers
                Log.i("LiteRtLm", "Ejecutando warm-up de verificación en $name...")
                testConv = eng.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(systemPrompt),
                        maxOutputToken = 8
                    )
                )

                withTimeout(12_000L) {
                    val warmupStream = testConv.sendMessageAsync("Diagnóstico de prueba")
                    warmupStream.firstOrNull()
                }

                testConv.close()
                testConv = null

                // Si superó el warm-up, creamos la conversación principal
                val activeConv = eng.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(systemPrompt),
                        samplerConfig = SamplerConfig(10, 0.95, 0.7, 42),
                        maxOutputToken = 300
                    )
                )

                validatedEngine = eng
                validatedConversation = activeConv
                selectedBackendName = name
                Log.i("LiteRtLm", "¡Backend $name validado exitosamente!")
                break
            } catch (t: Throwable) {
                Log.w("LiteRtLm", "Fallo al validar backend $name: ${t.message}. Probando siguiente backend...", t)
                lastErr = t
                try { testConv?.close() } catch (_: Exception) {}
                try { candidateEngine?.close() } catch (_: Exception) {}
            }
        }

        if (validatedEngine == null || validatedConversation == null) {
            _state.value = LiteRtLmState(
                isInitialized = false,
                activeModelName = "Error de Inicialización",
                lastError = lastErr?.message
            )
            _modelStatus.value = LiteRtModelStatus.Error("Error al iniciar motor IA: ${lastErr?.message}")
            return
        }

        engine = validatedEngine
        conversation = validatedConversation
        _state.value = LiteRtLmState(
            isInitialized = true,
            activeModelName = "LiteRT-LM ${modelFile.nameWithoutExtension} ($selectedBackendName) On-Device"
        )
        _modelStatus.value = LiteRtModelStatus.Ready(
            modelName = "LiteRT-LM ${modelFile.nameWithoutExtension} ($selectedBackendName) On-Device"
        )
    }

    override fun queryStream(
        userMessage: String,
        biometricContext: String,
        forceSimpler: Boolean,
        giveSteps: Boolean
    ): Flow<String> = channelFlow {
        if (!_state.value.isInitialized || engine == null) {
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

        var emittedAny = false
        try {
            withContext(dispatcher) {
                _state.value = _state.value.copy(isBusy = true)
                val currentEngine = checkNotNull(engine)

                if (conversation == null) {
                    conversation = currentEngine.createConversation(
                        ConversationConfig(
                            systemInstruction = Contents.of(systemPrompt),
                            samplerConfig = SamplerConfig(10, 0.95, 0.7, 42),
                            maxOutputToken = 300
                        )
                    )
                }

                val currentConv = checkNotNull(conversation)
                withTimeout(45_000L) {
                    val replies = currentConv.sendMessageAsync(prompt)
                    replies.collect { message ->
                        val text = message.toString()
                        if (text.isNotEmpty()) {
                            emittedAny = true
                            send(text)
                        }
                    }
                }
                _state.value = _state.value.copy(isBusy = false)
            }
        } catch (t: Throwable) {
            Log.e("LiteRtLm", "Error durante inferencia con LiteRT-LM: ${t.message}", t)
            _state.value = _state.value.copy(isBusy = false, lastError = t.message)

            // Si la conversación sufrió una interrupción o desbordamiento, reiniciarla limpiamente
            try { conversation?.close() } catch (_: Exception) {}
            conversation = null

            if (!emittedAny) {
                send("⚠️ Error en el motor de IA local (${t.localizedMessage ?: t.message}). He restablecido la sesión de conversación; por favor intenta enviar tu mensaje nuevamente.")
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

