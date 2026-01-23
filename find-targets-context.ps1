# Find context around {{targets}} in template
Add-Type -AssemblyName System.IO.Compression.FileSystem

$targetDir = "C:\Users\Christopher\JetBrainProjects\shu-he-management-system-backend\shuhe-module-project\src\main\resources\templates\report"
$files = Get-ChildItem -LiteralPath $targetDir -Filter "*.docx"

# Find files with exact {{targets}}
foreach ($f in $files) {
    $tempZip = [System.IO.Path]::Combine($env:TEMP, "ctx_$([System.IO.Path]::GetRandomFileName()).zip")
    $tempDir = [System.IO.Path]::Combine($env:TEMP, "ctx_$([System.IO.Path]::GetRandomFileName())")
    
    try {
        [System.IO.File]::Copy($f.FullName, $tempZip, $true)
        [System.IO.Compression.ZipFile]::ExtractToDirectory($tempZip, $tempDir)
        
        $xmlPath = [System.IO.Path]::Combine($tempDir, "word", "document.xml")
        $utf8 = New-Object System.Text.UTF8Encoding $false
        $content = [System.IO.File]::ReadAllText($xmlPath, $utf8)
        
        $match = [regex]::Match($content, '\{\{targets\}\}')
        if ($match.Success) {
            Write-Host "File: $($f.Name)"
            $start = [Math]::Max(0, $match.Index - 200)
            $length = [Math]::Min(500, $content.Length - $start)
            $context = $content.Substring($start, $length)
            
            # Check if it's inside a table (w:tbl)
            $beforeContext = $content.Substring(0, $match.Index)
            $lastTblStart = $beforeContext.LastIndexOf("<w:tbl")
            $lastTblEnd = $beforeContext.LastIndexOf("</w:tbl>")
            
            if ($lastTblStart -gt $lastTblEnd) {
                Write-Host "  In table: YES"
            } else {
                Write-Host "  In table: NO (lastTblStart=$lastTblStart, lastTblEnd=$lastTblEnd)"
            }
            Write-Host ""
        }
    }
    finally {
        Remove-Item $tempZip -Force -ErrorAction SilentlyContinue
        Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}
