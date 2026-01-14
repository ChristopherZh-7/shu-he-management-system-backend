package cn.shuhe.system.module.ticket.enums;

import cn.shuhe.system.framework.common.exception.ErrorCode;

/**
 * Ticket 错误码枚举类
 *
 * ticket 系统，使用 1-020-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 工单模块 1-020-001-000 ==========
    ErrorCode TICKET_NOT_EXISTS = new ErrorCode(1_020_001_000, "工单不存在");
    ErrorCode TICKET_STATUS_ERROR = new ErrorCode(1_020_001_001, "工单状态不正确");
    ErrorCode TICKET_ALREADY_ASSIGNED = new ErrorCode(1_020_001_002, "工单已分配");
    ErrorCode TICKET_NOT_ASSIGNED = new ErrorCode(1_020_001_003, "工单未分配");

    // ========== 工单分类模块 1-020-002-000 ==========
    ErrorCode TICKET_CATEGORY_NOT_EXISTS = new ErrorCode(1_020_002_000, "工单分类不存在");
    ErrorCode TICKET_CATEGORY_HAS_CHILDREN = new ErrorCode(1_020_002_001, "存在子分类，无法删除");

}
