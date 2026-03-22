$agentName = "mobile-developer"
$issues = @()

# 1. Version Catalog Check
$gradleFiles = Get-ChildItem -Path "app" -Filter "build.gradle*" -Recurse
foreach ($file in $gradleFiles) {
    if (Select-String -Path $file.FullName -Pattern '(implementation|api)\(|"[^"]+:[^"]+:[^"]+"') {
        # Ignorar si es el propio libs o si es project()
        if ((Select-String -Path $file.FullName -Pattern "implementation\(libs\." -Quiet) -eq $false -and (Select-String -Path $file.FullName -Pattern "project\(" -Quiet) -eq $false) {
            $issues += @{ type = "VersionCatalogViolation"; file = $file.FullName; message = "Hardcoded dependency format found. Use libs.versions.toml" }
        }
    }
}

# 2. Contract Compliance (Spec-Driven)
$specFile = ".agents/pipeline/specs/api_spec.json"
if (Test-Path $specFile) {
    $specRaw = Get-Content $specFile | ConvertFrom-Json
    $uiStateFiles = Get-ChildItem -Path "app/src/main/java" -Filter "*UiState.kt" -Recurse
    foreach ($uiState in $uiStateFiles) {
        $className = $uiState.Name.Replace(".kt", "")
        if ($specRaw.sealed_classes.$className) {
            $content = Get-Content $uiState.FullName -Raw
            foreach ($prop in $specRaw.sealed_classes.$className.required_properties) {
                if ($content -notmatch "val\s+$prop\s*:") {
                    $issues += @{ type = "ContractViolation"; file = $uiState.Name; message = "Missing required state property '$prop' defined in SDD spec." }
                }
            }
        }
    }
}

# 3. Clean Architecture: Data Models Leak
$domainFiles = Get-ChildItem -Path "app/src/main/java/*/domain" -Recurse -Filter "*.kt" -ErrorAction SilentlyContinue
foreach ($file in $domainFiles) {
    $dataImports = Select-String -Path $file.FullName -Pattern "^import .*\.data\..*"
    foreach ($match in $dataImports) {
        $issues += @{ type = "CleanArchViolation"; file = $file.Name; message = "Domain layer leaking data layer model. Lines: $($match.LineNumber)" }
    }
}

$status = if ($issues.Count -eq 0) { "PASSED" } else { "FAILED" }
$result = @{ agent = $agentName; status = $status; issues = $issues }

$outPath = ".agents/pipeline/logic_report.json"
$result | ConvertTo-Json -Depth 3 | Out-File $outPath -Force
Write-Host "Logic Audit Completed. Result saved to $outPath" -ForegroundColor Cyan
