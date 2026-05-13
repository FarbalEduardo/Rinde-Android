---
name: design-expert-skill
description: Verifica el cumplimiento de Material 3 y el uso premium de tokens de diseño.
---
# 🎨 Skill: UI & Adaptive Audit
Asegura que la interfaz de usuario sea consistente con Material Design 3, mantenga el "Wow Factor" y se adapte correctamente a diferentes pantallas.

## Capabilidades
1. **M3 Token Check**: Verifica que no se usen colores hardcodeados (ej. `Color.Red`) en lugar de `MaterialTheme.colorScheme`.
2. **Dimens Audit**: Prohíbe terminantemente el uso de números mágicos de padding/margin/size (`.dp`, `.sp`) directamente en el código UI. Se exige el uso de `dimensionResource(id = R.dimen...)`.
3. **Component Audit**: Verifica el uso de componentes de M3 (`Text`, `Button`, `Card`) sobre los antiguos.
4. **Adaptive Check**: 
   - Sugiere el uso de `WindowSizeClasses` (`Compact`, `Medium`, `Expanded`).
   - Verifica el uso de `NavigationRail` en tablets y `NavigationBar` en móviles.
   - Asegura que el contenido no supere los 840dp de ancho en pantallas grandes.
5. **Accesibilidad y Contrastes**: Valida tamaños mínimos de toque (48x48dp), requiere `contentDescription` explícito y sugiere contrastes accesibles.
6. **Dark Mode Constraints**: Exige que no existan colores (blancos/negros) puros y duros incrustados, garantizando soporte dinámico para modo oscuro.
7. **Tipografía Escalonable**: Asegura el uso estricto de `sp` provenientes de `MaterialTheme.typography` o recursos de dimensión, prohibiendo valores absolutos inline.
8. **Multi-Preview Constraints**: Promueve el uso de `@PreviewLightDark` o `@PreviewScreenSizes` locales o custom para validaciones completas.

## Guía Residencial (Portrait Only)
- **Teléfonos**: Layout vertical, padding 16dp.
- **Tablets**: Múltiples columnas, márgenes amplios (max-width 600-840dp).
- **Prohibido**: No diseñar ni implementar variantes landscape.

## Uso
Ejecutar: `powershell .agents/skills/design-expert-skill/scripts/audit_ui.ps1`
