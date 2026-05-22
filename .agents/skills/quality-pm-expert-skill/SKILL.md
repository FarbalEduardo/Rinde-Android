---
name: quality-pm-expert-skill
description: Escanea el proyecto en busca de deuda técnica, strings hardcodeados y falta de documentación.
---
# 🏆 Skill: Full Quality & Test Audit
Herramienta integral para medir la salud técnica y la cobertura de pruebas del proyecto.

## Capabilidades
1. **Tech Debt Scanner**: Busca `TODO`s o `FIXME` abandonados.
2. **Missing Tests Audit**: Detecta clases en `main` que no tienen su correspondiente clase de prueba en `test` o `androidTest`.
3. **Hardcoded Strings & Multi-Language**: 
   - Prohíbe terminantemente los strings fuera de `strings.xml`.
   - Exige la existencia de recursos en Español (`values/strings.xml`) e Inglés (`values-en/strings.xml`).
4. **Complexity & SOLID Audit**: Identifica violaciones de responsabilidad única o archivos excesivamente largos.
5. **Template Generator**: Genera esqueletos de tests para ViewModels, UseCases y Repositorios.
6. **Test Case Documentation**: Asegura que cada suite de pruebas tenga documentación clara de los escenarios cubiertos.
7. **Quality Gate Compliance**: Verifica el cumplimiento de los estándares de calidad antes de marcar una tarea como completada para entrega.
8. **Multi-Flavor Safety**: Verifica que los secretos de producción no se utilicen en el sabor `dev` y viceversa.
9. **Localization Audit**: Escanea en busca de recursos de texto faltantes en los archivos de idiomas soportados (ES/EN) y errores de visualización por longitud de texto.
10. **Pruebas de Componentes Compose (UI Testing)**: Exige la creación de pruebas de componentes visuales apoyadas en el árbol semántico (`composeTestRule`).
11. **Snapshot / Visual Regression Testing**: Recomienda la adopción de pruebas visuales y de capturas (ej. Paparazzi, Roborazzi) para prevenir regresiones accidentales en la UI.
12. **Validación de Code Smells por Linter**: Integra y exige la validación automática de reglas de higiene (por ejemplo, con Detekt) previo al cierre funcional de tareas como Quality Gate obligatorio.

## Uso
Ejecutar: `powershell .agents/skills/quality-pm-expert-skill/scripts/audit_quality.ps1`
