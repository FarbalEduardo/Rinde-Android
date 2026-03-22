Write-Host "Iniciando Auditoría de Arquitectura e Inyección de Dependencias..." -ForegroundColor Cyan

$domainFiles = Get-ChildItem -Path "app\src\main\java\*\domain" -Recurse -Filter "*.kt" -ErrorAction SilentlyContinue
$archFindings = 0

foreach ($file in $domainFiles) {
    # 1. Detectar Android imports en dominio
    $androidImports = Select-String -Path $file.FullName -Pattern "^import android\..*"
    foreach ($match in $androidImports) {
        $archFindings++
        Write-Host "[VIOLATION] $($file.Name):$($match.LineNumber) - Dominio contaminado con Android ($($match.Pattern))" -ForegroundColor Red
    }
}

# 2. Corrutinas: Buscar GlobalScope o MainScope
$coroutineLeaks = Get-ChildItem -Path "app\src\main\java" -Recurse -Filter "*.kt" | Select-String -Pattern "(GlobalScope|MainScope)"
foreach ($match in $coroutineLeaks) {
    $archFindings++
    Write-Host "[LEAK] $($match.Filename):$($match.LineNumber) - No usar GlobalScope/MainScope. Usar ViewmodelScope o coroutineScope." -ForegroundColor Yellow
}

# 3. Encapsulamiento en ViewModels
$vmFiles = Get-ChildItem -Path "app\src\main\java" -Recurse -Filter "*ViewModel.kt"
foreach ($file in $vmFiles) {
    # Buscar MutableStateFlow que no sea privado
    $publicState = Select-String -Path $file.FullName -Pattern "val\s+\w+\s*:\s*MutableStateFlow" | Where-Object { $_.Line -notlike "*private*" }
    foreach ($match in $publicState) {
        $archFindings++
        Write-Host "[ENCAPSULATION] $($file.Name):$($match.LineNumber) - MutableStateFlow debe ser privado. Exponer solo StateFlow." -ForegroundColor Cyan
    }
}

# 4. Hilt: Verificar @HiltViewModel
foreach ($file in $vmFiles) {
    $hiltMatch = Select-String -Path $file.FullName -Pattern "@HiltViewModel"
    if (-not $hiltMatch) {
        $archFindings++
        Write-Host "[DI] $($file.Name) no tiene @HiltViewModel." -ForegroundColor Red
    }
}

if ($archFindings -eq 0) {
    Write-Host "✅ Arquitectura sólida y bien inyectada." -ForegroundColor Green
} else {
    Write-Host "`n⚠️ Nivel de riesgo arquitectónico: $archFindings" -ForegroundColor Yellow
}
