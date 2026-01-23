# Check converted template content
Add-Type -AssemblyName System.IO.Compression.FileSystem

$targetDir = "C:\Users\Christopher\JetBrainProjects\shu-he-management-system-backend\shuhe-module-project\src\main\resources\templates\report"
$files = Get-ChildItem -LiteralPath $targetDir -Filter "*.docx"
$file = $files | Select-Object -First 1

Write-Host "Checking: $($file.Name)"

$tempZip = [System.IO.Path]::Combine($env:TEMP, "check_converted.zip")
$tempDir = [System.IO.Path]::Combine($env:TEMP, "check_converted")

if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
if (Test-Path $tempZip) { Remove-Item $tempZip -Force }

[System.IO.File]::Copy($file.FullName, $tempZip, $true)
[System.IO.Compression.ZipFile]::ExtractToDirectory($tempZip, $tempDir)

$xmlPath = [System.IO.Path]::Combine($tempDir, "word", "document.xml")
$utf8 = New-Object System.Text.UTF8Encoding $false
$content = [System.IO.File]::ReadAllText($xmlPath, $utf8)

# Find vulnTypes.contains patterns
$matches = [regex]::Matches($content, "vulnTypes\.contains\([^)]+\)")
Write-Host "Found $($matches.Count) vulnTypes.contains patterns"
$matches | Select-Object -First 10 | ForEach-Object { Write-Host $_.Value }

Remove-Item $tempZip -Force -ErrorAction SilentlyContinue
Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
