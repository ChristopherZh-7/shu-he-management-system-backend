package cn.shuhe.system.module.system.service.cost;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.system.controller.admin.cost.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.cost.ContractDeptAllocationDO;
import cn.shuhe.system.module.system.dal.dataobject.cost.ServiceItemAllocationDO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.mysql.cost.*;
import cn.shuhe.system.module.system.service.dept.DeptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.system.enums.ErrorCodeConstants.*;

/**
 * 合同收入分配 Service 实现
 */
@Slf4j
@Service
public class ContractAllocationServiceImpl implements ContractAllocationService {

    @Resource
    private ContractDeptAllocationMapper contractDeptAllocationMapper;

    @Resource
    private ServiceItemAllocationMapper serviceItemAllocationMapper;

    @Resource
    private ContractInfoMapper contractInfoMapper;

    @Resource
    private ServiceItemInfoMapper serviceItemInfoMapper;

    @Resource
    private OutsideCostRecordMapper outsideCostRecordMapper;

    @Resource
    private SecurityOperationContractInfoMapper securityOperationContractInfoMapper;

    @Resource
    private DeptService deptService;

    // ========== 合同部门分配 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContractDeptAllocation(ContractDeptAllocationSaveReqVO reqVO) {
        // 1. 校验合同是否存在
        Map<String, Object> contractInfo = contractInfoMapper.selectContractInfo(reqVO.getContractId());
        if (contractInfo == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }

        // 2. 校验部门是否存在
        DeptDO dept = deptService.getDept(reqVO.getDeptId());
        if (dept == null) {
            throw exception(DEPT_NOT_FOUND);
        }

        // 3. 校验该合同+部门是否已存在分配记录
        ContractDeptAllocationDO existing = contractDeptAllocationMapper.selectByContractIdAndDeptId(
                reqVO.getContractId(), reqVO.getDeptId());
        if (existing != null) {
            throw exception(CONTRACT_DEPT_ALLOCATION_EXISTS);
        }

        // 4. 校验金额不超过合同剩余可分配金额
        BigDecimal totalPrice = getBigDecimalValue(contractInfo.get("totalPrice"));
        BigDecimal currentAllocated = calculateAllocatedAmount(reqVO.getContractId());
        BigDecimal remaining = totalPrice.subtract(currentAllocated);
        if (reqVO.getAllocatedAmount().compareTo(remaining) > 0) {
            throw exception(CONTRACT_ALLOCATION_AMOUNT_EXCEED);
        }

        // 5. 创建分配记录
        ContractDeptAllocationDO allocation = new ContractDeptAllocationDO();
        allocation.setContractId(reqVO.getContractId());
        allocation.setContractNo((String) contractInfo.get("contractNo"));
        allocation.setCustomerName((String) contractInfo.get("customerName"));
        allocation.setDeptId(reqVO.getDeptId());
        allocation.setDeptName(dept.getName());
        allocation.setDeptType(dept.getDeptType()); // 保存部门类型
        allocation.setAllocatedAmount(reqVO.getAllocatedAmount());
        allocation.setRemark(reqVO.getRemark());

        contractDeptAllocationMapper.insert(allocation);
        return allocation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContractDeptAllocation(ContractDeptAllocationSaveReqVO reqVO) {
        // 1. 校验记录是否存在
        ContractDeptAllocationDO existing = contractDeptAllocationMapper.selectById(reqVO.getId());
        if (existing == null) {
            throw exception(CONTRACT_DEPT_ALLOCATION_NOT_EXISTS);
        }

        // 2. 校验金额不超过合同剩余可分配金额（排除当前记录）
        Map<String, Object> contractInfo = contractInfoMapper.selectContractInfo(existing.getContractId());
        if (contractInfo != null) {
            BigDecimal totalPrice = getBigDecimalValue(contractInfo.get("totalPrice"));
            BigDecimal currentAllocated = calculateAllocatedAmount(existing.getContractId());
            // 加回当前记录的金额
            BigDecimal remaining = totalPrice.subtract(currentAllocated).add(existing.getAllocatedAmount());
            if (reqVO.getAllocatedAmount().compareTo(remaining) > 0) {
                throw exception(CONTRACT_ALLOCATION_AMOUNT_EXCEED);
            }
        }

        // 3. 校验服务项分配金额是否超过新的部门分配金额
        BigDecimal serviceItemAllocated = calculateServiceItemAllocatedAmount(reqVO.getId());
        if (reqVO.getAllocatedAmount().compareTo(serviceItemAllocated) < 0) {
            throw exception(CONTRACT_DEPT_ALLOCATION_AMOUNT_LESS_THAN_SERVICE_ITEM);
        }

        // 4. 更新
        ContractDeptAllocationDO update = new ContractDeptAllocationDO();
        update.setId(reqVO.getId());
        update.setAllocatedAmount(reqVO.getAllocatedAmount());
        update.setRemark(reqVO.getRemark());

        contractDeptAllocationMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContractDeptAllocation(Long id) {
        // 1. 校验记录是否存在
        ContractDeptAllocationDO existing = contractDeptAllocationMapper.selectById(id);
        if (existing == null) {
            throw exception(CONTRACT_DEPT_ALLOCATION_NOT_EXISTS);
        }

        // 2. 删除关联的服务项分配
        serviceItemAllocationMapper.deleteByContractDeptAllocationId(id);

        // 3. 删除部门分配
        contractDeptAllocationMapper.deleteById(id);
    }

    @Override
    public ContractDeptAllocationRespVO getContractDeptAllocation(Long id) {
        ContractDeptAllocationDO allocation = contractDeptAllocationMapper.selectById(id);
        if (allocation == null) {
            return null;
        }
        return convertToRespVO(allocation, true);
    }

    @Override
    public PageResult<ContractDeptAllocationRespVO> getContractDeptAllocationPage(ContractDeptAllocationPageReqVO reqVO) {
        PageResult<ContractDeptAllocationDO> pageResult = contractDeptAllocationMapper.selectPage(
                reqVO.getContractId(), reqVO.getDeptId(), reqVO.getContractNo(), reqVO.getCustomerName(),
                reqVO.getPageNo(), reqVO.getPageSize());

        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        List<ContractDeptAllocationRespVO> voList = pageResult.getList().stream()
                .map(allocation -> convertToRespVO(allocation, false))
                .collect(Collectors.toList());

        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractAllocationDetailRespVO getContractAllocationDetail(Long contractId) {
        // 1. 获取合同信息
        Map<String, Object> contractInfo = contractInfoMapper.selectContractInfo(contractId);
        if (contractInfo == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }

        // 2. 获取已领取该合同的部门列表（根据服务项自动识别）
        List<Map<String, Object>> claimedDepts = serviceItemInfoMapper.selectDeptsByContractId(contractId);
        
        // 3. 获取现有的部门分配列表
        List<ContractDeptAllocationDO> existingAllocations = contractDeptAllocationMapper.selectByContractId(contractId);
        Map<Long, ContractDeptAllocationDO> allocationMap = existingAllocations.stream()
                .collect(Collectors.toMap(ContractDeptAllocationDO::getDeptId, a -> a, (a1, a2) -> a1));

        // 4. 为已领取但未创建分配记录的部门自动创建层级分配记录（金额为0）
        String contractNo = (String) contractInfo.get("contractNo");
        String customerName = (String) contractInfo.get("customerName");
        
        for (Map<String, Object> deptInfo : claimedDepts) {
            Long deptId = getLongValue(deptInfo.get("deptId"));
            if (deptId != null && !allocationMap.containsKey(deptId)) {
                // 创建该部门及其所有父部门的分配记录（层级结构）
                createAllocationHierarchy(contractId, contractNo, customerName, deptId, allocationMap);
            }
        }

        // 4.1 获取该合同下有跨部门费用记录的部门（包括支出方和收入方）
        // 这些部门可能没有服务项，但有跨部门支出记录，需要显示在合同分配页面
        List<Long> outsideCostDeptIds = outsideCostRecordMapper.selectDeptIdsByContractIdFromOutsideCost(contractId);
        for (Long deptId : outsideCostDeptIds) {
            if (deptId != null && !allocationMap.containsKey(deptId)) {
                // 创建该部门及其所有父部门的分配记录（层级结构）
                createAllocationHierarchy(contractId, contractNo, customerName, deptId, allocationMap);
                log.info("【合同分配】为跨部门费用涉及的部门创建层级分配记录，contractId={}, deptId={}",
                        contractId, deptId);
            }
        }

        // 5. 重新获取分配列表（包含新创建的记录）
        List<ContractDeptAllocationDO> allocations = contractDeptAllocationMapper.selectByContractId(contractId);
        List<ContractDeptAllocationRespVO> deptAllocations = allocations.stream()
                .map(allocation -> convertToRespVO(allocation, true))
                .collect(Collectors.toList());

        // 6. 计算金额（只统计第一级分配的总金额，因为下级分配是从上级分出来的）
        BigDecimal totalPrice = getBigDecimalValue(contractInfo.get("totalPrice"));
        BigDecimal allocatedAmount = allocations.stream()
                .filter(a -> a.getParentAllocationId() == null) // 只统计第一级
                .map(a -> a.getReceivedAmount() != null ? a.getReceivedAmount() : a.getAllocatedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 7. 构建响应
        ContractAllocationDetailRespVO resp = new ContractAllocationDetailRespVO();
        resp.setContractId(contractId);
        resp.setContractNo((String) contractInfo.get("contractNo"));
        resp.setContractName((String) contractInfo.get("contractName"));
        resp.setCustomerName((String) contractInfo.get("customerName"));
        resp.setTotalAmount(totalPrice);
        resp.setAllocatedAmount(allocatedAmount);
        resp.setRemainingAmount(totalPrice.subtract(allocatedAmount));
        resp.setDeptAllocations(deptAllocations);

        return resp;
    }

    /**
     * 为指定部门及其所有父部门创建分配记录（层级结构）
     * 
     * 例如：部门层级为 安服部 -> 安服部1营 -> 安服部1营1排 -> 安服部1营1排2班
     * 如果传入 "安服部1营1排2班"，则会创建整个层级的分配记录：
     * 1. 安服部（level=1, parentAllocationId=null）
     * 2. 安服部1营（level=2, parentAllocationId=安服部的分配ID）
     * 3. 安服部1营1排（level=3, parentAllocationId=安服部1营的分配ID）
     * 4. 安服部1营1排2班（level=4, parentAllocationId=安服部1营1排的分配ID）
     * 
     * 如果某个层级的分配记录已存在，则直接使用现有记录
     *
     * @param contractId   合同ID
     * @param contractNo   合同编号
     * @param customerName 客户名称
     * @param deptId       目标部门ID
     * @param allocationMap 现有分配记录Map（deptId -> allocation），会在方法中更新
     */
    private void createAllocationHierarchy(Long contractId, String contractNo, String customerName,
                                           Long deptId, Map<Long, ContractDeptAllocationDO> allocationMap) {
        // 1. 获取部门的完整层级路径（从当前部门到顶级部门）
        List<DeptDO> deptHierarchy = getDeptHierarchy(deptId);
        if (deptHierarchy.isEmpty()) {
            log.warn("【合同分配】无法获取部门层级，deptId={}", deptId);
            return;
        }

        // 2. 反转列表，从顶级部门开始创建（确保父部门先创建）
        Collections.reverse(deptHierarchy);

        // 3. 逐级创建分配记录
        Long parentAllocationId = null;
        int level = 1;
        
        for (DeptDO dept : deptHierarchy) {
            // 检查是否已存在分配记录
            if (allocationMap.containsKey(dept.getId())) {
                // 已存在，获取其ID作为下一级的父ID
                ContractDeptAllocationDO existingAllocation = allocationMap.get(dept.getId());
                parentAllocationId = existingAllocation.getId();
                level = (existingAllocation.getAllocationLevel() != null ? existingAllocation.getAllocationLevel() : level) + 1;
                log.debug("【合同分配】部门已有分配记录，跳过创建，deptId={}, deptName={}, allocationId={}",
                        dept.getId(), dept.getName(), existingAllocation.getId());
            } else {
                // 不存在，创建新的分配记录
                ContractDeptAllocationDO newAllocation = new ContractDeptAllocationDO();
                newAllocation.setContractId(contractId);
                newAllocation.setContractNo(contractNo);
                newAllocation.setCustomerName(customerName);
                newAllocation.setDeptId(dept.getId());
                newAllocation.setDeptName(dept.getName());
                newAllocation.setDeptType(dept.getDeptType());
                newAllocation.setParentAllocationId(parentAllocationId);
                newAllocation.setAllocationLevel(level);
                newAllocation.setAllocatedAmount(BigDecimal.ZERO);
                newAllocation.setReceivedAmount(BigDecimal.ZERO);
                newAllocation.setDistributedAmount(BigDecimal.ZERO);
                
                contractDeptAllocationMapper.insert(newAllocation);
                allocationMap.put(dept.getId(), newAllocation);
                
                log.info("【合同分配】创建层级分配记录，contractId={}, deptId={}, deptName={}, level={}, parentAllocationId={}",
                        contractId, dept.getId(), dept.getName(), level, parentAllocationId);
                
                parentAllocationId = newAllocation.getId();
                level++;
            }
        }
    }

    /**
     * 获取部门的完整层级路径（从当前部门到顶级部门）
     * 
     * @param deptId 部门ID
     * @return 部门层级列表，从当前部门到顶级部门（不包括根部门ID=0）
     */
    private List<DeptDO> getDeptHierarchy(Long deptId) {
        List<DeptDO> hierarchy = new ArrayList<>();
        
        Long currentDeptId = deptId;
        int maxDepth = 10; // 防止无限循环
        
        while (currentDeptId != null && currentDeptId != 0 && maxDepth-- > 0) {
            DeptDO dept = deptService.getDept(currentDeptId);
            if (dept == null) {
                break;
            }
            hierarchy.add(dept);
            currentDeptId = dept.getParentId();
        }
        
        return hierarchy;
    }
    
    /**
     * 安全获取 Long 值
     */
    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    @Override
    public List<ContractDeptAllocationDO> getContractDeptAllocationsByContractId(Long contractId) {
        return contractDeptAllocationMapper.selectByContractId(contractId);
    }

    @Override
    public List<ContractDeptAllocationRespVO> getContractDeptAllocationsByDeptId(Long deptId) {
        List<ContractDeptAllocationDO> allocations = contractDeptAllocationMapper.selectByDeptId(deptId);
        return allocations.stream()
                .map(allocation -> convertToRespVO(allocation, true))
                .collect(Collectors.toList());
    }

    // ========== 服务项分配 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createServiceItemAllocation(ServiceItemAllocationSaveReqVO reqVO) {
        // 1. 校验合同部门分配是否存在
        ContractDeptAllocationDO deptAllocation = contractDeptAllocationMapper.selectById(reqVO.getContractDeptAllocationId());
        if (deptAllocation == null) {
            throw exception(CONTRACT_DEPT_ALLOCATION_NOT_EXISTS);
        }

        // 确定分配类型，默认为服务项分配
        String allocationType = StrUtil.isBlank(reqVO.getAllocationType()) 
                ? ServiceItemAllocationDO.ALLOCATION_TYPE_SERVICE_ITEM 
                : reqVO.getAllocationType();

        // 判断是否是安全运营分配（费用类型级别）
        boolean isSecurityOperationAllocation = ServiceItemAllocationDO.ALLOCATION_TYPE_SO_MANAGEMENT.equals(allocationType)
                || ServiceItemAllocationDO.ALLOCATION_TYPE_SO_ONSITE.equals(allocationType);
        
        // 判断是否是安全服务分配（费用类型级别）
        boolean isSecurityServiceAllocation = ServiceItemAllocationDO.ALLOCATION_TYPE_SS_ONSITE.equals(allocationType)
                || ServiceItemAllocationDO.ALLOCATION_TYPE_SS_SECOND_LINE.equals(allocationType);
        
        // 判断是否是数据安全分配（费用类型级别）
        boolean isDataSecurityAllocation = ServiceItemAllocationDO.ALLOCATION_TYPE_DS_ONSITE.equals(allocationType)
                || ServiceItemAllocationDO.ALLOCATION_TYPE_DS_SECOND_LINE.equals(allocationType);
        
        // 判断是否是服务项级别分配（从费用类型分配到具体服务项）
        boolean isServiceItemLevelAllocation = ServiceItemAllocationDO.ALLOCATION_TYPE_SERVICE_ITEM.equals(allocationType)
                && reqVO.getParentAllocationId() != null;
        
        // 校验父级分配（如果有）
        ServiceItemAllocationDO parentAllocation = null;
        if (reqVO.getParentAllocationId() != null) {
            parentAllocation = serviceItemAllocationMapper.selectById(reqVO.getParentAllocationId());
            if (parentAllocation == null) {
                throw exception(SERVICE_ITEM_ALLOCATION_NOT_EXISTS);
            }
        }

        String serviceItemName;
        if (isSecurityOperationAllocation) {
            // 安全运营分配：校验是否已存在相同类型的分配
            ServiceItemAllocationDO existing = serviceItemAllocationMapper.selectByAllocationIdAndType(
                    reqVO.getContractDeptAllocationId(), allocationType);
            if (existing != null) {
                throw exception(SERVICE_ITEM_ALLOCATION_EXISTS);
            }
            // 设置名称
            serviceItemName = ServiceItemAllocationDO.ALLOCATION_TYPE_SO_MANAGEMENT.equals(allocationType) 
                    ? "管理费" : "驻场费";
        } else if (isSecurityServiceAllocation) {
            // 安全服务分配：校验是否已存在相同类型的分配
            ServiceItemAllocationDO existing = serviceItemAllocationMapper.selectByAllocationIdAndType(
                    reqVO.getContractDeptAllocationId(), allocationType);
            if (existing != null) {
                throw exception(SERVICE_ITEM_ALLOCATION_EXISTS);
            }
            // 设置名称
            serviceItemName = ServiceItemAllocationDO.ALLOCATION_TYPE_SS_ONSITE.equals(allocationType) 
                    ? "驻场服务费" : "二线服务费";
        } else if (isDataSecurityAllocation) {
            // 数据安全分配：校验是否已存在相同类型的分配
            ServiceItemAllocationDO existing = serviceItemAllocationMapper.selectByAllocationIdAndType(
                    reqVO.getContractDeptAllocationId(), allocationType);
            if (existing != null) {
                throw exception(SERVICE_ITEM_ALLOCATION_EXISTS);
            }
            // 设置名称
            serviceItemName = ServiceItemAllocationDO.ALLOCATION_TYPE_DS_ONSITE.equals(allocationType) 
                    ? "驻场服务费" : "二线服务费";
        } else {
            // 服务项分配：校验服务项是否存在
            if (reqVO.getServiceItemId() == null) {
                throw exception(SERVICE_ITEM_NOT_EXISTS);
            }
            Map<String, Object> serviceItemInfo = serviceItemInfoMapper.selectServiceItemInfo(reqVO.getServiceItemId());
            if (serviceItemInfo == null) {
                throw exception(SERVICE_ITEM_NOT_EXISTS);
            }

            // 校验该服务项是否已在同一父级下分配过
            if (isServiceItemLevelAllocation) {
                // 从费用类型分配到具体服务项：检查同一父级下是否已存在
                List<ServiceItemAllocationDO> siblings = serviceItemAllocationMapper.selectByParentAllocationId(reqVO.getParentAllocationId());
                boolean alreadyAllocated = siblings.stream()
                        .anyMatch(s -> reqVO.getServiceItemId().equals(s.getServiceItemId()));
                if (alreadyAllocated) {
                    throw exception(SERVICE_ITEM_ALLOCATION_EXISTS);
                }
            } else {
                // 传统服务项分配
                ServiceItemAllocationDO existing = serviceItemAllocationMapper.selectByAllocationIdAndServiceItemId(
                        reqVO.getContractDeptAllocationId(), reqVO.getServiceItemId());
                if (existing != null) {
                    throw exception(SERVICE_ITEM_ALLOCATION_EXISTS);
                }
            }
            serviceItemName = (String) serviceItemInfo.get("name");
        }

        // 校验金额：根据是否有父级分配来判断
        BigDecimal remaining;
        if (isServiceItemLevelAllocation && parentAllocation != null) {
            // 从费用类型分配到具体服务项：校验不超过父级剩余可分配金额
            BigDecimal parentAllocatedAmount = parentAllocation.getAllocatedAmount();
            BigDecimal childAllocated = calculateChildAllocatedAmount(reqVO.getParentAllocationId());
            remaining = parentAllocatedAmount.subtract(childAllocated);
        } else {
            // 费用类型级别分配：校验不超过部门剩余可分配金额
            BigDecimal deptAllocatedAmount = deptAllocation.getAllocatedAmount();
            BigDecimal serviceItemAllocated = calculateServiceItemAllocatedAmount(reqVO.getContractDeptAllocationId());
            remaining = deptAllocatedAmount.subtract(serviceItemAllocated);
        }
        if (reqVO.getAllocatedAmount().compareTo(remaining) > 0) {
            throw exception(SERVICE_ITEM_ALLOCATION_AMOUNT_EXCEED);
        }

        // 创建分配记录
        ServiceItemAllocationDO allocation = new ServiceItemAllocationDO();
        allocation.setContractDeptAllocationId(reqVO.getContractDeptAllocationId());
        allocation.setParentAllocationId(reqVO.getParentAllocationId());
        allocation.setAllocationType(allocationType);
        // 安全运营、安全服务、数据安全的固定分配类型不需要关联服务项
        allocation.setServiceItemId((isSecurityOperationAllocation || isSecurityServiceAllocation || isDataSecurityAllocation) ? null : reqVO.getServiceItemId());
        allocation.setServiceItemName(serviceItemName);
        allocation.setAllocatedAmount(reqVO.getAllocatedAmount());
        allocation.setRemark(reqVO.getRemark());

        serviceItemAllocationMapper.insert(allocation);

        // 安全运营分配：同步更新 security_operation_contract 表的费用字段
        if (isSecurityOperationAllocation) {
            syncSecurityOperationFees(reqVO.getContractDeptAllocationId(), allocationType);
        }

        return allocation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateServiceItemAllocation(ServiceItemAllocationSaveReqVO reqVO) {
        // 1. 校验记录是否存在
        ServiceItemAllocationDO existing = serviceItemAllocationMapper.selectById(reqVO.getId());
        if (existing == null) {
            throw exception(SERVICE_ITEM_ALLOCATION_NOT_EXISTS);
        }

        // 2. 校验金额不超过部门剩余可分配金额
        ContractDeptAllocationDO deptAllocation = contractDeptAllocationMapper.selectById(existing.getContractDeptAllocationId());
        if (deptAllocation != null) {
            BigDecimal deptAllocatedAmount = deptAllocation.getAllocatedAmount();
            BigDecimal serviceItemAllocated = calculateServiceItemAllocatedAmount(existing.getContractDeptAllocationId());
            // 加回当前记录的金额
            BigDecimal remaining = deptAllocatedAmount.subtract(serviceItemAllocated).add(existing.getAllocatedAmount());
            if (reqVO.getAllocatedAmount().compareTo(remaining) > 0) {
                throw exception(SERVICE_ITEM_ALLOCATION_AMOUNT_EXCEED);
            }
        }

        // 3. 更新
        ServiceItemAllocationDO update = new ServiceItemAllocationDO();
        update.setId(reqVO.getId());
        update.setAllocatedAmount(reqVO.getAllocatedAmount());
        update.setRemark(reqVO.getRemark());

        serviceItemAllocationMapper.updateById(update);

        // 安全运营分配：同步更新 security_operation_contract 表的费用字段
        String allocationType = existing.getAllocationType();
        boolean isSecurityOperationAllocation = ServiceItemAllocationDO.ALLOCATION_TYPE_SO_MANAGEMENT.equals(allocationType)
                || ServiceItemAllocationDO.ALLOCATION_TYPE_SO_ONSITE.equals(allocationType);
        if (isSecurityOperationAllocation) {
            syncSecurityOperationFees(existing.getContractDeptAllocationId(), allocationType);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteServiceItemAllocation(Long id) {
        ServiceItemAllocationDO existing = serviceItemAllocationMapper.selectById(id);
        if (existing == null) {
            throw exception(SERVICE_ITEM_ALLOCATION_NOT_EXISTS);
        }

        // 保存删除前的信息，用于同步
        Long allocationId = existing.getContractDeptAllocationId();
        String allocationType = existing.getAllocationType();
        boolean isSecurityOperationAllocation = ServiceItemAllocationDO.ALLOCATION_TYPE_SO_MANAGEMENT.equals(allocationType)
                || ServiceItemAllocationDO.ALLOCATION_TYPE_SO_ONSITE.equals(allocationType);

        serviceItemAllocationMapper.deleteById(id);

        // 安全运营分配：同步更新 security_operation_contract 表的费用字段（删除后费用变为0）
        if (isSecurityOperationAllocation) {
            syncSecurityOperationFees(allocationId, allocationType);
        }
    }

    @Override
    public ServiceItemAllocationRespVO getServiceItemAllocation(Long id) {
        ServiceItemAllocationDO allocation = serviceItemAllocationMapper.selectById(id);
        if (allocation == null) {
            return null;
        }
        return convertToServiceItemRespVO(allocation);
    }

    @Override
    public List<ServiceItemAllocationRespVO> getServiceItemAllocationsByDeptAllocationId(Long contractDeptAllocationId) {
        List<ServiceItemAllocationDO> allocations = serviceItemAllocationMapper.selectByContractDeptAllocationId(contractDeptAllocationId);
        return allocations.stream()
                .map(this::convertToServiceItemRespVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceItemAllocationDO> getServiceItemAllocationsByServiceItemId(Long serviceItemId) {
        return serviceItemAllocationMapper.selectByServiceItemId(serviceItemId);
    }

    @Override
    public List<ServiceItemAllocationRespVO> getServiceItemAllocationsByParentId(Long parentAllocationId) {
        List<ServiceItemAllocationDO> allocations = serviceItemAllocationMapper.selectByParentAllocationId(parentAllocationId);
        return allocations.stream()
                .map(this::convertToServiceItemRespVO)
                .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    /**
     * 计算合同已分配给部门的总金额
     */
    private BigDecimal calculateAllocatedAmount(Long contractId) {
        List<ContractDeptAllocationDO> allocations = contractDeptAllocationMapper.selectByContractId(contractId);
        return allocations.stream()
                .map(ContractDeptAllocationDO::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算部门已分配给服务项的总金额
     */
    private BigDecimal calculateServiceItemAllocatedAmount(Long contractDeptAllocationId) {
        List<ServiceItemAllocationDO> allocations = serviceItemAllocationMapper.selectByContractDeptAllocationId(contractDeptAllocationId);
        return allocations.stream()
                .map(ServiceItemAllocationDO::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算父级分配下的子分配总金额
     * 用于从费用类型分配到具体服务项的场景
     */
    private BigDecimal calculateChildAllocatedAmount(Long parentAllocationId) {
        List<ServiceItemAllocationDO> childAllocations = serviceItemAllocationMapper.selectByParentAllocationId(parentAllocationId);
        return childAllocations.stream()
                .map(ServiceItemAllocationDO::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 转换为 ContractDeptAllocationRespVO
     */
    private ContractDeptAllocationRespVO convertToRespVO(ContractDeptAllocationDO allocation, boolean includeServiceItems) {
        ContractDeptAllocationRespVO vo = new ContractDeptAllocationRespVO();
        vo.setId(allocation.getId());
        vo.setContractId(allocation.getContractId());
        vo.setContractNo(allocation.getContractNo());
        vo.setCustomerName(allocation.getCustomerName());
        vo.setDeptId(allocation.getDeptId());
        vo.setDeptName(allocation.getDeptName());
        vo.setAllocatedAmount(allocation.getAllocatedAmount());
        vo.setRemark(allocation.getRemark());
        vo.setCreateTime(allocation.getCreateTime());
        vo.setUpdateTime(allocation.getUpdateTime());

        // 分层分配字段
        vo.setParentAllocationId(allocation.getParentAllocationId());
        vo.setAllocationLevel(allocation.getAllocationLevel() != null ? allocation.getAllocationLevel() : 1);
        vo.setReceivedAmount(allocation.getReceivedAmount() != null ? allocation.getReceivedAmount() : allocation.getAllocatedAmount());
        vo.setDistributedAmount(allocation.getDistributedAmount() != null ? allocation.getDistributedAmount() : BigDecimal.ZERO);

        // 获取部门类型（优先使用存储的值，兼容历史数据）
        if (allocation.getDeptType() != null) {
            vo.setDeptType(allocation.getDeptType());
        } else {
            // 历史数据可能没有存储 deptType，从部门表获取
            DeptDO dept = deptService.getDept(allocation.getDeptId());
            if (dept != null) {
                vo.setDeptType(dept.getDeptType());
            }
        }

        // 计算服务项分配金额
        BigDecimal serviceItemAllocated = calculateServiceItemAllocatedAmount(allocation.getId());
        vo.setServiceItemAllocatedAmount(serviceItemAllocated);
        vo.setRemainingAmount(allocation.getAllocatedAmount().subtract(serviceItemAllocated));

        // 是否包含服务项分配详情
        if (includeServiceItems) {
            List<ServiceItemAllocationRespVO> serviceItemAllocations = getServiceItemAllocationsByDeptAllocationId(allocation.getId());
            vo.setServiceItemAllocations(serviceItemAllocations);
        }

        // 填充跨部门费用数据
        fillOutsideCostData(vo, allocation.getContractId(), allocation.getDeptId());

        return vo;
    }

    /**
     * 填充跨部门费用数据
     */
    private void fillOutsideCostData(ContractDeptAllocationRespVO vo, Long contractId, Long deptId) {
        try {
            // 支出（作为发起方）
            BigDecimal expense = outsideCostRecordMapper.sumExpenseByContractIdAndDeptId(contractId, deptId);
            Integer expenseCount = outsideCostRecordMapper.countExpenseByContractIdAndDeptId(contractId, deptId);
            vo.setOutsideCostExpense(expense != null ? expense : BigDecimal.ZERO);
            vo.setOutsideCostExpenseCount(expenseCount != null ? expenseCount : 0);

            // 收入（作为目标方）
            BigDecimal income = outsideCostRecordMapper.sumIncomeByContractIdAndDeptId(contractId, deptId);
            Integer incomeCount = outsideCostRecordMapper.countIncomeByContractIdAndDeptId(contractId, deptId);
            vo.setOutsideCostIncome(income != null ? income : BigDecimal.ZERO);
            vo.setOutsideCostIncomeCount(incomeCount != null ? incomeCount : 0);
        } catch (Exception e) {
            log.warn("填充跨部门费用数据失败，contractId={}, deptId={}", contractId, deptId, e);
            vo.setOutsideCostExpense(BigDecimal.ZERO);
            vo.setOutsideCostExpenseCount(0);
            vo.setOutsideCostIncome(BigDecimal.ZERO);
            vo.setOutsideCostIncomeCount(0);
        }
    }

    /**
     * 转换为 ServiceItemAllocationRespVO
     */
    private ServiceItemAllocationRespVO convertToServiceItemRespVO(ServiceItemAllocationDO allocation) {
        ServiceItemAllocationRespVO vo = new ServiceItemAllocationRespVO();
        vo.setId(allocation.getId());
        vo.setContractDeptAllocationId(allocation.getContractDeptAllocationId());
        vo.setParentAllocationId(allocation.getParentAllocationId());
        vo.setAllocationType(allocation.getAllocationType());
        vo.setServiceItemId(allocation.getServiceItemId());
        vo.setServiceItemName(allocation.getServiceItemName());
        vo.setAllocatedAmount(allocation.getAllocatedAmount());
        vo.setRemark(allocation.getRemark());
        vo.setCreateTime(allocation.getCreateTime());
        vo.setUpdateTime(allocation.getUpdateTime());

        // 只有服务项分配才获取额外信息
        if (allocation.getServiceItemId() != null) {
            Map<String, Object> serviceItemInfo = serviceItemInfoMapper.selectServiceItemInfo(allocation.getServiceItemId());
            if (serviceItemInfo != null) {
                vo.setServiceItemCode((String) serviceItemInfo.get("code"));
                vo.setServiceType((String) serviceItemInfo.get("serviceType"));
            }
        }

        // 费用类型分配（非服务项分配）：计算子分配信息
        String allocationType = allocation.getAllocationType();
        boolean isFeeTypeAllocation = ServiceItemAllocationDO.ALLOCATION_TYPE_SO_MANAGEMENT.equals(allocationType)
                || ServiceItemAllocationDO.ALLOCATION_TYPE_SO_ONSITE.equals(allocationType)
                || ServiceItemAllocationDO.ALLOCATION_TYPE_SS_ONSITE.equals(allocationType)
                || ServiceItemAllocationDO.ALLOCATION_TYPE_SS_SECOND_LINE.equals(allocationType)
                || ServiceItemAllocationDO.ALLOCATION_TYPE_DS_ONSITE.equals(allocationType)
                || ServiceItemAllocationDO.ALLOCATION_TYPE_DS_SECOND_LINE.equals(allocationType);
        
        if (isFeeTypeAllocation) {
            BigDecimal childAllocated = calculateChildAllocatedAmount(allocation.getId());
            vo.setChildAllocatedAmount(childAllocated);
            BigDecimal remaining = allocation.getAllocatedAmount().subtract(childAllocated);
            vo.setRemainingAmount(remaining);
            vo.setCanAllocate(remaining.compareTo(BigDecimal.ZERO) > 0);
        }

        return vo;
    }

    /**
     * 安全获取 BigDecimal 值
     */
    private BigDecimal getBigDecimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    /**
     * 同步安全运营费用到 security_operation_contract 表
     * 
     * 当 service_item_allocation 表中的安全运营分配（管理费/驻场费）发生变化时，
     * 同步更新 security_operation_contract 表中对应的费用字段，
     * 确保经营分析页面和合同分配页面数据一致。
     *
     * @param contractDeptAllocationId 合同部门分配ID
     * @param allocationType 分配类型（so_management 或 so_onsite）
     */
    private void syncSecurityOperationFees(Long contractDeptAllocationId, String allocationType) {
        try {
            // 从 service_item_allocation 表重新计算当前的费用汇总
            Map<String, BigDecimal> fees = securityOperationContractInfoMapper
                    .selectSecurityOperationFeesByAllocationId(contractDeptAllocationId);
            
            if (fees == null) {
                log.warn("[同步安全运营费用] 未找到费用汇总数据，allocationId={}", contractDeptAllocationId);
                return;
            }

            BigDecimal managementFee = fees.get("managementFee");
            BigDecimal onsiteFee = fees.get("onsiteFee");
            
            if (managementFee == null) managementFee = BigDecimal.ZERO;
            if (onsiteFee == null) onsiteFee = BigDecimal.ZERO;

            // 同时更新两个费用字段，确保数据一致性
            int updated1 = securityOperationContractInfoMapper.updateManagementFeeByAllocationId(
                    contractDeptAllocationId, managementFee);
            int updated2 = securityOperationContractInfoMapper.updateOnsiteFeeByAllocationId(
                    contractDeptAllocationId, onsiteFee);
            
            log.info("[同步安全运营费用] 同步完成，allocationId={}, managementFee={}, onsiteFee={}, affectedRows=({},{})", 
                    contractDeptAllocationId, managementFee, onsiteFee, updated1, updated2);
        } catch (Exception e) {
            // 同步失败不应影响主流程，仅记录警告日志
            log.warn("[同步安全运营费用] 同步失败，allocationId={}, allocationType={}", 
                    contractDeptAllocationId, allocationType, e);
        }
    }

    // ========== 分层分配实现 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFirstLevelAllocation(ContractAllocationFirstLevelReqVO reqVO) {
        // 1. 校验合同是否存在
        Map<String, Object> contractInfo = contractInfoMapper.selectContractInfo(reqVO.getContractId());
        if (contractInfo == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }

        // 2. 校验部门是否存在且是一级部门
        DeptDO dept = deptService.getDept(reqVO.getDeptId());
        if (dept == null) {
            throw exception(DEPT_NOT_FOUND);
        }
        // 一级部门的判断：父部门为根（0）或者父部门是公司根节点
        if (!isFirstLevelDept(dept)) {
            throw exception(DEPT_NOT_FIRST_LEVEL);
        }

        // 3. 校验该合同+部门是否已存在第一级分配记录
        ContractDeptAllocationDO existing = contractDeptAllocationMapper.selectByContractIdAndDeptId(
                reqVO.getContractId(), reqVO.getDeptId());
        if (existing != null) {
            throw exception(CONTRACT_DEPT_ALLOCATION_EXISTS);
        }

        // 4. 校验金额不超过合同剩余可分配金额
        BigDecimal totalPrice = getBigDecimalValue(contractInfo.get("totalPrice"));
        BigDecimal currentAllocated = calculateFirstLevelAllocatedAmount(reqVO.getContractId());
        BigDecimal remaining = totalPrice.subtract(currentAllocated);
        if (reqVO.getAmount().compareTo(remaining) > 0) {
            throw exception(CONTRACT_ALLOCATION_AMOUNT_EXCEED);
        }

        // 5. 创建第一级分配记录
        ContractDeptAllocationDO allocation = new ContractDeptAllocationDO();
        allocation.setContractId(reqVO.getContractId());
        allocation.setContractNo((String) contractInfo.get("contractNo"));
        allocation.setCustomerName((String) contractInfo.get("customerName"));
        allocation.setDeptId(reqVO.getDeptId());
        allocation.setDeptName(dept.getName());
        allocation.setDeptType(dept.getDeptType()); // 保存部门类型
        allocation.setParentAllocationId(null); // 第一级，无上级
        allocation.setAllocationLevel(1);
        allocation.setReceivedAmount(reqVO.getAmount());
        allocation.setDistributedAmount(BigDecimal.ZERO);
        allocation.setAllocatedAmount(reqVO.getAmount());
        allocation.setRemark(reqVO.getRemark());

        contractDeptAllocationMapper.insert(allocation);
        log.info("[分层分配] 创建第一级分配，contractId={}, deptId={}, amount={}", 
                reqVO.getContractId(), reqVO.getDeptId(), reqVO.getAmount());
        return allocation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long distributeToChildDept(ContractAllocationDistributeReqVO reqVO) {
        // 1. 校验父级分配记录是否存在
        ContractDeptAllocationDO parent = contractDeptAllocationMapper.selectById(reqVO.getParentAllocationId());
        if (parent == null) {
            throw exception(CONTRACT_DEPT_ALLOCATION_NOT_EXISTS);
        }

        // 2. 校验子部门是否存在且是父部门的直接下级
        DeptDO childDept = deptService.getDept(reqVO.getChildDeptId());
        if (childDept == null) {
            throw exception(DEPT_NOT_FOUND);
        }
        if (!parent.getDeptId().equals(childDept.getParentId())) {
            throw exception(CHILD_DEPT_NOT_VALID);
        }

        // 3. 校验金额不超过父级剩余可分配金额
        BigDecimal parentRemaining = (parent.getReceivedAmount() != null ? parent.getReceivedAmount() : BigDecimal.ZERO)
                .subtract(parent.getDistributedAmount() != null ? parent.getDistributedAmount() : BigDecimal.ZERO);
        if (reqVO.getAmount().compareTo(parentRemaining) > 0) {
            throw exception(ALLOCATION_AMOUNT_EXCEED_PARENT);
        }

        // 4. 检查该合同+子部门是否已存在分配记录
        ContractDeptAllocationDO existing = contractDeptAllocationMapper.selectByContractIdAndDeptId(
                parent.getContractId(), reqVO.getChildDeptId());
        
        Long childId;
        if (existing != null) {
            // 4.1 子部门已存在分配记录（由后端自动创建的层级），更新金额
            BigDecimal newReceivedAmount = (existing.getReceivedAmount() != null ? existing.getReceivedAmount() : BigDecimal.ZERO)
                    .add(reqVO.getAmount());
            existing.setReceivedAmount(newReceivedAmount);
            existing.setAllocatedAmount(newReceivedAmount);
            // 如果有备注，追加备注
            if (StrUtil.isNotBlank(reqVO.getRemark())) {
                String existingRemark = existing.getRemark();
                existing.setRemark(StrUtil.isBlank(existingRemark) ? reqVO.getRemark() : existingRemark + "; " + reqVO.getRemark());
            }
            // 确保父子关系正确
            if (existing.getParentAllocationId() == null) {
                existing.setParentAllocationId(reqVO.getParentAllocationId());
                existing.setAllocationLevel((parent.getAllocationLevel() != null ? parent.getAllocationLevel() : 1) + 1);
            }
            contractDeptAllocationMapper.updateById(existing);
            childId = existing.getId();
            log.info("[分层分配] 更新已存在的子部门分配记录，childId={}, newReceivedAmount={}", childId, newReceivedAmount);
        } else {
            // 4.2 子部门不存在分配记录，创建新记录
            ContractDeptAllocationDO child = new ContractDeptAllocationDO();
            child.setContractId(parent.getContractId());
            child.setContractNo(parent.getContractNo());
            child.setCustomerName(parent.getCustomerName());
            child.setDeptId(reqVO.getChildDeptId());
            child.setDeptName(childDept.getName());
            child.setDeptType(childDept.getDeptType());
            child.setParentAllocationId(reqVO.getParentAllocationId());
            child.setAllocationLevel((parent.getAllocationLevel() != null ? parent.getAllocationLevel() : 1) + 1);
            child.setReceivedAmount(reqVO.getAmount());
            child.setDistributedAmount(BigDecimal.ZERO);
            child.setAllocatedAmount(reqVO.getAmount());
            child.setRemark(reqVO.getRemark());
            contractDeptAllocationMapper.insert(child);
            childId = child.getId();
            log.info("[分层分配] 创建新的子部门分配记录，childId={}, amount={}", childId, reqVO.getAmount());
        }

        // 5. 更新父级的已分配金额
        BigDecimal parentDistributed = parent.getDistributedAmount() != null ? parent.getDistributedAmount() : BigDecimal.ZERO;
        parent.setDistributedAmount(parentDistributed.add(reqVO.getAmount()));
        contractDeptAllocationMapper.updateById(parent);

        log.info("[分层分配] 分配给子部门完成，parentAllocationId={}, childDeptId={}, amount={}", 
                reqVO.getParentAllocationId(), reqVO.getChildDeptId(), reqVO.getAmount());
        return childId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAllocationAmount(Long allocationId, BigDecimal newAmount) {
        // 1. 校验记录是否存在
        ContractDeptAllocationDO allocation = contractDeptAllocationMapper.selectById(allocationId);
        if (allocation == null) {
            throw exception(CONTRACT_DEPT_ALLOCATION_NOT_EXISTS);
        }

        // 2. 校验新金额不能小于已分配给下级的金额
        if (newAmount.compareTo(allocation.getDistributedAmount()) < 0) {
            throw exception(ALLOCATION_AMOUNT_LESS_THAN_DISTRIBUTED);
        }

        // 3. 如果有上级，校验新金额不能超过上级可分配给我的金额
        if (allocation.getParentAllocationId() != null) {
            ContractDeptAllocationDO parent = contractDeptAllocationMapper.selectById(allocation.getParentAllocationId());
            if (parent != null) {
                // 上级剩余 = 上级获得 - 上级已分配 + 当前分配给我的金额
                BigDecimal parentRemaining = parent.getReceivedAmount()
                        .subtract(parent.getDistributedAmount())
                        .add(allocation.getReceivedAmount());
                if (newAmount.compareTo(parentRemaining) > 0) {
                    throw exception(ALLOCATION_AMOUNT_EXCEED_PARENT);
                }
                
                // 更新上级的已分配金额
                BigDecimal diff = newAmount.subtract(allocation.getReceivedAmount());
                parent.setDistributedAmount(parent.getDistributedAmount().add(diff));
                contractDeptAllocationMapper.updateById(parent);
            }
        } else {
            // 第一级分配，校验不超过合同剩余
            Map<String, Object> contractInfo = contractInfoMapper.selectContractInfo(allocation.getContractId());
            if (contractInfo != null) {
                BigDecimal totalPrice = getBigDecimalValue(contractInfo.get("totalPrice"));
                BigDecimal currentAllocated = calculateFirstLevelAllocatedAmount(allocation.getContractId());
                // 加回当前记录的金额
                BigDecimal remaining = totalPrice.subtract(currentAllocated).add(allocation.getReceivedAmount());
                if (newAmount.compareTo(remaining) > 0) {
                    throw exception(CONTRACT_ALLOCATION_AMOUNT_EXCEED);
                }
            }
        }

        // 4. 更新金额
        allocation.setReceivedAmount(newAmount);
        allocation.setAllocatedAmount(newAmount);
        contractDeptAllocationMapper.updateById(allocation);

        log.info("[分层分配] 更新分配金额，allocationId={}, newAmount={}", allocationId, newAmount);
    }

    @Override
    public List<ContractDeptAllocationRespVO> getContractAllocationTree(Long contractId) {
        // 1. 获取该合同的所有分配记录
        List<ContractDeptAllocationDO> allAllocations = contractDeptAllocationMapper.selectByContractId(contractId);
        if (CollUtil.isEmpty(allAllocations)) {
            return Collections.emptyList();
        }

        // 2. 转换为VO并构建ID映射
        Map<Long, ContractDeptAllocationRespVO> voMap = new HashMap<>();
        for (ContractDeptAllocationDO allocation : allAllocations) {
            ContractDeptAllocationRespVO vo = convertToRespVO(allocation, false);
            vo.setChildren(new ArrayList<>());
            // 判断是否可以继续分配（有剩余金额）
            BigDecimal received = allocation.getReceivedAmount() != null ? allocation.getReceivedAmount() : allocation.getAllocatedAmount();
            BigDecimal distributed = allocation.getDistributedAmount() != null ? allocation.getDistributedAmount() : BigDecimal.ZERO;
            BigDecimal remaining = received.subtract(distributed);
            vo.setCanDistribute(remaining.compareTo(BigDecimal.ZERO) > 0);
            voMap.put(allocation.getId(), vo);
        }

        // 3. 构建树形结构
        List<ContractDeptAllocationRespVO> rootList = new ArrayList<>();
        for (ContractDeptAllocationDO allocation : allAllocations) {
            ContractDeptAllocationRespVO vo = voMap.get(allocation.getId());
            if (allocation.getParentAllocationId() == null) {
                // 第一级，加入根列表
                rootList.add(vo);
            } else {
                // 非第一级，加入父级的children
                ContractDeptAllocationRespVO parentVo = voMap.get(allocation.getParentAllocationId());
                if (parentVo != null && parentVo.getChildren() != null) {
                    parentVo.getChildren().add(vo);
                }
            }
        }

        return rootList;
    }

    @Override
    public List<DeptDO> getFirstLevelDepts() {
        // 获取所有一级业务部门（父部门为公司根节点，且有deptType的）
        List<DeptDO> allDepts = deptService.getDeptList(new cn.shuhe.system.module.system.controller.admin.dept.vo.dept.DeptListReqVO());
        return allDepts.stream()
                .filter(this::isFirstLevelDept)
                .filter(dept -> dept.getDeptType() != null)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeptDO> getChildDepts(Long parentDeptId) {
        List<DeptDO> allDepts = deptService.getDeptList(new cn.shuhe.system.module.system.controller.admin.dept.vo.dept.DeptListReqVO());
        return allDepts.stream()
                .filter(dept -> parentDeptId.equals(dept.getParentId()))
                .collect(Collectors.toList());
    }

    /**
     * 判断是否为一级部门（业务部门的第一层）
     * 判断逻辑：有deptType且父部门无deptType（或父部门是根）
     */
    private boolean isFirstLevelDept(DeptDO dept) {
        if (dept.getDeptType() == null) {
            return false;
        }
        if (DeptDO.PARENT_ID_ROOT.equals(dept.getParentId())) {
            return true;
        }
        DeptDO parent = deptService.getDept(dept.getParentId());
        return parent == null || parent.getDeptType() == null;
    }

    /**
     * 计算合同第一级分配的总金额
     */
    private BigDecimal calculateFirstLevelAllocatedAmount(Long contractId) {
        List<ContractDeptAllocationDO> firstLevelAllocations = contractDeptAllocationMapper.selectFirstLevelByContractId(contractId);
        return firstLevelAllocations.stream()
                .map(ContractDeptAllocationDO::getReceivedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
