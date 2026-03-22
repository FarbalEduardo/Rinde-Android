Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "🚀 STARTING LOCAL INTEGRATION PIPELINE 🚀" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Clean previous reports
Remove-Item -Path ".agents/pipeline/*_report.json" -ErrorAction SilentlyContinue

# 2. Run Agents in sequence
Write-Host "`n[1/4] Running Mobile Developer (Logic) Audit..." -ForegroundColor Blue
powershell -ExecutionPolicy Bypass -File .agents/skills/mobile-developer-skill/scripts/audit_logic.ps1

Write-Host "`n[2/4] Running Design Expert (UI) Audit..." -ForegroundColor Magenta
powershell -ExecutionPolicy Bypass -File .agents/skills/design-expert-skill/scripts/audit_ui.ps1

Write-Host "`n[3/4] Running Quality Expert (QA) Audit..." -ForegroundColor Yellow
powershell -ExecutionPolicy Bypass -File .agents/skills/quality-pm-expert-skill/scripts/audit_qa.ps1

Write-Host "`n[4/4] Running Security Expert Audit..." -ForegroundColor Red
powershell -ExecutionPolicy Bypass -File .agents/skills/security-expert-skill/scripts/audit_sec.ps1

# 3. Aggregate Results
Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host "📊 PIPELINE AGGREGATED REPORT 📊" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

$reports = Get-ChildItem -Path ".agents/pipeline" -Filter "*_report.json"
$pipelineFailed = $false
$allIssues = @()

foreach ($report in $reports) {
    $data = Get-Content $report.FullName | ConvertFrom-Json
    $color = if ($data.status -eq "PASSED") { "Green" } else { "Red" }
    Write-Host "Agent: $($data.agent.PadRight(20)) | Status: $($data.status)" -ForegroundColor $color
    
    if ($data.status -eq "FAILED") {
        $pipelineFailed = $true
        $allIssues += @{ Agent = $data.agent; File = $data.issues.file; Type = $data.issues.type; Message = $data.issues.message }
    }
}

if ($pipelineFailed) {
    Write-Host "`n❌ PIPELINE FAILED" -ForegroundColor Red
    Write-Host "Please check .agents/pipeline/*_report.json for detailed issues." -ForegroundColor Red
    exit 1
} else {
    Write-Host "`n✅ PIPELINE COMPLETED SUCCESSFULLY" -ForegroundColor Green
    exit 0
}
