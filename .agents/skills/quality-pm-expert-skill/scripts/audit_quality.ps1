Write-Host "Iniciando Auditoría de Calidad Profesional..." -ForegroundColor Cyan

$qualityFindings = 0

# 1. Buscar TODOs/FIXMEs
$todos = Get-ChildItem -Path . -Include "*.kt", "*.java" -Recurse -Exclude "build", ".gradle", ".git" | Select-String -Pattern "TODO|FIXME"
foreach ($match in $todos) {
    $qualityFindings++
    Write-Host "[DEBT] $($match.Filename):$($match.LineNumber) - Pendiente: $($match.Line.Trim())" -ForegroundColor Yellow
}

# 2. Buscar archivos excesivamente largos (> 300 líneas)
$largeFiles = Get-ChildItem -Path "app\src\main" -Include "*.kt" -Recurse | Where-Object { (Get-Content $_.FullName).Count -gt 300 }
foreach ($file in $largeFiles) {
    $qualityFindings++
    Write-Host "[COMPLEXITY] El archivo $($file.Name) tiene más de 300 líneas. Violas SRP." -ForegroundColor Red
}

# 3. Buscar funciones gigantes (> 50 líneas)
$ktFiles = Get-ChildItem -Path "app\src\main" -Include "*.kt" -Recurse
foreach ($file in $ktFiles) {
    $lines = Get-Content $file.FullName
    $funcBody = 0
    $inFunc = $false
    for ($i=0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match "fun\s+\w+") { $inFunc = $true; $funcBody = 0 }
        if ($inFunc) { $funcBody++ }
        if ($lines[$i] -match "\}" -and $inFunc -and $funcBody -gt 50) {
            $qualityFindings++
            Write-Host "[COMPLEXITY] $($file.Name):$($i) - Función demasiado larga ($funcBody líneas)." -ForegroundColor Yellow
            $inFunc = $false
        }
    }
}

# 4. Verificar Previews en UI
$uiFiles = Get-ChildItem -Path "app\src\main\java" -Recurse -Filter "*Screen.kt"
foreach ($file in $uiFiles) {
    $previews = Select-String -Path $file.FullName -Pattern "@Preview"
    if (-not $previews) {
        $qualityFindings++
        Write-Host "[QUALITY] $($file.Name) no tiene @Preview." -ForegroundColor Gray
    }
}

if ($qualityFindings -eq 0) {
    Write-Host "✅ Código limpio y bajo control." -ForegroundColor Green
} else {
    Write-Host "`n⚠️ Total hallazgos: $qualityFindings" -ForegroundColor Yellow
}
