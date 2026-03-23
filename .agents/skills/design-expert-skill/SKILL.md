---
name: design-expert-skill
description: Verifica el cumplimiento de Material 3 y el uso premium de tokens de diseño.
---
# 🎨 Skill: UI & Adaptive Audit
Asegura que la interfaz de usuario sea consistente con Material Design 3, mantenga el "Wow Factor" y se adapte correctamente a diferentes pantallas.

## Capabilidades
1. **M3 Token Check**: Verifica que no se usen colores hardcodeados (ej. `Color.Red`) en lugar de `MaterialTheme.colorScheme`.
2. **Dimens Audit**: Detecta números mágicos de padding/margin, sugiriendo el uso de un sistema de grid de 8dp.
3. **Component Audit**: Verifica el uso de componentes de M3 (`Text`, `Button`, `Card`) sobre los antiguos.
4. **Adaptive Check**: 
   - Sugiere el uso de `WindowSizeClasses` (`Compact`, `Medium`, `Expanded`).
   - Verifica el uso de `NavigationRail` en tablets y `NavigationBar` en móviles.
   - Asegura que el contenido no supere los 840dp de ancho en pantallas grandes.

## Guía Residencial (Portrait Only)
- **Teléfonos**: Layout vertical, padding 16dp.
- **Tablets**: Múltiples columnas, márgenes amplios (max-width 600-840dp).
- **Prohibido**: No diseñar ni implementar variantes landscape.

## Uso
Ejecutar: `powershell .agents/skills/design-expert-skill/scripts/audit_ui.ps1`


