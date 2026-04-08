# 轮次分发到报告生成器 - 接口设计文档

## 1. 背景

当管理系统创建轮次（ProjectRound）后，需要将轮次数据自动分发到本地运行的「安全报告生成器」Electron 桌面应用，在报告生成器中创建对应的项目和测试目标，方便后续录入漏洞和生成报告。

## 2. 架构方案

```
┌─────────────────────┐                    ┌─────────────────────┐
│   管理系统前端        │  localhost:23480   │  安全报告生成器       │
│   (浏览器)           │ ─────────────────► │  (Electron App)      │
│                     │   POST /api/round  │                     │
│ 用户点击「分发」按钮  │   /dispatch        │  接收数据并创建项目   │
└────────┬────────────┘                    └─────────────────────┘
         │                                          │
         │ 获取轮次详情                               │ 写入本地数据库
         ▼                                          ▼
┌─────────────────────┐                    ┌─────────────────────┐
│   管理系统后端        │                    │  报告生成器 MySQL    │
│   (Spring Boot)     │                    │  (projects/targets/ │
│                     │                    │   vulnerabilities)  │
└─────────────────────┘                    └─────────────────────┘
```

**选择此方案的原因：**
- 管理系统前端和报告生成器都运行在员工本机，通过 `localhost` 通信零延迟且无需暴露端口
- 报告生成器添加轻量 HTTP 服务即可，无需复杂的 WebSocket 或消息队列
- 前端负责组装完整数据并推送，后端无需知道员工桌面 IP

## 3. 报告生成器端 - 接收接口

### 3.1 接收轮次分发

报告生成器需要在 Electron 主进程中启动一个本地 HTTP 服务。

**端口:** `23480`（可配置，避免与 network-share 的 23456/23457 冲突）

---

#### `POST /api/round/dispatch`

接收管理系统分发的轮次数据，在报告生成器中创建对应项目。

**Request Headers:**

| Header | Value | 说明 |
|--------|-------|------|
| Content-Type | application/json | |
| X-Dispatch-Source | shuhe-management-system | 来源标识 |

**Request Body:**

```json
{
  "round": {
    "id": 123,
    "name": "第1次渗透测试",
    "roundNo": 1,
    "status": 1,
    "deadline": "2026-05-01",
    "planEndTime": "2026-05-15",
    "executorNames": "欧德炜, 张三"
  },
  "project": {
    "id": 456,
    "name": "XX公司信息安全服务项目",
    "customerName": "XX科技有限公司",
    "customerAddress": "北京市朝阳区XX路XX号"
  },
  "serviceItem": {
    "id": 789,
    "name": "渗透测试",
    "deptType": 1,
    "planStartTime": "2026-04-01",
    "planEndTime": "2026-05-15"
  },
  "targets": [
    {
      "id": 1,
      "name": "官网系统",
      "url": "https://www.example.com",
      "type": "web",
      "remark": ""
    },
    {
      "id": 2,
      "name": "后台管理系统",
      "url": "https://admin.example.com",
      "type": "web",
      "remark": ""
    }
  ],
  "vulnerabilities": []
}
```

**字段映射（管理系统 → 报告生成器）：**

| 管理系统字段 | 报告生成器字段 | 说明 |
|-------------|--------------|------|
| `project.name` + `round.name` | `projects.name` | 拼接为项目名，如 "XX公司-第1次渗透测试" |
| `project.customerName` | `projects.description` | 客户名称作为描述 |
| `serviceItem.planStartTime` | `projects.start_date` | 计划开始时间 |
| `serviceItem.planEndTime` | `projects.end_date` | 计划结束时间 |
| `project.customerAddress` | `projects.address` | 客户地址 |
| `round.executorNames` | `projects.tester` | 执行人 |
| - | `projects.main_domain` | 从 targets 中提取主域名 |
| `targets[].name` | `targets.name` | 测试目标名称 |
| `targets[].url` | `targets.url` | 测试目标 URL |

**Response - 成功 (200):**

```json
{
  "success": true,
  "data": {
    "projectId": 42,
    "projectName": "XX公司-第1次渗透测试",
    "targetsCreated": 2,
    "message": "项目已创建并打开"
  }
}
```

**Response - 项目已存在 (409):**

```json
{
  "success": false,
  "code": "DUPLICATE_PROJECT",
  "message": "项目 'XX公司-第1次渗透测试' 已存在",
  "data": {
    "existingProjectId": 38,
    "existingProjectName": "XX公司-第1次渗透测试"
  }
}
```

**Response - 报告生成器未运行 (连接拒绝):**

前端捕获到 `fetch` 错误（`ERR_CONNECTION_REFUSED`）时，提示用户启动报告生成器。

---

#### `GET /api/status`

检查报告生成器是否在线。

**Response (200):**

```json
{
  "success": true,
  "data": {
    "version": "1.0.43",
    "dbType": "mysql",
    "dbConnected": true
  }
}
```

---

#### `POST /api/round/sync-back`（可选 - 反向同步漏洞）

将报告生成器中录入的漏洞数据同步回管理系统。

**Request Body:**

```json
{
  "roundId": 123,
  "vulnerabilities": [
    {
      "location": "https://www.example.com/login",
      "severity": "high",
      "type": "SQL注入",
      "process": "在登录页面输入...",
      "url": "https://www.example.com/login",
      "targetName": "官网系统",
      "retestStatus": "fixed"
    }
  ]
}
```

**Response (200):**

```json
{
  "success": true,
  "data": {
    "synced": 3,
    "skipped": 1,
    "message": "已同步 3 条漏洞，跳过 1 条重复"
  }
}
```

## 4. 管理系统前端 - 调用逻辑

### 4.1 分发入口

在轮次详情页 `RoundDetailPage.vue` 添加「分发到报告生成器」按钮。

**触发时机：**
- 用户手动点击按钮
- 可选：轮次创建后自动触发

**调用流程：**

```
1. 前端先调 GET http://localhost:23480/api/status 检查报告生成器是否在线
2. 前端从管理系统后端 API 获取完整的轮次 + 项目 + 服务项 + 目标数据
3. 前端组装 dispatch payload
4. 前端调 POST http://localhost:23480/api/round/dispatch 推送数据
5. 显示结果提示
```

### 4.2 前端 API 封装示例

```typescript
const REPORT_GENERATOR_BASE = 'http://localhost:23480';

export async function checkReportGeneratorStatus() {
  try {
    const res = await fetch(`${REPORT_GENERATOR_BASE}/api/status`, {
      signal: AbortSignal.timeout(2000),
    });
    return await res.json();
  } catch {
    return { success: false, message: '报告生成器未启动' };
  }
}

export async function dispatchRoundToReportGenerator(payload: RoundDispatchPayload) {
  const res = await fetch(`${REPORT_GENERATOR_BASE}/api/round/dispatch`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Dispatch-Source': 'shuhe-management-system',
    },
    body: JSON.stringify(payload),
  });
  return await res.json();
}
```

## 5. 管理系统后端 - 数据查询接口

报告生成器或前端组装 dispatch 数据时，需要以下后端接口（大部分已存在）：

| 接口 | 路径 | 状态 |
|------|------|------|
| 获取轮次详情 | `GET /project/round/get?id={id}` | ✅ 已有 |
| 获取轮次目标列表 | `GET /project/round-target/list-by-round?roundId={id}` | ✅ 已有 |
| 获取轮次漏洞列表 | `GET /project/round-vulnerability/list-by-round?roundId={id}` | ✅ 已有 |
| 获取项目详情 | `GET /project/info/get?id={id}` | ✅ 已有 |
| 获取服务项详情 | `GET /project/service-item/get?id={id}` | ✅ 已有 |
| **分发完整数据包** | `GET /project/round/dispatch-data?id={roundId}` | ❌ 待新建 |

### 5.1 新增：获取分发数据包（可选优化）

将轮次 + 项目 + 服务项 + 目标 + 漏洞一次性打包返回，减少前端多次请求。

**`GET /project/round/dispatch-data?id={roundId}`**

**Response:**

```json
{
  "code": 0,
  "data": {
    "round": { ... },
    "project": { ... },
    "serviceItem": { ... },
    "targets": [ ... ],
    "vulnerabilities": [ ... ]
  }
}
```

## 6. 数据模型对照

### 6.1 项目映射

| 管理系统 | 报告生成器 | 转换规则 |
|---------|-----------|---------|
| ProjectDO.name | projects.name | `{project.name}-{round.name}` |
| ProjectDO.customerName | projects.description | 直接映射 |
| ServiceItemDO.planStartTime | projects.start_date | 格式化为 YYYY-MM-DD |
| ServiceItemDO.planEndTime | projects.end_date | 格式化为 YYYY-MM-DD |
| ProjectDO.customerAddress | projects.address | 直接映射 |
| ProjectRoundDO.executorNames | projects.tester | 直接映射 |
| - | projects.completed | 默认 0 |
| 从 targets 提取 | projects.main_domain | 取第一个目标的域名 |
| 'pentest' | projects.type | 固定渗透测试 |

### 6.2 目标映射

| 管理系统 | 报告生成器 | 转换规则 |
|---------|-----------|---------|
| ProjectRoundTargetDO.name | targets.name | 直接映射 |
| ProjectRoundTargetDO.url | targets.url | 直接映射 |

### 6.3 漏洞映射（反向同步时使用）

| 报告生成器 | 管理系统 | 转换规则 |
|-----------|---------|---------|
| vulnerabilities.location | ProjectRoundVulnerabilityDO.location | 直接映射 |
| vulnerabilities.severity | ProjectRoundVulnerabilityDO.severity | 直接映射 (high/medium/low) |
| vulnerabilities.type | ProjectRoundVulnerabilityDO.type | 直接映射 |
| vulnerabilities.process | ProjectRoundVulnerabilityDO.process | 直接映射 |
| vulnerabilities.url | ProjectRoundVulnerabilityDO.url | 直接映射 |
| vulnerabilities.target_id | 通过 target name 匹配 | 按目标名称关联 |
| vulnerabilities.retest | retestStatus + retestReport + retestDate | JSON 拆分 |

## 7. 安全考虑

1. **仅监听 localhost** - 报告生成器的 HTTP 服务只绑定 `127.0.0.1`，不暴露到网络
2. **来源校验** - 检查 `X-Dispatch-Source` Header
3. **CORS** - 允许 `http://localhost:*` 和管理系统域名的跨域请求
4. **数据校验** - 报告生成器端验证 payload 完整性

## 8. 实现清单

### 报告生成器端（Electron）

- [ ] 在 `main.js` 中添加 HTTP 服务模块（`http.createServer`，端口 23480）
- [ ] 实现 `POST /api/round/dispatch` 路由
- [ ] 实现 `GET /api/status` 路由
- [ ] 接收数据后调用 `MySQLDatabaseManager.createProject()` 创建项目
- [ ] 创建项目后自动切换到该项目页面
- [ ] 处理重复项目名检测

### 管理系统前端

- [ ] 在 `RoundDetailPage.vue` 添加「分发到报告生成器」按钮
- [ ] 封装 `dispatchRoundToReportGenerator()` 前端 API
- [ ] 添加报告生成器在线检测逻辑
- [ ] 组装 dispatch payload（聚合多个 API 返回值）

### 管理系统后端（可选优化）

- [ ] 新增 `GET /project/round/dispatch-data` 聚合接口
- [ ] 返回轮次 + 项目 + 服务项 + 目标 + 漏洞完整数据包
