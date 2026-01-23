# -*- coding: utf-8 -*-
"""
修复模板中包含特殊字符的 check_ 占位符
将 check_xxx xxx.yyy 格式转换为 check_xxx_xxx_yyy 格式
"""

import os
import re
import shutil
import zipfile
from pathlib import Path

# 模板目录
TEMPLATE_DIR = Path("shuhe-module-project/src/main/resources/templates/report")

# 需要修复的模板
TEMPLATES = [
    "戍合科技渗透测试模板.docx",
    "戍合科技渗透测试复测模板.docx",
    "天融信渗透测试模版.docx",
    "天融信渗透测试复测模版.docx",
]


def normalize_key_name(name):
    """标准化 key 名称"""
    # 去掉 check_ 前缀后处理
    normalized = re.sub(r'[\-/\(\)（）,，\.\s—–―]+', '_', name.strip())
    normalized = re.sub(r'_+', '_', normalized)
    normalized = re.sub(r'^_|_$', '', normalized)
    return normalized


def fix_check_pattern(match):
    """修复单个 check_ 占位符"""
    original = match.group(0)
    # 提取 check_ 后面的内容
    content = original[6:]  # 去掉 "check_"
    # 标准化
    normalized = normalize_key_name(content)
    return f"check_{normalized}"


def process_template(template_path: Path):
    """处理单个模板文件"""
    print(f"处理模板: {template_path.name}")
    
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
        
        # 找出所有 check_ 占位符并修复
        # 匹配 {{check_...}} 格式
        pattern = r'\{\{check_([^}]+)\}\}'
        
        def replace_func(match):
            inner = match.group(1)
            normalized = normalize_key_name(inner)
            return f"{{{{check_{normalized}}}}}"
        
        new_content = re.sub(pattern, replace_func, content)
        
        # 统计变更
        if new_content != content:
            # 计算有多少处变更
            old_matches = set(re.findall(pattern, content))
            new_matches = set(re.findall(r'\{\{check_([^}]+)\}\}', new_content))
            changed = old_matches - new_matches
            print(f"  修复了 {len(changed)} 个占位符")
            for c in list(changed)[:5]:
                print(f"    - {c[:50]}...")
        else:
            print("  无需修复")
        
        # 保存修改后的 document.xml
        with open(document_xml_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        # 重新打包 docx
        with zipfile.ZipFile(template_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
            for root, dirs, files in os.walk(temp_dir):
                for file in files:
                    file_path = Path(root) / file
                    arcname = file_path.relative_to(temp_dir)
                    zipf.write(file_path, arcname)
        
        print(f"模板已更新: {template_path.name}")
        
    finally:
        # 清理临时目录
        if temp_dir.exists():
            shutil.rmtree(temp_dir)


def main():
    print("=== 修复 check_ 占位符中的特殊字符 ===\n")
    
    for template_name in TEMPLATES:
        template_path = TEMPLATE_DIR / template_name
        
        if not template_path.exists():
            print(f"模板文件不存在: {template_path}")
            continue
        
        process_template(template_path)
        print()
    
    print("完成!")


if __name__ == "__main__":
    main()
