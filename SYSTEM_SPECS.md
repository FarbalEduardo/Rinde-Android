# 🧠 Rinde - Agent Instruction & System Specs

## 🏗️ Core Architecture (The Google Way 2026)
- **Engine:** Kotlin 2.1.0 + K2 Compiler.
- **Pattern:** MVVM + Clean Architecture + UDF (Unidirectional Data Flow).
- **Offline-First:** Room as Single Source of Truth (SSOT).
- **Cloud Sync:** Firebase Firestore (NoSQL) para datos comunitarios y Storage para imágenes.
- **DI:** Hilt (Dagger) con Scopes definidos para evitar fugas de memoria.

## 💾 Data Strategy (Hybrid Mode)
1. **Local:** Room maneja el caché de productos escaneados y la wishlist.
2. **Remote:** Firebase sincroniza reportes globales y validaciones sociales.
3. **Repository Pattern:** El repositorio decide si sirve datos de Room (si no hay red) o actualiza desde Firebase.

## 🛡️ Security & Quality Specs
- **Secret Management:** No Hardcoded Keys. Uso de `local.properties` y GitHub Secrets.
- **SDD:** Cada feature debe tener un test unitario en la capa de Domain antes de pasar a la UI.
- **Antigravity:** Los ViewModels no deben conocer a Firebase ni a Room, solo a los Casos de Uso (Use Cases).

## 🎨 Design & Aesthetic Specs (Premium Look)
- **Palette:** Principalmente **Azul** y sus variantes cromáticas.
- **Modes:** Soporte completo para **Dark Theme** y **Light Theme**.
- **Style:** Diseño moderno, dinámico y "novedoso" (Premium).
