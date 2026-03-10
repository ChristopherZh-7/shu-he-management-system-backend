package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.datapermission.core.annotation.DataPermission;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectDeptServicePageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectDeptServiceSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptServiceDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectDeptServiceMapper;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 项目-部门服务单 Service 实现类
 */
@Service
@Validated
@Slf4j
public class ProjectDeptServiceServiceImpl implements ProjectDeptServiceService {

    @Resource
    private ProjectDeptServiceMapper deptServiceMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Resource
    @Lazy
    private ProjectService projectService;

    @Override
    public Long createDeptService(ProjectDeptServiceSaveReqVO createReqVO) {
        // 校验同一项目下同一部门类型不能重复
        ProjectDeptServiceDO existing = deptServiceMapper.selectByProjectIdAndDeptType(
                createReqVO.getProjectId(), createReqVO.getDeptType());
        if (existing != null) {
            throw exception(PROJECT_DEPT_SERVICE_EXISTS);
        }

        // 转换并插入
        ProjectDeptServiceDO deptService = BeanUtils.toBean(createReqVO, ProjectDeptServiceDO.class);
        deptService.setStatus(0); // 默认待领取状态
        deptService.setClaimed(false);
        deptService.setProgress(0);
        deptServiceMapper.insert(deptService);

        log.info("【部门服务单】创建部门服务单，projectId={}, deptType={}, id={}",
                createReqVO.getProjectId(), createReqVO.getDeptType(), deptService.getId());

        return deptService.getId();
    }

    @Override
    public void updateDeptService(ProjectDeptServiceSaveReqVO updateReqVO) {
        // 校验存在
        validateDeptServiceExists(updateReqVO.getId());

        // 更新
        ProjectDeptServiceDO updateObj = BeanUtils.toBean(updateReqVO, ProjectDeptServiceDO.class);
        deptServiceMapper.updateById(updateObj);

        log.info("【部门服务单】更新部门服务单，id={}", updateReqVO.getId());
    }

    @Override
    public void deleteDeptService(Long id) {
        // 校验存在
        validateDeptServiceExists(id);

        // 删除
        deptServiceMapper.deleteById(id);

        log.info("【部门服务单】删除部门服务单，id={}", id);
    }

    @Override
    @DataPermission(enable = false)
    public ProjectDeptServiceDO getDeptService(Long id) {
        return deptServiceMapper.selectById(id);
    }

    @Override
    @DataPermission(enable = false) // dept_id 在创建时为 NULL，数据权限过滤会导致记录不可见
    public PageResult<ProjectDeptServiceDO> getDeptServicePage(ProjectDeptServicePageReqVO pageReqVO) {
        return deptServiceMapper.selectPage(pageReqVO);
    }

    @Override
    @DataPermission(enable = false)
    public List<ProjectDeptServiceDO> getDeptServiceListByProjectId(Long projectId) {
        return deptServiceMapper.selectListByProjectId(projectId);
    }

    @Override
    @DataPermission(enable = false)
    public ProjectDeptServiceDO getDeptServiceByProjectIdAndDeptType(Long projectId, Integer deptType) {
        return deptServiceMapper.selectByProjectIdAndDeptType(projectId, deptType);
    }

    @Override
    @DataPermission(enable = false)
    public List<ProjectDeptServiceDO> getDeptServiceListByContractId(Long contractId) {
        return deptServiceMapper.selectListByContractId(contractId);
    }

    @Override
    @DataPermission(enable = false)
    public ProjectDeptServiceDO getDeptServiceByContractIdAndDeptType(Long contractId, Integer deptType) {
        return deptServiceMapper.selectByContractIdAndDeptType(contractId, deptType);
    }

    @Override
    public void updateDeptServiceStatus(Long id, Integer status) {
        // 校验存在
        ProjectDeptServiceDO deptService = validateDeptServiceExists(id);

        // 如果是开始状态，记录实际开始时间
        ProjectDeptServiceDO updateObj = new ProjectDeptServiceDO();
        updateObj.setId(id);
        updateObj.setStatus(status);

        // 如果是进行中状态(2)，记录实际开始时间
        if (status == 2 && deptService.getActualStartTime() == null) {
            updateObj.setActualStartTime(LocalDateTime.now());
        }
        // 如果是完成状态(4)，记录实际结束时间
        if (status == 4) {
            updateObj.setActualEndTime(LocalDateTime.now());
            updateObj.setProgress(100);
        }

        deptServiceMapper.updateById(updateObj);

        log.info("【部门服务单】更新状态，id={}, status={}", id, status);
    }

    @Override
    public void setDeptServiceManagers(Long id, List<Long> managerIds, List<String> managerNames,
                                       BigDecimal deptBudget, BigDecimal onsiteBudget, BigDecimal secondLineBudget) {
        ProjectDeptServiceDO existing = validateDeptServiceExists(id);
        BigDecimal effectiveDeptBudget = resolveEffectiveDeptBudget(existing.getDeptBudget(), deptBudget);
        validateBudgetNotExceeded(effectiveDeptBudget, onsiteBudget, secondLineBudget);

        ProjectDeptServiceDO updateObj = new ProjectDeptServiceDO();
        updateObj.setId(id);
        updateObj.setManagerIds(managerIds);
        updateObj.setManagerNames(managerNames);
        if (deptBudget != null && existing.getDeptBudget() == null) {
            updateObj.setDeptBudget(deptBudget); // 提前投入项目：允许首次填写部门总预算
        }
        if (onsiteBudget != null) updateObj.setOnsiteBudget(onsiteBudget);
        if (secondLineBudget != null) updateObj.setSecondLineBudget(secondLineBudget);

        resolveActualDept(updateObj, managerIds);
        deptServiceMapper.updateById(updateObj);
        log.info("【部门服务单】设置负责人，id={}, managerIds={}, deptBudget={}, onsiteBudget={}, secondLineBudget={}",
                id, managerIds, deptBudget, onsiteBudget, secondLineBudget);

        addManagersToGroupChat(id, managerIds);
    }

    @Override
    public void setSecurityServiceManagers(Long id,
                                            List<Long> onsiteManagerIds, List<String> onsiteManagerNames,
                                            List<Long> secondLineManagerIds, List<String> secondLineManagerNames,
                                            BigDecimal deptBudget, BigDecimal onsiteBudget, BigDecimal secondLineBudget) {
        ProjectDeptServiceDO deptService = validateDeptServiceExists(id);
        if (deptService.getDeptType() != 1) {
            throw exception(PROJECT_DEPT_SERVICE_NOT_SECURITY_SERVICE);
        }
        BigDecimal effectiveDeptBudget = resolveEffectiveDeptBudget(deptService.getDeptBudget(), deptBudget);
        validateBudgetNotExceeded(effectiveDeptBudget, onsiteBudget, secondLineBudget);

        ProjectDeptServiceDO updateObj = buildManagerUpdateObj(id,
                onsiteManagerIds, onsiteManagerNames, secondLineManagerIds, secondLineManagerNames);
        if (deptBudget != null && deptService.getDeptBudget() == null) {
            updateObj.setDeptBudget(deptBudget); // 提前投入项目：允许首次填写部门总预算
        }
        if (onsiteBudget != null) updateObj.setOnsiteBudget(onsiteBudget);
        if (secondLineBudget != null) updateObj.setSecondLineBudget(secondLineBudget);

        List<Long> firstManagerList = onsiteManagerIds != null && !onsiteManagerIds.isEmpty()
                ? onsiteManagerIds : secondLineManagerIds;
        resolveActualDept(updateObj, firstManagerList);

        deptServiceMapper.updateById(updateObj);
        log.info("【部门服务单】设置安全服务负责人，id={}, deptBudget={}, onsiteBudget={}, secondLineBudget={}",
                id, deptBudget, onsiteBudget, secondLineBudget);

        List<Long> allIds = mergeIds(onsiteManagerIds, secondLineManagerIds);
        addManagersToGroupChat(id, allIds);
    }

    @Override
    public void setDataSecurityManagers(Long id,
                                         List<Long> onsiteManagerIds, List<String> onsiteManagerNames,
                                         List<Long> secondLineManagerIds, List<String> secondLineManagerNames,
                                         BigDecimal deptBudget, BigDecimal onsiteBudget, BigDecimal secondLineBudget) {
        ProjectDeptServiceDO deptService = validateDeptServiceExists(id);
        if (deptService.getDeptType() != 3) {
            throw exception(PROJECT_DEPT_SERVICE_NOT_DATA_SECURITY);
        }
        BigDecimal effectiveDeptBudget = resolveEffectiveDeptBudget(deptService.getDeptBudget(), deptBudget);
        validateBudgetNotExceeded(effectiveDeptBudget, onsiteBudget, secondLineBudget);

        ProjectDeptServiceDO updateObj = buildManagerUpdateObj(id,
                onsiteManagerIds, onsiteManagerNames, secondLineManagerIds, secondLineManagerNames);
        if (deptBudget != null && deptService.getDeptBudget() == null) {
            updateObj.setDeptBudget(deptBudget); // 提前投入项目：允许首次填写部门总预算
        }
        if (onsiteBudget != null) updateObj.setOnsiteBudget(onsiteBudget);
        if (secondLineBudget != null) updateObj.setSecondLineBudget(secondLineBudget);

        List<Long> firstManagerList = onsiteManagerIds != null && !onsiteManagerIds.isEmpty()
                ? onsiteManagerIds : secondLineManagerIds;
        resolveActualDept(updateObj, firstManagerList);

        deptServiceMapper.updateById(updateObj);
        log.info("【部门服务单】设置数据安全负责人，id={}, deptBudget={}, onsiteBudget={}, secondLineBudget={}",
                id, deptBudget, onsiteBudget, secondLineBudget);

        List<Long> allIds = mergeIds(onsiteManagerIds, secondLineManagerIds);
        addManagersToGroupChat(id, allIds);
    }


    // ========== 私有辅助方法 ==========

    private ProjectDeptServiceDO buildManagerUpdateObj(Long id,
                                                        List<Long> onsiteManagerIds, List<String> onsiteManagerNames,
                                                        List<Long> secondLineManagerIds, List<String> secondLineManagerNames) {
        ProjectDeptServiceDO obj = new ProjectDeptServiceDO();
        obj.setId(id);
        obj.setOnsiteManagerIds(onsiteManagerIds);
        obj.setOnsiteManagerNames(onsiteManagerNames);
        obj.setSecondLineManagerIds(secondLineManagerIds);
        obj.setSecondLineManagerNames(secondLineManagerNames);
        List<Long> allIds = mergeIds(onsiteManagerIds, secondLineManagerIds);
        List<String> allNames = mergeNames(onsiteManagerNames, secondLineManagerNames);
        obj.setManagerIds(allIds.isEmpty() ? null : allIds);
        obj.setManagerNames(allNames.isEmpty() ? null : allNames);
        return obj;
    }

    private List<Long> mergeIds(List<Long> a, List<Long> b) {
        List<Long> result = new ArrayList<>();
        if (a != null) result.addAll(a);
        if (b != null) result.addAll(b);
        return result;
    }

    private List<String> mergeNames(List<String> a, List<String> b) {
        List<String> result = new ArrayList<>();
        if (a != null) result.addAll(a);
        if (b != null) result.addAll(b);
        return result;
    }

    private void resolveActualDept(ProjectDeptServiceDO updateObj, List<Long> managerIds) {
        if (managerIds == null || managerIds.isEmpty()) return;
        Long firstManagerId = managerIds.get(0);
        AdminUserRespDTO firstManager = adminUserApi.getUser(firstManagerId);
        if (firstManager != null && firstManager.getDeptId() != null) {
            Long actualDeptId = firstManager.getDeptId();
            DeptRespDTO actualDept = deptApi.getDept(actualDeptId);
            if (actualDept != null) {
                updateObj.setActualDeptId(actualDeptId);
                updateObj.setActualDeptName(actualDept.getName());
            }
        }
    }

    private void addManagersToGroupChat(Long deptServiceId, List<Long> managerIds) {
        if (managerIds == null || managerIds.isEmpty()) return;
        ProjectDeptServiceDO saved = deptServiceMapper.selectById(deptServiceId);
        if (saved != null && saved.getProjectId() != null) {
            projectService.addUsersToProjectGroupChat(saved.getProjectId(), managerIds);
        }
    }

    @Override
    public List<ProjectDeptServiceDO> batchCreateDeptServiceForBusiness(Long projectId, Long businessId,
                                                                         Long customerId, String customerName,
                                                                         List<Integer> deptTypes,
                                                                         java.util.Map<Integer, BigDecimal> deptTypeBudgetMap) {
        List<ProjectDeptServiceDO> result = new ArrayList<>();

        for (Integer deptType : deptTypes) {
            ProjectDeptServiceDO existing = deptServiceMapper.selectByProjectIdAndDeptType(projectId, deptType);
            if (existing != null) {
                log.warn("【部门服务单-商机】部门服务单已存在，跳过创建，projectId={}, deptType={}", projectId, deptType);
                result.add(existing);
                continue;
            }

            BigDecimal deptBudget = deptTypeBudgetMap != null ? deptTypeBudgetMap.get(deptType) : null;

            ProjectDeptServiceDO deptService = ProjectDeptServiceDO.builder()
                    .projectId(projectId)
                    .businessId(businessId)
                    .customerId(customerId)
                    .customerName(customerName)
                    .deptType(deptType)
                    .status(1) // 待开始（无需领取）
                    .progress(0)
                    .claimed(true) // 跳过领取流程
                    .deptBudget(deptBudget) // 从合同 deptAllocations 带入预算
                    .build();

            deptServiceMapper.insert(deptService);
            result.add(deptService);

            log.info("【部门服务单-商机】批量创建，projectId={}, deptType={}, id={}, deptBudget={}",
                    projectId, deptType, deptService.getId(), deptBudget);
        }

        return result;
    }

    @Override
    @DataPermission(enable = false)
    public void updateDeptBudgetByProjectId(Long projectId, java.util.Map<Integer, BigDecimal> deptTypeBudgetMap) {
        if (projectId == null || deptTypeBudgetMap == null || deptTypeBudgetMap.isEmpty()) {
            return;
        }
        List<ProjectDeptServiceDO> list = deptServiceMapper.selectListByProjectId(projectId);
        for (ProjectDeptServiceDO pds : list) {
            BigDecimal budget = deptTypeBudgetMap.get(pds.getDeptType());
            if (budget != null) {
                ProjectDeptServiceDO update = new ProjectDeptServiceDO();
                update.setId(pds.getId());
                update.setDeptBudget(budget);
                deptServiceMapper.updateById(update);
                log.info("【部门服务单】更新合同预算，id={}, deptType={}, deptBudget={}", pds.getId(), pds.getDeptType(), budget);
            }
        }
    }

    /**
     * 解析有效的部门总预算（用于校验）。
     * 优先使用已有值；若为 null 且本次提交了 deptBudget，则用本次值参与校验。
     */
    private BigDecimal resolveEffectiveDeptBudget(BigDecimal existing, BigDecimal submitted) {
        if (existing != null && existing.compareTo(BigDecimal.ZERO) > 0) {
            return existing;
        }
        return submitted;
    }

    /**
     * 校验驻场预算 + 二线预算不超过合同总预算。
     * 仅在 deptBudget 不为 null 时才校验（未设置总预算时不限制）。
     */
    private void validateBudgetNotExceeded(BigDecimal deptBudget, BigDecimal onsiteBudget, BigDecimal secondLineBudget) {
        if (deptBudget == null || deptBudget.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal onsite = onsiteBudget != null ? onsiteBudget : BigDecimal.ZERO;
        BigDecimal secondLine = secondLineBudget != null ? secondLineBudget : BigDecimal.ZERO;
        if (onsite.add(secondLine).compareTo(deptBudget) > 0) {
            throw exception(PROJECT_DEPT_SERVICE_BUDGET_EXCEEDED);
        }
    }

    private ProjectDeptServiceDO validateDeptServiceExists(Long id) {
        ProjectDeptServiceDO deptService = deptServiceMapper.selectById(id);
        if (deptService == null) {
            throw exception(PROJECT_DEPT_SERVICE_NOT_EXISTS);
        }
        return deptService;
    }

}
