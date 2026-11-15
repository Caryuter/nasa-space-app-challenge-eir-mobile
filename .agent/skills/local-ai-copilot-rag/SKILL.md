---
name: local-ai-copilot-rag
description: Implementación del copiloto conversacional Eir a pantalla completa, inyección de contexto de salud local y protocolos de emergencia SOS paso a paso. Actívalo al trabajar en el chat IA o en el modo SOS.
---

# Copiloto IA Offline y Protocolos SOS Paso a Paso

1. **Personalidad y Reglas del Asistente Eir (`Agente IA`):**
    - Vista de chat a **pantalla completa** (nunca dentro de cajas pequeñas con scroll interno).
    - Muestra una píldora superior discreta indicando que tiene contexto actualizado de los últimos registros locales (signos, sueño, agua y ánimo).
    - **Tono:** Médico de cabecera paciente y empático. Explica todo con analogías cotidianas y cero jerga técnica.
    - Incluye debajo de cada respuesta del asistente dos acciones rápidas: `"Explícamelo aún más simple"` y `"Dame los pasos a seguir"`.

2. **Motor Offline-First con Fallback Garantizado:**
    - Implementa una interfaz `EirAiEngine` que soporte inferencia local en dispositivo (ej. **MediaPipe LLM Inference / Gemma**) y cuente siempre con un **motor de respaldo local determinista en Kotlin/JSON** enriquecido con conocimiento del *NASA Human Research Program*. Esto garantiza respuestas instantáneas (< 500 ms) sin internet ni dependencia de servidores externos.

3. **Protocolos de Emergencia SOS (`Modo Emergencia`):**
    - Totalmente aislado de la navegación normal.
    - Diseñado para un tripulante en pánico sin conocimientos médicos:
        - Muestra **una sola instrucción gigante por pantalla** con barra de progreso (`Paso 1 de 4`).
        - Traduce el equipo médico a referencias visuales del botiquín (ej. *"Saca el estuche azul con la cruz blanca y coloca la pinza en el dedo índice"*).
        - Integra `TextToSpeech` (TTS) nativo de Android para leer automáticamente cada instrucción en voz alta mientras el astronauta mantiene las manos ocupadas.