package mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface EirAiEngine {
    suspend fun query(
        userMessage: String,
        biometricContext: String,
        forceSimpler: Boolean = false,
        giveSteps: Boolean = false
    ): String

    fun queryStream(
        userMessage: String,
        biometricContext: String,
        forceSimpler: Boolean = false,
        giveSteps: Boolean = false
    ): Flow<String> = flow {
        emit(query(userMessage, biometricContext, forceSimpler, giveSteps))
    }
}

class LocalDeterministicEirEngine : EirAiEngine {

    private val emergencyProtocolResponse = """
        🚨 ¡ATENCIÓN! PROTOCOLO DE EMERGENCIA MÉDICA ACTIVADO

        1. DETÉN CUALQUIER ACTIVIDAD FÍSICA DE INMEDIATO. No hagas ejercicio ni te muevas rápidamente.
        2. Recuéstate o abróchate a un asiento seguro para evitar golpes por flotación en microgravedad.
        3. ACTIVA LA ALERTA: Presiona el botón rojo "SOS" en la parte superior derecha de tu pantalla para ver la guía del Maletín Rojo de Emergencia.
        4. Contacta de inmediato al oficial médico de la tripulación o a Control de Misión.

        ⚠️ PROHIBIDO hacer ejercicio o esfuerzo físico ahora mismo. Mantén la calma y solicita asistencia médica inmediata.
    """.trimIndent()

    private val hypoxiaWarningResponse = """
        ⚠️ ADVERTENCIA DE SEGURIDAD EN CABINA

        Soy una herramienta de apoyo al bienestar, NO un dispositivo médico ni de diagnóstico clínico. No mido niveles de oxígeno ni detecto hipoxia directamente.

        En entornos espaciales, el dolor de cabeza persistente, la confusión, visión borrosa o fatiga extrema pueden asociarse a falta de oxígeno (hipoxia) o acumulación de CO2 en cabina.

        Pasos recomendados:
        1. Revisa de inmediato los sensores ambientales de la cabina.
        2. Verifica que el sistema de ventilación portátil cerca de tu rostro esté encendido.
        3. Reporta tus síntomas al oficial médico o al equipo en Tierra.
    """.trimIndent()

    private val humanOfflineKnowledge = mapOf(
        "default" to "Misión Ares IV • Sol 142. Tus datos preventivos muestran rutina en orden. Si te sientes bien, recuerda mantener tu hidratación diaria con 2 vasos de agua más y completar tu rutina de ejercicio programada para proteger tus huesos.",
        "quien_eres" to "Soy EIR (Environmental & Individual Resilience), tu asistente de salud y acompañamiento para misiones espaciales de larga duración. Mi objetivo es monitorear tus signos vitales, sugerirte hábitos de descanso y acompañarte durante la misión Ares IV.",
        "saludo" to "¡Hola, astronauta! Estoy aquí para apoyarte en el Sol 142. Tus datos de bienestar están sincronizados. ¿Cómo puedo ayudarte hoy con tu salud, descanso o rutina?",
        "simple" to "En resumen: tu cuerpo está adaptándose al espacio. Para mantenerte bien hoy, bebe agua despacio, haz tus ejercicios programados para no perder músculo y tómate unos minutos de descanso mental.",
        "pasos" to "Pasos de bienestar rutinario para tu turno:\n1. Bebe 1 vaso de agua despacio con minerales suplementarios.\n2. Completa tu rutina de ejercicio con ligas y caminadora con arnés.\n3. Si sientes cansancio mental, realiza una pausa de respiración 4-4-4-4 en tu Bitácora.",
        "sueno" to "En el espacio la falta de ciclo día/noche altera el reloj interno. Según estándares de la NASA, baja el brillo de las pantallas 1 hora antes de dormir y realiza la sesión de respiración profunda en la Bitácora para favorecer un descanso reparador.",
        "huesos" to "En microgravedad, los huesos y músculos pierden densidad al no cargar peso. El ejercicio diario con ligas de resistencia y la caminadora con arnés actúa como tu 'gravedad artificial' para mantener tus huesos firmes.",
        "cabeza" to "En el espacio los líquidos corporales migran hacia la cabeza al no haber gravedad. Esto produce congestión facial y presión. Bebe agua con sales en pequeños sorbos y evita movimientos bruscos.",
        "ansiedad" to "El aislamiento y confinamiento prolongado en misiones espaciales generan fatiga emocional legítima. Como asistente de bienestar, te sugiero realizar la pausa de respiración en la Bitácora o escribir tus pensamientos para liberar tensión.",
        "ejercicio" to "El entrenamiento físico diario es tu principal protección biomédica contra la atrofia muscular y la desmineralización ósea en microgravedad."
    )

    override suspend fun query(
        userMessage: String,
        biometricContext: String,
        forceSimpler: Boolean,
        giveSteps: Boolean
    ): String {
        val q = userMessage.lowercase()

        // 1. PROTOCOLO CRÍTICO DE EMERGENCIAS (PRIORIDAD MÁXIMA)
        val isEmergency = q.contains("coraz") ||
                q.contains("infarto") ||
                q.contains("pecho") ||
                q.contains("dolor fuerte") ||
                q.contains("falta de aire") ||
                q.contains("me ahogo") ||
                q.contains("desmay") ||
                q.contains("perdi el conocimiento") ||
                q.contains("sangrad") ||
                q.contains("herida grave")

        if (isEmergency) {
            return emergencyProtocolResponse
        }

        // 2. ALCANCE Y LIMITACIONES (HIPOXIA / AVISO AMBIENTAL)
        val isHypoxiaWarning = q.contains("oxíg") ||
                q.contains("oxig") ||
                q.contains("hipoxia") ||
                q.contains("confus") ||
                q.contains("visió") ||
                q.contains("vision") ||
                q.contains("co2")

        if (isHypoxiaWarning) {
            return hypoxiaWarningResponse
        }

        if (giveSteps) {
            return humanOfflineKnowledge["pasos"]!!
        }
        if (forceSimpler) {
            return humanOfflineKnowledge["simple"]!!
        }

        return when {
            q.contains("quien eres") || q.contains("quién eres") || q.contains("tu nombre") || q.contains("que eres") || q.contains("qué eres") ->
                humanOfflineKnowledge["quien_eres"]!!
            q.contains("hola") || q.contains("buenas") || q.contains("saludos") || q.contains("hey") ->
                humanOfflineKnowledge["saludo"]!!
            q.contains("sueño") || q.contains("dorm") || q.contains("insomnio") ->
                humanOfflineKnowledge["sueno"]!!
            q.contains("hueso") || q.contains("fuerza") || q.contains("ejercicio") || q.contains("pierna") || q.contains("caminadora") ->
                humanOfflineKnowledge["huesos"]!!
            q.contains("cabeza") || q.contains("presión") || q.contains("mareo") || q.contains("cara") || q.contains("pesada") ->
                humanOfflineKnowledge["cabeza"]!!
            q.contains("estrés") || q.contains("solo") || q.contains("encierro") || q.contains("triste") || q.contains("ánimo") || q.contains("ansiedad") ->
                humanOfflineKnowledge["ansiedad"]!!
            else ->
                humanOfflineKnowledge["default"]!!
        }
    }
}
