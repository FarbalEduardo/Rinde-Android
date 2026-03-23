Write-Host "Iniciando Escaneo de Seguridad OWASP MASVS..." -ForegroundColor Cyan

$rules = @(
    @{
        Pattern = 'android:allowBackup="true"'
        Message = "MASVS-STORAGE-1: allowBackup habilitado."
        Level = "HIGH"
        Ext = "*.xml"
    },
    @{
        Pattern = 'android:debuggable="true"'
        Message = "MASVS-RESILIENCE-4: App depurable."
        Level = "CRITICAL"
        Ext = "*.xml"
    },
    @{
        Pattern = 'http://[a-zA-Z0-9./?=_-]+'
        Message = "MASVS-NETWORK-1: Uso de HTTP insecure."
        Level = "MEDIUM"
        Ext = "*.kt", "*.java", "*.xml"
    },
    @{
        Pattern = '(?i)(api[_-]?key|secret|password|token|private[_-]?key)\s*[:=]\s*["''][a-zA-Z0-9]{10,}["'']'
        Message = "SECRETO HARDCODEADO."
        Level = "CRITICAL"
        Ext = "*.kt", "*.java", "*.xml", "*.json"
    },
    @{
        Pattern = 'Log\.(d|v|i|e)\(.*?token|password|secret.*?\)'
        Message = "Logging sensible detectado."
        Level = "HIGH"
        Ext = "*.kt", "*.java"
    },
    @{
        Pattern = 'javascriptEnabled\s*=\s*true'
        Message = "MASVS-PLATFORM-2: WebView con JS habilitado."
        Level = "MEDIUM"
        Ext = "*.kt", "*.java"
    }
)

$findingsCount = 0

foreach ($rule in $rules) {
    foreach ($ext in $rule.Ext) {
        $foundItems = Get-ChildItem -Path . -Include $ext -Recurse | 
                   Where-Object { $_.FullName -notmatch '\\build\\' -and $_.FullName -notmatch '\\\.gradle\\' -and $_.FullName -notmatch '\\\.idea\\' -and $_.FullName -notmatch '\\\.git\\' } |
                   Select-String -Pattern $rule.Pattern -ErrorAction SilentlyContinue
                   
        foreach ($match in $foundItems) {
            $findingsCount++
            Write-Host "[$($rule.Level)] $($match.Filename):$($match.LineNumber) - $($rule.Message)"
        }
    }
}

if ($findingsCount -eq 0) {
    Write-Host "✅ No se detectaron fallos críticos por escaneo de patrones." -ForegroundColor Green
} else {
    Write-Host "`n⚠️ Hallazgos de seguridad: $findingsCount" -ForegroundColor Red
}
