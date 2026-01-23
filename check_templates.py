# -*- coding: utf-8 -*-
"""检查模板中是否有包含点号的 check_ 占位符"""

import zipfile
import re
from pathlib import Path

templates = [
    "戍合科技渗透测试模板.docx",
    "天融信渗透测试模版.docx",
]
template_dir = Path("shuhe-module-project/src/main/resources/templates/report")

for t in templates:
    p = template_dir / t
    if p.exists():
        with zipfile.ZipFile(p, 'r') as zf:
            content = zf.read('word/document.xml').decode('utf-8')
            # Find check_ patterns with dots
            pattern = r'check_[^}]*\.[^}]*'
            matches = re.findall(pattern, content)
            if matches:
                print(f"{t}: Found patterns with dots:")
                for m in set(matches[:10]):  # Show unique, limit to 10
                    print(f"  - {m}")
            else:
                print(f"{t}: No patterns with dots found")
    else:
        print(f"{t}: file not found")
