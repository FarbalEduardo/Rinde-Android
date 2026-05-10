---
name: mobile-developer-skill
description: Verifica violaciones de arquitectura (Clean Architecture) y principios SOLID en la capa de datos y dominio.
---
# 🏗️ Skill: Architecture Linter
Este skill automatiza la revisión de la estructura del proyecto para asegurar que se sigan los patrones de Clean Architecture.

## Capabilidades
1. **Aislamiento de Dominio**: Detecta imports de Android (`android.*`) en la capa de dominio.
2. **Patrón Repository**: Verifica que los Repositorios tengan interfaces y que las implementaciones estén en la capa de `data`.
3. **Uso de Inyectables**: Detecta el uso de `new` para clases que deberían ser proveídas por Hilt/Inyección de dependencias.
4. **Kotlin Idiomático**: Propone el uso de `apply`, `also`, `let`, `run`, y `extension functions` para mejorar la legibilidad.
5. **Documentación KDoc**: Asegura que cada clase y función pública tenga su bloque KDoc describiendo parámetros y retorno.
6. **Gestión de Ramas**: Verifica que el desarrollo ocurra en ramas `feature/*` y no directamente en `develop` o `main`.
715. **Aislamiento de Modelos de Datos**: Detecta si modelos de la capa `data` o anotaciones específicas de frameworks de datos (`@Entity`, `@SerializedName`, `@Json`, etc.) se filtran hacia las capas de `domain` o `presentation`.
16. **Asincronía y Firebase Tasks**: Verifica que las operaciones de Firebase (Firestore, Auth) que retornan `Task` sean correctamente esperadas usando `.await()` dentro de corrutinas, evitando fallos de sincronización por ejecución "fire-and-forget" incompleta.
17. **State Hoisting & Rendimiento en Compose**: Requiere separación clara entre Composables con Estado (Stateful) y sin Estado (Stateless). Identifica el uso incorrecto de objetos inestables que causan recomposiciones excesivas.
18. **Lifecycle-Aware Collections**: Asegura que las colecciones de estado (Flows) en la interfaz gráfica utilicen obligatoriamente `collectAsStateWithLifecycle()` para prevenir fugas de memoria y recomposiciones ocultas.
19. **Manejo Funcional de Errores**: Promueve la estandarización mediante el uso de contenedores explícitos como `Result<T>` o dicotomías funcionales (`Either`) en Casos de Uso en vez de lanzar excepciones con `throw`.
20. **Mentalidad de Mentor**: Dado que el usuario es aprendiz, siempre incluye una sección "Explicación Técnica" al finalizar tareas, explicando conceptos de Clean Architecture y Kotlin usados.

## 🤝 Protocolos de Integración (Agent-to-Agent Contracts)
1. **Con Diseño (`design_expert`)**: Exportar el `UiState` (como `sealed class` o `sealed interface`) como un contrato de vista. Esto proporciona al experto en diseño un mapa exacto de los estados que debe pintar (ej. `Loading`, `Success`, `Error`, `Empty`), permitiéndole diseñar `@Previews` para cada estado.
2. **Con Calidad (`quality_pm_expert`)**: El código generado para lógicas de la aplicación (ej. `UseCases`, `ViewModels`) debe incluir siempre una estructura base de Unit Test utilizando **MockK** (para mockear dependencias como repositorios). Esto facilita que el agente de calidad evalúe y amplíe la cobertura probando casos frontera.
3. **Con Seguridad (`security_expert`)**: La capa de red (clientes de Retrofit y OkHttp) debe configurarse de manera que los interceptores soporten fácilmente la inyección de los headers de seguridad definidos globalmente por el agente de seguridad (Tokens, SSL Pinning, User-Agent, etc).

## Uso
Ejecutar: `powershell .agents/skills/mobile-developer-skill/scripts/audit_arch.ps1`

