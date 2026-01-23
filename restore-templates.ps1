# Restore templates from poi-tl-final directory
$sourceDir = "C:\Users\Christopher\WebstormProjects\Shuhe-ReportGenerator\src\templates\poi-tl-final\pentest"
$targetDir = "C:\Users\Christopher\JetBrainProjects\shu-he-management-system-backend\shuhe-module-project\src\main\resources\templates\report"

Get-ChildItem -LiteralPath $sourceDir -Filter "*.docx" | ForEach-Object {
    $targetPath = Join-Path $targetDir $_.Name
    Copy-Item -LiteralPath $_.FullName -Destination $targetPath -Force
    Write-Host "Restored: $($_.Name)"
}

# Also restore retest templates
$sourceRetestDir = "C:\Users\Christopher\WebstormProjects\Shuhe-ReportGenerator\src\templates\poi-tl-final\retest"
Get-ChildItem -LiteralPath $sourceRetestDir -Filter "*.docx" | ForEach-Object {
    $targetPath = Join-Path $targetDir $_.Name
    Copy-Item -LiteralPath $_.FullName -Destination $targetPath -Force
    Write-Host "Restored: $($_.Name)"
}

Write-Host "Done!"
