package mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.CoroutineScope
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

    private val fallbackEngine = LocalDeterministicEirEngine()

    private val _state = MutableStateFlow(LiteRtLmState())
    val state: StateFlow<LiteRtLmState> = _state.asStateFlow()

    init {
        scope.launch {
            tryInitLiteRtLmModel()
        }
    }

    private suspend fun tryInitLiteRtLmModel() {
        mutex.withLock {
            val possibleModelFiles = listOf(
                File(context.filesDir, "gemma-4-E2B-it.litertlm"),
                File(context.filesDir, "model.litertlm"),
                File(context.filesDir, "Qwen3-0.6B.litertlm")
            )

            val existingModel = possibleModelFiles.firstOrNull { it.exists() && it.length() > 0 }

            if (existingModel != null) {
                try {
                    val config = EngineConfig(
                        modelPath = existingModel.absolutePath,
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
                        activeModelName = "LiteRT-LM Gemma On-Device (NASA/CSA Protocol)"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = LiteRtLmState(
                        isInitialized = false,
                        activeModelName = "Modo Mindfulness (Respuesta Local)",
                        lastError = e.message
                    )
                }
            } else {
                _state.value = LiteRtLmState(
                    isInitialized = false,
                    activeModelName = "Modo Mindfulness (Respuesta Local)"
                )
            }
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
