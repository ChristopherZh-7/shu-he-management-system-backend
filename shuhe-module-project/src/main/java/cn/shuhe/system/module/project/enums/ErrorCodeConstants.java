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

    // ========== 项目成员 1-030-002-000 ==========
    ErrorCode PROJECT_MEMBER_NOT_EXISTS = new ErrorCode(1_030_002_000, "项目成员不存在");
    ErrorCode PROJECT_MEMBER_DUPLICATE = new ErrorCode(1_030_002_001, "项目成员已存在");

    // ========== 项目任务 1-030-003-000 ==========
    ErrorCode PROJECT_TASK_NOT_EXISTS = new ErrorCode(1_030_003_000, "项目任务不存在");

    // ========== 项目轮次 1-030-004-000 ==========
    ErrorCode PROJECT_ROUND_NOT_EXISTS = new ErrorCode(1_030_004_000, "项目轮次不存在");

    // ========== 测试目标 1-030-005-000 ==========
    ErrorCode PROJECT_ROUND_TARGET_NOT_EXISTS = new ErrorCode(1_030_005_000, "测试目标不存在");

    // ========== 漏洞管理 1-030-006-000 ==========
    ErrorCode PROJECT_VULNERABILITY_NOT_EXISTS = new ErrorCode(1_030_006_000, "漏洞不存在");
    ErrorCode PROJECT_VULNERABILITY_DUPLICATE = new ErrorCode(1_030_006_001, "漏洞已存在");

}
