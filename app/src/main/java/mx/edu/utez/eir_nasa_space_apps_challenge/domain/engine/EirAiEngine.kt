package mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine

interface EirAiEngine {
    suspend fun query(
        userMessage: String,
        biometricContext: String,
        forceSimpler: Boolean = false,
        giveSteps: Boolean = false
    ): String
}

class LocalDeterministicEirEngine : EirAiEngine {

    private val humanOfflineKnowledge = mapOf(
        "default" to "Revisé tus últimos datos del Sol 142: tu corazón late con calma y tu batería física está en buen nivel. Solo recuerda tomar 2 vasos más de agua y hacer tus 15 minutos de caminadora para que tus piernas y huesos no pierdan fuerza.",
        "simple" to "En pocas palabras: tu cuerpo está bien, pero como aquí no hay gravedad, tus líquidos suben un poco hacia la cabeza y tus huesos trabajan menos. Toma agua y haz tus ejercicios de hoy para mantenerte fuerte.",
        "pasos" to "Pasos recomendados para tu turno:\n1. Bebe 1 vaso de agua despacio con sales.\n2. Completa tus 15 min en la caminadora con arnés.\n3. Si sientes pesadez en la cabeza, haz 3 minutos de respiración profunda en la Bitácora.",
        "sueno" to "En el espacio no vemos amanecer ni atardecer como en la Tierra, así que tu reloj interno se confunde. Baja el brillo de las pantallas 1 hora antes de dormir y haz la pausa de respiración en tu Bitácora para descansar profundo.",
        "huesos" to "Sin gravedad, tus huesos y músculos sienten que no cargan nada y se van debilitando poco a poco. Por eso es tan importante que hagas tus ejercicios con ligas y caminadora todos los días: es como darle peso artificial a tu cuerpo.",
        "cabeza" to "Sentir la cabeza pesada o la cara un poco hinchada es súper normal en el espacio: sin gravedad, los líquidos que normalmente bajan a tus piernas se quedan arriba. Toma agua despacio y evita girar la cabeza muy rápido.",
        "ansiedad" to "Estar lejos de casa y en un espacio cerrado cansa la mente, y es totalmente normal sentirlo. Estoy aquí contigo sin depender de la señal con la Tierra. Si quieres, hagamos juntos una pausa de respiración en la Bitácora.",
        "ejercicio" to "Hacer ejercicio en órbita no es solo deporte, es tu escudo contra la pérdida de masa ósea. Las ligas y arneses le recuerdan a tus células que deben seguir firmes."
    )

    override suspend fun query(
        userMessage: String,
        biometricContext: String,
        forceSimpler: Boolean,
        giveSteps: Boolean
    ): String {
        if (giveSteps) {
            return humanOfflineKnowledge["pasos"]!!
        }
        if (forceSimpler) {
            return humanOfflineKnowledge["simple"]!!
        }

        val q = userMessage.lowercase()
        return when {
            q.contains("sueño") || q.contains("dorm") || q.contains("cansad") ->
                humanOfflineKnowledge["sueno"]!!
            q.contains("hueso") || q.contains("fuerza") || q.contains("ejercicio") || q.contains("pierna") || q.contains("caminadora") ->
                humanOfflineKnowledge["huesos"]!!
            q.contains("cabeza") || q.contains("presión") || q.contains("mareo") || q.contains("cara") || q.contains("pesada") ->
                humanOfflineKnowledge["cabeza"]!!
            q.contains("estrés") || q.contains("solo") || q.contains("encierro") || q.contains("triste") || q.contains("ánimo") ->
                humanOfflineKnowledge["ansiedad"]!!
            else ->
                humanOfflineKnowledge["default"]!!
        }
    }
}
