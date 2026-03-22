$agentName = "design-expert"
$issues = @()

$composeFiles = Get-ChildItem -Path "app/src/main/java/*/ui/screen", "app/src/main/java/*/ui/components" -Filter "*.kt" -Recurse -ErrorAction SilentlyContinue

foreach ($file in $composeFiles) {
    # 1. Theme Mirroring: Buscar colores hardcodeados como Color(0xFF...)
    $hardcodedColors = Select-String -Path $file.FullName -Pattern "Color\(0x[A-Fa-f0-9]{8}\)"
    foreach ($match in $hardcodedColors) {
        $issues += @{ type = "ThemeMirroringViolated"; file = $file.Name; message = "Hardcoded color found at line $($match.LineNumber). Use MaterialTheme.colorScheme." }
    }

    # 2. Compose Stability: Detectar Listas en parámetros sin protección de inmutabilidad (List<T>) en Composables
    $content = Get-Content $file.FullName -Raw
    # Regex para @Composable fun Name( ... List< ...
    if ($content -match "@Composable\s*fun\s*\w*\([^)]*(?<!Immutable)List<[^>]+>[^)]*\)") {
        $issues += @{ type = "ComposeStabilityWarning"; file = $file.Name; message = "Unstable standard List parameter or raw List found. Consider using kotlinx.collections.immutable.ImmutableList or wrapper classes." }
    }
}

$status = if ($issues.Count -eq 0) { "PASSED" } else { "FAILED" }
$result = @{ agent = $agentName; status = $status; issues = $issues }

$outPath = ".agents/pipeline/ui_report.json"
$result | ConvertTo-Json -Depth 3 | Out-File $outPath -Force
Write-Host "UI Audit Completed. Result saved to $outPath" -ForegroundColor Magenta
