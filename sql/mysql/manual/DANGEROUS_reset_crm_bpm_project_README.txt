================================================================================
危险操作：清空「商机 / 合同 / 回款流程 / 项目」相关运行业务数据，便于重新走流程
================================================================================

使用前必读
----------
1. 必须在「已备份 shuhe-ms 整库」或至少备份下列表之后再执行。
2. 下列操作会删除大量业务数据；「客户 master、产品、合同配置」默认保留（见 reset_transactional_crm_project.sql 注释）。
3. 建议先在测试库验证 SQL。
4. 执行顺序：先走 BPM 清理接口，再执行 SQL（见下）。

步骤一：用接口清 Flowable 流程实例（按流程 key，对应 CRM 里用到的定义）
----------
在管理后台用有权限的账号，对每个模型各调用一次（或用 curl），模型 id 需从库中查询：

  SELECT id_, key_, name_ FROM ACT_RE_MODEL
  WHERE key_ IN (
    'crm-business-audit',
    'crm-business-early-investment',
    'crm-contract-audit',
    'crm-receivable-audit'
  );

接口（与 deploy 类似，需 Bearer Token 与 bpm:model:clean 权限）：

  DELETE /admin-api/bpm/model/clean?id=<上查到的 id>

说明：会删除该 key 下所有运行中 + 历史流程实例，以及 bpm_process_instance_copy 等扩展表中的关联数据。

步骤二：执行 SQL 清空 CRM 业务表 + 项目表
----------
见同目录 reset_transactional_crm_project.sql。执行前再次确认已备份。

步骤三：重启应用（可选）
----------
若前端仍有缓存或旧任务列表，可刷新或重启后端。

================================================================================
