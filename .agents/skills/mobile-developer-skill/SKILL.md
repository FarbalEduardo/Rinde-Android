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

## Uso
Ejecutar: `powershell .agents/skills/mobile-developer-skill/scripts/audit_arch.ps1`

