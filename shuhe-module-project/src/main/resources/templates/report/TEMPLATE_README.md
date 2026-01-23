# 报告模板说明

本目录存放 Word 报告模板文件（.docx 格式），使用 **poi-tl** 模板引擎进行渲染。

---

## 语法对照表（docx-templates → poi-tl）

如果你有 docx-templates 格式的模板，按以下规则转换：

| docx-templates | poi-tl | 说明 |
|----------------|--------|------|
| `+++INS variable+++` | `{{variable}}` | 文本替换 |
| `+++FOR v IN list+++` | `{{#list}}` | 开始循环 |
| `+++END-FOR v+++` | `{{/list}}` | 结束循环 |
| `+++IMAGE getImageData(img)+++` | `{{@img}}` | 图片插入 |
| `+++IF condition+++` | `{{?condition}}` | 条件开始 |
| `+++END-IF+++` | `{{/condition}}` | 条件结束 |
| `$v.field`（循环内） | `{{field}}`（循环内） | 循环内字段 |

---

## poi-tl 模板语法详解

### 1. 文本替换
```
{{变量名}}
```

### 2. 图片插入
```
{{@imageName}}
```

### 3. 表格行循环（重要！）
poi-tl 表格行循环非常强大，在表格的某一行放置循环标记，会自动复制该行：

**模板写法：**
| 序号 | 姓名 | 部门 |
|------|------|------|
| {{members.seq}} | {{members.userName}} | {{members.userDeptName}} |

或使用区块语法：
| 序号 | 姓名 | 部门 |
|------|------|------|
| {{#members}} |  |  |
| {{seq}} | {{userName}} | {{userDeptName}} |
| {{/members}} |  |  |

### 4. 列表循环（非表格）
```
{{#list}}
内容：{{field1}}，{{field2}}
{{/list}}
```

### 5. 条件判断
```
{{?hasData}}
    有数据时显示的内容
{{/hasData}}
```

### 6. 区块对（Section）
用于复杂的重复结构：
```
{{#vulnerabilities}}
漏洞名称：{{vul_type}}
漏洞位置：{{location}}
危害等级：{{risk_level}}
{{/vulnerabilities}}
```

### 7. 嵌套表格
poi-tl 支持在循环内嵌套表格，非常适合复杂报告。

---

## 图片处理

poi-tl 图片需要使用 `PictureRenderData` 对象：

```java
// 从 Base64 创建图片
PictureRenderData picture = Pictures.ofBase64(base64Data, PictureType.PNG)
    .size(400, 300)  // 宽度400px，高度300px
    .create();
data.put("screenshot", picture);

// 从文件创建图片
PictureRenderData picture = Pictures.ofLocal("path/to/image.png")
    .size(400, 300)
    .create();

// 从 URL 创建图片
PictureRenderData picture = Pictures.ofUrl("http://example.com/image.png")
    .size(400, 300)
    .create();
```

模板中使用：`{{@screenshot}}`

---

## 表格高级用法

### 动态表格（行循环）
使用 `LoopRowTableRenderPolicy`：

```java
LoopRowTableRenderPolicy policy = new LoopRowTableRenderPolicy();
Configure config = Configure.builder()
    .bind("members", policy)
    .build();
```

模板表格：
| 序号 | 姓名 | 部门 |
|------|------|------|
| {{members.seq}} | {{members.userName}} | {{members.userDeptName}} |

### 动态表格（列循环）
使用 `LoopColumnTableRenderPolicy` 可以动态生成列。

### 合并单元格
poi-tl 支持自动处理合并单元格的复制。

---

## 外协请求模板可用变量

### 基本信息
| 变量名 | 说明 | 示例 |
|-------|------|------|
| `{{projectName}}` | 项目名称 | XX银行渗透测试项目 |
| `{{serviceItemName}}` | 服务项名称 | 渗透测试 |
| `{{serviceType}}` | 服务类型编码 | penetration_test |
| `{{requestUserName}}` | 发起人姓名 | 张三 |
| `{{requestDeptName}}` | 发起人部门 | 安全服务部 |
| `{{targetDeptName}}` | 目标部门 | 安全运营部 |
| `{{destination}}` | 外出地点 | XX银行总部 |
| `{{reason}}` | 外出事由 | 协助进行渗透测试 |
| `{{remark}}` | 备注 | 无 |

### 时间信息
| 变量名 | 说明 | 示例 |
|-------|------|------|
| `{{planStartTime}}` | 计划开始时间 | 2026年01月22日 09:00 |
| `{{planEndTime}}` | 计划结束时间 | 2026年01月25日 18:00 |
| `{{actualStartTime}}` | 实际开始时间 | 2026年01月22日 09:30 |
| `{{actualEndTime}}` | 实际结束时间 | 2026年01月25日 17:00 |
| `{{planDateRange}}` | 计划日期范围 | 2026年01月22日 至 2026年01月25日 |
| `{{actualDateRange}}` | 实际日期范围 | 2026年01月22日 至 2026年01月25日 |

### 状态与生成信息
| 变量名 | 说明 | 示例 |
|-------|------|------|
| `{{status}}` | 状态文本 | 已通过 |
| `{{generateDate}}` | 报告生成日期 | 2026年01月22日 |
| `{{generateDateTime}}` | 报告生成时间 | 2026年01月22日 11:20:30 |

### 人员信息
| 变量名 | 说明 | 示例 |
|-------|------|------|
| `{{memberNames}}` | 外出人员（顿号分隔） | 李四、王五、赵六 |
| `{{memberCount}}` | 外出人员数量 | 3 |

### 人员表格（使用 LoopRowTableRenderPolicy）

**模板表格写法：**
| 序号 | 姓名 | 部门 |
|------|------|------|
| {{members.seq}} | {{members.userName}} | {{members.userDeptName}} |

每行数据字段：
- `seq` - 序号
- `userName` - 人员姓名
- `userDeptName` - 人员部门

---

## 渗透测试报告可用变量（参考 Shuhe-ReportGenerator）

如果需要扩展支持渗透测试报告，可参考以下变量：

### 项目信息
| poi-tl | 说明 |
|--------|------|
| `{{title}}` | 项目名称 |
| `{{start_time}}` | 项目开始时间 |
| `{{end_time}}` | 项目结束时间 |
| `{{date}}` | 报告日期 |
| `{{address}}` | 项目地址 |
| `{{main_domain}}` | 主域名 |
| `{{testers}}` | 测试人员 |

### 漏洞统计
| poi-tl | 说明 |
|--------|------|
| `{{vul_total_number}}` | 漏洞总数 |
| `{{high_risk_num}}` | 高危数量 |
| `{{medium_risk_num}}` | 中危数量 |
| `{{low_risk_num}}` | 低危数量 |
| `{{vul_list_vertical}}` | 漏洞列表（换行） |
| `{{vul_list_horizontal}}` | 漏洞列表（顿号） |

### 系统状态
| poi-tl | 说明 |
|--------|------|
| `{{system_status}}` | 状态码：good/warning/serious/critical |
| `{{system_status_text}}` | 状态文本：良好/预警/严重/紧急 |
| `{{system_status_color}}` | 状态颜色（十六进制） |

### 目标系统表格
| 序号 | 名称 | URL |
|------|------|-----|
| {{targets.seq}} | {{targets.name}} | {{targets.url}} |

### 漏洞详情表格
| 序号 | 等级 | 漏洞信息 |
|------|------|---------|
| {{vulnerability_table.seq}} | {{vulnerability_table.risk_level}} | {{vulnerability_table.hostname_and_type}} |

### 漏洞循环（区块）
```
{{#vulnerabilities}}
漏洞序号：{{seq}}
漏洞位置：{{location}}
漏洞类型：{{vul_type}}
危害等级：{{risk_level}}
漏洞描述：{{vul_description}}
修复建议：{{vul_advice}}
漏洞URL：{{url}}
主域名：{{main_domain}}
截图：{{@process_image}}
{{/vulnerabilities}}
```

### 复测相关
| poi-tl | 说明 |
|--------|------|
| `{{retest_status}}` | fixed/unfixed/partial |
| `{{retest_status_text}}` | 已修复/未修复/部分修复 |
| `{{retest_date}}` | 复测日期 |
| `{{retest_report}}` | 复测报告内容 |

---

## 模板文件命名

模板文件名格式：`{templateCode}.docx`

例如：
- `outside-request.docx` - 默认外协请求报告模板
- `pentest-report.docx` - 渗透测试报告模板
- `retest-report.docx` - 复测报告模板

---

## 转换示例：docx-templates → poi-tl

### 示例1：文本替换
**docx-templates:**
```
项目名称：+++INS title+++
开始时间：+++INS start_time+++
```

**poi-tl:**
```
项目名称：{{title}}
开始时间：{{start_time}}
```

### 示例2：表格循环
**docx-templates（表格内）:**
```
序号｜名称｜URL
+++FOR t IN targets+++
+++INS $t.seq+++｜+++INS $t.name+++｜+++INS $t.url+++
+++END-FOR t+++
```

**poi-tl（表格内）:**
```
序号｜名称｜URL
{{targets.seq}}｜{{targets.name}}｜{{targets.url}}
```

### 示例3：漏洞详情循环
**docx-templates:**
```
+++FOR v IN vulnerabilities+++
漏洞序号：+++INS $v.seq+++
漏洞位置：+++INS $v.location+++
漏洞类型：+++INS $v.vul_type+++
危害等级：+++INS $v.risk_level+++
+++IMAGE getImageData($v.process_image)+++
+++END-FOR v+++
```

**poi-tl:**
```
{{#vulnerabilities}}
漏洞序号：{{seq}}
漏洞位置：{{location}}
漏洞类型：{{vul_type}}
危害等级：{{risk_level}}
{{@process_image}}
{{/vulnerabilities}}
```

### 示例4：条件判断
**docx-templates:**
```
+++IF system_status === 'critical'+++
⚠️ 系统存在严重安全风险！
+++END-IF+++
```

**poi-tl:**
```
{{?isCritical}}
⚠️ 系统存在严重安全风险！
{{/isCritical}}
```

### 示例5：三元表达式
**docx-templates:**
```
+++INS hasVulnType('SQL注入') ? '不通过' : '通过'+++
```

**poi-tl（需要在代码中预处理）:**
```
{{sqlInjectionResult}}
```
Java 代码：
```java
data.put("sqlInjectionResult", hasVulnType("SQL注入") ? "不通过" : "通过");
```

---

## 调用接口

### 外协请求报告
```
GET /project/outside-request/export-report?id=1&templateCode=outside-request
```

---

## 注意事项

1. **文件格式**：必须是 `.docx` 格式（Office Open XML），不支持 `.doc`
2. **变量名**：区分大小写
3. **表格循环**：使用 `LoopRowTableRenderPolicy`，标签格式为 `{{tableName.field}}`
4. **图片**：需要在代码中构建 `PictureRenderData` 对象
5. **条件判断**：poi-tl 的条件判断基于变量是否存在/为真，复杂逻辑需要在代码中预处理
6. **空值处理**：poi-tl 会自动处理 null 值，显示为空字符串

---

## 从 Shuhe-ReportGenerator 迁移模板

1. 打开原模板 `.docx` 文件
2. 使用"查找替换"功能：
   - `+++INS ` → `{{`
   - `+++` → `}}`（注意顺序）
   - `+++FOR ` → `{{#`
   - `+++END-FOR ` → `{{/`
   - `+++IMAGE ` → `{{@`
   - `$v.` → ``（删除循环变量前缀）
   - `$t.` → ``（删除循环变量前缀）
3. 手动调整复杂语法（如三元表达式、函数调用）
4. 测试模板渲染
