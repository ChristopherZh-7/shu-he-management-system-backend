# -*- coding: utf-8 -*-
"""
恢复原始模板并重新转换
"""

import os
import re
import shutil
import zipfile
from pathlib import Path

# 模板目录
TEMPLATE_DIR = Path("shuhe-module-project/src/main/resources/templates/report")

# 要处理的模板文件（所有模板）
TEMPLATES = [
    "奇安信渗透测试模板.docx",
    "奇安信渗透测试复测模板.docx",
    "天融信渗透测试模版.docx",
    "天融信渗透测试复测模版.docx",
    "安恒渗透测试模板.docx",
    "安恒渗透测试复测模板.docx",
    "戍合科技渗透测试模板.docx",
    "戍合科技渗透测试复测模板.docx",
    "网信办-检测报告.docx",
    "网信办-检测复测报告.docx",
    "长亭渗透测试模板.docx",
    "长亭渗透测试报告模板2.docx",
]

# 完整的检测项列表
CHECK_ITEMS = [
    # Web安全
    "SQL注入", "XSS攻击", "XML外部实体注入", "CSRF攻击", "SSRF服务器端请求伪造",
    "文件上传漏洞", "任意文件下载", "目录遍历", "源代码泄漏", "信息泄露",
    "CRLF注入", "命令注入", "未验证的重定向", "JSON劫持", "不安全的第三方组件",
    "不安全的文件包含", "远程代码执行", "缺少X-XSS-Protection响应头", "Flash 跨域漏洞",
    "缺少CSRF保护", "明文传输", "通过 GET 传输用户名和密码", "未配置 X-Frame-Options 响应头",
    "任意文件删除", "越权漏洞", "短信/邮件轰炸", "验证码漏洞", "弱口令",
    "暴力破解", "会话固定", "会话劫持", "点击劫持", "敏感数据暴露",
    "SSL/TLS 弱加密", "SSL/TLS 证书问题", "SSL/TLS 存在 FREAK 攻击风险（CVE-2015-0204）",
    "允许 HTTP OPTIONS 方法", "不安全的HTTP方法", "物理路径泄漏", "敏感数据外泄",
    
    # 额外的检测项
    "绝对路径泄露",
    "Cookie 未设置 HttpOnly 属性",
    " X-Forwarded-For 伪造",
    "任意文件读取",
    "网络传输加密方式不安全",
    "使用不安全的 Telnet 协议",
    "验证码爆破,验证码失效,验证码绕过",
    "不安全的反序列化",
    "用户名枚举",
    "用户密码枚举",
    "会话固定攻击",
    "平行越权",
    "垂直越权",
    "未授权访问",
    "业务逻辑漏洞",
    "短信轰炸",
    "Flash 未混淆导致反编译",
    "中间件配置缺陷",
    "中间件弱口令",
    "JBoss 反序列化导致远程命令执行",
    "JBoss 反序列化导致远程命令执行 ",
    "WebSphere 反序列化导致远程命令执行",
    "Jenkins 反序列化命令执行",
    "WebLogic 反序列化导致远程命令执行",
    "Apache Tomcat 样例目录与 Session 操纵",
    "文件解析导致代码执行",
    "域传送（DNS Zone Transfer）漏洞",
    "Redis 未授权访问",
    "MongoDB 未授权访问",
    "操作系统弱口令",
    "数据库弱口令",
    "本地权限提升",
    "已存在的脚本/木马",
    "永恒之蓝（EternalBlue）利用",
    " MSSQL 信息探测",
    "Windows 操作系统漏洞",
    "数据库远程连接暴露",
    "权限分配不合理",
    " HTTP.sys 远程代码执行漏洞（相关补丁缺失）",
    "存储型跨站脚本",
    " SNMP 使用默认团体字符串",
    "任意用户密码修改/重置（未授权）",
    " SSRF服务器端请求伪造",
    " XML外部实体注入",
    "Apache Struts2 远程命令执行（S2-045）",
    "Drupal 版本过低导致多个漏洞",
    "PHP 版本过低导致多个漏洞",
    "Apache Tomcat 文件包含/读取（Ghostcat — CVE-2020-1938 / CNVD-2020-10487）",
    "Apache Tomcat 版本过低",
    "Apache Shiro RememberMe 反序列化命令执行",
    "Fastjson 远程代码执行漏洞",
    "OpenSSL 版本过低导致多个漏洞",
    "Host 头注入/Host 头攻击",
    "Flash 跨域（crossdomain.xml 配置过宽）",
    "IIS 短文件名（8.3）泄露",
    "Apache Tomcat 示例目录/示例应用泄露",
    "Apache Tomcat examples 目录可访问导致多个漏洞",
    "框架注入漏洞",
    "rsync 未授权访问",
    "FTP 匿名登录",
    "验证码绕过",
    " phpinfo 页面泄露",
    "源码泄漏",
    " SSL 3.0 POODLE 攻击（CVE-2014-3566）",
    "链接注入漏洞",
    "验证码失效",
    "管理后台泄漏",
    "备份文件泄漏",
    "版本信息泄漏",
    "内网 IP 泄露",
    " jQuery 版本过低",
    "密码暴力破解风险",
    " WEB-INF/web.xml 泄露",
    "Bazaar 存储库泄露 (.bzr) ",
    "Snoop Servlet 信息泄露",
    ".DS_Store 文件泄漏",
    "目录列表",
    " .idea 目录信息泄露",
    "用户凭据明文传输",
    "ASP.NET 调试模式已启用（Remote/Local Debugging / customErrors 未关闭）",
    "SSL/TLS 存在 Bar Mitzvah 攻击风险（RC4 弱点）",
]


def normalize_key_name(name):
    """标准化 key 名称，去掉 SpEL 不支持的特殊字符（减号、斜杠、括号、逗号、空格、点号、特殊破折号等）"""
    # 注意：点号(.)在 SpEL 中是属性访问运算符，必须替换
    # em dash (—, U+2014), en dash (–, U+2013), horizontal bar (―, U+2015) 等特殊字符也需要替换
    normalized = re.sub(r'[\-/\(\)（）,，\.\s—–―]+', '_', name.strip())
    normalized = re.sub(r'_+', '_', normalized)
    normalized = re.sub(r'^_|_$', '', normalized)
    return normalized


def process_template(template_path: Path):
    """处理单个模板文件"""
    print(f"处理模板: {template_path.name}")
    
    temp_dir = Path("temp_docx_convert")
    
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
        
        replacement_count = 0
        
        for item in CHECK_ITEMS:
            # 转义特殊字符用于正则表达式
            escaped_item = re.escape(item)
            
            # 匹配完整的三元表达式
            pattern = rf"vulnTypes\.contains\('{escaped_item}'\)\s*\?\s*'[^']*'\s*:\s*'[^']*'"
            
            matches = re.findall(pattern, content)
            if matches:
                # 标准化 key 名称
                normalized_name = normalize_key_name(item)
                check_key = f"check_{normalized_name}"
                content = re.sub(pattern, check_key, content)
                replacement_count += 1
                print(f"  替换: {item} -> {check_key}")
        
        # 处理被 XML 标签分割的占位符
        # Flash 跨域 - 注意替换目标中的点号也要变成下划线
        flash_pattern = r"vulnTypes\.contains\('Flash [^']*crossdomain\.xml[^']*'\)(\s*\?\s*'[^']*'\s*:\s*'[^']*')?"
        if re.search(flash_pattern, content):
            content = re.sub(flash_pattern, "check_Flash_跨域_crossdomain_xml_配置过宽", content)
            replacement_count += 1
            print("  替换: Flash 跨域（被分割）")
        
        # HTTP OPTIONS
        options_pattern = r"vulnTypes\.contains\('允许 HTTP OPTIONS [^']*'\)(\s*\?\s*'[^']*'\s*:\s*'[^']*')?"
        if re.search(options_pattern, content):
            content = re.sub(options_pattern, "check_允许_HTTP_OPTIONS_方法", content)
            replacement_count += 1
            print("  替换: HTTP OPTIONS（被分割）")
        
        print(f"共替换 {replacement_count} 个检测项")
        
        # 保存修改后的 document.xml
        with open(document_xml_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
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
    # 先恢复原始模板
    print("=== 恢复原始模板 ===")
    for template_name in TEMPLATES:
        template_path = TEMPLATE_DIR / template_name
        backup_path = template_path.with_suffix('.docx.backup')
        
        if backup_path.exists():
            print(f"恢复: {template_name}")
            shutil.copy2(backup_path, template_path)
    
    print("\n=== 转换模板 ===")
    for template_name in TEMPLATES:
        template_path = TEMPLATE_DIR / template_name
        
        if not template_path.exists():
            print(f"模板文件不存在: {template_path}")
            continue
        
        process_template(template_path)
    
    print("\n完成!")


if __name__ == "__main__":
    main()
