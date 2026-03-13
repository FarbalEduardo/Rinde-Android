# 🌍 Global Agent Rules - Rinde

## 🏗 Architecture & Patterns
- **Clean Architecture**: Follow the data -> domain -> ui flow.
- **MVVM**: VMs must not have dependencies on Android Framework classes except for Lifecycle.
- **UDF**: Ensure state flows down and events flow up.
- **Dependency Injection**: Use Hilt. Always define scopes correctly.

## 💾 Data & Logic
- **Offline-First**: Room is the Source of Truth. Firebase is for sync.
- **Repository Pattern**: All data access MUST go through a repository.
- **Use Cases**: Every interaction (even simple ones) should have a dedicated Use Case.

## 🎨 UI & Aesthetics
- **Premium Look**: Use high-quality gradients, animations, and shadows.
- **Color Palette**: Stick to the Blues defined in `SYSTEM_SPECS.md`.
- **Responsive**: Support tablet and mobile.

## 🤖 AI Context Maintenance
- **Check `.agent/rules/`**: Before starting any task, read the rules in this folder.
- **Check `docs/features/`**: Before working on a specific feature, check for its context file.
- **Task Boundaries**: Always define task boundaries and keep `task.md` updated.
