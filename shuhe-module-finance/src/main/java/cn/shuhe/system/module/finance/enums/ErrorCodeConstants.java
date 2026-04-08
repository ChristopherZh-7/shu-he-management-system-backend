package cn.shuhe.system.module.finance.enums;

import cn.shuhe.system.framework.common.exception.ErrorCode;

/**
 * Finance 错误码枚举类
 *
 * finance 系统，使用 1-031-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 项目预算 1-031-001-000 ==========
    ErrorCode FINANCE_PROJECT_BUDGET_NOT_EXISTS = new ErrorCode(1_031_001_000, "项目预算记录不存在");
    ErrorCode FINANCE_PROJECT_BUDGET_ALREADY_EXISTS = new ErrorCode(1_031_001_001, "该部门服务单已存在预算记录");
    ErrorCode FINANCE_SUB_BUDGET_EXCEEDED = new ErrorCode(1_031_001_002, "驻场预算 + 二线预算不能超过部门总预算");

    // ========== 服务项收入分配 1-031-002-000 ==========
    ErrorCode FINANCE_SERVICE_ALLOCATION_NOT_EXISTS = new ErrorCode(1_031_002_000, "服务项收入分配记录不存在");
    ErrorCode FINANCE_SERVICE_ALLOCATION_EXCEEDED = new ErrorCode(1_031_002_001, "分配金额超出可用预算");
    ErrorCode FINANCE_ALLOCATION_ALREADY_INITIALIZED = new ErrorCode(1_031_002_002, "该合同的部门分配已初始化，请勿重复操作");
    ErrorCode FINANCE_CONTRACT_NO_DEPT_ALLOCATIONS = new ErrorCode(1_031_002_003, "该合同没有部门金额分配数据");

}
