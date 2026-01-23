# Fix targets template issue
# Replace {{targets}} outside of table with {{targets_list}}
Add-Type -AssemblyName System.IO.Compression.FileSystem

$targetDir = "C:\Users\Christopher\JetBrainProjects\shu-he-management-system-backend\shuhe-module-project\src\main\resources\templates\report"
$files = Get-ChildItem -LiteralPath $targetDir -Filter "*.docx"

foreach ($f in $files) {
    $tempZip = [System.IO.Path]::Combine($env:TEMP, "fix_targets_$([System.IO.Path]::GetRandomFileName()).zip")
    $tempDir = [System.IO.Path]::Combine($env:TEMP, "fix_targets_$([System.IO.Path]::GetRandomFileName())")
    
    try {
        [System.IO.File]::Copy($f.FullName, $tempZip, $true)
        if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
        [System.IO.Compression.ZipFile]::ExtractToDirectory($tempZip, $tempDir)
        
        $xmlPath = [System.IO.Path]::Combine($tempDir, "word", "document.xml")
        $utf8NoBom = New-Object System.Text.UTF8Encoding $false
        $content = [System.IO.File]::ReadAllText($xmlPath, $utf8NoBom)
        
        $changed = $false
        
        # Count occurrences
        $matches = [regex]::Matches($content, '\{\{targets\}\}')
        
        if ($matches.Count -gt 1) {
            # If there are multiple {{targets}}, the first one (outside table) should be {{targets_list}}
            # Actually, looking at the log, it seems like the issue is that targets appears both
            # in normal text and in table. We need to rename the non-table one.
            
            # Strategy: Replace first occurrence only (which is typically outside table)
            $content = [regex]::Replace($content, '\{\{targets\}\}', '{{targets_list}}', 1)
            $changed = $true
        }
        
        if ($changed) {
            [System.IO.File]::WriteAllText($xmlPath, $content, $utf8NoBom)
            Remove-Item -LiteralPath $f.FullName -Force
            [System.IO.Compression.ZipFile]::CreateFromDirectory($tempDir, $f.FullName)
            Write-Host "[FIXED] $($f.Name)"
        } else {
            Write-Host "[SKIP] $($f.Name) - no multiple targets"
        }
    }
    finally {
        Remove-Item $tempZip -Force -ErrorAction SilentlyContinue
        Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "Done!"
