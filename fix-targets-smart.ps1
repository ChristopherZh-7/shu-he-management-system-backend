# Fix targets: replace {{targets}} NOT in table with {{targets_text}}
Add-Type -AssemblyName System.IO.Compression.FileSystem

$targetDir = "C:\Users\Christopher\JetBrainProjects\shu-he-management-system-backend\shuhe-module-project\src\main\resources\templates\report"
$files = Get-ChildItem -LiteralPath $targetDir -Filter "*.docx"

foreach ($f in $files) {
    $tempZip = [System.IO.Path]::Combine($env:TEMP, "fix_smart_$([System.IO.Path]::GetRandomFileName()).zip")
    $tempDir = [System.IO.Path]::Combine($env:TEMP, "fix_smart_$([System.IO.Path]::GetRandomFileName())")
    
    try {
        [System.IO.File]::Copy($f.FullName, $tempZip, $true)
        if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
        [System.IO.Compression.ZipFile]::ExtractToDirectory($tempZip, $tempDir)
        
        $xmlPath = [System.IO.Path]::Combine($tempDir, "word", "document.xml")
        $utf8NoBom = New-Object System.Text.UTF8Encoding $false
        $content = [System.IO.File]::ReadAllText($xmlPath, $utf8NoBom)
        
        $changed = $false
        
        # Find all {{targets}} matches
        $matches = [regex]::Matches($content, '\{\{targets\}\}')
        
        if ($matches.Count -gt 0) {
            # For each match, check if it's inside a table
            $newContent = $content
            $offset = 0
            
            foreach ($m in $matches) {
                $pos = $m.Index
                $beforeContext = $content.Substring(0, $pos)
                $lastTblStart = $beforeContext.LastIndexOf("<w:tbl")
                $lastTblEnd = $beforeContext.LastIndexOf("</w:tbl>")
                
                # If lastTblStart > lastTblEnd, we're inside a table - keep it
                # Otherwise, replace with targets_text
                if ($lastTblStart -le $lastTblEnd) {
                    # Not in table - replace
                    $adjustedPos = $pos + $offset
                    $newContent = $newContent.Substring(0, $adjustedPos) + "{{targets_text}}" + $newContent.Substring($adjustedPos + 11)
                    $offset += 5  # "targets_text" is 5 chars longer than "targets"
                    $changed = $true
                }
            }
            
            if ($changed) {
                [System.IO.File]::WriteAllText($xmlPath, $newContent, $utf8NoBom)
                Remove-Item -LiteralPath $f.FullName -Force
                [System.IO.Compression.ZipFile]::CreateFromDirectory($tempDir, $f.FullName)
                Write-Host "[FIXED] $($f.Name)"
            } else {
                Write-Host "[OK] $($f.Name) - all targets in tables"
            }
        } else {
            Write-Host "[SKIP] $($f.Name) - no {{targets}}"
        }
    }
    finally {
        Remove-Item $tempZip -Force -ErrorAction SilentlyContinue
        Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "Done!"
