Write-Host "Iniciando Auditoría Avanzada de UI y Diseño (M3)..." -ForegroundColor Cyan

$uiFindings = 0
$uiFiles = Get-ChildItem -Path "app\src\main\java" -Recurse -Filter "*.kt" | Where-Object { $_.FullName -like "*\ui\*" }

foreach ($file in $uiFiles) {
    $content = Get-Content $file.FullName
    
    # 1. Buscar colores hardcodeados
    $hardcodedColors = Select-String -Path $file.FullName -Pattern "Color\.(Red|Blue|Green|White|Black|Yellow|Cyan|Magenta)"
    foreach ($match in $hardcodedColors) {
        $uiFindings++
        Write-Host "[DESIGN RISK] $($file.Name):$($match.LineNumber) - Uso de color hardcodeado. Usar MaterialTheme.colorScheme." -ForegroundColor Yellow
    }

    # 2. Buscar números mágicos en padding/spacer (ej. .padding(13.dp))
    $magicNumbers = Select-String -Path $file.FullName -Pattern "\.padding\(\s*(?!(4|8|12|16|20|24|32|48|64))\d+\.dp\s*\)"
    foreach ($match in $magicNumbers) {
        $uiFindings++
        Write-Host "[SPACING] $($file.Name):$($match.LineNumber) - Número mágico en padding. Usar sistema de 8dp." -ForegroundColor Gray
    }

    # 3. ACCESIBILIDAD: Buscar Icon/Image sin contentDescription (o nulo)
    # Patrón: busca la palabra Image o Icon y verifica si el bloque tiene contentDescription
    # Nota: Es una aproximación simple para scripts
    $accessibilityGap = Select-String -Path $file.FullName -Pattern "(Icon|Image)\s*\((\s*[^)]*)\)" | Where-Object { $_.Line -notlike "*contentDescription*" }
    foreach ($match in $accessibilityGap) {
        $uiFindings++
        Write-Host "[ACCESSIBILITY] $($file.Name):$($match.LineNumber) - $($match.Line.Trim().Substring(0, [Math]::Min($match.Line.Length, 30))) - Falta contentDescription." -ForegroundColor Yellow
    }

    # 4. TYPOGRAPHY: Buscar Text que no use style del tema
    $textStyleGap = Select-String -Path $file.FullName -Pattern "Text\s*\((\s*[^)]*)\)" | Where-Object { $_.Line -notlike "*style = MaterialTheme.typography*" -and $_.Line -notlike "*style = MaterialTheme.colorScheme*" }
    foreach ($match in $textStyleGap) {
        # $uiFindings++
        # Write-Host "[TYPOGRAPHY] $($file.Name):$($match.LineNumber) - Text sin estilo de MaterialTheme." -ForegroundColor Gray
    }
}

if ($uiFindings -eq 0) {
    Write-Host "✅ UI alineada con estándares modernos de M3 y Accesibilidad." -ForegroundColor Green
} else {
    Write-Host "`n⚠️ Hallazgos de UI: $uiFindings" -ForegroundColor Yellow
}
