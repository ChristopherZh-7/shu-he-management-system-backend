package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceLaunchPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceLaunchRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceLaunchSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchMemberDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceLaunchMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceLaunchMemberMapper;
import cn.shuhe.system.module.system.api.cost.OutsideCostApi;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 统一服务发起 Service 实现
 */
@Service
@Validated
@Slf4j
public class ServiceLaunchServiceImpl implements ServiceLaunchService {

    @Resource
    private ServiceLaunchMapper serviceLaunchMapper;

    @Resource
    private ServiceLaunchMemberMapper serviceLaunchMemberMapper;

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

    @Resource
    private OutsideCostApi outsideCostApi;

    @Resource
    private ServiceItemService serviceItemService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createServiceLaunch(ServiceLaunchSaveReqVO createReqVO) {
        // 1. 校验服务项存在
        ServiceItemDO serviceItem = serviceItemMapper.selectById(createReqVO.getServiceItemId());
        if (serviceItem == null) {
            throw exception(SERVICE_ITEM_NOT_EXISTS);
        }

        // 1.1 校验服务项执行次数限制（在创建时就校验，避免审批通过后才发现无法执行）
        if (!serviceItemService.canStartExecution(createReqVO.getServiceItemId())) {
            throw exception(SERVICE_ITEM_EXECUTION_LIMIT_EXCEEDED);
        }

        // 2. 获取当前用户信息
        Long userId = getLoginUserId();
        AdminUserRespDTO user = adminUserApi.getUser(userId);

        // 3. 获取相关部门信息
        Long serviceItemDeptId = serviceItem.getDeptId();
        Long userDeptId = user != null ? user.getDeptId() : null;

        // 4. 跨部门判断使用前端传过来的值（前端考虑了部门层级关系，更准确）
        // 前端逻辑：请求方部门 vs 执行部门，同一条组织线上的父子部门不算跨部门
        Boolean isCrossDept = createReqVO.getIsCrossDept();

        // 5. 确定请求方部门：如果是代发起，使用代发起人的部门；否则使用当前用户的部门
        Long requestDeptId = userDeptId;
        if (Boolean.TRUE.equals(createReqVO.getIsDelegation()) && createReqVO.getDelegateUserId() != null) {
            AdminUserRespDTO delegateUser = adminUserApi.getUser(createReqVO.getDelegateUserId());
            if (delegateUser != null && delegateUser.getDeptId() != null) {
                requestDeptId = delegateUser.getDeptId();
                log.info("【统一服务发起】代发起模式，使用代发起人部门作为请求方部门。delegateUserId={}, requestDeptId={}", 
                        createReqVO.getDelegateUserId(), requestDeptId);
            }
        }

        // 6. 确定审批人（向上递归查找负责人）
        Long executeDeptId = createReqVO.getExecuteDeptId();
        Long approverDeptId = deptApi.findLeaderDeptIdRecursively(executeDeptId);
        Long approverUserId = deptApi.findLeaderUserIdRecursively(executeDeptId);
        
        // 判断是否需要在审批时选择执行的子部门
        // 如果审批人所在部门不是用户选择的执行部门（即审批人是父部门负责人），则需要选择
        boolean needSelectExecuteDept = approverDeptId != null && !approverDeptId.equals(executeDeptId);
        
        log.info("【统一服务发起】执行部门={}, 审批人部门={}, 审批人={}, 需要选择子部门={}",
                executeDeptId, approverDeptId, approverUserId, needSelectExecuteDept);

        // 7. 创建发起记录
        ServiceLaunchDO launch = BeanUtils.toBean(createReqVO, ServiceLaunchDO.class);
        launch.setProjectId(serviceItem.getProjectId());
        launch.setServiceItemDeptId(serviceItemDeptId);
        launch.setRequestUserId(userId);
        launch.setRequestDeptId(requestDeptId);
        launch.setIsCrossDept(Boolean.TRUE.equals(isCrossDept));
        launch.setStatus(0); // 待审批
        
        // 设置审批人相关信息
        launch.setApproverDeptId(approverDeptId);
        launch.setNeedSelectExecuteDept(needSelectExecuteDept);
        // 如果不需要选择子部门，实际执行部门就是用户选择的部门
        if (!needSelectExecuteDept) {
            launch.setActualExecuteDeptId(executeDeptId);
        }

        // 渗透测试附件（转换为JSON存储）
        if (createReqVO.getAuthorizationUrls() != null && !createReqVO.getAuthorizationUrls().isEmpty()) {
            launch.setAuthorizationUrls(JSONUtil.toJsonStr(createReqVO.getAuthorizationUrls()));
        }
        if (createReqVO.getTestScopeUrls() != null && !createReqVO.getTestScopeUrls().isEmpty()) {
            launch.setTestScopeUrls(JSONUtil.toJsonStr(createReqVO.getTestScopeUrls()));
        }
        if (createReqVO.getCredentialsUrls() != null && !createReqVO.getCredentialsUrls().isEmpty()) {
            launch.setCredentialsUrls(JSONUtil.toJsonStr(createReqVO.getCredentialsUrls()));
        }

        serviceLaunchMapper.insert(launch);

        return launch.getId();
    }

    @Override
    public void updateServiceLaunch(ServiceLaunchSaveReqVO updateReqVO) {
        validateServiceLaunchExists(updateReqVO.getId());
        ServiceLaunchDO updateObj = BeanUtils.toBean(updateReqVO, ServiceLaunchDO.class);
        serviceLaunchMapper.updateById(updateObj);
    }

    @Override
    public void updateProcessInstanceId(Long id, String processInstanceId) {
        ServiceLaunchDO updateObj = new ServiceLaunchDO();
        updateObj.setId(id);
        updateObj.setProcessInstanceId(processInstanceId);
        serviceLaunchMapper.updateById(updateObj);
    }

    @Override
    public void setExecutors(Long id, List<Long> executorIds) {
        ServiceLaunchDO launch = validateServiceLaunchExists(id);
        if (launch.getStatus() != 0) {
            throw exception(SERVICE_LAUNCH_STATUS_NOT_PENDING);
        }

        String executorNames = "";
        if (CollUtil.isNotEmpty(executorIds)) {
            List<AdminUserRespDTO> users = adminUserApi.getUserList(executorIds);
            executorNames = users.stream()
                    .map(AdminUserRespDTO::getNickname)
                    .collect(Collectors.joining(","));
        }

        ServiceLaunchDO updateObj = new ServiceLaunchDO();
        updateObj.setId(id);
        updateObj.setExecutorIds(JSONUtil.toJsonStr(executorIds));
        updateObj.setExecutorNames(executorNames);
        serviceLaunchMapper.updateById(updateObj);
    }

    @Override
    public void deleteServiceLaunch(Long id) {
        ServiceLaunchDO launch = validateServiceLaunchExists(id);
        if (launch.getStatus() != 0) {
            throw exception(SERVICE_LAUNCH_STATUS_NOT_PENDING);
        }
        serviceLaunchMapper.deleteById(id);
    }

    @Override
    public ServiceLaunchDO getServiceLaunch(Long id) {
        return serviceLaunchMapper.selectById(id);
    }

    @Override
    public ServiceLaunchRespVO getServiceLaunchDetail(Long id) {
        ServiceLaunchDO launch = serviceLaunchMapper.selectById(id);
        if (launch == null) {
            return null;
        }
        return buildRespVO(launch);
    }

    @Override
    public PageResult<ServiceLaunchRespVO> getServiceLaunchPage(ServiceLaunchPageReqVO pageReqVO) {
        PageResult<ServiceLaunchDO> pageResult = serviceLaunchMapper.selectPage(pageReqVO);
        return new PageResult<>(
                pageResult.getList().stream().map(this::buildRespVO).collect(Collectors.toList()),
                pageResult.getTotal()
        );
    }

    @Override
    public PageResult<ServiceLaunchRespVO> getMyServiceLaunchPage(ServiceLaunchPageReqVO pageReqVO) {
        Long userId = getLoginUserId();
        PageResult<ServiceLaunchDO> pageResult = serviceLaunchMapper.selectPageByUserId(pageReqVO, userId);
        return new PageResult<>(
                pageResult.getList().stream().map(this::buildRespVO).collect(Collectors.toList()),
                pageResult.getTotal()
        );
    }

    @Override
    public List<Map<String, Object>> getContractListForLaunch() {
        // 获取当前用户
        Long userId = getLoginUserId();
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            return Collections.emptyList();
        }

        // 查询有关联合同的项目列表（通过ProjectDO获取合同信息，不依赖CRM模块）
        List<ProjectDO> projects = projectMapper.selectList();

        // 去重：按合同ID去重
        Map<Long, Map<String, Object>> contractMap = new LinkedHashMap<>();
        for (ProjectDO project : projects) {
            if (project.getContractId() != null && !contractMap.containsKey(project.getContractId())) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", project.getContractId());
                map.put("no", project.getContractNo() != null ? project.getContractNo() : "");
                map.put("name", project.getName()); // 使用项目名称
                map.put("customerName", project.getCustomerName() != null ? project.getCustomerName() : "");
                contractMap.put(project.getContractId(), map);
            }
        }

        return new ArrayList<>(contractMap.values());
    }

    @Override
    public List<Map<String, Object>> getServiceItemListByContract(Long contractId) {
        // 根据合同找到项目
        ProjectDO project = projectMapper.selectByContractId(contractId);
        if (project == null) {
            return Collections.emptyList();
        }

        // 获取项目下所有服务项（包含所有可见服务项）
        List<ServiceItemDO> serviceItems = serviceItemMapper.selectListByProjectId(project.getId());

        return serviceItems.stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == 1) // 进行中
                .filter(item -> !"outside".equals(item.getServiceType())) // 排除纯外出类型（因为已合并）
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", item.getId());
                    map.put("projectId", item.getProjectId());
                    map.put("name", item.getName());
                    map.put("serviceType", item.getServiceType());
                    map.put("deptType", item.getDeptType());
                    map.put("deptId", item.getDeptId());
                    map.put("status", item.getStatus());
                    map.put("customerName", item.getCustomerName());
                    map.put("contractNo", item.getContractNo());
                    map.put("frequencyType", item.getFrequencyType());
                    map.put("maxCount", item.getMaxCount());
                    map.put("usedCount", item.getUsedCount());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getDeptList() {
        // 获取所有安全类型部门的叶子部门（deptType = 1, 2, 3）
        // 叶子部门：没有子部门的部门，用户只能选择叶子部门
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 分别获取三种类型的叶子部门
        for (int deptType = 1; deptType <= 3; deptType++) {
            List<cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO> leafDepts = deptApi.getLeafDeptListByDeptType(deptType);
            if (leafDepts != null) {
                for (cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO dept : leafDepts) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", dept.getId());
                    map.put("name", dept.getName());
                    map.put("parentId", dept.getParentId());
                    map.put("deptType", deptType);
                    
                    // 向上递归查找负责人（当前部门没有则找父部门）
                    Long leaderUserId = deptApi.findLeaderUserIdRecursively(dept.getId());
                    Long leaderDeptId = deptApi.findLeaderDeptIdRecursively(dept.getId());
                    
                    map.put("leaderUserId", leaderUserId);
                    map.put("leaderDeptId", leaderDeptId);
                    // 标记审批人是否来自父部门（需要在审批时先选子部门）
                    map.put("isLeaderFromParent", leaderDeptId != null && !leaderDeptId.equals(dept.getId()));
                    
                    // 获取负责人名称
                    if (leaderUserId != null) {
                        cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO leader = adminUserApi.getUser(leaderUserId);
                        map.put("leaderUserName", leader != null ? leader.getNickname() : null);
                    }
                    
                    // 获取审批人所在部门名称
                    if (leaderDeptId != null) {
                        cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO leaderDept = deptApi.getDept(leaderDeptId);
                        map.put("leaderDeptName", leaderDept != null ? leaderDept.getName() : null);
                    }
                    
                    result.add(map);
                }
            }
        }
        
        log.info("【获取执行部门列表】返回{}个叶子部门, 详情: {}", result.size(), result);
        return result;
    }
    
    /**
     * 获取所有部门列表（包含非叶子部门，用于部门层级判断）
     */
    public List<Map<String, Object>> getAllDeptList() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (int deptType = 1; deptType <= 3; deptType++) {
            List<cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO> depts = deptApi.getDeptListByDeptType(deptType);
            if (depts != null) {
                for (cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO dept : depts) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", dept.getId());
                    map.put("name", dept.getName());
                    map.put("parentId", dept.getParentId());
                    map.put("deptType", deptType);
                    map.put("leaderUserId", dept.getLeaderUserId());
                    
                    if (dept.getLeaderUserId() != null) {
                        cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO leader = adminUserApi.getUser(dept.getLeaderUserId());
                        map.put("leaderUserName", leader != null ? leader.getNickname() : null);
                    }
                    
                    result.add(map);
                }
            }
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getUserListByDept(Long deptId) {
        // 获取指定部门及其子部门的所有用户
        // 用于审批页面"选择执行人"功能
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (deptId == null) {
            return result;
        }
        
        // 收集部门及其所有子部门ID
        Set<Long> allDeptIds = new HashSet<>();
        allDeptIds.add(deptId);
        
        // 获取子部门列表
        List<cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO> childDepts = deptApi.getChildDeptList(deptId);
        if (childDepts != null && !childDepts.isEmpty()) {
            for (cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO childDept : childDepts) {
                allDeptIds.add(childDept.getId());
            }
        }
        
        log.info("【获取部门用户】deptId={}, 包含子部门共{}个部门", deptId, allDeptIds.size());
        
        // 获取所有部门的用户
        List<cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO> users = adminUserApi.getUserListByDeptIds(allDeptIds);
        log.info("【获取部门用户】找到{}个用户", users != null ? users.size() : 0);
        
        if (users != null && !users.isEmpty()) {
            // 构建部门ID到名称的映射
            Map<Long, String> deptNameMap = new HashMap<>();
            cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO mainDept = deptApi.getDept(deptId);
            if (mainDept != null) {
                deptNameMap.put(deptId, mainDept.getName());
            }
            if (childDepts != null) {
                for (cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO childDept : childDepts) {
                    deptNameMap.put(childDept.getId(), childDept.getName());
                }
            }
            
            for (cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO user : users) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", user.getId());
                map.put("nickname", user.getNickname());
                map.put("deptId", user.getDeptId());
                map.put("deptName", deptNameMap.getOrDefault(user.getDeptId(), ""));
                
                result.add(map);
            }
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getDeptLeaderList() {
        // 获取所有安全部门（deptType = 1, 2, 3）的负责人列表
        // 用于"代他人发起"功能，允许选择任意部门的管理人员
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 使用Set去重（同一个人可能负责多个部门）
        Set<Long> addedUserIds = new HashSet<>();
        
        // 分别获取三种类型的部门
        for (int deptType = 1; deptType <= 3; deptType++) {
            List<cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO> depts = deptApi.getDeptListByDeptType(deptType);
            if (depts != null) {
                for (cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO dept : depts) {
                    // 获取部门负责人
                    if (dept.getLeaderUserId() != null && !addedUserIds.contains(dept.getLeaderUserId())) {
                        cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO leader = adminUserApi.getUser(dept.getLeaderUserId());
                        if (leader != null) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", leader.getId());
                            map.put("nickname", leader.getNickname());
                            map.put("deptId", dept.getId());
                            map.put("deptName", dept.getName());
                            
                            result.add(map);
                            addedUserIds.add(leader.getId());
                        }
                    }
                }
            }
        }
        
        log.info("【获取部门管理人员】找到{}个部门负责人", result.size());
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long handleApproved(Long id, List<Long> executorUserIds) {
        ServiceLaunchDO launch = serviceLaunchMapper.selectById(id);
        if (launch == null) {
            log.warn("【统一服务发起】记录不存在，id={}", id);
            return null;
        }

        // 再次校验服务项执行次数限制（处理并发情况：多个人同时发起同一个服务项的审批）
        if (!serviceItemService.canStartExecution(launch.getServiceItemId())) {
            log.warn("【统一服务发起】服务项 {} 执行次数已达上限，审批无法通过。launchId={}", 
                    launch.getServiceItemId(), id);
            // 更新状态为已拒绝（状态2）- 因为执行次数超限，系统自动拒绝
            updateStatus(id, 2);
            return null;
        }

        // 如果传入了执行人员，先设置执行人员（必须在更新状态之前，因为setExecutors会检查状态）
        if (executorUserIds != null && !executorUserIds.isEmpty()) {
            setExecutors(id, executorUserIds);
            // 重新读取更新后的记录
            launch = serviceLaunchMapper.selectById(id);
        }

        // 更新状态为已通过
        updateStatus(id, 1);

        // 创建执行轮次（带外出和跨部门标识）- 此时已经校验过执行次数，应该不会再抛异常
        Long roundId = projectRoundService.createRoundByServiceItem(
                launch.getServiceItemId(),
                launch.getProcessInstanceId(),
                launch.getPlanStartTime(),
                launch.getPlanEndTime(),
                launch.getIsOutside(),
                launch.getIsCrossDept(),
                launch.getId()
        );

        // 设置轮次执行人
        if (launch.getExecutorIds() != null && !launch.getExecutorIds().isEmpty()) {
            try {
                List<Long> executorIds = JSONUtil.toList(launch.getExecutorIds(), Long.class);
                projectRoundService.setExecutors(roundId, executorIds);
            } catch (Exception e) {
                log.error("【统一服务发起】设置轮次执行人失败: {}", e.getMessage(), e);
            }
        }

        // 更新记录的轮次ID
        ServiceLaunchDO updateObj = new ServiceLaunchDO();
        updateObj.setId(id);
        updateObj.setRoundId(roundId);
        serviceLaunchMapper.updateById(updateObj);

        // 如果是跨部门，创建跨部门费用记录
        // 使用实际执行部门（如果已选择）或原执行部门
        Long effectiveExecuteDeptId = launch.getActualExecuteDeptId() != null 
                ? launch.getActualExecuteDeptId() 
                : launch.getExecuteDeptId();
        if (Boolean.TRUE.equals(launch.getIsCrossDept())) {
            log.info("【统一服务发起】跨部门服务，创建费用记录。launchId={}, serviceItemDeptId={}, effectiveExecuteDeptId={}",
                    id, launch.getServiceItemDeptId(), effectiveExecuteDeptId);
            try {
                Long costRecordId = outsideCostApi.createCostRecordByServiceLaunch(id);
                if (costRecordId != null) {
                    log.info("【统一服务发起】跨部门费用记录创建成功。launchId={}, costRecordId={}", id, costRecordId);
                }
            } catch (Exception e) {
                log.error("【统一服务发起】创建跨部门费用记录失败: {}", e.getMessage(), e);
                // 不影响主流程，继续执行
            }
        }

        log.info("【统一服务发起】审批通过，已创建轮次。launchId={}, roundId={}", id, roundId);
        return roundId;
    }

    @Override
    public void handleRejected(Long id) {
        updateStatus(id, 2);
    }

    @Override
    public void handleCancelled(Long id) {
        updateStatus(id, 3);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long handleApprovedWithDept(Long id, Long actualExecuteDeptId, List<Long> executorUserIds) {
        ServiceLaunchDO launch = serviceLaunchMapper.selectById(id);
        if (launch == null) {
            log.warn("【统一服务发起】记录不存在，id={}", id);
            return null;
        }

        // 如果需要选择子部门，必须传入实际执行部门
        if (Boolean.TRUE.equals(launch.getNeedSelectExecuteDept()) && actualExecuteDeptId == null) {
            log.warn("【统一服务发起】需要选择执行子部门但未传入，launchId={}", id);
            throw exception(SERVICE_LAUNCH_NEED_SELECT_DEPT);
        }

        // 设置实际执行部门
        if (actualExecuteDeptId != null) {
            setActualExecuteDept(id, actualExecuteDeptId);
        }

        // 调用原有的审批通过逻辑
        return handleApproved(id, executorUserIds);
    }

    @Override
    public void setActualExecuteDept(Long id, Long actualExecuteDeptId) {
        ServiceLaunchDO launch = validateServiceLaunchExists(id);
        if (launch.getStatus() != 0) {
            throw exception(SERVICE_LAUNCH_STATUS_NOT_PENDING);
        }

        // 获取部门名称
        String deptName = "";
        if (actualExecuteDeptId != null) {
            DeptRespDTO dept = deptApi.getDept(actualExecuteDeptId);
            deptName = dept != null ? dept.getName() : "";
        }

        ServiceLaunchDO updateObj = new ServiceLaunchDO();
        updateObj.setId(id);
        updateObj.setActualExecuteDeptId(actualExecuteDeptId);
        serviceLaunchMapper.updateById(updateObj);
        
        log.info("【统一服务发起】设置实际执行部门，launchId={}, actualExecuteDeptId={}, deptName={}", 
                id, actualExecuteDeptId, deptName);
    }

    @Override
    public List<Map<String, Object>> getChildDeptList(Long parentDeptId) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (parentDeptId == null) {
            return result;
        }

        // 获取所有子部门（包括孙子部门）
        List<DeptRespDTO> allChildDepts = deptApi.getChildDeptList(parentDeptId);
        if (allChildDepts == null || allChildDepts.isEmpty()) {
            return result;
        }
        
        // 只返回直接子部门（parentId == parentDeptId）
        for (DeptRespDTO dept : allChildDepts) {
            if (parentDeptId.equals(dept.getParentId())) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", dept.getId());
                map.put("name", dept.getName());
                map.put("parentId", dept.getParentId());
                map.put("leaderUserId", dept.getLeaderUserId());
                
                // 获取负责人名称
                if (dept.getLeaderUserId() != null) {
                    AdminUserRespDTO leader = adminUserApi.getUser(dept.getLeaderUserId());
                    map.put("leaderUserName", leader != null ? leader.getNickname() : null);
                }
                
                result.add(map);
            }
        }
        
        log.info("【获取子部门列表】parentDeptId={}, 总子部门{}个, 直接子部门{}个", 
                parentDeptId, allChildDepts.size(), result.size());
        return result;
    }

    private void updateStatus(Long id, Integer status) {
        ServiceLaunchDO updateObj = new ServiceLaunchDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        serviceLaunchMapper.updateById(updateObj);
    }

    private ServiceLaunchDO validateServiceLaunchExists(Long id) {
        ServiceLaunchDO launch = serviceLaunchMapper.selectById(id);
        if (launch == null) {
            throw exception(SERVICE_LAUNCH_NOT_EXISTS);
        }
        return launch;
    }

    private ServiceLaunchRespVO buildRespVO(ServiceLaunchDO launch) {
        ServiceLaunchRespVO respVO = BeanUtils.toBean(launch, ServiceLaunchRespVO.class);

        // 获取项目信息（包含合同编号）
        if (launch.getProjectId() != null) {
            ProjectDO project = projectMapper.selectById(launch.getProjectId());
            if (project != null) {
                respVO.setProjectName(project.getName());
                respVO.setContractNo(project.getContractNo());
            }
        }

        // 获取服务项信息
        if (launch.getServiceItemId() != null) {
            ServiceItemDO serviceItem = serviceItemMapper.selectById(launch.getServiceItemId());
            if (serviceItem != null) {
                respVO.setServiceItemName(serviceItem.getName());
                respVO.setServiceType(serviceItem.getServiceType());
                respVO.setDeptType(serviceItem.getDeptType());
                respVO.setCustomerName(serviceItem.getCustomerName());
                // 如果项目没有合同编号，从服务项获取
                if (respVO.getContractNo() == null) {
                    respVO.setContractNo(serviceItem.getContractNo());
                }
            }
        }

        // 获取服务项归属部门名称
        if (launch.getServiceItemDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(launch.getServiceItemDeptId());
            if (dept != null) {
                respVO.setServiceItemDeptName(dept.getName());
            }
        }

        // 获取执行部门名称
        if (launch.getExecuteDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(launch.getExecuteDeptId());
            if (dept != null) {
                respVO.setExecuteDeptName(dept.getName());
            }
        }

        // 获取发起人信息
        if (launch.getRequestUserId() != null) {
            AdminUserRespDTO user = adminUserApi.getUser(launch.getRequestUserId());
            if (user != null) {
                respVO.setRequestUserName(user.getNickname());
            }
        }

        // 获取发起人部门信息
        if (launch.getRequestDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(launch.getRequestDeptId());
            if (dept != null) {
                respVO.setRequestDeptName(dept.getName());
            }
        }

        // 获取被代发起人信息
        if (launch.getDelegateUserId() != null) {
            AdminUserRespDTO delegateUser = adminUserApi.getUser(launch.getDelegateUserId());
            if (delegateUser != null) {
                respVO.setDelegateUserName(delegateUser.getNickname());
            }
        }

        // 解析执行人ID列表
        if (launch.getExecutorIds() != null && !launch.getExecutorIds().isEmpty()) {
            try {
                respVO.setExecutorIds(JSONUtil.toList(launch.getExecutorIds(), Long.class));
            } catch (Exception e) {
                log.warn("解析执行人ID列表失败: {}", launch.getExecutorIds());
            }
        }

        // 解析渗透测试附件URL列表
        if (launch.getAuthorizationUrls() != null && !launch.getAuthorizationUrls().isEmpty()) {
            try {
                respVO.setAuthorizationUrls(JSONUtil.toList(launch.getAuthorizationUrls(), String.class));
            } catch (Exception e) {
                log.warn("解析授权书URL列表失败: {}", launch.getAuthorizationUrls());
            }
        }
        if (launch.getTestScopeUrls() != null && !launch.getTestScopeUrls().isEmpty()) {
            try {
                respVO.setTestScopeUrls(JSONUtil.toList(launch.getTestScopeUrls(), String.class));
            } catch (Exception e) {
                log.warn("解析测试范围URL列表失败: {}", launch.getTestScopeUrls());
            }
        }
        if (launch.getCredentialsUrls() != null && !launch.getCredentialsUrls().isEmpty()) {
            try {
                respVO.setCredentialsUrls(JSONUtil.toList(launch.getCredentialsUrls(), String.class));
            } catch (Exception e) {
                log.warn("解析账号密码URL列表失败: {}", launch.getCredentialsUrls());
            }
        }

        // 获取审批人所在部门名称
        if (launch.getApproverDeptId() != null) {
            DeptRespDTO approverDept = deptApi.getDept(launch.getApproverDeptId());
            if (approverDept != null) {
                respVO.setApproverDeptName(approverDept.getName());
            }
        }

        // 获取实际执行部门名称
        if (launch.getActualExecuteDeptId() != null) {
            DeptRespDTO actualExecuteDept = deptApi.getDept(launch.getActualExecuteDeptId());
            if (actualExecuteDept != null) {
                respVO.setActualExecuteDeptName(actualExecuteDept.getName());
            }
        }

        return respVO;
    }

    // ==================== 执行人相关 ====================

    @Override
    public List<ServiceLaunchMemberDO> getLaunchMembers(Long launchId) {
        return serviceLaunchMemberMapper.selectListByLaunchId(launchId);
    }

    @Override
    public ServiceLaunchMemberDO getLaunchMember(Long memberId) {
        return serviceLaunchMemberMapper.selectById(memberId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishLaunchMember(Long memberId, Boolean hasAttachment, String attachmentUrl, String remark) {
        ServiceLaunchMemberDO member = serviceLaunchMemberMapper.selectById(memberId);
        if (member == null) {
            throw exception(SERVICE_LAUNCH_MEMBER_NOT_EXISTS);
        }

        // 验证是否是当前用户
        Long currentUserId = getLoginUserId();
        if (!member.getUserId().equals(currentUserId)) {
            throw exception(SERVICE_LAUNCH_MEMBER_NOT_SELF);
        }

        // 更新完成状态
        ServiceLaunchMemberDO updateObj = new ServiceLaunchMemberDO();
        updateObj.setId(memberId);
        updateObj.setFinishStatus(Boolean.TRUE.equals(hasAttachment) ? 2 : 1); // 1=无附件完成, 2=有附件完成
        updateObj.setFinishTime(java.time.LocalDateTime.now());
        if (attachmentUrl != null) {
            updateObj.setAttachmentUrl(attachmentUrl);
        }
        if (remark != null) {
            updateObj.setFinishRemark(remark);
        }
        serviceLaunchMemberMapper.updateById(updateObj);

        log.info("【服务发起】执行人完成任务，memberId={}, hasAttachment={}", memberId, hasAttachment);

        // 检查是否所有执行人都完成了
        checkAndUpdateLaunchStatus(member.getLaunchId());
    }

    /**
     * 检查并更新服务发起状态（所有执行人完成后更新为已完成）
     */
    private void checkAndUpdateLaunchStatus(Long launchId) {
        List<ServiceLaunchMemberDO> members = serviceLaunchMemberMapper.selectListByLaunchId(launchId);
        if (CollUtil.isEmpty(members)) {
            return;
        }

        // 检查是否所有人都完成
        boolean allFinished = members.stream()
                .allMatch(m -> m.getFinishStatus() != null && m.getFinishStatus() > 0);

        if (allFinished) {
            // 所有人都完成，更新服务发起状态为已完成
            ServiceLaunchDO updateObj = new ServiceLaunchDO();
            updateObj.setId(launchId);
            updateObj.setStatus(4); // 4=已完成（假设状态码）
            serviceLaunchMapper.updateById(updateObj);
            log.info("【服务发起】所有执行人已完成，更新服务发起状态为已完成。launchId={}", launchId);
        }
    }

    @Override
    public PageResult<ServiceLaunchRespVO> getOutsideServiceLaunchPage(ServiceLaunchPageReqVO pageReqVO) {
        // 设置外出筛选条件
        pageReqVO.setIsOutside(true);
        PageResult<ServiceLaunchDO> pageResult = serviceLaunchMapper.selectPage(pageReqVO);

        // 转换为 RespVO
        List<ServiceLaunchRespVO> list = pageResult.getList().stream()
                .map(this::buildRespVO)
                .collect(Collectors.toList());

        return new PageResult<>(list, pageResult.getTotal());
    }

}
