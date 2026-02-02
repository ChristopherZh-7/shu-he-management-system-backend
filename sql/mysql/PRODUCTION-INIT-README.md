# 生产环境数据库初始化指南

## 概述

本文档说明如何将开发环境数据库迁移到生产环境，清空测试数据并保留系统必要的基础数据。

## 初始化方式

### 方式一：全新初始化（推荐）

适用于：全新的生产环境部署

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE \`shuhe-ms\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 导入表结构和基础数据
mysql -u root -p shuhe-ms < sql/mysql/shuhe-ms.sql

# 3. 清理测试数据
mysql -u root -p shuhe-ms < sql/mysql/shuhe-ms-production-init.sql
```

### 方式二：从开发环境迁移

适用于：从现有开发环境迁移到生产环境

```bash
# 1. 备份开发环境数据库（重要！）
mysqldump -u root -p shuhe-ms > shuhe-ms-dev-backup-$(date +%Y%m%d).sql

# 2. 在生产服务器创建数据库
mysql -u root -p -e "CREATE DATABASE \`shuhe-ms\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. 导入开发环境数据
mysql -u root -p shuhe-ms < shuhe-ms-dev-backup-xxx.sql

# 4. 执行生产初始化脚本清理测试数据
mysql -u root -p shuhe-ms < sql/mysql/shuhe-ms-production-init.sql
```

## 数据分类说明

### 必须保留的基础数据

| 表名 | 说明 | 备注 |
|-----|------|-----|
| `system_menu` | 菜单数据 | 系统功能菜单 |
| `system_dict_type` | 字典类型 | 系统字典定义 |
| `system_dict_data` | 字典数据 | 系统字典值 |
| `system_role` | 角色数据 | 至少保留超级管理员 |
| `system_role_menu` | 角色菜单 | 权限关联 |
| `system_users` | 用户数据 | 至少保留管理员 |
| `system_dept` | 部门数据 | 组织架构 |
| `system_post` | 岗位数据 | 岗位定义 |
| `system_tenant` | 租户数据 | 多租户配置 |
| `system_tenant_package` | 租户套餐 | 套餐定义 |
| `infra_config` | 系统配置 | 系统参数 |
| `infra_job` | 定时任务 | 任务配置 |

### 需要清空的数据

| 表名 | 说明 |
|-----|------|
| `infra_api_access_log` | API 访问日志 |
| `infra_api_error_log` | API 错误日志 |
| `infra_job_log` | 定时任务日志 |
| `infra_codegen_*` | 代码生成配置 |
| `infra_file` / `infra_file_content` | 测试上传的文件 |
| `system_login_log` | 登录日志 |
| `system_operate_log` | 操作日志 |
| `system_mail_log` | 邮件日志 |
| `system_sms_log` / `system_sms_code` | 短信日志和验证码 |
| `system_notify_message` | 站内信消息 |
| `system_oauth2_*` | OAuth2 Token |
| `system_social_*` | 社交登录数据 |
| `system_notice` | 测试公告 |

### 可选删除的表

| 表名 | 说明 |
|-----|------|
| `shuhe_demo*` | 演示模块表（生产环境不需要） |

## 初始化后检查清单

- [ ] 修改管理员密码（默认 admin123）
- [ ] 配置正确的文件存储（MinIO/OSS）
- [ ] 配置短信渠道（阿里云/腾讯云）
- [ ] 配置邮件账户
- [ ] 修改 OAuth2 客户端密钥
- [ ] 检查定时任务配置
- [ ] 配置正确的租户信息
- [ ] 删除或修改测试用的第三方密钥

## 安全注意事项

1. **密码安全**：生产环境必须修改默认密码
2. **密钥安全**：清理所有测试用的 API 密钥
3. **文件配置**：修改文件存储的 AccessKey 和 SecretKey
4. **数据库备份**：执行任何操作前请先备份

## 常见问题

### Q: 如何只重置某个模块的数据？

可以单独执行清空语句，例如只清空日志：

```sql
TRUNCATE TABLE infra_api_access_log;
TRUNCATE TABLE infra_api_error_log;
TRUNCATE TABLE system_login_log;
TRUNCATE TABLE system_operate_log;
```

### Q: 如何保留某些测试用户？

修改 `shuhe-ms-production-init.sql` 中的用户删除语句：

```sql
-- 保留指定用户（如 id=1,2,3）
DELETE FROM system_users WHERE id NOT IN (1, 2, 3) AND tenant_id = 1;
```

### Q: 如何处理业务模块的数据？

项目可能包含其他业务模块的表（如 BPM、CRM、ERP 等），需要根据实际情况添加对应的清理语句：

```sql
-- 清空 BPM 工作流相关数据
TRUNCATE TABLE bpm_oa_leave;
TRUNCATE TABLE bpm_process_instance_ext;
-- ... 其他 BPM 表

-- 清空 CRM 相关数据（如需要）
-- TRUNCATE TABLE crm_customer;
-- ...
```
