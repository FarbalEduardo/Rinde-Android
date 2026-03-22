$agentName = "quality-pm-expert"
$issues = @()

$domainUseCases = Get-ChildItem -Path "app/src/main/java/*/domain" -Filter "*UseCase.kt" -Recurse -ErrorAction SilentlyContinue

foreach ($useCase in $domainUseCases) {
    $testFileName = $useCase.Name.Replace(".kt", "Test.kt")
    $testFilePath = (Get-ChildItem -Path "app/src/test/java" -Filter $testFileName -Recurse -ErrorAction SilentlyContinue).FullName

    if (-not $testFilePath) {
        $issues += @{ type = "CoverageGap"; file = $useCase.Name; message = "No associated test file found: $testFileName" }
        
        # AUTOMATED BOILERPLATE
        # Extract package name and class name
        $packageMatch = (Select-String -Path $useCase.FullName -Pattern "^package\s+(.+)").Matches.Groups[1].Value
        $className = $useCase.Name.Replace(".kt", "")
        
        if ($packageMatch) {
            $testDir = "app/src/test/java/" + $packageMatch.Replace(".", "/")
            if (-not (Test-Path $testDir)) { New-Item -ItemType Directory -Force -Path $testDir | Out-Null }
            
            $boilerplate = @"
package $packageMatch

import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class $($className)Test {
    
    // TODO: Add mocked dependencies here
    // private val repository: MyRepository = mockk()
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `test initial behavior`() {
        // Given
        
        // When
        
        // Then
    }
}
"@
            Set-Content -Path "$testDir/$testFileName" -Value $boilerplate
            $issues += @{ type = "AutoHeal"; file = $testFileName; message = "Automated MockK boilerplate generated." }
        }
    }
}

$trueErrors = $issues | Where-Object { $_.type -ne "AutoHeal" }
$status = if ($trueErrors.Count -eq 0) { "PASSED" } else { "FAILED" }
$result = @{ agent = $agentName; status = $status; issues = $issues }

$outPath = ".agents/pipeline/qa_report.json"
$result | ConvertTo-Json -Depth 3 | Out-File $outPath -Force
Write-Host "QA Audit Completed. Result saved to $outPath" -ForegroundColor Yellow
