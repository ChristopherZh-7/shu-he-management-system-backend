package cn.shuhe.system.module.project.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemBatchSaveReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemDeptBudgetRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemImportExcelVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemImportRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptServiceDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectDeptServiceMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.system.service.dept.DeptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
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
    private cn.shuhe.system.module.project.dal.mysql.ProjectMapper projectMapper;

    @Resource
    private cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper projectRoundMapper;

    @Resource
    private cn.shuhe.system.module.project.dal.mysql.ContractTimeMapper contractTimeMapper;

    @Resource
    @org.springframework.context.annotation.Lazy // 延迟加载，避免与 ProjectRoundServiceImpl 循环依赖
    private ProjectRoundService projectRoundService;

    @Resource
    private ProjectDeptServiceMapper projectDeptServiceMapper;

    @Resource
    private DeptService deptService;

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
        
        // 6. 如果未提供名称，自动生成
        if (StrUtil.isBlank(serviceItem.getName())) {
            serviceItem.setName(generateServiceItemName(createReqVO, code));
        }
        // 处理标签
        if (createReqVO.getTags() != null && !createReqVO.getTags().isEmpty()) {
            serviceItem.setTags(JSONUtil.toJsonStr(createReqVO.getTags()));
        }

        // 7. 校验分配金额不超出对应资金池（新建时无排除ID）
        if (serviceItem.getAllocatedAmount() != null
                && serviceItem.getAllocatedAmount().compareTo(BigDecimal.ZERO) > 0
                && serviceItem.getDeptServiceId() != null) {
            validateAllocatedAmountNotExceedPool(
                    serviceItem.getDeptServiceId(),
                    serviceItem.getServiceMode(),
                    serviceItem.getServiceMemberType(),
                    serviceItem.getAllocatedAmount(),
                    null);
        }

        serviceItemMapper.insert(serviceItem);
        return serviceItem.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateServiceItem(ServiceItemSaveReqVO updateReqVO) {
        // 1. 校验存在
        ServiceItemDO existing = validateServiceItemExists(updateReqVO.getId());

        // 2. 校验分配金额不超出对应资金池（编辑时排除自身）
        if (updateReqVO.getAllocatedAmount() != null
                && updateReqVO.getAllocatedAmount().compareTo(BigDecimal.ZERO) > 0) {
            Long deptServiceId = existing.getDeptServiceId();
            Integer serviceMode = updateReqVO.getServiceMode() != null
                    ? updateReqVO.getServiceMode() : existing.getServiceMode();
            Integer memberType = updateReqVO.getServiceMemberType() != null
                    ? updateReqVO.getServiceMemberType() : existing.getServiceMemberType();
            if (deptServiceId != null) {
                validateAllocatedAmountNotExceedPool(
                        deptServiceId, serviceMode, memberType,
                        updateReqVO.getAllocatedAmount(), existing.getId());
            }
        }

        // 3. 更新
        ServiceItemDO updateObj = BeanUtils.toBean(updateReqVO, ServiceItemDO.class);
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
     * 校验分配金额不超出对应资金池（驻场池 or 二线/管理池）。
     *
     * @param deptServiceId   部门服务单ID
     * @param serviceMode     服务模式：1=驻场 2=二线（安全服务/数据安全）
     * @param memberType      成员类型：1=驻场 2=管理（安全运营）
     * @param newAmount       本次分配金额
     * @param excludeItemId   编辑时排除自身的服务项ID，新建传 null
     */
    private void validateAllocatedAmountNotExceedPool(Long deptServiceId, Integer serviceMode,
                                                       Integer memberType, BigDecimal newAmount,
                                                       Long excludeItemId) {
        ProjectDeptServiceDO pds = projectDeptServiceMapper.selectById(deptServiceId);
        if (pds == null) return;

        // 确定适用的资金池
        BigDecimal pool;
        Integer filterMode = null;
        Integer filterMemberType = null;
        if (serviceMode != null && serviceMode == 1) {
            pool = pds.getOnsiteBudget();
            filterMode = 1;
        } else if (serviceMode != null && serviceMode == 2) {
            pool = pds.getSecondLineBudget();
            filterMode = 2;
        } else if (memberType != null && memberType == 1) {
            pool = pds.getOnsiteBudget();
            filterMemberType = 1;
        } else if (memberType != null && memberType == 2) {
            pool = pds.getSecondLineBudget();
            filterMemberType = 2;
        } else {
            return; // 未指定服务模式，不校验
        }

        if (pool == null || pool.compareTo(BigDecimal.ZERO) <= 0) {
            return; // 资金池未设置，不校验
        }

        // 该池下已分配金额之和（排除自身）
        BigDecimal alreadyUsed = serviceItemMapper.sumAllocatedAmountByDeptServiceId(
                deptServiceId, filterMode, filterMemberType);
        if (alreadyUsed == null) alreadyUsed = BigDecimal.ZERO;

        // 编辑时减去自身当前已分配金额
        if (excludeItemId != null) {
            ServiceItemDO self = serviceItemMapper.selectById(excludeItemId);
            if (self != null && self.getAllocatedAmount() != null) {
                alreadyUsed = alreadyUsed.subtract(self.getAllocatedAmount()).max(BigDecimal.ZERO);
            }
        }

        if (alreadyUsed.add(newAmount).compareTo(pool) > 0) {
            BigDecimal remaining = pool.subtract(alreadyUsed).max(BigDecimal.ZERO);
            throw exception(SERVICE_ITEM_BUDGET_EXCEEDED,
                    alreadyUsed.toPlainString(), pool.toPlainString(), remaining.toPlainString());
        }
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
     * 自动生成服务项名称
     * 格式：{客户名称}-{服务类型}-{编号后4位} 或 服务项-{编号后4位}
     */
    private String generateServiceItemName(ServiceItemSaveReqVO reqVO, String code) {
        StringBuilder name = new StringBuilder();
        // 添加客户名称
        if (StrUtil.isNotBlank(reqVO.getCustomerName())) {
            name.append(reqVO.getCustomerName()).append("-");
        }
        // 添加服务类型
        if (StrUtil.isNotBlank(reqVO.getServiceType())) {
            name.append(reqVO.getServiceType()).append("-");
        }
        // 如果没有任何信息，使用默认前缀
        if (name.isEmpty()) {
            name.append("服务项-");
        }
        // 添加编号后4位作为唯一标识
        name.append(code.substring(code.length() - 4));
        return name.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchCreateServiceItem(ServiceItemBatchSaveReqVO batchReqVO, Long deptId) {
        List<Long> createdIds = new ArrayList<>();

        for (ServiceItemSaveReqVO item : batchReqVO.getItems()) {
            // 设置公共字段
            item.setProjectId(batchReqVO.getProjectId());
            item.setDeptType(batchReqVO.getDeptType());
            item.setDeptId(deptId);

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
                // 校验必填字段
                if (StrUtil.isBlank(excelVO.getName())) {
                    failureRecords.put(rowNum, "服务项名称不能为空");
                    continue;
                }

                // 构建保存VO
                ServiceItemSaveReqVO saveReqVO = new ServiceItemSaveReqVO();
                saveReqVO.setProjectId(projectId);
                saveReqVO.setDeptType(deptType);
                saveReqVO.setDeptId(deptId);
                saveReqVO.setName(excelVO.getName());
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

    // ========== 资金池预算查询 ==========

    @Override
    public ServiceItemDeptBudgetRespVO getDeptBudget(Long contractId, Long deptId) {
        ServiceItemDeptBudgetRespVO result = new ServiceItemDeptBudgetRespVO();
        result.setContractId(contractId);
        result.setDeptId(deptId);

        // 从 project_dept_service 查询预算（新的数据源）
        ProjectDeptServiceDO deptService = projectDeptServiceMapper.selectByContractIdAndDeptId(contractId, deptId);
        if (deptService == null || deptService.getDeptBudget() == null
                || deptService.getDeptBudget().compareTo(BigDecimal.ZERO) == 0) {
            result.setBudgetSet(false);
            result.setAllocatedAmount(null);
            result.setUsedAmount(BigDecimal.ZERO);
            result.setRemainingAmount(null);
        } else {
            BigDecimal budget = deptService.getDeptBudget();
            BigDecimal used = serviceItemMapper.sumAllocatedAmountByDeptServiceId(deptService.getId(), null, null);
            if (used == null) used = BigDecimal.ZERO;
            result.setBudgetSet(true);
            result.setAllocatedAmount(budget);
            result.setUsedAmount(used);
            result.setRemainingAmount(budget.subtract(used).max(BigDecimal.ZERO));
        }
        return result;
    }

    @Override
    public Map<String, Object> getRemainingBudget(Long deptServiceId, Integer serviceMode, Integer serviceMemberType) {
        ProjectDeptServiceDO deptService = projectDeptServiceMapper.selectById(deptServiceId);
        Map<String, Object> result = new LinkedHashMap<>();

        if (deptService == null) {
            result.put("deptServiceId", deptServiceId);
            result.put("totalBudget", null);
            result.put("usedAmount", BigDecimal.ZERO);
            result.put("remainingAmount", null);
            return result;
        }

        // 根据 serviceMode / serviceMemberType 确定池子总额
        BigDecimal totalBudget;
        if (serviceMode != null && serviceMode == 1) {
            // 驻场池
            totalBudget = deptService.getOnsiteBudget();
        } else if (serviceMode != null && serviceMode == 2) {
            // 二线池
            totalBudget = deptService.getSecondLineBudget();
        } else if (serviceMemberType != null && serviceMemberType == 1) {
            // 安全运营驻场人员池
            totalBudget = deptService.getOnsiteBudget();
        } else if (serviceMemberType != null && serviceMemberType == 2) {
            // 安全运营管理人员池
            totalBudget = deptService.getSecondLineBudget();
        } else {
            // 未指定模式，返回总预算
            totalBudget = deptService.getDeptBudget();
        }

        BigDecimal usedAmount = serviceItemMapper.sumAllocatedAmountByDeptServiceId(
                deptServiceId, serviceMode, serviceMemberType);
        if (usedAmount == null) usedAmount = BigDecimal.ZERO;

        result.put("deptServiceId", deptServiceId);
        result.put("totalBudget", totalBudget);
        result.put("usedAmount", usedAmount);
        result.put("remainingAmount", totalBudget != null ? totalBudget.subtract(usedAmount).max(BigDecimal.ZERO) : null);
        return result;
    }

    @Override
    public BigDecimal getServiceItemAllocatedAmount(Long serviceItemId) {
        ServiceItemDO item = serviceItemMapper.selectById(serviceItemId);
        return item != null ? item.getAllocatedAmount() : null;
    }

}
