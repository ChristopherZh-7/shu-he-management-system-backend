# -*- coding: utf-8 -*-
"""
修复被 Word XML 标签分割的占位符
"""

import os
import re
import shutil
import zipfile
from pathlib import Path

template_path = Path("shuhe-module-project/src/main/resources/templates/report") / "奇安信渗透测试模板.docx"
temp_dir = Path("temp_docx_fix")

# 清理临时目录
if temp_dir.exists():
    shutil.rmtree(temp_dir)
temp_dir.mkdir()

try:
    # 解压 docx
    with zipfile.ZipFile(template_path, 'r') as zip_ref:
        zip_ref.extractall(temp_dir)
    
    # 读取 document.xml
    document_xml_path = temp_dir / "word" / "document.xml"
    with open(document_xml_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 修复 Flash 跨域
    # 原始格式（被 XML 标签分割）：vulnTypes.contains('Flash 跨域</w:t>...<w:t>（crossdomain.xml 配置过宽）')
    # 需要替换整个包含这个模式的结构
    
    # 使用更宽松的正则表达式匹配被分割的占位符
    # 匹配 vulnTypes.contains('Flash ... crossdomain.xml ... ') ? '不通过' : '通过'
    flash_pattern = r"vulnTypes\.contains\('Flash [^']*crossdomain\.xml[^']*'\)(\s*\?\s*'[^']*'\s*:\s*'[^']*')?"
    
    def clean_xml_tags(match_text):
        """移除 XML 标签，只保留文本"""
        # 移除所有 XML 标签
        text = re.sub(r'<[^>]+>', '', match_text)
        return text
    
    # 查找并替换 Flash 跨域
    flash_matches = list(re.finditer(flash_pattern, content))
    print(f"找到 {len(flash_matches)} 个 Flash 跨域匹配")
    for m in flash_matches:
        print(f"  原始: {m.group()[:100]}...")
    
    if flash_matches:
        # 直接替换整个三元表达式
        content = re.sub(flash_pattern, "check_Flash 跨域（crossdomain.xml 配置过宽）", content)
        print("  已替换 Flash 跨域")
    
    # 修复 HTTP OPTIONS
    options_pattern = r"vulnTypes\.contains\('允许 HTTP OPTIONS [^']*'\)(\s*\?\s*'[^']*'\s*:\s*'[^']*')?"
    
    options_matches = list(re.finditer(options_pattern, content))
    print(f"找到 {len(options_matches)} 个 HTTP OPTIONS 匹配")
    for m in options_matches:
        print(f"  原始: {m.group()[:100]}...")
    
    if options_matches:
        content = re.sub(options_pattern, "check_允许 HTTP OPTIONS 方法", content)
        print("  已替换 HTTP OPTIONS")
    
    # 保存修改后的 document.xml
    with open(document_xml_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    # 备份原文件
    backup_path = template_path.with_suffix('.docx.backup3')
    if backup_path.exists():
        backup_path.unlink()
    shutil.copy2(template_path, backup_path)
    
    # 重新打包 docx
    with zipfile.ZipFile(template_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk(temp_dir):
            for file in files:
                file_path = Path(root) / file
                arcname = file_path.relative_to(temp_dir)
                zipf.write(file_path, arcname)
    
    print("模板已更新!")
    
finally:
    # 清理临时目录
    if temp_dir.exists():
        shutil.rmtree(temp_dir)
