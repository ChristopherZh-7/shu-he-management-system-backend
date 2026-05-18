/**
 * ticket 模块下，统一工单中心。
 *
 * 作为跨业务的任务入口和聚合层，承载：
 * <ul>
 *   <li>通用工单（business_type=general）的完整生命周期：提单 → 分派 → 处理 → 完成 → 关闭</li>
 *   <li>外协请求等现有业务（business_type=outside_request 等）的统一可见和通知</li>
 *   <li>评论 / 附件 / 操作日志 / 通知（站内信 + 钉钉）</li>
 * </ul>
 *
 * 设计原则：
 * <ul>
 *   <li>不动现有业务表（outside_request / service_launch 等），通过事件适配层联动</li>
 *   <li>dept_id + assignee_id 双字段对齐 {@link cn.shuhe.system.framework.datapermission.core.rule.dept.DeptDataPermissionRule} 数据权限</li>
 *   <li>权限走标准三层：菜单 / 按钮（permission 字符串） / 数据权限（自动 SQL 重写）</li>
 *   <li>预留 process_instance_id，复杂工单可挂 BPM 流程</li>
 * </ul>
 */
package cn.shuhe.system.module.ticket;
