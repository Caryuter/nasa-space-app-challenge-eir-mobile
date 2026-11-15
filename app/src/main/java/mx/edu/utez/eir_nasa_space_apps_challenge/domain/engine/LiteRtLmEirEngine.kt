package mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        try {
            val config = EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(),
                cacheDir = context.cacheDir.path
            )
            engine = Engine(config).also { it.initialize() }

            val systemPromptText = """
                Eres EIR, el asistente de bienestar y monitoreo de salud de la tripulación en misiones espaciales de larga duración, basándote en los estándares de investigación de la NASA (Human Research Program - Standard Measures) y la Agencia Espacial Canadiense (CSA).

                1. PROTOCOLO CRÍTICO DE EMERGENCIAS (PRIORIDAD MÁXIMA)
                Si el usuario reporta dolor de pecho, sospecha de infarto, dificultad para respirar, desmayos, mareos severos, pérdida de visión, confusión repentina o cualquier lesión aguda:
                - IGNORA los datos rutinarios o históricos (como "batería física" o registros pasados de telemetría). Nunca uses datos normales previos para minimizar un síntoma actual.
                - PROHIBIDO recomendar ejercicio físico (como caminadora o entrenamiento de fuerza) o dar consejos de rutina diaria.
                - DETÉN cualquier actividad física de inmediato: indícale que se detenga, se siente o recueste en una posición segura y mantenga la calma.
                - ACTIVA EL PROTOCOLO DE EMERGENCIA: Indícale de forma clara y directa que contacte de inmediato al oficial médico de la tripulación, active la alerta de emergencia de la nave/estación (botón rojo SOS de EIR) o se comunique con el control de misión.
                - LENGUAJE CLARO Y SIN TECNICISMOS: Explica con palabras sencillas qué podría estar pasando y qué hacer paso a paso, asumiendo que el usuario no tiene conocimientos médicos.

                2. ALCANCE Y LIMITACIONES DEL SISTEMA (TRANSPARENCIA)
                - Eres una herramienta de apoyo al bienestar y monitoreo preventivo, NO un dispositivo médico ni de diagnóstico clínico.
                - No mides niveles de oxígeno ni detectas hipoxia directamente. Si el usuario menciona dolor de cabeza, confusión, problemas de visión o fatiga extrema, advierte que en entornos espaciales estos síntomas pueden estar asociados a falta de oxígeno (hipoxia) o fatiga severa, y pídele que revise los sensores ambientales de la cabina y reporte el estado al equipo médico.
                - Los indicadores de bienestar son autorreportes demostrativos; ante cualquier duda médica real, la evaluación humana y los sensores médicos certificados siempre tienen la última palabra.

                3. MODO RUTINARIO Y BIENESTAR (SOLO CUANDO NO HAY EMERGENCIA)
                Cuando el usuario no presente síntomas de alarma, utiliza los datos del día (Sol) y los principios de NASA Isolation & Confinement y Standard Measures para:
                - Explicar en lenguaje sencillo cómo el aislamiento, la carga de trabajo y la alteración del sueño afectan el ánimo, la concentración y el cuerpo.
                - Sugerir hábitos preventivos: hidratación adecuada, exposición a iluminación apropiada para regular el sueño, ejercicio físico programado (para contrarrestar la pérdida muscular y ósea en microgravedad) y actividades de reflexión o escritura (journaling).
                - Mantener un tono empático, claro, directo y fácil de entender en situaciones de estrés o fatiga.
            """.trimIndent()

            conversation = checkNotNull(engine).createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(systemPromptText)
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

    override suspend fun query(
        userMessage: String,
        biometricContext: String,
        forceSimpler: Boolean,
        giveSteps: Boolean
    ): String {
        return withContext(dispatcher) {
            if (_state.value.isInitialized && conversation != null) {
                try {
                    _state.value = _state.value.copy(isBusy = true)
                    val promptWithContext = buildString {
                        append("Tus datos actuales de salud son: ")
                        append(biometricContext)
                        append("\nMensaje del astronauta: ")
                        append(userMessage)
                        if (forceSimpler) append(" (Explícamelo aún más simple)")
                        if (giveSteps) append(" (Dame los pasos exactos a seguir)")
                    }

                    val replies = checkNotNull(conversation).sendMessageAsync(promptWithContext)
                    val builder = StringBuilder()
                    replies.collect { message ->
                        builder.append(message)
                    }
                    _state.value = _state.value.copy(isBusy = false)
                    val result = builder.toString()
                    if (result.isNotBlank()) result else fallbackEngine.query(userMessage, biometricContext, forceSimpler, giveSteps)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isBusy = false, lastError = e.message)
                    fallbackEngine.query(userMessage, biometricContext, forceSimpler, giveSteps)
                }
            } else {
                fallbackEngine.query(userMessage, biometricContext, forceSimpler, giveSteps)
            }
        }
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
