# -*- coding: utf-8 -*-
import zipfile
from pathlib import Path
import re

template_path = Path("shuhe-module-project/src/main/resources/templates/report") / "奇安信渗透测试模板.docx"
with zipfile.ZipFile(template_path, 'r') as zip_ref:
    with zip_ref.open('word/document.xml') as f:
        content = f.read().decode('utf-8')
        
        # 搜索所有 vulnTypes.contains 模式
        pattern = r"vulnTypes\.contains\([^)]+\)"
        matches = re.findall(pattern, content)
        print(f"找到 {len(matches)} 个未替换的 vulnTypes.contains 模式:")
        for m in matches:
            print(f"  {m}")
