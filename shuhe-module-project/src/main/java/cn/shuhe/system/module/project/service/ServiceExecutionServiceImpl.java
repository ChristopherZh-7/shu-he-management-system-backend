package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceExecutionPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceExecutionRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceExecutionSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceExecutionDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceExecutionMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 服务执行申请 Service 实现
 */
@Service
@Validated
@Slf4j
public class ServiceExecutionServiceImpl implements ServiceExecutionService {

    @Resource
    private ServiceExecutionMapper serviceExecutionMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private ProjectRoundService projectRoundService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createServiceExecution(ServiceExecutionSaveReqVO createReqVO) {
        // 1. 校验项目存在
        ProjectDO project = projectMapper.selectById(createReqVO.getProjectId());
        if (project == null) {
            throw exception(PROJECT_NOT_EXISTS);
        }

        // 2. 校验服务项存在
        ServiceItemDO serviceItem = serviceItemMapper.selectById(createReqVO.getServiceItemId());
        if (serviceItem == null) {
            throw exception(SERVICE_ITEM_NOT_EXISTS);
        }

        // 3. 获取当前用户信息
        Long userId = getLoginUserId();
        AdminUserRespDTO user = adminUserApi.getUser(userId);

        // 4. 创建申请记录
        ServiceExecutionDO execution = BeanUtils.toBean(createReqVO, ServiceExecutionDO.class);
        execution.setRequestUserId(userId);
        execution.setRequestDeptId(user != null ? user.getDeptId() : null);
        execution.setStatus(0); // 待审批

        // 渗透测试附件（转换为JSON存储）
        if (createReqVO.getAuthorizationUrls() != null && !createReqVO.getAuthorizationUrls().isEmpty()) {
            execution.setAuthorizationUrls(JSONUtil.toJsonStr(createReqVO.getAuthorizationUrls()));
        }
        if (createReqVO.getTestScopeUrls() != null && !createReqVO.getTestScopeUrls().isEmpty()) {
            execution.setTestScopeUrls(JSONUtil.toJsonStr(createReqVO.getTestScopeUrls()));
        }
        if (createReqVO.getCredentialsUrls() != null && !createReqVO.getCredentialsUrls().isEmpty()) {
            execution.setCredentialsUrls(JSONUtil.toJsonStr(createReqVO.getCredentialsUrls()));
        }

        serviceExecutionMapper.insert(execution);

        return execution.getId();
    }

    @Override
    public void updateServiceExecution(ServiceExecutionSaveReqVO updateReqVO) {
        // 校验存在
        validateServiceExecutionExists(updateReqVO.getId());

        // 更新
        ServiceExecutionDO updateObj = BeanUtils.toBean(updateReqVO, ServiceExecutionDO.class);
        serviceExecutionMapper.updateById(updateObj);
    }

    @Override
    public void setExecutors(Long id, List<Long> executorIds) {
        // 校验存在
        ServiceExecutionDO execution = validateServiceExecutionExists(id);
        if (execution.getStatus() != 0) {
            throw exception(SERVICE_EXECUTION_STATUS_NOT_PENDING);
        }

        // 获取执行人姓名
        String executorNames = "";
        if (CollUtil.isNotEmpty(executorIds)) {
            List<AdminUserRespDTO> users = adminUserApi.getUserList(executorIds);
            executorNames = users.stream()
                    .map(AdminUserRespDTO::getNickname)
                    .collect(Collectors.joining(","));
        }

        // 更新执行人
        ServiceExecutionDO updateObj = new ServiceExecutionDO();
        updateObj.setId(id);
        updateObj.setExecutorIds(JSONUtil.toJsonStr(executorIds));
        updateObj.setExecutorNames(executorNames);
        serviceExecutionMapper.updateById(updateObj);
    }

    @Override
    public void updateProcessInstanceId(Long id, String processInstanceId) {
        ServiceExecutionDO updateObj = new ServiceExecutionDO();
        updateObj.setId(id);
        updateObj.setProcessInstanceId(processInstanceId);
        serviceExecutionMapper.updateById(updateObj);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        ServiceExecutionDO updateObj = new ServiceExecutionDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        serviceExecutionMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long handleApproved(Long id, String processInstanceId) {
        // 1. 获取申请记录
        ServiceExecutionDO execution = serviceExecutionMapper.selectById(id);
        if (execution == null) {
            log.warn("【服务执行】申请记录不存在，id={}", id);
            return null;
        }

        // 2. 更新状态为已通过
        updateStatus(id, 1);

        // 3. 创建执行轮次
        Long roundId = projectRoundService.createRoundByServiceItem(
                execution.getServiceItemId(),
                processInstanceId,
                execution.getPlanStartTime(),
                execution.getPlanEndTime()
        );

        // 4. 设置轮次执行人
        if (execution.getExecutorIds() != null && !execution.getExecutorIds().isEmpty()) {
            try {
                List<Long> executorIds = JSONUtil.toList(execution.getExecutorIds(), Long.class);
                projectRoundService.setExecutors(roundId, executorIds);
            } catch (Exception e) {
                log.error("【服务执行】设置轮次执行人失败: {}", e.getMessage(), e);
            }
        }

        // 5. 更新申请记录的轮次ID
        ServiceExecutionDO updateObj = new ServiceExecutionDO();
        updateObj.setId(id);
        updateObj.setRoundId(roundId);
        serviceExecutionMapper.updateById(updateObj);

        log.info("【服务执行】审批通过，已创建轮次。executionId={}, roundId={}", id, roundId);
        return roundId;
    }

    @Override
    public void deleteServiceExecution(Long id) {
        // 校验存在
        ServiceExecutionDO execution = validateServiceExecutionExists(id);
        // 只能删除待审批的
        if (execution.getStatus() != 0) {
            throw exception(SERVICE_EXECUTION_STATUS_NOT_PENDING);
        }
        serviceExecutionMapper.deleteById(id);
    }

    @Override
    public ServiceExecutionDO getServiceExecution(Long id) {
        return serviceExecutionMapper.selectById(id);
    }

    @Override
    public ServiceExecutionRespVO getServiceExecutionDetail(Long id) {
        ServiceExecutionDO execution = serviceExecutionMapper.selectById(id);
        if (execution == null) {
            return null;
        }
        return buildRespVO(execution);
    }

    @Override
    public PageResult<ServiceExecutionRespVO> getServiceExecutionPage(ServiceExecutionPageReqVO pageReqVO) {
        PageResult<ServiceExecutionDO> pageResult = serviceExecutionMapper.selectPage(pageReqVO);
        return new PageResult<>(
                pageResult.getList().stream().map(this::buildRespVO).collect(Collectors.toList()),
                pageResult.getTotal()
        );
    }

    @Override
    public PageResult<ServiceExecutionRespVO> getMyServiceExecutionPage(ServiceExecutionPageReqVO pageReqVO) {
        Long userId = getLoginUserId();
        PageResult<ServiceExecutionDO> pageResult = serviceExecutionMapper.selectPageByUserId(pageReqVO, userId);
        return new PageResult<>(
                pageResult.getList().stream().map(this::buildRespVO).collect(Collectors.toList()),
                pageResult.getTotal()
        );
    }

    @Override
    public ServiceExecutionDO getByProcessInstanceId(String processInstanceId) {
        return serviceExecutionMapper.selectOne(ServiceExecutionDO::getProcessInstanceId, processInstanceId);
    }

    /**
     * 校验申请存在
     */
    private ServiceExecutionDO validateServiceExecutionExists(Long id) {
        ServiceExecutionDO execution = serviceExecutionMapper.selectById(id);
        if (execution == null) {
            throw exception(SERVICE_EXECUTION_NOT_EXISTS);
        }
        return execution;
    }

    /**
     * 构建响应 VO
     */
    private ServiceExecutionRespVO buildRespVO(ServiceExecutionDO execution) {
        ServiceExecutionRespVO respVO = BeanUtils.toBean(execution, ServiceExecutionRespVO.class);

        // 获取项目信息
        ProjectDO project = projectMapper.selectById(execution.getProjectId());
        if (project != null) {
            respVO.setProjectName(project.getName());
        }

        // 获取服务项信息
        ServiceItemDO serviceItem = serviceItemMapper.selectById(execution.getServiceItemId());
        if (serviceItem != null) {
            respVO.setServiceItemName(serviceItem.getName());
            respVO.setServiceType(serviceItem.getServiceType());
            respVO.setDeptType(serviceItem.getDeptType());
            respVO.setCustomerName(serviceItem.getCustomerName());
            respVO.setContractNo(serviceItem.getContractNo());
        }

        // 获取发起人信息
        if (execution.getRequestUserId() != null) {
            AdminUserRespDTO user = adminUserApi.getUser(execution.getRequestUserId());
            if (user != null) {
                respVO.setRequestUserName(user.getNickname());
            }
        }

        // 获取发起人部门信息
        if (execution.getRequestDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(execution.getRequestDeptId());
            if (dept != null) {
                respVO.setRequestDeptName(dept.getName());
            }
        }

        // 解析执行人ID列表
        if (execution.getExecutorIds() != null && !execution.getExecutorIds().isEmpty()) {
            try {
                respVO.setExecutorIds(JSONUtil.toList(execution.getExecutorIds(), Long.class));
            } catch (Exception e) {
                log.warn("解析执行人ID列表失败: {}", execution.getExecutorIds());
            }
        }

        // 解析渗透测试附件URL列表
        if (execution.getAuthorizationUrls() != null && !execution.getAuthorizationUrls().isEmpty()) {
            try {
                respVO.setAuthorizationUrls(JSONUtil.toList(execution.getAuthorizationUrls(), String.class));
            } catch (Exception e) {
                log.warn("解析授权书URL列表失败: {}", execution.getAuthorizationUrls());
            }
        }
        if (execution.getTestScopeUrls() != null && !execution.getTestScopeUrls().isEmpty()) {
            try {
                respVO.setTestScopeUrls(JSONUtil.toList(execution.getTestScopeUrls(), String.class));
            } catch (Exception e) {
                log.warn("解析测试范围URL列表失败: {}", execution.getTestScopeUrls());
            }
        }
        if (execution.getCredentialsUrls() != null && !execution.getCredentialsUrls().isEmpty()) {
            try {
                respVO.setCredentialsUrls(JSONUtil.toList(execution.getCredentialsUrls(), String.class));
            } catch (Exception e) {
                log.warn("解析账号密码URL列表失败: {}", execution.getCredentialsUrls());
            }
        }

        return respVO;
    }

}
