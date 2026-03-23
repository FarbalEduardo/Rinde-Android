$agentName = "security-expert"
$issues = @()

# 1. SAST - Manifest Analysis
$manifestPath = "app/src/main/AndroidManifest.xml"
if (Test-Path $manifestPath) {
    $manifest = Get-Content $manifestPath -Raw
    # Si detecta exported=true SIN permission Y NO es el MAIN/LAUNCHER
    if ($manifest -match 'android:exported="true"' -and $manifest -notmatch 'android:permission=' -and $manifest -notmatch 'android.intent.action.MAIN') {
        $issues += @{ type = "ManifestVulnerability"; file = "AndroidManifest.xml"; message = "Component exported=true found without android:permission restriction." }
    }
}

# 2. SAST - PendingIntents
$ktFiles = Get-ChildItem -Path "app/src/main/java" -Filter "*.kt" -Recurse -ErrorAction SilentlyContinue
foreach ($file in $ktFiles) {
    $content = Get-Content $file.FullName -Raw
    if ($content -match "PendingIntent\.get(Activity|Broadcast|Service)") {
        if ($content -notmatch "(FLAG_IMMUTABLE|FLAG_MUTABLE)") {
            $issues += @{ type = "PendingIntentVulnerability"; file = $file.Name; message = "PendingIntent lacks explicit immutability flag (FLAG_IMMUTABLE)." }
        }
    }
}

# 3. Dependency Check (Static Version checks)
$tomlPath = "gradle/libs.versions.toml"
if (Test-Path $tomlPath) {
    $toml = Get-Content $tomlPath -Raw
    # Retrofit < 2.9 es un riesgo (ejemplo estático)
    if ($toml -match 'retrofit\s*=\s*"2\.[0-8]\.[0-9]"') {
         $issues += @{ type = "DependencyVulnerability"; file = "libs.versions.toml"; message = "Outdated Retrofit version with known risks. Update to 2.9.0+" }
    }
    # OkHttp < 4.9.0 
    if ($toml -match 'okhttp\s*=\s*"(3\.[0-9]+\.|4\.[0-8]\.)') {
        $issues += @{ type = "DependencyVulnerability"; file = "libs.versions.toml"; message = "Outdated OkHttp version. Update to 4.9.0+ for better TLS defaults." }
    }
}

$status = if ($issues.Count -eq 0) { "PASSED" } else { "FAILED" }
$result = @{ agent = $agentName; status = $status; issues = $issues }

$outPath = ".agents/pipeline/sec_report.json"
$result | ConvertTo-Json -Depth 3 | Out-File $outPath -Force
Write-Host "Security Audit Completed. Result saved to $outPath" -ForegroundColor Red
