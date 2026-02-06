package cn.shuhe.system.module.system.controller.admin.cost;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisRespVO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.dal.mysql.cost.SecurityOperationContractInfoMapper;
import cn.shuhe.system.module.system.service.dept.DeptService;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import cn.shuhe.system.module.system.service.cost.BusinessAnalysisService;
import cn.shuhe.system.module.system.service.cost.BusinessAnalysisCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

/**
 * 经营分析 Controller
 */
@Tag(name = "管理后台 - 经营分析")
@RestController
@RequestMapping("/system/business-analysis")
@Validated
public class BusinessAnalysisController {

    @Resource
    private BusinessAnalysisService businessAnalysisService;

    @Resource
    private BusinessAnalysisCacheService businessAnalysisCacheService;

    @Resource
    private SecurityOperationContractInfoMapper securityOperationContractInfoMapper;

    @Resource
    private DeptService deptService;

    @Resource
    private AdminUserService adminUserService;

    @GetMapping("/summary")
    @Operation(summary = "获取经营分析汇总")
    @PreAuthorize("@ss.hasPermission('system:business-analysis:query')")
    public CommonResult<BusinessAnalysisRespVO> getBusinessAnalysisSummary(@Valid BusinessAnalysisReqVO reqVO) {
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        
        // 【性能优化】优先从Redis缓存获取
        int year = reqVO.getYear() != null ? reqVO.getYear() : LocalDate.now().getYear();
        LocalDate cutoffDate = reqVO.getCutoffDate() != null ? reqVO.getCutoffDate() : LocalDate.now();
        
        try {
            BusinessAnalysisRespVO cachedResult = businessAnalysisCacheService.getBusinessAnalysis(year, cutoffDate, currentUserId);
            if (cachedResult != null) {
                return success(cachedResult);
            }
        } catch (Exception e) {
            // 缓存获取失败，降级到直接计算
        }
        
        // 缓存未命中或获取失败，直接计算
        return success(businessAnalysisService.getBusinessAnalysis(reqVO, currentUserId));
    }

    @GetMapping("/dept/{deptId}")
    @Operation(summary = "获取部门详情（含子部门和员工）")
    @PreAuthorize("@ss.hasPermission('system:business-analysis:query')")
    public CommonResult<BusinessAnalysisRespVO.DeptAnalysis> getDeptDetail(
            @PathVariable("deptId") Long deptId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "cutoffDate", required = false) LocalDate cutoffDate) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        if (cutoffDate == null) {
            cutoffDate = LocalDate.now();
        }
        return success(businessAnalysisService.getDeptDetail(deptId, year, cutoffDate));
    }

    @GetMapping("/employee/{userId}")
    @Operation(summary = "获取员工详情")
    @PreAuthorize("@ss.hasPermission('system:business-analysis:query')")
    public CommonResult<BusinessAnalysisRespVO.EmployeeAnalysis> getEmployeeDetail(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "cutoffDate", required = false) LocalDate cutoffDate) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        if (cutoffDate == null) {
            cutoffDate = LocalDate.now();
        }
        return success(businessAnalysisService.getEmployeeAnalysis(userId, year, cutoffDate));
    }

    @GetMapping("/diagnostic/security-operation-members")
    @Operation(summary = "诊断接口 - 获取所有安全运营成员数据", 
            description = "用于排查经营分析中安全运营收入问题，返回 security_operation_member 表的原始数据")
    @PreAuthorize("@ss.hasPermission('system:business-analysis:query')")
    public CommonResult<Map<String, Object>> getDiagnosticSecurityOperationMembers() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取所有成员记录
        List<Map<String, Object>> allMembers = securityOperationContractInfoMapper.selectAllMembersForDiagnostic();
        result.put("allMembers", allMembers);
        result.put("totalCount", allMembers.size());
        result.put("message", "此接口用于诊断安全运营收入问题。如果 allMembers 为空，说明 security_operation_member 表中没有有效记录，需要在安全运营合同管理中添加人员。");
        
        return success(result);
    }

    @GetMapping("/diagnostic/security-operation-member/{userId}")
    @Operation(summary = "诊断接口 - 获取指定用户的安全运营参与记录")
    @Parameter(name = "userId", description = "用户ID", required = true)
    @PreAuthorize("@ss.hasPermission('system:business-analysis:query')")
    public CommonResult<Map<String, Object>> getDiagnosticUserParticipation(@PathVariable("userId") Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取用户参与的合同
        List<Map<String, Object>> participation = securityOperationContractInfoMapper.selectMemberParticipation(userId);
        result.put("userId", userId);
        result.put("participation", participation);
        result.put("participationCount", participation.size());
        
        if (participation.isEmpty()) {
            result.put("message", "该用户未在 security_operation_member 表中找到任何参与记录。请检查：1) 用户ID是否正确；2) 是否已在安全运营合同中添加该用户为成员。");
        } else {
            result.put("message", "找到 " + participation.size() + " 条参与记录");
        }
        
        return success(result);
    }

    @GetMapping("/diagnostic/full-analysis")
    @Operation(summary = "诊断接口 - 完整诊断安全运营数据问题",
            description = "全面检查安全运营部门员工与成员表的匹配情况，用于排查为何没有收入数据")
    @PreAuthorize("@ss.hasPermission('system:business-analysis:query')")
    public CommonResult<Map<String, Object>> getDiagnosticFullAnalysis() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        // 1. 【性能优化】从缓存获取所有部门
        List<DeptDO> allDepts = deptService.getAllDeptListFromCache();
        List<Map<String, Object>> operationDepts = new ArrayList<>();
        Set<Long> operationDeptIds = new HashSet<>();
        
        for (DeptDO dept : allDepts) {
            if (isSecurityOperationDept(dept)) {
                operationDeptIds.add(dept.getId());
                Map<String, Object> deptInfo = new LinkedHashMap<>();
                deptInfo.put("deptId", dept.getId());
                deptInfo.put("deptName", dept.getName());
                deptInfo.put("deptType", dept.getDeptType());
                deptInfo.put("parentId", dept.getParentId());
                operationDepts.add(deptInfo);
            }
        }
        result.put("step1_安全运营部门列表", operationDepts);
        result.put("step1_安全运营部门数量", operationDeptIds.size());
        
        // 2. 获取安全运营部门下的所有员工
        List<AdminUserDO> operationEmployees = new ArrayList<>();
        if (!operationDeptIds.isEmpty()) {
            operationEmployees = adminUserService.getUserListByDeptIds(operationDeptIds);
        }
        
        List<Map<String, Object>> employeeList = new ArrayList<>();
        for (AdminUserDO user : operationEmployees) {
            Map<String, Object> userInfo = new LinkedHashMap<>();
            userInfo.put("userId", user.getId());
            userInfo.put("userName", user.getNickname());
            userInfo.put("deptId", user.getDeptId());
            // 查找该用户在成员表中的记录
            List<Map<String, Object>> memberRecords = securityOperationContractInfoMapper.selectMemberParticipation(user.getId());
            userInfo.put("memberRecordCount", memberRecords.size());
            userInfo.put("hasParticipation", !memberRecords.isEmpty());
            employeeList.add(userInfo);
        }
        result.put("step2_安全运营部门员工列表", employeeList);
        result.put("step2_安全运营部门员工数量", operationEmployees.size());
        
        // 3. 获取成员表中的所有记录
        List<Map<String, Object>> allMembers = securityOperationContractInfoMapper.selectAllMembersForDiagnostic();
        result.put("step3_成员表记录", allMembers);
        result.put("step3_成员表记录数量", allMembers.size());
        
        // 4. 检查成员表中的 userId 是否存在于系统用户表中
        List<Map<String, Object>> memberUserIdCheck = new ArrayList<>();
        Set<Long> memberUserIds = new HashSet<>();
        for (Map<String, Object> member : allMembers) {
            Object userIdObj = member.get("userId");
            if (userIdObj != null) {
                Long userId = ((Number) userIdObj).longValue();
                memberUserIds.add(userId);
            }
        }
        
        for (Long userId : memberUserIds) {
            AdminUserDO user = adminUserService.getUser(userId);
            Map<String, Object> check = new LinkedHashMap<>();
            check.put("memberId_userId", userId);
            check.put("userExists", user != null);
            check.put("userName", user != null ? user.getNickname() : "用户不存在");
            check.put("userDeptId", user != null ? user.getDeptId() : null);
            if (user != null && user.getDeptId() != null) {
                check.put("isInOperationDept", operationDeptIds.contains(user.getDeptId()));
            }
            memberUserIdCheck.add(check);
        }
        result.put("step4_成员表userId有效性检查", memberUserIdCheck);
        
        // 5. 问题诊断总结
        Map<String, Object> diagnosis = new LinkedHashMap<>();
        if (operationDeptIds.isEmpty()) {
            diagnosis.put("问题1", "未找到安全运营相关部门！请检查部门名称是否包含'运营'关键字，或部门类型(deptType)是否设置为2");
        }
        if (operationEmployees.isEmpty()) {
            diagnosis.put("问题2", "安全运营部门下没有员工！请检查员工是否正确分配到安全运营部门");
        }
        if (allMembers.isEmpty()) {
            diagnosis.put("问题3", "security_operation_member 表中没有任何成员记录！请在安全运营合同管理中添加管理人员和驻场人员");
        }
        
        // 检查成员表中的userId是否匹配安全运营部门员工
        Set<Long> operationEmployeeIds = operationEmployees.stream()
                .map(AdminUserDO::getId)
                .collect(Collectors.toSet());
        Set<Long> matchedUserIds = new HashSet<>(memberUserIds);
        matchedUserIds.retainAll(operationEmployeeIds);
        
        if (matchedUserIds.isEmpty() && !memberUserIds.isEmpty() && !operationEmployeeIds.isEmpty()) {
            diagnosis.put("问题4", "成员表中的用户ID与安全运营部门员工ID完全不匹配！成员表userId=" + memberUserIds + "，安全运营部门员工userId=" + operationEmployeeIds);
        }
        
        // 检查费用设置
        boolean hasFeeIssue = false;
        for (Map<String, Object> member : allMembers) {
            Object managementFee = member.get("managementFee");
            Object onsiteFee = member.get("onsiteFee");
            if ((managementFee == null || ((Number) managementFee).doubleValue() == 0) 
                && (onsiteFee == null || ((Number) onsiteFee).doubleValue() == 0)) {
                hasFeeIssue = true;
                break;
            }
        }
        if (hasFeeIssue) {
            diagnosis.put("问题5", "存在合同的管理费和驻场费都为0或空！请在安全运营合同中设置正确的费用");
        }
        
        if (diagnosis.isEmpty()) {
            diagnosis.put("状态", "数据配置正常，但仍需检查具体收入计算逻辑");
        }
        
        result.put("step5_问题诊断", diagnosis);
        
        return success(result);
    }

    /**
     * 判断是否是安全运营部门
     */
    private boolean isSecurityOperationDept(DeptDO dept) {
        // 检查部门类型
        if (dept.getDeptType() != null && dept.getDeptType() == 2) { // DEPT_TYPE_SECURITY_OPERATION
            return true;
        }
        // 检查名称
        String name = dept.getName();
        if (name != null && name.contains("运营")) {
            return true;
        }
        return false;
    }

}
