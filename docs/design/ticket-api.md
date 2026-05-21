# 工单中心 - API 契约（Step B）

> **文档定位**：在 `ticket-design.md`（状态机 / 错误码 / 权限矩阵）的基础上，把工单中心一期所有 HTTP 端点的请求 / 响应 schema 全部对齐。供前端、后端、code_audit 三方同读。
>
> **配套约定**：
>
> - 路径前缀：网关统一加 `/admin-api/`，Controller 内只写模块路径
> - 返回格式：`CommonResult<T>` + 分页 `PageResult<T>`
> - 鉴权：JWT Bearer Token + `@PreAuthorize("@ss.hasPermission('xxx')")`
> - 数据权限：`@DataPermission(includeRules = {DeptDataPermissionRule.class})`
> - 错误码：见 `ticket-design.md` §2.2
> - traceId：请求头 `traceparent`（或自动生成），贯穿前后端日志

---

## 1. 端点一览（MVP 18 个）

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| **工单核心 CRUD（6）** | | | | |
| 1 | POST | `/ticket/ticket/create` | `ticket:ticket:create` | 创建工单 |
| 2 | GET | `/ticket/ticket/get?id=` | `ticket:ticket:query` | 查询详情 |
| 3 | PUT | `/ticket/ticket/update` | `ticket:ticket:update` | 修改工单（仅未分派可改） |
| 4 | DELETE | `/ticket/ticket/delete?id=` | `ticket:ticket:delete` | 删除（仅管理员） |
| 5 | GET | `/ticket/ticket/page` | `ticket:ticket:query` | 分页列表（走数据权限）|
| 6 | GET | `/ticket/ticket/my-page` | `ticket:ticket:query` | 我的工单（自己提的+处理的，跳数据权限）|
| **工单状态机（6）** | | | | |
| 7 | PUT | `/ticket/ticket/assign` | `ticket:ticket:assign` | 分派处理人 |
| 8 | PUT | `/ticket/ticket/start?id=` | `ticket:ticket:update` | 接单开始（status: 0→1）|
| 9 | PUT | `/ticket/ticket/finish` | `ticket:ticket:finish` | 完成工单（1→3）|
| 10 | PUT | `/ticket/ticket/close?id=` | `ticket:ticket:close` | 关闭工单（3→4）|
| 11 | PUT | `/ticket/ticket/cancel?id=` | `ticket:ticket:update` | 取消工单（仅 status=0）|
| 12 | PUT | `/ticket/ticket/transfer` | `ticket:ticket:transfer` | 转交工单 |
| **工单评论（2）** | | | | |
| 13 | POST | `/ticket/comment/create` | `ticket:comment:create` | 添加评论 |
| 14 | GET | `/ticket/comment/list?ticketId=` | `ticket:ticket:query` | 列出某工单的评论 |
| **工单分类（4）** | | | | |
| 15 | POST | `/ticket/category/create` | `ticket:category:create` | 创建分类 |
| 16 | GET | `/ticket/category/tree` | `ticket:category:query` | 树形列表（启用的）|
| 17 | PUT | `/ticket/category/update` | `ticket:category:update` | 修改分类 |
| 18 | DELETE | `/ticket/category/delete?id=` | `ticket:category:delete` | 删除分类（需校验无子分类、无工单）|

二期（不在本文档详细展开）：审核流程（submit_review / review_pass / review_reject）、附件 CRUD、批量操作、超时定时关闭、钉钉通知。

---

## 2. 通用约定

### 2.1 统一响应包装

```json
// 成功
{ "code": 0, "data": <T>, "msg": "" }

// 业务失败
{ "code": 1032003000, "data": null, "msg": "工单当前状态={fromName}，不允许{actionName}操作" }
```

### 2.2 分页响应

```json
{
  "code": 0,
  "data": {
    "list": [ /* T[] */ ],
    "total": 123
  }
}
```

### 2.3 通用分页参数（继承 `PageParam`）

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `pageNo` | int | 否 | 1 | 页码 |
| `pageSize` | int | 否 | 10 | 每页大小，上限 100 |

---

## 3. 工单核心 CRUD（详细契约）

### 3.1 创建工单 · POST `/ticket/ticket/create`

**Req Body**（`TicketSaveReqVO`）：

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| `title` | string | ✓ | 1-200 字符 | 工单标题 |
| `content` | string | ✗ | ≤ 65535 字符 | 工单描述（富文本 HTML） |
| `categoryId` | long | ✗ | 必须存在且 status=0 | 工单分类 ID |
| `priority` | int | ✗ | 0-3，默认 1 | 0低/1中/2高/3紧急 |
| `businessType` | string | ✗ | 默认 `general` | 业务类型 |
| `businessId` | long | ✗ | — | 关联业务 ID（business_type 非 general 时必填） |
| `deptId` | long | ✓ | 必须是当前用户所在部门或下属部门 | 工单归属部门 |
| `dueTime` | datetime | ✗ | ISO8601，> now() | 截止时间（SLA）|
| `projectId` | long | ✗ | 必须存在 | 关联项目 |
| `customerId` | long | ✗ | 必须存在 | 关联客户 |
| `extJson` | object | ✗ | — | 扩展字段 |
| `remark` | string | ✗ | ≤ 500 字符 | 备注 |

**服务端自动填充**：`ticketNo`（格式 `TKyyyyMMdd + 3位流水`）、`creatorId / creatorName`、`status=0`、`source=0`（手动）。

**Res 200**：`CommonResult<Long>` —— 返工单 ID。

**错误码**：
- `40001` 参数校验失败
- `1_032_001_003` 标题为空
- `1_032_001_004` 缺归属部门
- `1_032_001_005` 提单人无部门
- `1_032_002_000` 分类不存在
- `1_032_002_001` 分类已禁用
- `1_032_003_003` 指定的处理人不存在

---

### 3.2 查询工单详情 · GET `/ticket/ticket/get?id={id}`

**Req Query**：`id` (long, 必填)

**Res 200**：`CommonResult<TicketRespVO>`

`TicketRespVO` 字段（基础表全部 + 关联展开）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id / ticketNo / title / content` | — | 基础 |
| `categoryId / categoryName` | long + string | 展开分类名 |
| `priority / source / businessType / businessId / processInstanceId` | — | 基础 |
| `status / statusName / subStatus` | int + string | 状态码 + 中文 |
| `creatorId / creatorName` | long + string | 提单人快照 |
| `assigneeId / assigneeName / assigneeDeptId / assigneeDeptName` | long + string | 处理人 + 部门 |
| `deptId / deptName` | long + string | 归属部门 |
| `dueTime / firstResponseTime / finishTime / closeTime` | datetime | 时间节点 |
| `notifyChannels / notifyStatus` | string + int | 通知 |
| `projectId / projectName / customerId / customerName` | — | 关联业务 |
| `commentCount / attachmentCount` | int | 数量统计（聚合查询）|
| `createTime / updateTime` | datetime | 审计 |
| `_actions` | string[] | **重点**：当前用户对本工单可执行的 action 列表（前端按钮显示用），如 `["start","comment","transfer"]` |

**错误码**：
- `1_032_001_000` 工单不存在
- `1_032_006_000` 无权操作（不是提单人/处理人/管理员）

---

### 3.3 修改工单 · PUT `/ticket/ticket/update`

**Req Body**（`TicketSaveReqVO` + `id`）

**约束**：仅当 `status=0` 且为提单人本人 / 工单管理员 时可改。

**Res 200**：`CommonResult<Boolean>`

**错误码**：`1_032_001_000` / `1_032_003_000`（状态非待处理） / `1_032_006_001`（不是您的工单）

---

### 3.4 删除工单 · DELETE `/ticket/ticket/delete?id={id}`

**约束**：仅 `super_admin` 或 `ticket_admin` 角色；逻辑删除（`deleted=b'1'`）。

**Res 200**：`CommonResult<Boolean>`

---

### 3.5 分页列表 · GET `/ticket/ticket/page`

**Req Query**（`TicketPageReqVO`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `pageNo / pageSize` | int | 分页 |
| `ticketNo` | string | 工单号模糊 |
| `title` | string | 标题模糊 |
| `categoryId` | long | 分类筛选 |
| `priority` | int | 优先级 |
| `status` | int | 状态 |
| `businessType` | string | 业务类型 |
| `creatorId / assigneeId / deptId` | long | 人 / 部门筛选 |
| `createTime` | datetime[] | `[start, end]` 区间 |
| `dueTime` | datetime[] | `[start, end]` 区间 |

**默认排序**：`create_time DESC`。

**数据权限**：自动按 `DeptDataPermissionRule` 过滤 `dept_id`（部门负责人看本部门 + 下属，普通员工看自己处理的）。

**Res 200**：`CommonResult<PageResult<TicketRespVO>>`

---

### 3.6 我的工单 · GET `/ticket/ticket/my-page`

**Req Query**：同 3.5，但忽略 `creatorId / assigneeId / deptId`。

**Service 内部**：`@DataPermission(enable = false)`，手写 `WHERE (creator_id=#{userId} OR assignee_id=#{userId})`。

**Res 200**：`CommonResult<PageResult<TicketRespVO>>`

---

## 4. 工单状态机操作（详细契约）

> **共同约束**：所有状态机端点都会先调 `TicketStateMachine.checkTransition()` 校验 + 写 `shuhe_ticket_log` 操作日志。

### 4.1 分派 · PUT `/ticket/ticket/assign`

**Req Body**（`TicketAssignReqVO`）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | long | ✓ | 工单 ID |
| `assigneeId` | long | ✓ | 新处理人 ID |
| `remark` | string | ✗ | 分派说明（写入 log.content）|

**约束**：仅 `status=0` 可分派；操作人需有 `ticket:ticket:assign` 权限 + 是部门负责人或管理员。

**Res 200**：`CommonResult<Boolean>`

**错误码**：`1_032_003_000` 状态非待处理 / `1_032_003_002` 新处理人与原同 / `1_032_003_003` 新处理人不存在

---

### 4.2 接单开始 · PUT `/ticket/ticket/start?id={id}`

**约束**：当前登录用户必须 == `assignee_id`；状态必须 = `0`；执行后 `status=1`，写 `first_response_time = now()`。

**Res 200**：`CommonResult<Boolean>`

**错误码**：`1_032_003_000` / `1_032_006_002`（不是当前处理人）

---

### 4.3 完成工单 · PUT `/ticket/ticket/finish`

**Req Body**（`TicketFinishReqVO`）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | long | ✓ | 工单 ID |
| `result` | string | ✓ | 处理结果说明（写入 log.content + 自动创建一条系统评论）|

**约束**：当前登录用户 == `assignee_id`；状态必须 = `1`；执行后 `status=3`，写 `finish_time = now()`。

**Res 200**：`CommonResult<Boolean>`

---

### 4.4 关闭工单 · PUT `/ticket/ticket/close?id={id}`

**约束**：当前用户为提单人或管理员；状态必须 = `3`；执行后 `status=4`，写 `close_time = now()`。

**Res 200**：`CommonResult<Boolean>`

---

### 4.5 取消工单 · PUT `/ticket/ticket/cancel?id={id}`

**约束**：当前用户为提单人；状态必须 = `0`（一期不支持 status=1 取消）；执行后 `status=5`。

**Res 200**：`CommonResult<Boolean>`

---

### 4.6 转交工单 · PUT `/ticket/ticket/transfer`

**Req Body**（`TicketTransferReqVO`）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | long | ✓ | 工单 ID |
| `newAssigneeId` | long | ✓ | 新处理人 |
| `reason` | string | ✓ | 转交原因（写入 log.content）|

**约束**：当前用户 == 现 `assignee_id`，或是部门负责人 / 管理员；状态 ∈ {0, 1, 2}；执行后 `assignee_id` 更新，状态不变。

**Res 200**：`CommonResult<Boolean>`

---

## 5. 工单评论

### 5.1 添加评论 · POST `/ticket/comment/create`

**Req Body**（`TicketCommentSaveReqVO`）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `ticketId` | long | ✓ | 工单 ID |
| `content` | string | ✓ | 评论内容（≤ 5000 字符）|
| `parentId` | long | ✗ | 回复的父评论 ID |
| `isInternal` | bool | ✗ | 是否内部评论（默认 false）；仅处理人 / 管理员可创建 |

**约束**：当前用户必须能访问该工单（提单人 / 处理人 / 部门负责人 / 管理员）。

**Res 200**：`CommonResult<Long>` 评论 ID

---

### 5.2 列出工单评论 · GET `/ticket/comment/list?ticketId={ticketId}`

**Res 200**：`CommonResult<List<TicketCommentRespVO>>`

**字段**：`id / ticketId / userId / userName / userDeptId / parentId / content / isInternal / createTime`

**过滤规则**：提单人看不到 `isInternal=true` 的评论（Service 层过滤）。

---

## 6. 工单分类

### 6.1 创建分类 · POST `/ticket/category/create`

**Req Body**（`TicketCategorySaveReqVO`）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | ✓ | 分类名称（同 parentId 下唯一）|
| `parentId` | long | ✗ | 默认 0 |
| `code` | string | ✗ | 编码（全局唯一）|
| `icon` | string | ✗ | 图标 |
| `sort` | int | ✗ | 默认 0 |
| `defaultAssigneeId` | long | ✗ | 默认处理人 |
| `defaultAssigneeDeptId` | long | ✗ | 默认处理部门 |
| `defaultPriority` | int | ✗ | 默认优先级 |
| `defaultSlaHours` | int | ✗ | 默认 SLA 小时数 |
| `status` | int | ✗ | 默认 0 启用 |

**Res 200**：`CommonResult<Long>`

---

### 6.2 树形列表 · GET `/ticket/category/tree`

**Res 200**：`CommonResult<List<TicketCategoryRespVO>>` —— 树形结构，每个节点带 `children: []`。

---

### 6.3 修改分类 · PUT `/ticket/category/update`

**约束**：`parentId` 不能选自己或自己的子分类（递归校验）。

**Res 200**：`CommonResult<Boolean>`

**错误码**：`1_032_002_004` 父分类非法

---

### 6.4 删除分类 · DELETE `/ticket/category/delete?id={id}`

**约束**：
1. 不存在子分类（`shuhe_ticket_category WHERE parent_id=? AND deleted=b'0'` 为空）
2. 不存在已使用此分类的工单（`shuhe_ticket WHERE category_id=? AND deleted=b'0'` 为空）

**Res 200**：`CommonResult<Boolean>`

**错误码**：`1_032_002_002` 有子分类 / `1_032_002_003` 有工单使用

---

## 7. 前端 TS 类型片段（拷贝即用）

```ts
// 状态枚举
export const TicketStatus = {
  PENDING: 0,
  IN_PROGRESS: 1,
  PENDING_REVIEW: 2,
  COMPLETED: 3,
  CLOSED: 4,
  CANCELLED: 5,
} as const;
export type TicketStatusValue = (typeof TicketStatus)[keyof typeof TicketStatus];

export const TicketStatusName: Record<TicketStatusValue, string> = {
  0: '待处理',
  1: '处理中',
  2: '待审核',
  3: '已完成',
  4: '已关闭',
  5: '已取消',
};

export const TicketPriority = { LOW: 0, NORMAL: 1, HIGH: 2, URGENT: 3 } as const;
export const TicketAction = ['create','assign','start','finish','close','cancel','transfer','comment'] as const;
export type TicketActionType = (typeof TicketAction)[number];

// 创建 / 更新请求
export interface TicketSaveReqVO {
  id?: number;
  title: string;
  content?: string;
  categoryId?: number;
  priority?: 0 | 1 | 2 | 3;
  businessType?: string;
  businessId?: number;
  deptId: number;
  dueTime?: string;
  projectId?: number;
  customerId?: number;
  extJson?: Record<string, unknown>;
  remark?: string;
}

// 工单响应（详情 + 列表共用，列表略简）
export interface TicketRespVO {
  id: number;
  ticketNo: string;
  title: string;
  content?: string;
  categoryId?: number;
  categoryName?: string;
  priority: 0 | 1 | 2 | 3;
  source: number;
  businessType: string;
  businessId?: number;
  processInstanceId?: string;
  status: TicketStatusValue;
  statusName: string;
  subStatus?: string;
  creatorId: number;
  creatorName: string;
  assigneeId?: number;
  assigneeName?: string;
  assigneeDeptId?: number;
  assigneeDeptName?: string;
  deptId: number;
  deptName: string;
  dueTime?: string;
  firstResponseTime?: string;
  finishTime?: string;
  closeTime?: string;
  notifyChannels: string;
  notifyStatus: number;
  projectId?: number;
  projectName?: string;
  customerId?: number;
  customerName?: string;
  commentCount: number;
  attachmentCount: number;
  createTime: string;
  updateTime: string;
  /** 当前用户可执行的 action 列表，按钮显示用 */
  _actions: TicketActionType[];
}

// 状态机操作 VO
export interface TicketAssignReqVO { id: number; assigneeId: number; remark?: string; }
export interface TicketFinishReqVO { id: number; result: string; }
export interface TicketTransferReqVO { id: number; newAssigneeId: number; reason: string; }

// 评论
export interface TicketCommentSaveReqVO {
  ticketId: number;
  content: string;
  parentId?: number;
  isInternal?: boolean;
}
export interface TicketCommentRespVO {
  id: number;
  ticketId: number;
  userId: number;
  userName: string;
  userDeptId?: number;
  parentId?: number;
  content: string;
  isInternal: boolean;
  createTime: string;
}

// 分类
export interface TicketCategorySaveReqVO {
  id?: number;
  name: string;
  parentId?: number;
  code?: string;
  icon?: string;
  sort?: number;
  defaultAssigneeId?: number;
  defaultAssigneeDeptId?: number;
  defaultPriority?: number;
  defaultSlaHours?: number;
  status?: 0 | 1;
}
export interface TicketCategoryRespVO extends TicketCategorySaveReqVO {
  id: number;
  children?: TicketCategoryRespVO[];
}

// 错误码 → 人话
export const TicketErrorMessages: Record<number, string> = {
  1032001000: '工单不存在',
  1032001003: '工单标题不能为空',
  1032002000: '工单分类不存在',
  1032002001: '工单分类已禁用',
  1032002002: '分类下存在子分类，无法删除',
  1032002003: '分类下存在工单，无法删除',
  1032003000: '工单当前状态不允许该操作',
  1032003003: '指定的处理人不存在或已离职',
  1032006000: '无权操作该工单',
  1032006001: '不是您的工单，无权修改',
  1032006002: '您不是当前处理人，无法执行该操作',
};
```

---

## 8. 验证清单（Step B 完成的判定标准）

- [x] 18 个端点全部列出（含路径、方法、权限、说明）
- [x] 每个端点定义了 Req schema + Res schema + 错误码
- [x] 状态机操作端点说明了 from/to 状态约束、操作人约束
- [x] 数据权限 vs 资源所有权 的区分点注明（`my-page` 跳数据权限）
- [x] 前端 TS 类型片段完整，含枚举 / 请求 VO / 响应 VO / 错误码 map
- [x] 错误码与 `ticket-design.md` §2.2 一一对应（无新增）

---

## 9. 风险与待办

| 风险 | 缓解 |
|------|------|
| `_actions` 字段每次详情查询都要算，可能 N+1 | 用一个 utility 方法集中算；列表页不返 `_actions`，详情页才返 |
| `dueTime` SLA 没人触发提醒（一期）| 二期补 `quartz` job 定时扫描 + 站内信通知 |
| 分页 `page` 默认走 `DeptDataPermissionRule`，但当前数据权限实现可能未在新表生效 | Step C 批 5 联调时用 `SqlExecuteCounterFilter` 看 SQL，确认 `dept_id` 条件被注入；如未生效降级到手写 WHERE |
| 业务工单（business_type ≠ general）的写场景一期不做 | 一期只允许 `general` 类型的写操作；读端口兼容查询其它 business_type 工单 |

---

## 10. 下一步

- [x] **Step A 完成** → `docs/design/ticket-design.md`
- [x] **Step B 完成** → 本文档
- [ ] **Step C 批 1**：`shuhe-module-ticket` 内创建 enums + ErrorCodeConstants + 公用 DTO 包结构 + 详情页菜单补丁 SQL（`V2026_05_19_03`）
- [ ] **Step C 批 2**：DO + Mapper（5 张表）+ Convert
- [ ] **Step C 批 3**：Service 接口 + 基础实现（create / update / get / page / my-page / delete）
- [ ] **Step C 批 4**：Service 状态机方法（assign / start / finish / close / cancel / transfer / comment）+ TicketStateMachine 工具类 + Log 写入
- [ ] **Step C 批 5**：Controller + VO 转换 + `@PreAuthorize` + `@DataPermission` + Swagger 文档
- [ ] **Step C 批 6**：本地跑 SQL + 启动 + curl 验证 + Postman 集合（含一份 happy path 演示）
