package cn.shuhe.system.module.ticket.enums;

import cn.shuhe.system.framework.common.exception.ErrorCode;

/**
 * Ticket 错误码枚举类
 *
 * ticket 系统，使用 1-032-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 工单主表 1_032_001_xxx ==========
    ErrorCode TICKET_NOT_EXISTS           = new ErrorCode(1_032_001_000, "工单不存在");
    ErrorCode TICKET_NO_DUPLICATE         = new ErrorCode(1_032_001_001, "工单编号重复");
    ErrorCode TICKET_NO_GENERATE_FAIL     = new ErrorCode(1_032_001_002, "工单编号生成失败");
    ErrorCode TICKET_TITLE_EMPTY          = new ErrorCode(1_032_001_003, "工单标题不能为空");
    ErrorCode TICKET_DEPT_REQUIRED        = new ErrorCode(1_032_001_004, "工单必须指定归属部门");
    ErrorCode TICKET_CREATOR_DEPT_MISSING = new ErrorCode(1_032_001_005, "提单人未配置所属部门，无法创建工单");
    ErrorCode TICKET_BUSINESS_TYPE_INVALID = new ErrorCode(1_032_001_006, "业务工单不支持该操作");
    ErrorCode TICKET_SERVICE_ITEM_REQUIRED = new ErrorCode(1_032_001_007, "请选择合同或已审批提前投入项目下的服务项");
    ErrorCode TICKET_SERVICE_ITEM_NOT_AVAILABLE = new ErrorCode(1_032_001_008, "该服务项不可申请，可能尚未启动、已完成或执行次数已用完");

    // ========== 工单分类 1_032_002_xxx ==========
    ErrorCode TICKET_CATEGORY_NOT_EXISTS     = new ErrorCode(1_032_002_000, "工单分类不存在");
    ErrorCode TICKET_CATEGORY_DISABLED       = new ErrorCode(1_032_002_001, "工单分类已禁用");
    ErrorCode TICKET_CATEGORY_HAS_CHILDREN   = new ErrorCode(1_032_002_002, "分类下存在子分类，无法删除");
    ErrorCode TICKET_CATEGORY_HAS_TICKETS    = new ErrorCode(1_032_002_003, "分类下存在工单，无法删除");
    ErrorCode TICKET_CATEGORY_PARENT_INVALID = new ErrorCode(1_032_002_004, "父分类不能选择自己或自己的子分类");
    ErrorCode TICKET_CATEGORY_NAME_DUPLICATE = new ErrorCode(1_032_002_005, "同级分类下名称已存在");

    // ========== 状态机 1_032_003_xxx ==========
    ErrorCode TICKET_STATUS_INVALID      = new ErrorCode(1_032_003_000, "工单当前状态={}，不允许{}操作");
    ErrorCode TICKET_ASSIGNEE_REQUIRED   = new ErrorCode(1_032_003_001, "请先分派处理人");
    ErrorCode TICKET_ASSIGNEE_SAME       = new ErrorCode(1_032_003_002, "目标处理人与当前处理人相同");
    ErrorCode TICKET_ASSIGNEE_NOT_EXISTS = new ErrorCode(1_032_003_003, "目标处理人不存在或已离职");
    ErrorCode TICKET_REOPEN_EXPIRED      = new ErrorCode(1_032_003_004, "工单完成/关闭已超过 {} 天，不允许重开，请新建工单并关联原工单号");
    ErrorCode TICKET_REOPEN_LIMIT        = new ErrorCode(1_032_003_005, "工单重开次数已达上限（{} 次），请新建工单");

    // ========== 评论 1_032_004_xxx ==========
    ErrorCode TICKET_COMMENT_NOT_EXISTS    = new ErrorCode(1_032_004_000, "评论不存在");
    ErrorCode TICKET_COMMENT_EMPTY         = new ErrorCode(1_032_004_001, "评论内容不能为空");
    ErrorCode TICKET_COMMENT_INTERNAL_DENY = new ErrorCode(1_032_004_002, "无权查看内部评论");

    // ========== 附件 1_032_005_xxx ==========
    ErrorCode TICKET_ATTACHMENT_NOT_EXISTS  = new ErrorCode(1_032_005_000, "附件不存在");
    ErrorCode TICKET_ATTACHMENT_SIZE_LIMIT  = new ErrorCode(1_032_005_001, "附件大小超出限制（默认 20MB）");
    ErrorCode TICKET_ATTACHMENT_TYPE_DENIED = new ErrorCode(1_032_005_002, "附件类型不允许（仅支持文档/图片）");

    // ========== 权限 1_032_006_xxx ==========
    ErrorCode TICKET_NO_PERMISSION = new ErrorCode(1_032_006_000, "无权操作该工单");
    ErrorCode TICKET_NOT_OWN       = new ErrorCode(1_032_006_001, "不是您的工单，无权修改");
    ErrorCode TICKET_NOT_ASSIGNEE  = new ErrorCode(1_032_006_002, "您不是当前处理人，无法执行该操作");
    ErrorCode TICKET_NOT_DEPT_LEADER = new ErrorCode(1_032_006_003, "您不是工单归属部门的负责人，无法接单");

    // ========== 接单 / 执行人 1_032_007_xxx ==========
    ErrorCode TICKET_EXECUTOR_EMPTY      = new ErrorCode(1_032_007_001, "执行人列表不能为空");
    ErrorCode TICKET_EXECUTOR_NOT_EXISTS = new ErrorCode(1_032_007_002, "执行人不存在或已离职：{}");
    ErrorCode TICKET_DRIVER_FAILED       = new ErrorCode(1_032_007_003, "业务驱动器处理失败：{}");
    ErrorCode TICKET_EXECUTOR_OUT_OF_SCOPE = new ErrorCode(1_032_007_004, "执行人 {} 不在您管辖的部门范围内，只能指派自己或下属部门成员");
    ErrorCode TICKET_PRIMARY_EXECUTOR_REQUIRED = new ErrorCode(1_032_007_005, "请选择主执行人");
    ErrorCode TICKET_TECH_REVIEWER_REQUIRED = new ErrorCode(1_032_007_006, "请选择技术审核人");
    ErrorCode TICKET_ROLE_CONFLICT = new ErrorCode(1_032_007_007, "主执行人不能同时担任技术审核人");
}
