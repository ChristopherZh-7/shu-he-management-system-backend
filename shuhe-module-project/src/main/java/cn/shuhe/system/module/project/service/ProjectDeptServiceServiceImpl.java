package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    public ProjectDeptServiceDO getDeptService(Long id) {
        return deptServiceMapper.selectById(id);
    }

    @Override
    public PageResult<ProjectDeptServiceDO> getDeptServicePage(ProjectDeptServicePageReqVO pageReqVO) {
        return deptServiceMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ProjectDeptServiceDO> getDeptServiceListByProjectId(Long projectId) {
        return deptServiceMapper.selectListByProjectId(projectId);
    }

    @Override
    public ProjectDeptServiceDO getDeptServiceByProjectIdAndDeptType(Long projectId, Integer deptType) {
        return deptServiceMapper.selectByProjectIdAndDeptType(projectId, deptType);
    }

    @Override
    public List<ProjectDeptServiceDO> getDeptServiceListByContractId(Long contractId) {
        return deptServiceMapper.selectListByContractId(contractId);
    }

    @Override
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
    public void setDeptServiceManagers(Long id, List<Long> managerIds, List<String> managerNames) {
        // 校验存在
        validateDeptServiceExists(id);

        // 更新负责人
        ProjectDeptServiceDO updateObj = new ProjectDeptServiceDO();
        updateObj.setId(id);
        updateObj.setManagerIds(managerIds);
        updateObj.setManagerNames(managerNames);

        // 根据第一个负责人的部门确定实际执行部门
        if (managerIds != null && !managerIds.isEmpty()) {
            Long firstManagerId = managerIds.get(0);
            AdminUserRespDTO firstManager = adminUserApi.getUser(firstManagerId);
            if (firstManager != null && firstManager.getDeptId() != null) {
                Long actualDeptId = firstManager.getDeptId();
                DeptRespDTO actualDept = deptApi.getDept(actualDeptId);
                if (actualDept != null) {
                    updateObj.setActualDeptId(actualDeptId);
                    updateObj.setActualDeptName(actualDept.getName());
                    log.info("【部门服务单】根据负责人{}确定实际执行部门: id={}, name={}",
                            firstManagerId, actualDeptId, actualDept.getName());
                }
            }
        }

        deptServiceMapper.updateById(updateObj);

        log.info("【部门服务单】设置负责人，id={}, managerIds={}", id, managerIds);
    }

    @Override
    public void setSecurityServiceManagers(Long id,
                                            List<Long> onsiteManagerIds, List<String> onsiteManagerNames,
                                            List<Long> secondLineManagerIds, List<String> secondLineManagerNames) {
        // 校验存在
        ProjectDeptServiceDO deptService = validateDeptServiceExists(id);

        // 校验是否为安全服务
        if (deptService.getDeptType() != 1) {
            throw exception(PROJECT_DEPT_SERVICE_NOT_SECURITY_SERVICE);
        }

        // 更新驻场和二线负责人
        ProjectDeptServiceDO updateObj = new ProjectDeptServiceDO();
        updateObj.setId(id);
        updateObj.setOnsiteManagerIds(onsiteManagerIds);
        updateObj.setOnsiteManagerNames(onsiteManagerNames);
        updateObj.setSecondLineManagerIds(secondLineManagerIds);
        updateObj.setSecondLineManagerNames(secondLineManagerNames);

        // 同时更新 managerIds 和 managerNames（合并驻场和二线负责人）用于兼容
        List<Long> allManagerIds = new ArrayList<>();
        List<String> allManagerNames = new ArrayList<>();
        if (onsiteManagerIds != null) {
            allManagerIds.addAll(onsiteManagerIds);
        }
        if (secondLineManagerIds != null) {
            allManagerIds.addAll(secondLineManagerIds);
        }
        if (onsiteManagerNames != null) {
            allManagerNames.addAll(onsiteManagerNames);
        }
        if (secondLineManagerNames != null) {
            allManagerNames.addAll(secondLineManagerNames);
        }
        updateObj.setManagerIds(allManagerIds.isEmpty() ? null : allManagerIds);
        updateObj.setManagerNames(allManagerNames.isEmpty() ? null : allManagerNames);

        // 根据第一个负责人的部门确定实际执行部门（优先使用驻场负责人）
        List<Long> firstManagerList = onsiteManagerIds != null && !onsiteManagerIds.isEmpty() 
                ? onsiteManagerIds : secondLineManagerIds;
        if (firstManagerList != null && !firstManagerList.isEmpty()) {
            Long firstManagerId = firstManagerList.get(0);
            AdminUserRespDTO firstManager = adminUserApi.getUser(firstManagerId);
            if (firstManager != null && firstManager.getDeptId() != null) {
                Long actualDeptId = firstManager.getDeptId();
                DeptRespDTO actualDept = deptApi.getDept(actualDeptId);
                if (actualDept != null) {
                    updateObj.setActualDeptId(actualDeptId);
                    updateObj.setActualDeptName(actualDept.getName());
                    log.info("【部门服务单】根据负责人{}确定实际执行部门: id={}, name={}",
                            firstManagerId, actualDeptId, actualDept.getName());
                }
            }
        }

        deptServiceMapper.updateById(updateObj);

        log.info("【部门服务单】设置安全服务负责人，id={}, onsiteManagerIds={}, secondLineManagerIds={}", 
                id, onsiteManagerIds, secondLineManagerIds);
    }

    @Override
    public void setDataSecurityManagers(Long id,
                                         List<Long> onsiteManagerIds, List<String> onsiteManagerNames,
                                         List<Long> secondLineManagerIds, List<String> secondLineManagerNames) {
        // 校验存在
        ProjectDeptServiceDO deptService = validateDeptServiceExists(id);

        // 校验是否为数据安全
        if (deptService.getDeptType() != 3) {
            throw exception(PROJECT_DEPT_SERVICE_NOT_DATA_SECURITY);
        }

        // 更新驻场和二线负责人（与安全服务逻辑一致）
        ProjectDeptServiceDO updateObj = new ProjectDeptServiceDO();
        updateObj.setId(id);
        updateObj.setOnsiteManagerIds(onsiteManagerIds);
        updateObj.setOnsiteManagerNames(onsiteManagerNames);
        updateObj.setSecondLineManagerIds(secondLineManagerIds);
        updateObj.setSecondLineManagerNames(secondLineManagerNames);

        // 同时更新 managerIds 和 managerNames（合并驻场和二线负责人）用于兼容
        List<Long> allManagerIds = new ArrayList<>();
        List<String> allManagerNames = new ArrayList<>();
        if (onsiteManagerIds != null) {
            allManagerIds.addAll(onsiteManagerIds);
        }
        if (secondLineManagerIds != null) {
            allManagerIds.addAll(secondLineManagerIds);
        }
        if (onsiteManagerNames != null) {
            allManagerNames.addAll(onsiteManagerNames);
        }
        if (secondLineManagerNames != null) {
            allManagerNames.addAll(secondLineManagerNames);
        }
        updateObj.setManagerIds(allManagerIds.isEmpty() ? null : allManagerIds);
        updateObj.setManagerNames(allManagerNames.isEmpty() ? null : allManagerNames);

        // 根据第一个负责人的部门确定实际执行部门（优先使用驻场负责人）
        List<Long> firstManagerList = onsiteManagerIds != null && !onsiteManagerIds.isEmpty() 
                ? onsiteManagerIds : secondLineManagerIds;
        if (firstManagerList != null && !firstManagerList.isEmpty()) {
            Long firstManagerId = firstManagerList.get(0);
            AdminUserRespDTO firstManager = adminUserApi.getUser(firstManagerId);
            if (firstManager != null && firstManager.getDeptId() != null) {
                Long actualDeptId = firstManager.getDeptId();
                DeptRespDTO actualDept = deptApi.getDept(actualDeptId);
                if (actualDept != null) {
                    updateObj.setActualDeptId(actualDeptId);
                    updateObj.setActualDeptName(actualDept.getName());
                    log.info("【部门服务单】根据负责人{}确定实际执行部门: id={}, name={}",
                            firstManagerId, actualDeptId, actualDept.getName());
                }
            }
        }

        deptServiceMapper.updateById(updateObj);

        log.info("【部门服务单】设置数据安全负责人，id={}, onsiteManagerIds={}, secondLineManagerIds={}", 
                id, onsiteManagerIds, secondLineManagerIds);
    }

    @Override
    public void claimDeptService(Long id, Long deptId, String deptName, Long userId, String userName) {
        // 校验存在
        ProjectDeptServiceDO deptService = validateDeptServiceExists(id);

        // 校验是否已领取
        if (Boolean.TRUE.equals(deptService.getClaimed())) {
            throw exception(PROJECT_DEPT_SERVICE_ALREADY_CLAIMED);
        }

        // 更新领取信息和状态
        ProjectDeptServiceDO updateObj = new ProjectDeptServiceDO();
        updateObj.setId(id);
        updateObj.setDeptId(deptId);
        updateObj.setDeptName(deptName);
        updateObj.setClaimUserId(userId);
        updateObj.setClaimUserName(userName);
        updateObj.setClaimTime(LocalDateTime.now());
        updateObj.setClaimed(true);
        updateObj.setStatus(1); // 更新状态为待开始（已领取但未设置负责人和开始项目）
        deptServiceMapper.updateById(updateObj);

        log.info("【部门服务单】领取成功，id={}, deptId={}, userId={}, status=1(待开始)", id, deptId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ProjectDeptServiceDO> batchCreateDeptService(Long projectId, Long contractId, String contractNo,
                                                              Long customerId, String customerName, List<Integer> deptTypes) {
        List<ProjectDeptServiceDO> result = new ArrayList<>();

        for (Integer deptType : deptTypes) {
            // 检查是否已存在
            ProjectDeptServiceDO existing = deptServiceMapper.selectByProjectIdAndDeptType(projectId, deptType);
            if (existing != null) {
                log.warn("【部门服务单】部门服务单已存在，跳过创建，projectId={}, deptType={}", projectId, deptType);
                result.add(existing);
                continue;
            }

            // 创建部门服务单
            ProjectDeptServiceDO deptService = ProjectDeptServiceDO.builder()
                    .projectId(projectId)
                    .contractId(contractId)
                    .contractNo(contractNo)
                    .customerId(customerId)
                    .customerName(customerName)
                    .deptType(deptType)
                    .status(0) // 待领取
                    .progress(0)
                    .claimed(false)
                    .build();

            deptServiceMapper.insert(deptService);
            result.add(deptService);

            log.info("【部门服务单】批量创建，projectId={}, deptType={}, id={}",
                    projectId, deptType, deptService.getId());
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
