# 🔑 Login Feature Context

## 🎯 Objective
Allow users to authenticate using Google or Email/Password via Firebase.

## 📜 Rules
- **Validation**: Email must be validated locally before calling Firebase.
- **Security**: Never store passwords in plain text or local storage.
- **State**: The "LoggedIn" state must be persisted in `DataStore` or `SharedPreferences` for fast startup.

## 🏗 Structure
- **Domain**: `LoginUseCase`, `LogoutUseCase`.
- **Data**: `AuthRepository` implementing `FirebaseAuth`.
- **UI**: `LoginScreen` (Compose), `LoginViewModel`.

## 🎨 Aesthetics
- Use a "Glassmorphism" effect for the login card.
- Subtle background animation for the blue gradients.
