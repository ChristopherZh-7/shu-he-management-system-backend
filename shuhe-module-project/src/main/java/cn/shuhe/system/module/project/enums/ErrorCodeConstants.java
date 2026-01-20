package cn.shuhe.system.module.project.enums;

import cn.shuhe.system.framework.common.exception.ErrorCode;

/**
 * Project 错误码枚举类
 *
 * project 系统，使用 1-030-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 项目模块 1-030-001-000 ==========
    ErrorCode PROJECT_NOT_EXISTS = new ErrorCode(1_030_001_000, "项目不存在");
    ErrorCode PROJECT_NAME_DUPLICATE = new ErrorCode(1_030_001_001, "项目名称已存在");
    ErrorCode PROJECT_STATUS_ERROR = new ErrorCode(1_030_001_002, "项目状态错误");
    ErrorCode PROJECT_HAS_SERVICE_ITEMS = new ErrorCode(1_030_001_003, "项目下存在服务项，无法删除");

    // ========== 服务项模块 1-030-007-000 ==========
    ErrorCode SERVICE_ITEM_NOT_EXISTS = new ErrorCode(1_030_007_000, "服务项不存在");
    ErrorCode SERVICE_ITEM_NAME_DUPLICATE = new ErrorCode(1_030_007_001, "服务项名称已存在");
    ErrorCode SERVICE_ITEM_STATUS_ERROR = new ErrorCode(1_030_007_002, "服务项状态错误");
    ErrorCode SERVICE_ITEM_DEPT_NOT_SET = new ErrorCode(1_030_007_003, "创建服务项失败：当前用户未设置所属部门");
    ErrorCode SERVICE_ITEM_EXECUTION_LIMIT_EXCEEDED = new ErrorCode(1_030_007_004, "服务项执行次数已达上限，无法继续发起");
    ErrorCode SERVICE_ITEM_CANNOT_START_EXECUTION = new ErrorCode(1_030_007_005, "服务项无法发起执行：已达次数上限或状态异常");

    // ========== 项目成员 1-030-002-000 ==========
    ErrorCode PROJECT_MEMBER_NOT_EXISTS = new ErrorCode(1_030_002_000, "项目成员不存在");
    ErrorCode PROJECT_MEMBER_DUPLICATE = new ErrorCode(1_030_002_001, "项目成员已存在");

    // ========== 项目任务 1-030-003-000 ==========
    ErrorCode PROJECT_TASK_NOT_EXISTS = new ErrorCode(1_030_003_000, "项目任务不存在");

    // ========== 项目轮次 1-030-004-000 ==========
    ErrorCode PROJECT_ROUND_NOT_EXISTS = new ErrorCode(1_030_004_000, "项目轮次不存在");
    ErrorCode PROJECT_ROUND_TIME_BEFORE_CONTRACT = new ErrorCode(1_030_004_001, "轮次开始时间不能早于合同开始时间（{}）");
    ErrorCode PROJECT_ROUND_TIME_AFTER_CONTRACT = new ErrorCode(1_030_004_002, "轮次结束时间不能晚于合同结束时间（{}）");
    ErrorCode PROJECT_ROUND_COUNT_EXCEED_LIMIT = new ErrorCode(1_030_004_003, "轮次数量已达上限（最多{}次），无法创建新轮次");

    // ========== 测试目标 1-030-005-000 ==========
    ErrorCode PROJECT_ROUND_TARGET_NOT_EXISTS = new ErrorCode(1_030_005_000, "测试目标不存在");

    // ========== 漏洞管理 1-030-006-000 ==========
    ErrorCode PROJECT_VULNERABILITY_NOT_EXISTS = new ErrorCode(1_030_006_000, "漏洞不存在");
    ErrorCode PROJECT_VULNERABILITY_DUPLICATE = new ErrorCode(1_030_006_001, "漏洞已存在");

}
