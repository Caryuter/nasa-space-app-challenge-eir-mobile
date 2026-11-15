---
name: compose-m3-emotional-ui
description: Estándares de UI/UX en Jetpack Compose, Material Design 3 y diseño emocional dual para SOLIS. Actívalo siempre que vayas a crear o modificar pantallas, componentes visuales, temas, copy o navegación.
---

# Diseño Dual y Cero Tecnicismos (Eir UI)

1. **Stack UI Estricto:**
    - Usa únicamente **Jetpack Compose**, **Material Design 3** y **Navigation Compose (type-safe)**. Prohibido usar vistas XML.
    - Desactiva `dynamicColor` en el tema para mantener consistencia visual.

2. **Regla de Cero Tecnicismos (Traducción Humana):**
    - Prohibido mostrar siglas clínicas (`HRV`, `SpO2`, `BPM`, `mmHg`) como título principal de una tarjeta o alerta.
    - Usa etiquetas cotidianas con analogías claras:
        - *Ritmo del corazón* (en lugar de Frecuencia Cardíaca)
        - *Batería de tu cuerpo* (en lugar de HRV / Recuperación)
        - *Aire en tu sangre* (en lugar de Oxigenación SpO2)
        - *Fuerza de circulación* (en lugar de Presión Arterial)
    - Incluye siempre un botón de ayuda (`?`) o sección desplegable que responda en lenguaje sencillo: *"¿Qué significa esto?"* y *"¿Qué hago?"*.

3. **Diseño Emocional Dual:**
    - **Salud y Métricas:** Alto contraste, jerarquía limpia, tarjetas compactas y tipografía tabular (`fontFeatureSettings = "tnum"`) en todos los números para evitar saltos visuales.
    - **Bitácora Emocional:** Atmósfera serena con degradados suaves, bordes sutiles, mayor espacio en blanco (*whitespace*) y microcopy empático.
    - **Emergencia SOS:** Pantalla aislada en rojo de alto contraste (`Color(0xFFE11D48)`), sin distracciones, con una sola instrucción gigante por pantalla.

4. **Arquitectura de Navegación:**
    - **Header Superior:** Nombre de la app (`SOLIS`), día de misión (`Sol 142`), badge `IA Offline Lista`, toggle independiente `Claro / Oscuro` y botón rojo aislado **SOS**.
    - **Barra Inferior (5 elementos):** `Salud`, `Hábitos`, **Botón Central Elevado (`+` Chequeo)**, `Bitácora` y `Agente IA`.
    - **Prohibido usar formularios largos:** No uses múltiples `OutlinedTextField`. Usa controles táctiles `- / +`, `Slider`, chips seleccionables y una cuadrícula interactiva 2D para ánimo/energía.