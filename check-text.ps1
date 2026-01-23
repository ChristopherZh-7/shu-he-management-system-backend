# Check if pass/fail text is correct
Add-Type -AssemblyName System.IO.Compression.FileSystem

$targetDir = "C:\Users\Christopher\JetBrainProjects\shu-he-management-system-backend\shuhe-module-project\src\main\resources\templates\report"
$files = Get-ChildItem -LiteralPath $targetDir -Filter "*.docx"
$file = $files | Select-Object -First 1

$tempZip = [System.IO.Path]::Combine($env:TEMP, "check_text.zip")
$tempDir = [System.IO.Path]::Combine($env:TEMP, "check_text")

if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
if (Test-Path $tempZip) { Remove-Item $tempZip -Force }

[System.IO.File]::Copy($file.FullName, $tempZip, $true)
[System.IO.Compression.ZipFile]::ExtractToDirectory($tempZip, $tempDir)

$xmlPath = [System.IO.Path]::Combine($tempDir, "word", "document.xml")
$utf8 = New-Object System.Text.UTF8Encoding $false
$content = [System.IO.File]::ReadAllText($xmlPath, $utf8)

# Check for the ternary expression
$pattern = "\{\{vulnTypes\.contains\([^)]+\)\s*\?\s*'([^']+)'\s*:\s*'([^']+)'\}\}"
$matches = [regex]::Matches($content, $pattern)
Write-Host "Found $($matches.Count) complete expressions"

if ($matches.Count -gt 0) {
    $first = $matches[0]
    Write-Host "First match full: $($first.Value)"
    Write-Host "Fail text: $($first.Groups[1].Value)"
    Write-Host "Pass text: $($first.Groups[2].Value)"
    
    # Check if Chinese is correct
    $failText = $first.Groups[1].Value
    $passText = $first.Groups[2].Value
    
    # Expected values
    $expectedFail = [char]0x4E0D + [char]0x901A + [char]0x8FC7  # 不通过
    $expectedPass = [char]0x901A + [char]0x8FC7  # 通过
    
    Write-Host ""
    Write-Host "Expected fail: $expectedFail"
    Write-Host "Expected pass: $expectedPass"
    Write-Host "Match fail: $($failText -eq $expectedFail)"
    Write-Host "Match pass: $($passText -eq $expectedPass)"
}

Remove-Item $tempZip -Force -ErrorAction SilentlyContinue
Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
