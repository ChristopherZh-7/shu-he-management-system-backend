# Verify template conversion
Add-Type -AssemblyName System.IO.Compression.FileSystem

$targetDir = "C:\Users\Christopher\JetBrainProjects\shu-he-management-system-backend\shuhe-module-project\src\main\resources\templates\report"
$files = Get-ChildItem -LiteralPath $targetDir -Filter "*.docx"

foreach ($f in $files) {
    $tempZip = [System.IO.Path]::Combine($env:TEMP, "verify_$([System.IO.Path]::GetRandomFileName()).zip")
    $tempDir = [System.IO.Path]::Combine($env:TEMP, "verify_$([System.IO.Path]::GetRandomFileName())")
    
    try {
        [System.IO.File]::Copy($f.FullName, $tempZip, $true)
        [System.IO.Compression.ZipFile]::ExtractToDirectory($tempZip, $tempDir)
        
        $xmlPath = [System.IO.Path]::Combine($tempDir, "word", "document.xml")
        $utf8NoBom = New-Object System.Text.UTF8Encoding $false
        $content = [System.IO.File]::ReadAllText($xmlPath, $utf8NoBom)
        
        $oldCount = [regex]::Matches($content, 'getColoredStatusXml').Count
        $newCount = [regex]::Matches($content, 'vulnTypes\.contains').Count
        
        $status = if ($oldCount -eq 0 -and $newCount -gt 0) { "[OK]" } elseif ($oldCount -gt 0) { "[NEED FIX]" } else { "[NO FUNC]" }
        
        Write-Host "$status $($f.Name): old=$oldCount, new=$newCount"
    }
    finally {
        Remove-Item $tempZip -Force -ErrorAction SilentlyContinue
        Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}
