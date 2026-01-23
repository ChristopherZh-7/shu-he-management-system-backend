# Fix Qianxin template
Add-Type -AssemblyName System.IO.Compression.FileSystem

$passText = [char]0x901A + [char]0x8FC7  
$failText = [char]0x4E0D + [char]0x901A + [char]0x8FC7  

$sourceDir = "C:\Users\Christopher\WebstormProjects\Shuhe-ReportGenerator\src\templates\poi-tl-final\pentest"
$targetDir = "C:\Users\Christopher\JetBrainProjects\shu-he-management-system-backend\shuhe-module-project\src\main\resources\templates\report"

$sourceFiles = Get-ChildItem -LiteralPath $sourceDir -Filter "*.docx"

Write-Host "Source files found: $($sourceFiles.Count)"

foreach ($f in $sourceFiles) {
    Write-Host "Processing: $($f.Name)"
    
    $targetPath = [System.IO.Path]::Combine($targetDir, $f.Name)
    
    $tempZip = [System.IO.Path]::Combine($env:TEMP, "qax_$([System.IO.Path]::GetRandomFileName()).zip")
    $tempDir = [System.IO.Path]::Combine($env:TEMP, "qax_$([System.IO.Path]::GetRandomFileName())")
    
    if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
    
    [System.IO.File]::Copy($f.FullName, $tempZip, $true)
    [System.IO.Compression.ZipFile]::ExtractToDirectory($tempZip, $tempDir)
    
    $xmlPath = [System.IO.Path]::Combine($tempDir, "word", "document.xml")
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    $content = [System.IO.File]::ReadAllText($xmlPath, $utf8NoBom)
    
    $oldCount = [regex]::Matches($content, 'getColoredStatusXml').Count
    
    $pattern = '\{\{getColoredStatusXml\s*\(\s*hasVulnType\s*\(\s*[''"]([^''"]+)[''"]\s*\)\s*\)\s*\}\}'
    $newContent = [regex]::Replace($content, $pattern, {
        param($match)
        $vulnType = $match.Groups[1].Value
        return "{{vulnTypes.contains('$vulnType') ? '$failText' : '$passText'}}"
    })
    
    $newContent = [regex]::Replace($newContent, '\{\{getSystemStatusXml\s*\([^)]*\)\s*\}\}', '{{system_status_text}}')
    
    $newCount = [regex]::Matches($newContent, 'vulnTypes\.contains').Count
    
    if ($oldCount -gt 0) {
        [System.IO.File]::WriteAllText($xmlPath, $newContent, $utf8NoBom)
        
        if (Test-Path $targetPath) { Remove-Item -LiteralPath $targetPath -Force }
        [System.IO.Compression.ZipFile]::CreateFromDirectory($tempDir, $targetPath)
        
        Write-Host "  Converted: $oldCount -> $newCount"
    } else {
        Write-Host "  Skipped (no changes needed)"
    }
    
    Remove-Item $tempZip -Force -ErrorAction SilentlyContinue
    Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "Done!"
