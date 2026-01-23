# Check targets occurrences in template
Add-Type -AssemblyName System.IO.Compression.FileSystem

$targetDir = "C:\Users\Christopher\JetBrainProjects\shu-he-management-system-backend\shuhe-module-project\src\main\resources\templates\report"
$files = Get-ChildItem -LiteralPath $targetDir -Filter "*.docx"

# Find the Qianxin template
$qaxFile = $files | Select-Object -First 1

$tempZip = [System.IO.Path]::Combine($env:TEMP, "check_qax.zip")
$tempDir = [System.IO.Path]::Combine($env:TEMP, "check_qax")

if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
if (Test-Path $tempZip) { Remove-Item $tempZip -Force }

[System.IO.File]::Copy($qaxFile.FullName, $tempZip, $true)
[System.IO.Compression.ZipFile]::ExtractToDirectory($tempZip, $tempDir)

$xmlPath = [System.IO.Path]::Combine($tempDir, "word", "document.xml")
$utf8 = New-Object System.Text.UTF8Encoding $false
$content = [System.IO.File]::ReadAllText($xmlPath, $utf8)

# Find all targets patterns
$pattern = '\{\{targets[^}]*\}\}'
$matches = [regex]::Matches($content, $pattern)

Write-Host "File: $($qaxFile.Name)"
Write-Host "Found $($matches.Count) targets-related patterns:"
foreach ($m in $matches) {
    Write-Host "  $($m.Value)"
}

# Also check for targets_text
$textMatches = [regex]::Matches($content, 'targets_text')
Write-Host ""
Write-Host "targets_text count: $($textMatches.Count)"

Remove-Item $tempZip -Force -ErrorAction SilentlyContinue
Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
