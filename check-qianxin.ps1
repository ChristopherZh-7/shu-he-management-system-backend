# Check specific template
Add-Type -AssemblyName System.IO.Compression.FileSystem

$targetDir = "C:\Users\Christopher\JetBrainProjects\shu-he-management-system-backend\shuhe-module-project\src\main\resources\templates\report"
$files = Get-ChildItem -LiteralPath $targetDir -Filter "*.docx"

# Find the file with specific name pattern
foreach ($f in $files) {
    if ($f.Name -match "奇安信") {
        Write-Host "Found: $($f.Name)"
        
        $tempZip = [System.IO.Path]::Combine($env:TEMP, "check_qax.zip")
        $tempDir = [System.IO.Path]::Combine($env:TEMP, "check_qax")
        
        if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
        if (Test-Path $tempZip) { Remove-Item $tempZip -Force }
        
        [System.IO.File]::Copy($f.FullName, $tempZip, $true)
        [System.IO.Compression.ZipFile]::ExtractToDirectory($tempZip, $tempDir)
        
        $xmlPath = [System.IO.Path]::Combine($tempDir, "word", "document.xml")
        $utf8 = New-Object System.Text.UTF8Encoding $false
        $content = [System.IO.File]::ReadAllText($xmlPath, $utf8)
        
        # Check for old format
        $oldMatches = [regex]::Matches($content, "getColoredStatusXml")
        Write-Host "Old format (getColoredStatusXml): $($oldMatches.Count)"
        
        # Check for new format  
        $newMatches = [regex]::Matches($content, "vulnTypes\.contains")
        Write-Host "New format (vulnTypes.contains): $($newMatches.Count)"
        
        Remove-Item $tempZip -Force -ErrorAction SilentlyContinue
        Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}
