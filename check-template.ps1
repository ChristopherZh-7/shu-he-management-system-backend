# Check template content
Add-Type -AssemblyName System.IO.Compression.FileSystem

$sourceDir = "C:\Users\Christopher\WebstormProjects\Shuhe-ReportGenerator\src\templates\poi-tl-final\pentest"
$files = Get-ChildItem -LiteralPath $sourceDir -Filter "*.docx"
$file = $files | Select-Object -First 1

Write-Host "Checking first file: $($file.Name)"

$tempZip = [System.IO.Path]::Combine($env:TEMP, "check_template.zip")
$tempDir = [System.IO.Path]::Combine($env:TEMP, "check_template")

if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
if (Test-Path $tempZip) { Remove-Item $tempZip -Force }

[System.IO.File]::Copy($file.FullName, $tempZip, $true)
[System.IO.Compression.ZipFile]::ExtractToDirectory($tempZip, $tempDir)

$xmlPath = [System.IO.Path]::Combine($tempDir, "word", "document.xml")
$utf8 = New-Object System.Text.UTF8Encoding $false
$content = [System.IO.File]::ReadAllText($xmlPath, $utf8)

$matches = [regex]::Matches($content, '\{\{[^}]+\}\}')
Write-Host "Found $($matches.Count) template tags"
$matches | Select-Object -First 50 | ForEach-Object { Write-Host $_.Value }

Remove-Item $tempZip -Force -ErrorAction SilentlyContinue
Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
