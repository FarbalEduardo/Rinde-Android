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

## 🚀 SDD & CI/CD Workflow
- **Spec-Driven Development (SDD)**: REQUIRED. Before writing any code, validate or create a specification file (`.md`). NO CODE is written without its corresponding test.
- **Test-First**: Generate the test file first, observe it fail, then write the implementation in Domain/Data to make it pass.
- **Conventional Commits**: REQUIRED. All suggested commits must follow Semantic Versioning format (`feat:`, `fix:`, `chore:`, `docs:`, etc.) to trigger Google Release Please correctly.
- **Local Validation**: Always remind the user to run `./gradlew test` locally ensuring checks pass before pushing.
- **CI/CD Integrity**: DO NOT modify `.github/workflows/tests.yml` or `.github/workflows/release-please.yml` unless explicitly requested for Android pipeline optimization.
