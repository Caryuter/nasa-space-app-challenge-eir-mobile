# Guía de Arquitectura y Diseño — Proyecto SOLIS (NASA Space Apps)

Antes de escribir o modificar cualquier código en este proyecto, consulta y aplica estrictamente los skills definidos en `.agent/skills/` según la capa en la que estés trabajando:

1. **Interfaz, Navegación y Copy (`UI/UX`):** `.agent/skills/compose-m3-emotional-ui/SKILL.md`
2. **Cámara y Voz (`Sensores Nativos`):** `.agent/skills/multimodal-biomarker-capture/SKILL.md`
3. **Base de Datos y Estado (`Offline-First`):** `.agent/skills/offline-first-health-architecture/SKILL.md`
4. **Asistente IA y Emergencias SOS (`Copiloto`):** `.agent/skills/local-ai-copilot-rag/SKILL.md`

## Reglas Globales Innegociables
- **Cero Hardware Externo:** No integres APIs de wearables, Health Connect ni Bluetooth. Todos los datos provienen de la cámara, el micrófono o la interacción táctil del usuario.
- **100% Offline-First:** La aplicación debe operar por completo sin conexión a internet.
- **Cero Tecnicismos Médicos:** El usuario es un astronauta sin formación médica. Traduce siempre cualquier métrica o instrucción a lenguaje cotidiano claro.