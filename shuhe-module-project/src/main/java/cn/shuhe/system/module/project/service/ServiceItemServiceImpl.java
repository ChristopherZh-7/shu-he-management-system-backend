package cn.shuhe.system.module.project.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemBatchSaveReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemImportExcelVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemImportRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptServiceDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectDeptServiceMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.system.service.dept.DeptService;
import cn.shuhe.system.framework.datapermission.core.util.DataPermissionUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 服务项 Service 实现类
 */
@Service
@Validated
@Slf4j
public class ServiceItemServiceImpl implements ServiceItemService {

    /** 频次类型：按需（不限制） */
    private static final int FREQUENCY_TYPE_ON_DEMAND = 0;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private ProjectDeptServiceMapper projectDeptServiceMapper;

    @Resource
    private cn.shuhe.system.module.project.dal.mysql.ProjectMapper projectMapper;

    @Resource
    private cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper projectRoundMapper;

    @Resource
    private cn.shuhe.system.module.project.dal.mysql.ContractTimeMapper contractTimeMapper;

    @Resource
    @org.springframework.context.annotation.Lazy // 延迟加载，避免与 ProjectRoundServiceImpl 循环依赖
    private ProjectRoundService projectRoundService;

    @Resource
    private DeptService deptService;

    @Resource
    @org.springframework.context.annotation.Lazy
    private ProjectService projectService;

    @Resource
    private cn.shuhe.system.module.system.api.user.AdminUserApi adminUserApi;

    @Resource
    private cn.shuhe.system.module.system.api.dept.DeptApi deptApi;

    @Resource
    private cn.shuhe.system.module.system.api.dict.DictDataApi dictDataApi;

    /**
     * 部门类型 → 服务类型字典 type 映射（与前端 DEPT_TYPE_SERVICE_MAP 保持一致）。
     */
    private static final java.util.Map<Integer, String> DEPT_TYPE_SERVICE_DICT_MAP =
            java.util.Map.of(
                    1, "project_service_type_security",
                    2, "project_service_type_operation",
                    3, "project_service_type_data");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createServiceItem(ServiceItemSaveReqVO createReqVO) {
        // 1. 从项目获取客户和合同信息
        cn.shuhe.system.module.project.dal.dataobject.ProjectDO project = null;
        if (createReqVO.getProjectId() != null) {
            project = projectMapper.selectById(createReqVO.getProjectId());
            if (project == null) {
                throw exception(PROJECT_NOT_EXISTS);
            }
        }

        // 2. 生成服务项编号
        String code = generateServiceItemCode(createReqVO.getDeptType());

        // 3. 转换并保存
        ServiceItemDO serviceItem = BeanUtils.toBean(createReqVO, ServiceItemDO.class);
        serviceItem.setCode(code);
        serviceItem.setProgress(0); // 初始进度为0
        if (serviceItem.getStatus() == null) {
            serviceItem.setStatus(0); // 默认草稿状态
        }
        // 默认可见，除非明确指定为隐藏
        if (serviceItem.getVisible() == null) {
            serviceItem.setVisible(1);
        }
        normalizeDeliveryType(serviceItem);
        bindToDeptService(serviceItem);
        
        // 4. 自动从项目获取客户和合同信息
        if (project != null) {
            serviceItem.setCustomerId(project.getCustomerId());
            serviceItem.setCustomerName(project.getCustomerName());
            serviceItem.setContractId(project.getContractId());
            serviceItem.setContractNo(project.getContractNo());
            
            // 5. 自动从合同获取计划开始/结束时间
            if (project.getContractId() != null) {
                java.util.Map<String, LocalDateTime> contractTime = contractTimeMapper.selectContractTime(project.getContractId());
                if (contractTime != null) {
                    serviceItem.setPlanStartTime(contractTime.get("startTime"));
                    serviceItem.setPlanEndTime(contractTime.get("endTime"));
                }
            }
        }
        
        // 处理标签
        if (createReqVO.getTags() != null && !createReqVO.getTags().isEmpty()) {
            serviceItem.setTags(JSONUtil.toJsonStr(createReqVO.getTags()));
        }

        serviceItemMapper.insert(serviceItem);

        // 自动将服务项对应 deptType 的部门负责人加入项目成员
        autoAddDeptLeaderToProject(serviceItem.getProjectId(), serviceItem.getDeptType());

        return serviceItem.getId();
    }

    /**
     * 自动将指定 deptType 的大部门负责人加入项目成员（roleType=3 审核人员）
     * 解决跨部门场景：PM在A部门链上，但为B部门创建服务项时，B部门主管需要能看到项目
     */
    private void autoAddDeptLeaderToProject(Long projectId, Integer deptType) {
        if (projectId == null || deptType == null) {
            return;
        }
        try {
            // 绕过数据权限查找部门（跨部门查询不应受当前用户权限限制）
            cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO dept =
                    DataPermissionUtils.executeIgnore(() -> deptService.getDeptByDeptType(deptType));
            if (dept == null || dept.getLeaderUserId() == null) {
                return;
            }
            Long leaderUserId = dept.getLeaderUserId();
            // 检查是否已是项目成员（避免重复添加或覆盖更高权限角色）
            Integer existingRole = projectService.getUserRoleInProject(projectId, leaderUserId);
            if (existingRole != null) {
                return;
            }
            cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO leader = adminUserApi.getUser(leaderUserId);
            String leaderName = leader != null ? leader.getNickname() : "";
            projectService.addProjectMember(projectId, leaderUserId, leaderName, 3);
            log.info("[autoAddDeptLeaderToProject] 已将 deptType={} 的部门负责人 {} ({}) 加入项目 {} 成员",
                    deptType, leaderUserId, leaderName, projectId);
        } catch (Exception e) {
            log.warn("[autoAddDeptLeaderToProject] 自动添加部门负责人失败: projectId={}, deptType={}", projectId, deptType, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateServiceItem(ServiceItemSaveReqVO updateReqVO) {
        // 1. 校验存在
        ServiceItemDO existing = validateServiceItemExists(updateReqVO.getId());

        // 2. 更新
        ServiceItemDO updateObj = BeanUtils.toBean(updateReqVO, ServiceItemDO.class);
        updateObj.setProjectId(existing.getProjectId());
        normalizeDeliveryType(updateObj);
        bindToDeptService(updateObj);
        if (updateReqVO.getTags() != null) {
            updateObj.setTags(JSONUtil.toJsonStr(updateReqVO.getTags()));
        }
        serviceItemMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteServiceItem(Long id) {
        // 1. 校验存在
        validateServiceItemExists(id);

        // 2. 级联删除关联的轮次
        List<cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO> rounds = 
                projectRoundMapper.selectListByServiceItemId(id);
        if (rounds != null && !rounds.isEmpty()) {
            for (cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO round : rounds) {
                projectRoundService.deleteProjectRound(round.getId());
            }
            log.info("【删除服务项】服务项 {} 关联的 {} 个轮次已删除", id, rounds.size());
        }

        // 4. 删除服务项
        serviceItemMapper.deleteById(id);
    }

    @Override
    public ServiceItemDO getServiceItem(Long id) {
        return serviceItemMapper.selectById(id);
    }

    @Override
    public PageResult<ServiceItemDO> getServiceItemPage(ServiceItemPageReqVO pageReqVO) {
        return serviceItemMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ServiceItemDO> getServiceItemListByProjectId(Long projectId) {
        return serviceItemMapper.selectListByProjectId(projectId);
    }

    @Override
    public List<ServiceItemDO> getServiceItemListByProjectIdAndDeptId(Long projectId, Long deptId) {
        return serviceItemMapper.selectListByProjectIdAndDeptId(projectId, deptId);
    }

    @Override
    public List<ServiceItemDO> getServiceItemListByProjectIdAndDeptIds(Long projectId, List<Long> deptIds) {
        return serviceItemMapper.selectListByProjectIdAndDeptIds(projectId, deptIds);
    }

    @Override
    public List<ServiceItemDO> getServiceItemListByProjectIdAndServiceMode(Long projectId, Integer serviceMode) {
        return serviceItemMapper.selectListByProjectIdAndServiceMode(projectId, serviceMode);
    }

    @Override
    public List<ServiceItemDO> getServiceItemListByProjectIdAndDeptType(Long projectId, Integer deptType) {
        return serviceItemMapper.selectListByProjectIdAndDeptType(projectId, deptType);
    }

    @Override
    public List<ServiceItemDO> getServiceItemListByProjectIdAndDeptTypeAndMemberType(Long projectId, Integer deptType, Integer serviceMemberType) {
        return serviceItemMapper.selectListByProjectIdAndDeptTypeAndMemberType(projectId, deptType, serviceMemberType);
    }

    @Override
    public void updateServiceItemStatus(Long id, Integer status) {
        // 1. 校验存在
        ServiceItemDO serviceItem = validateServiceItemExists(id);

        // 2. 更新状态
        ServiceItemDO updateObj = new ServiceItemDO();
        updateObj.setId(id);
        updateObj.setStatus(status);

        // 如果状态变为进行中，且实际开始时间为空，则设置实际开始时间
        if (status == 1 && serviceItem.getActualStartTime() == null) {
            updateObj.setActualStartTime(LocalDateTime.now());
        }
        // 如果状态变为已完成，则设置实际结束时间和进度100%
        if (status == 3) {
            updateObj.setActualEndTime(LocalDateTime.now());
            updateObj.setProgress(100);
        }

        serviceItemMapper.updateById(updateObj);
    }

    @Override
    public void updateServiceItemProgress(Long id, Integer progress) {
        // 1. 校验存在
        validateServiceItemExists(id);

        // 2. 更新进度
        ServiceItemDO updateObj = new ServiceItemDO();
        updateObj.setId(id);
        updateObj.setProgress(progress);
        serviceItemMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignServiceItemToDept(Long id, Long deptId) {
        // 1. 校验服务项存在
        ServiceItemDO serviceItem = validateServiceItemExists(id);

        // 2. 更新 deptId
        ServiceItemDO updateObj = new ServiceItemDO();
        updateObj.setId(id);
        updateObj.setDeptId(deptId);
        serviceItemMapper.updateById(updateObj);
        log.info("[assignServiceItemToDept] 服务项 {} 已分配到部门 {}", id, deptId);

        // 3. 将目标部门的负责人加入项目成员（roleType=2 执行人员）
        if (serviceItem.getProjectId() != null) {
            cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO dept = deptApi.getDept(deptId);
            if (dept != null && dept.getLeaderUserId() != null) {
                Long leaderUserId = dept.getLeaderUserId();
                cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO leader = adminUserApi.getUser(leaderUserId);
                String leaderName = leader != null ? leader.getNickname() : "";
                try {
                    projectService.addProjectMember(serviceItem.getProjectId(), leaderUserId, leaderName, 2);
                    log.info("[assignServiceItemToDept] 已将部门 {} 负责人 {} ({}) 加入项目 {} 成员",
                            deptId, leaderUserId, leaderName, serviceItem.getProjectId());
                } catch (Exception e) {
                    log.warn("[assignServiceItemToDept] 添加部门负责人为项目成员失败: deptId={}, leaderUserId={}",
                            deptId, leaderUserId, e);
                }
            }
        }
    }

    private void bindToDeptService(ServiceItemDO serviceItem) {
        if (serviceItem.getProjectId() == null || serviceItem.getDeptType() == null) {
            throw exception(PROJECT_DEPT_SERVICE_NOT_EXISTS);
        }
        ProjectDeptServiceDO deptService = DataPermissionUtils.executeIgnore(() ->
                projectDeptServiceMapper.selectByProjectIdAndDeptType(
                        serviceItem.getProjectId(), serviceItem.getDeptType()));
        if (deptService == null) {
            throw exception(PROJECT_DEPT_SERVICE_NOT_EXISTS);
        }
        if (serviceItem.getDeptServiceId() != null
                && !serviceItem.getDeptServiceId().equals(deptService.getId())) {
            throw exception(PROJECT_DEPT_SERVICE_NOT_EXISTS);
        }
        serviceItem.setDeptServiceId(deptService.getId());
        if (serviceItem.getDeptId() == null) {
            serviceItem.setDeptId(deptService.getDeptId());
        }
    }

    /**
     * 新增和导入的历史入口可能未传交付类型。统一给出与旧逻辑一致的默认值，
     * 避免服务项长期处于“尚未配置”且无法区分驻场/二线。
     */
    private void normalizeDeliveryType(ServiceItemDO serviceItem) {
        if (Integer.valueOf(2).equals(serviceItem.getDeptType())) {
            if (serviceItem.getServiceMemberType() == null) {
                serviceItem.setServiceMemberType(ServiceItemDO.SERVICE_MEMBER_TYPE_MANAGEMENT);
            }
            return;
        }
        if (serviceItem.getServiceMode() == null) {
            serviceItem.setServiceMode(ServiceItemDO.SERVICE_MODE_REMOTE);
        }
    }

    /**
     * 校验服务项是否存在
     */
    private ServiceItemDO validateServiceItemExists(Long id) {
        ServiceItemDO serviceItem = serviceItemMapper.selectById(id);
        if (serviceItem == null) {
            throw exception(SERVICE_ITEM_NOT_EXISTS);
        }
        return serviceItem;
    }

    /**
     * 生成服务项编号
     * 格式：SVC-{部门类型}-{年月日}-{4位随机数}
     * 例如：SVC-1-20260116-0001
     */
    private String generateServiceItemCode(Integer deptType) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return StrUtil.format("SVC-{}-{}-{}", deptType, date, random);
    }

    /**
     * 把服务类型字典 value（英文）反查成 label（中文）。
     * 字典查询失败时静默返回原值，避免命名链路因外部依赖崩溃。
     */
    @Override
    public String resolveServiceTypeLabel(Integer deptType, String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String dictType = deptType == null ? null : DEPT_TYPE_SERVICE_DICT_MAP.get(deptType);
        if (StrUtil.isBlank(dictType)) {
            return value;
        }
        try {
            java.util.List<cn.shuhe.system.framework.common.biz.system.dict.dto.DictDataRespDTO> list =
                    dictDataApi.getDictDataList(dictType);
            if (list == null) {
                return value;
            }
            return list.stream()
                    .filter(d -> value.equals(d.getValue()))
                    .map(cn.shuhe.system.framework.common.biz.system.dict.dto.DictDataRespDTO::getLabel)
                    .filter(StrUtil::isNotBlank)
                    .findFirst()
                    .orElse(value);
        } catch (Exception ex) {
            log.warn("【服务项】字典查询失败 dictType={} value={}: {}", dictType, value, ex.getMessage());
            return value;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchCreateServiceItem(ServiceItemBatchSaveReqVO batchReqVO, Long deptId) {
        List<Long> createdIds = new ArrayList<>();

        for (ServiceItemSaveReqVO item : batchReqVO.getItems()) {
            // 设置公共字段
            item.setProjectId(batchReqVO.getProjectId());
            if (item.getDeptType() == null) {
                item.setDeptType(batchReqVO.getDeptType());
            }
            if (deptId != null) {
                item.setDeptId(deptId);
            }

            // 复用单条创建逻辑
            Long id = createServiceItem(item);
            createdIds.add(id);
        }

        return createdIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceItemImportRespVO importServiceItemList(Long projectId, Integer deptType,
            List<ServiceItemImportExcelVO> list, Long deptId) {
        int createCount = 0;
        Map<Integer, String> failureRecords = new LinkedHashMap<>();

        for (int i = 0; i < list.size(); i++) {
            ServiceItemImportExcelVO excelVO = list.get(i);
            int rowNum = i + 2; // Excel行号从2开始（第1行是表头）

            try {
                // 服务类型必填（用于前端展示与统计；服务项名称字段已废弃）
                if (StrUtil.isBlank(excelVO.getServiceType())) {
                    failureRecords.put(rowNum, "服务类型不能为空");
                    continue;
                }

                // 构建保存VO
                ServiceItemSaveReqVO saveReqVO = new ServiceItemSaveReqVO();
                saveReqVO.setProjectId(projectId);
                saveReqVO.setDeptType(deptType);
                saveReqVO.setDeptId(deptId);
                saveReqVO.setServiceType(excelVO.getServiceType());
                saveReqVO.setCustomerName(excelVO.getCustomerName());
                saveReqVO.setRemark(excelVO.getRemark());

                // 处理时间字段
                if (StrUtil.isNotBlank(excelVO.getPlanStartTime())) {
                    try {
                        saveReqVO.setPlanStartTime(LocalDateTime.parse(excelVO.getPlanStartTime(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    } catch (Exception e) {
                        log.warn("解析计划开始时间失败: {}", excelVO.getPlanStartTime());
                    }
                }
                if (StrUtil.isNotBlank(excelVO.getPlanEndTime())) {
                    try {
                        saveReqVO.setPlanEndTime(LocalDateTime.parse(excelVO.getPlanEndTime(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    } catch (Exception e) {
                        log.warn("解析计划结束时间失败: {}", excelVO.getPlanEndTime());
                    }
                }

                // 创建服务项
                createServiceItem(saveReqVO);
                createCount++;
            } catch (Exception e) {
                failureRecords.put(rowNum, e.getMessage());
                log.error("导入服务项失败，行号: {}, 原因: {}", rowNum, e.getMessage());
            }
        }

        return ServiceItemImportRespVO.builder()
                .createCount(createCount)
                .failureCount(failureRecords.size())
                .failureRecords(failureRecords)
                .build();
    }

    // ========== 服务执行次数管理 ==========

    @Override
    public boolean canStartExecution(Long serviceItemId) {
        ServiceItemDO serviceItem = validateServiceItemExists(serviceItemId);
        Integer frequencyType = serviceItem.getFrequencyType();
        
        // 按需(0)：不限制，始终可以执行
        if (frequencyType == null || frequencyType == FREQUENCY_TYPE_ON_DEMAND) {
            return true;
        }
        
        // 获取合同期内最大次数，默认1次
        int maxCount = serviceItem.getMaxCount() != null ? serviceItem.getMaxCount() : 1;
        
        // 检查该服务项已开始执行的轮次数量（状态为执行中或已完成）
        int executedCount = projectRoundMapper.selectStartedCountByServiceItemId(serviceItemId);
        
        if (executedCount >= maxCount) {
            log.info("【服务项执行限制】服务项 {} 已执行 {} 次，已达合同期内上限 {} 次",
                    serviceItemId, executedCount, maxCount);
            return false;
        }
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int incrementUsedCount(Long serviceItemId) {
        ServiceItemDO serviceItem = validateServiceItemExists(serviceItemId);
        
        // 检查当前周期是否可以执行
        if (!canStartExecution(serviceItemId)) {
            throw exception(SERVICE_ITEM_EXECUTION_LIMIT_EXCEEDED);
        }
        
        // 增加历史执行总次数（用于统计）
        int newUsedCount = (serviceItem.getUsedCount() != null ? serviceItem.getUsedCount() : 0) + 1;
        ServiceItemDO updateObj = new ServiceItemDO();
        updateObj.setId(serviceItemId);
        updateObj.setUsedCount(newUsedCount);
        serviceItemMapper.updateById(updateObj);
        
        log.info("【服务项执行】服务项 {} 历史执行总次数更新为 {}", serviceItemId, newUsedCount);
        return newUsedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int decrementUsedCount(Long serviceItemId) {
        ServiceItemDO serviceItem = validateServiceItemExists(serviceItemId);
        
        // 减少历史执行总次数（用于统计），最小为0
        int currentCount = serviceItem.getUsedCount() != null ? serviceItem.getUsedCount() : 0;
        int newUsedCount = Math.max(0, currentCount - 1);
        
        ServiceItemDO updateObj = new ServiceItemDO();
        updateObj.setId(serviceItemId);
        updateObj.setUsedCount(newUsedCount);
        serviceItemMapper.updateById(updateObj);
        
        log.info("【服务项执行】服务项 {} 历史执行总次数更新为 {}（减少）", serviceItemId, newUsedCount);
        return newUsedCount;
    }

    @Override
    public int getExecutedCount(Long serviceItemId) {
        // 直接从数据库查询已开始执行的轮次数量（状态为执行中1或已完成2）
        return projectRoundMapper.selectStartedCountByServiceItemId(serviceItemId);
    }

    @Override
    public int getRemainingCount(Long serviceItemId) {
        ServiceItemDO serviceItem = validateServiceItemExists(serviceItemId);
        Integer frequencyType = serviceItem.getFrequencyType();
        
        // 按需，不限
        if (frequencyType == null || frequencyType == FREQUENCY_TYPE_ON_DEMAND) {
            return -1;
        }
        
        int maxCount = serviceItem.getMaxCount() != null ? serviceItem.getMaxCount() : 1;
        int executedCount = projectRoundMapper.selectCountByServiceItemId(serviceItemId);
        return Math.max(0, maxCount - executedCount);
    }

    @Override
    public List<ServiceItemDO> getServiceItemListByContractId(Long contractId) {
        return serviceItemMapper.selectListByContractId(contractId);
    }

    @Override
    public void updateServiceItemVisible(Long id, Integer visible) {
        // 校验存在
        validateServiceItemExists(id);

        // 更新可见性
        ServiceItemDO updateObj = new ServiceItemDO();
        updateObj.setId(id);
        updateObj.setVisible(visible);
        serviceItemMapper.updateById(updateObj);

        log.info("【服务项】更新服务项可见性，id={}, visible={}", id, visible);
    }

    @Override
    public List<ServiceItemDO> getOutsideServiceItemListByDeptId(Long deptId) {
        return serviceItemMapper.selectOutsideServiceItemListByDeptId(deptId);
    }

    @Override
    public List<ServiceItemDO> getOutsideServiceItemListByProjectId(Long projectId) {
        return serviceItemMapper.selectOutsideServiceItemListByProjectId(projectId);
    }

}
