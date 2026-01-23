# 批量替换模板中的检测项格式
# 将 {{vulnTypes.contains('xxx') ? '不通过' : '通过'}} 改成 {{check_xxx}}

$templateDir = "shuhe-module-project\src\main\resources\templates\report"
$tempDir = "temp_docx_convert"

# 要处理的模板文件
$templates = @(
    "奇安信渗透测试模板.docx"
)

foreach ($templateName in $templates) {
    $templatePath = Join-Path $templateDir $templateName
    
    if (-not (Test-Path $templatePath)) {
        Write-Host "模板文件不存在: $templatePath" -ForegroundColor Red
        continue
    }
    
    Write-Host "处理模板: $templateName" -ForegroundColor Cyan
    
    # 创建临时目录
    if (Test-Path $tempDir) {
        Remove-Item -Recurse -Force $tempDir
    }
    New-Item -ItemType Directory -Path $tempDir | Out-Null
    
    # 解压 docx
    $zipPath = Join-Path $tempDir "template.zip"
    Copy-Item $templatePath $zipPath
    Expand-Archive -Path $zipPath -DestinationPath $tempDir -Force
    Remove-Item $zipPath
    
    # 读取 document.xml
    $documentXmlPath = Join-Path $tempDir "word\document.xml"
    $content = Get-Content -Path $documentXmlPath -Raw -Encoding UTF8
    
    # 常见的检测项列表
    $checkItems = @(
        "SQL注入", "XSS攻击", "XML外部实体注入", "CSRF攻击", "SSRF服务器端请求伪造",
        "文件上传漏洞", "任意文件下载", "目录遍历", "源代码泄漏", "信息泄露",
        "CRLF注入", "命令注入", "未验证的重定向", "JSON劫持", "不安全的第三方组件",
        "不安全的文件包含", "远程代码执行", "缺少X-XSS-Protection响应头", "Flash 跨域漏洞",
        "缺少CSRF保护", "明文传输", "通过 GET 传输用户名和密码", "未配置 X-Frame-Options 响应头",
        "任意文件删除", "越权漏洞", "短信/邮件轰炸", "验证码漏洞", "弱口令",
        "暴力破解", "会话固定", "会话劫持", "点击劫持", "敏感数据暴露",
        "SSL/TLS 弱加密", "SSL/TLS 证书问题", "SSL/TLS 存在 FREAK 攻击风险（CVE-2015-0204）",
        "允许 HTTP OPTIONS 方法", "不安全的HTTP方法", "物理路径泄漏", "敏感数据外泄"
    )
    
    $replacementCount = 0
    
    foreach ($item in $checkItems) {
        # 转义特殊字符用于正则表达式
        $escapedItem = [regex]::Escape($item)
        
        # 匹配完整的三元表达式
        $pattern = "vulnTypes\.contains\('$escapedItem'\)\s*\?\s*'[^']*'\s*:\s*'[^']*'"
        
        $matches = [regex]::Matches($content, $pattern)
        if ($matches.Count -gt 0) {
            $content = [regex]::Replace($content, $pattern, "check_$item")
            $replacementCount++
            Write-Host "  替换: $item" -ForegroundColor Green
        }
    }
    
    Write-Host "共替换 $replacementCount 个检测项" -ForegroundColor Cyan
    
    # 保存修改后的 document.xml
    [System.IO.File]::WriteAllText($documentXmlPath, $content, [System.Text.Encoding]::UTF8)
    
    # 重新打包 docx
    $outputPath = $templatePath + ".new.docx"
    
    # 使用 .NET 压缩
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    
    if (Test-Path $outputPath) {
        Remove-Item $outputPath
    }
    
    [System.IO.Compression.ZipFile]::CreateFromDirectory($tempDir, $outputPath)
    
    # 备份原文件并替换
    $backupPath = $templatePath + ".backup"
    if (Test-Path $backupPath) {
        Remove-Item $backupPath
    }
    Move-Item $templatePath $backupPath
    Move-Item $outputPath $templatePath
    
    Write-Host "模板已更新: $templateName" -ForegroundColor Green
    
    # 清理临时目录
    Remove-Item -Recurse -Force $tempDir
}

Write-Host "完成!" -ForegroundColor Cyan
