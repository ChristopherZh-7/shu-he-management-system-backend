package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.datapermission.core.annotation.DataPermission;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectDeptServicePageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectDeptServiceSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptServiceDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectDeptServiceMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectMemberMapper;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.permission.PermissionApi;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

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

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    @Resource
    private PermissionApi permissionApi;


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
    @DataPermission(enable = false) // 与 getDeptService/page 一致：按 id 更新时不能以 dept_id 过滤，否则承接部门与用户部门不一致会误判「不存在」
    public void updateDeptService(ProjectDeptServiceSaveReqVO updateReqVO) {
        // 校验存在
        validateDeptServiceExists(updateReqVO.getId());

        // 更新
        ProjectDeptServiceDO updateObj = BeanUtils.toBean(updateReqVO, ProjectDeptServiceDO.class);
        deptServiceMapper.updateById(updateObj);

        log.info("【部门服务单】更新部门服务单，id={}", updateReqVO.getId());
    }

    @Override
    @DataPermission(enable = false)
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
    @DataPermission(enable = false)
    public PageResult<ProjectDeptServiceDO> getDeptServicePage(ProjectDeptServicePageReqVO pageReqVO) {
        return deptServiceMapper.selectPage(pageReqVO);
    }

    @Override
    @DataPermission(enable = false)
    public PageResult<ProjectDeptServiceDO> getDeptServicePage(ProjectDeptServicePageReqVO pageReqVO, Long userId) {
        boolean isSuperAdmin = permissionApi.hasAnyRoles(userId, "super_admin");
        if (isSuperAdmin) {
            return deptServiceMapper.selectPage(pageReqVO);
        }
        List<Long> projectIds = projectMemberMapper.selectProjectIdsByUserId(userId);
        if (CollUtil.isEmpty(projectIds)) {
            return PageResult.empty();
        }
        return deptServiceMapper.selectPageByProjectIds(pageReqVO, projectIds);
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
    @DataPermission(enable = false)
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
    @DataPermission(enable = false)
    public void setDeptServiceManagers(Long id, List<Long> managerIds, List<String> managerNames) {
        validateDeptServiceExists(id);

        ProjectDeptServiceDO updateObj = new ProjectDeptServiceDO();
        updateObj.setId(id);
        updateObj.setManagerIds(managerIds);
        updateObj.setManagerNames(managerNames);

        resolveActualDept(updateObj, managerIds);
        deptServiceMapper.updateById(updateObj);
        log.info("【部门服务单】设置负责人，id={}, managerIds={}", id, managerIds);

        addManagersToGroupChat(id, managerIds);
    }

    @Override
    @DataPermission(enable = false)
    public void setSecurityServiceManagers(Long id,
                                            List<Long> onsiteManagerIds, List<String> onsiteManagerNames,
                                            List<Long> secondLineManagerIds, List<String> secondLineManagerNames) {
        ProjectDeptServiceDO deptService = validateDeptServiceExists(id);
        if (deptService.getDeptType() != 1) {
            throw exception(PROJECT_DEPT_SERVICE_NOT_SECURITY_SERVICE);
        }

        ProjectDeptServiceDO updateObj = buildManagerUpdateObj(id,
                onsiteManagerIds, onsiteManagerNames, secondLineManagerIds, secondLineManagerNames);

        List<Long> firstManagerList = onsiteManagerIds != null && !onsiteManagerIds.isEmpty()
                ? onsiteManagerIds : secondLineManagerIds;
        resolveActualDept(updateObj, firstManagerList);

        deptServiceMapper.updateById(updateObj);
        log.info("【部门服务单】设置安全服务负责人，id={}", id);

        List<Long> allIds = mergeIds(onsiteManagerIds, secondLineManagerIds);
        addManagersToGroupChat(id, allIds);
    }

    @Override
    @DataPermission(enable = false)
    public void setDataSecurityManagers(Long id,
                                         List<Long> onsiteManagerIds, List<String> onsiteManagerNames,
                                         List<Long> secondLineManagerIds, List<String> secondLineManagerNames) {
        ProjectDeptServiceDO deptService = validateDeptServiceExists(id);
        if (deptService.getDeptType() != 3) {
            throw exception(PROJECT_DEPT_SERVICE_NOT_DATA_SECURITY);
        }

        ProjectDeptServiceDO updateObj = buildManagerUpdateObj(id,
                onsiteManagerIds, onsiteManagerNames, secondLineManagerIds, secondLineManagerNames);

        List<Long> firstManagerList = onsiteManagerIds != null && !onsiteManagerIds.isEmpty()
                ? onsiteManagerIds : secondLineManagerIds;
        resolveActualDept(updateObj, firstManagerList);

        deptServiceMapper.updateById(updateObj);
        log.info("【部门服务单】设置数据安全负责人，id={}", id);

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
                                                                         java.util.Map<Integer, Long> deptTypeToDeptId,
                                                                         java.util.Map<Integer, String> deptTypeToDeptName) {
        List<ProjectDeptServiceDO> result = new ArrayList<>();

        for (Integer deptType : deptTypes) {
            ProjectDeptServiceDO existing = deptServiceMapper.selectByProjectIdAndDeptType(projectId, deptType);
            if (existing != null) {
                log.warn("【部门服务单-商机】部门服务单已存在，跳过创建，projectId={}, deptType={}", projectId, deptType);
                result.add(existing);
                continue;
            }

            Long deptId = deptTypeToDeptId != null ? deptTypeToDeptId.get(deptType) : null;
            String deptName = deptTypeToDeptName != null ? deptTypeToDeptName.get(deptType) : null;

            ProjectDeptServiceDO deptService = ProjectDeptServiceDO.builder()
                    .projectId(projectId)
                    .businessId(businessId)
                    .customerId(customerId)
                    .customerName(customerName)
                    .deptType(deptType)
                    .deptId(deptId)
                    .deptName(deptName)
                    .status(1)
                    .progress(0)
                    .claimed(true)
                    .build();

            deptServiceMapper.insert(deptService);
            result.add(deptService);

            log.info("【部门服务单-商机】批量创建，projectId={}, deptType={}, id={}, deptId={}",
                    projectId, deptType, deptService.getId(), deptId);
        }

        return result;
    }

    private ProjectDeptServiceDO validateDeptServiceExists(Long id) {
        ProjectDeptServiceDO deptService = deptServiceMapper.selectById(id);
        if (deptService == null) {
            throw exception(PROJECT_DEPT_SERVICE_NOT_EXISTS);
        }
        return deptService;
    }

}
