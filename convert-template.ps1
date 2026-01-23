# POI-TL Template Conversion Script (UTF-8 Fixed)

Add-Type -AssemblyName System.IO.Compression.FileSystem

$baseDir = "C:\Users\Christopher\JetBrainProjects\shu-he-management-system-backend\shuhe-module-project\src\main\resources\templates\report"

# Use Unicode escape to avoid encoding issues
$passText = [char]0x901A + [char]0x8FC7  # 通过
$failText = [char]0x4E0D + [char]0x901A + [char]0x8FC7  # 不通过

function Convert-TemplateExpressions {
    param([string]$content, [string]$pass, [string]$fail)
    
    # Convert {{getColoredStatusXml(hasVulnType('xxx'))}} 
    # to {{vulnTypes.contains('xxx') ? 'fail' : 'pass'}}
    $pattern = '\{\{getColoredStatusXml\s*\(\s*hasVulnType\s*\(\s*[''"]([^''"]+)[''"]\s*\)\s*\)\s*\}\}'
    
    $content = [regex]::Replace($content, $pattern, {
        param($match)
        $vulnType = $match.Groups[1].Value
        return "{{vulnTypes.contains('$vulnType') ? '$fail' : '$pass'}}"
    })
    
    # Convert {{getSystemStatusXml(...)}} to {{system_status_text}}
    $content = [regex]::Replace($content, '\{\{getSystemStatusXml\s*\([^)]*\)\s*\}\}', '{{system_status_text}}')
    
    return $content
}

$files = Get-ChildItem -LiteralPath $baseDir -Filter "*.docx"

foreach ($file in $files) {
    $InputFile = $file.FullName
    $tempZip = [System.IO.Path]::Combine($env:TEMP, "template_$([System.IO.Path]::GetRandomFileName()).zip")
    $tempDir = [System.IO.Path]::Combine($env:TEMP, "template_$([System.IO.Path]::GetRandomFileName())")
    
    try {
        Write-Host "Processing: $($file.Name)"
        
        [System.IO.File]::Copy($InputFile, $tempZip, $true)
        
        if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
        [System.IO.Compression.ZipFile]::ExtractToDirectory($tempZip, $tempDir)
        
        $changed = $false
        $utf8NoBom = New-Object System.Text.UTF8Encoding $false
        
        $xmlPath = [System.IO.Path]::Combine($tempDir, "word", "document.xml")
        if (Test-Path $xmlPath) {
            $content = [System.IO.File]::ReadAllText($xmlPath, $utf8NoBom)
            $newContent = Convert-TemplateExpressions -content $content -pass $passText -fail $failText
            
            if ($newContent -ne $content) {
                [System.IO.File]::WriteAllText($xmlPath, $newContent, $utf8NoBom)
                $changed = $true
                Write-Host "  - document.xml: Converted"
            }
        }
        
        Get-ChildItem -LiteralPath ([System.IO.Path]::Combine($tempDir, "word")) -Filter "*.xml" -ErrorAction SilentlyContinue | 
            Where-Object { $_.Name -match "header|footer" } | 
            ForEach-Object {
                $content = [System.IO.File]::ReadAllText($_.FullName, $utf8NoBom)
                $newContent = Convert-TemplateExpressions -content $content -pass $passText -fail $failText
                if ($newContent -ne $content) {
                    [System.IO.File]::WriteAllText($_.FullName, $newContent, $utf8NoBom)
                    $changed = $true
                    Write-Host "  - $($_.Name): Converted"
                }
            }
        
        if ($changed) {
            Remove-Item -LiteralPath $InputFile -Force
            [System.IO.Compression.ZipFile]::CreateFromDirectory($tempDir, $InputFile)
            Write-Host "  [OK] Saved" -ForegroundColor Green
        } else {
            Write-Host "  [SKIP] No changes needed" -ForegroundColor Yellow
        }
    }
    catch {
        Write-Host "  [ERROR] $_" -ForegroundColor Red
    }
    finally {
        if (Test-Path $tempZip) { Remove-Item $tempZip -Force -ErrorAction SilentlyContinue }
        if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue }
    }
}

Write-Host "Done!" -ForegroundColor Cyan
