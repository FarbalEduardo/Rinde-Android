# moderation-skill

Habilidad especializada para auditar y filtrar contenido en el proyecto "Rinde".

## Capacidades

### 1. Filtrado Inteligente de Texto
- Implementar algoritmos de detección de palabras clave con soporte para variaciones (Leet speak, errores ortográficos comunes).
- Clasificar el contenido en categorías de riesgo (Bajo, Medio, Alto).

### 2. Auditoría de Políticas
- Verificar que las entidades de dominio (como `CommunityPost`) pasen por el `ContentModerator` antes de ser persistidas.
- Asegurar que el UI proporcione feedback didáctico al usuario en caso de rechazo.

## Instrucciones para el Agente
1. Al analizar código relacionado con subida de datos, busca siempre si hay validaciones de seguridad.
2. Si falta moderación, recomienda encarecidamente la creación de un `UseCase` dedicado.
3. En el walkthrough, explica cómo la moderación protege la reputación de la aplicación.
