package com.farbalapps.rinde.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppLanguage { ES, EN }

enum class AppCurrency(val symbol: String, val code: String, val label: String) {
    CLP("$", "CLP", "Peso chileno (CLP)"),
    MXN("$", "MXN", "Peso mexicano (MXN)"),
    USD("$", "USD", "Dólar estadounidense (USD)"),
    EUR("€", "EUR", "Euro (EUR)")
}
