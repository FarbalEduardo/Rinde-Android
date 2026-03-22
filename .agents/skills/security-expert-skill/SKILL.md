---
name: security-expert-skill
description: Automatiza la detección de vulnerabilidades comunes en aplicaciones Android siguiendo OWASP MASVS.
---

# 🕵️ Skill: Security Checker

Este skill proporciona capacidades de auditoría automática para identificar riesgos de seguridad en el código fuente, manifiestos y archivos de configuración de Android.

## Capabilidades

1.  **Auditoría de Manifiesto**: Detecta configuraciones inseguras como `allowBackup`, `debuggable` o componentes exportados sin protección.
2.  **Escaneo de Secretos**: Busca patrones de llaves API, tokens y contraseñas hardcoreadas.
3.  **Seguridad de Red**: Identifica el uso de protocolos inseguros (`http://`) y falta de `networkSecurityConfig`.
4.  **Criptografía e Inyección**: Detecta el uso de algoritmos débiles o inyecciones SQL potenciales.

## Cómo usar este Skill

Cuando el usuario pida una revisión de seguridad o se esté implementando una nueva funcionalidad sensible, el Agente debe:

1.  Ejecutar el script de auditoría: `powershell .agents/skills/security-expert-skill/scripts/audit.ps1`.
2.  Analizar los resultados del script.
3.  Presentar los hallazgos al usuario con recomendaciones basadas en **OWASP MASVS**.

## Scripts Incluidos

- `scripts/audit.ps1`: Herramienta principal de escaneo estático basada en PowerShell.
