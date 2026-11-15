---
name: offline-first-health-architecture
description: Arquitectura MVVM/MVI, persistencia local con Room, inyección con Hilt y sincronización reactiva de estado en SOLIS. Actívalo al crear modelos de datos, DAOs, repositorios, ViewModels o lógica de negocio.
---

# Arquitectura Offline-First (Single Source of Truth)

1. **Patrón y Concurrencia:**
    - Usa **MVVM / MVI** con `ViewModel`, `StateFlow` inmutable para estados de UI (`UiState`) y `SharedFlow` para eventos únicos (navegación, alertas SOS).
    - Inyección de dependencias con **Hilt**.
    - Ejecuta operaciones de base de datos y análisis en `Dispatchers.IO`.

2. **Persistencia Local con Room:**
    - **Room** es la única fuente de verdad (*Single Source of Truth*). Toda la app debe funcionar en modo avión / espacio profundo sin llamadas de red obligatorias.
    - Entidades principales mínimas:
        - `DailyCheckupEntity`: Almacena los resultados de los 4 pasos obligatorios (resultado facial, métricas vocales, signos básicos, coordenadas 2D de ánimo/energía y timestamp).
        - `BodySymptomEntity`: Síntomas reportados por zona del cuerpo (`Cabeza`, `Pecho`, `Abdomen`, `Extremidades`) con descripción cotidiana y severidad.
        - `HabitLogEntity`: Registro diario de vasos de agua (`250 ml`), raciones/calorías y minutos de ejercicio resistivo.
        - `JournalEntryEntity`: Entradas de la bitácora emocional.

3. **Reactividad entre Módulos:**
    - Al guardar un `DailyCheckupEntity` al finalizar el paso 4:
        1. Recalcula automáticamente los umbrales de riesgo (basados en referencias del *NASA Human Research Program*).
        2. Actualiza el estado visual de las zonas del **Body Scan** en la pestaña `Salud`.
        3. Actualiza el resumen de contexto que consume el **Agente IA** para personalizar sus respuestas.