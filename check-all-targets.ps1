# Check all templates for targets patterns
Add-Type -AssemblyName System.IO.Compression.FileSystem

$targetDir = "C:\Users\Christopher\JetBrainProjects\shu-he-management-system-backend\shuhe-module-project\src\main\resources\templates\report"
$files = Get-ChildItem -LiteralPath $targetDir -Filter "*.docx"

foreach ($f in $files) {
    $tempZip = [System.IO.Path]::Combine($env:TEMP, "check_all_$([System.IO.Path]::GetRandomFileName()).zip")
    $tempDir = [System.IO.Path]::Combine($env:TEMP, "check_all_$([System.IO.Path]::GetRandomFileName())")
    
    try {
        [System.IO.File]::Copy($f.FullName, $tempZip, $true)
        [System.IO.Compression.ZipFile]::ExtractToDirectory($tempZip, $tempDir)
        
        $xmlPath = [System.IO.Path]::Combine($tempDir, "word", "document.xml")
        $utf8 = New-Object System.Text.UTF8Encoding $false
        $content = [System.IO.File]::ReadAllText($xmlPath, $utf8)
        
        # Find {{targets}} (exact, not targets_text)
        $exactMatches = [regex]::Matches($content, '\{\{targets\}\}')
        $textMatches = [regex]::Matches($content, '\{\{targets_text\}\}')
        
        if ($exactMatches.Count -gt 0) {
            Write-Host "[HAS {{targets}}] $($f.Name): $($exactMatches.Count) exact, $($textMatches.Count) text"
        } else {
            Write-Host "[OK] $($f.Name): $($textMatches.Count) text only"
        }
    }
    finally {
        Remove-Item $tempZip -Force -ErrorAction SilentlyContinue
        Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}
