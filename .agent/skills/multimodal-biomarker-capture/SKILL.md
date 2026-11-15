---
name: multimodal-biomarker-capture
description: Implementación del flujo obligatorio de 4 pasos del botón central (+) usando CameraX, ML Kit Face Detection y captura de voz/audio sin hardware externo. Actívalo al trabajar en el modal de Chequeo Diario, cámara o micrófono.
---

# Captura Multimodal y Chequeo Diario Obligatorio

1. **Flujo Secuencial Obligatorio (4 Pasos Bloqueados):**
    - Al pulsar el botón central `+` (`Chequeo`), abre un flujo secuencial con barra de progreso (`1 de 4` a `4 de 4`) donde **ningún paso puede saltarse** hasta completar su medición:
        - **Paso 1 — Escaneo Facial (CameraX):** Vista previa con cámara frontal y una guía ovalada dibujada en `Canvas`. Analiza signos visuales de microgravedad (hinchazón facial por fluidos, cansancio ocular y coloración). No guardes fotos pesadas en disco; extrae el resultado biométrico y libera el buffer.
        - **Paso 2 — Prueba de Voz Guiada (AudioRecord / SpeechRecognizer):** Muestra en pantalla una **frase predefinida de calibración** (ej. *"Bitácora del Sol 142, tripulación en orden y lista para iniciar operaciones"*). Mide duración, pausas respiratorias y estabilidad vocal de forma 100% local.
        - **Paso 3 — Signos Básicos Táctiles:** Controles steppers `- / +` pre-posicionados en el último valor registrado para pulso, presión, temperatura y horas de sueño, con explicación cotidiana de cada rango.
        - **Paso 4 — Estado Emocional y Energía:** Cuadrícula táctil 2D arrastrable (*Alta/Baja Energía* en eje Y vs. *Estado Bajo/Óptimo* en eje X) + chips de sensaciones cotidianas.

2. **Uso Secundario de Cámara (Body Scan):**
    - La cámara también puede invocarse desde el mapa corporal (`Salud`) para documentar un síntoma físico localizado (ej. irritación o inflamación). Prohibido crear funciones de escaneo de comida.

3. **Permisos y Resiliencia en Demo:**
    - Gestiona permisos de `CAMERA` y `RECORD_AUDIO` de forma limpia en Compose. Si el emulador no tiene cámara/micrófono disponible, provee un modo de simulación fluida para no bloquear el flujo durante una demostración en vivo.