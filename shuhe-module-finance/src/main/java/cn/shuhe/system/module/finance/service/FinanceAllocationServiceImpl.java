package cn.shuhe.system.module.finance.service;

import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.finance.api.crm.FinanceCrmDataApi;
import cn.shuhe.system.module.finance.api.crm.dto.ContractAllocationsDTO;
import cn.shuhe.system.module.finance.api.crm.dto.ContractSummaryDTO;
import cn.shuhe.system.module.finance.api.crm.dto.DeptAllocationDTO;
import cn.shuhe.system.module.finance.controller.admin.vo.FinanceAllocationRespVO;
import cn.shuhe.system.module.finance.controller.admin.vo.FinanceAllocationSaveReqVO;
import cn.shuhe.system.module.finance.controller.admin.vo.FinanceContractSummaryVO;
import cn.shuhe.system.module.finance.controller.admin.vo.FinanceInitAllocationReqVO;
import cn.shuhe.system.module.finance.dal.dataobject.FinanceAllocationDO;
import cn.shuhe.system.module.finance.dal.mysql.FinanceAllocationMapper;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.service.dept.DeptService;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.finance.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class FinanceAllocationServiceImpl implements FinanceAllocationService {

    @Resource
    private FinanceAllocationMapper allocationMapper;

    @Resource
    @Lazy
    private FinanceCrmDataApi financeCrmDataApi;

    @Resource
    private AdminUserService adminUserService;

    @Resource
    private DeptService deptService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAllocation(FinanceAllocationSaveReqVO createReqVO) {
        FinanceAllocationDO allocation = BeanUtils.toBean(createReqVO, FinanceAllocationDO.class);
        allocationMapper.insert(allocation);
        return allocation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAllocation(FinanceAllocationSaveReqVO updateReqVO) {
        validateAllocationExists(updateReqVO.getId());
        FinanceAllocationDO updateObj = BeanUtils.toBean(updateReqVO, FinanceAllocationDO.class);
        allocationMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAllocation(Long id) {
        validateAllocationExists(id);
        List<FinanceAllocationDO> children = allocationMapper.selectListByParentId(id);
        if (!children.isEmpty()) {
            for (FinanceAllocationDO child : children) {
                deleteAllocation(child.getId());
            }
        }
        allocationMapper.deleteById(id);
    }

    @Override
    public FinanceAllocationDO getAllocation(Long id) {
        return allocationMapper.selectById(id);
    }

    @Override
    public List<FinanceAllocationRespVO> getAllocationTree(Long contractId) {
        List<FinanceAllocationDO> allAllocations = allocationMapper.selectListByContractId(contractId);
        if (allAllocations.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<FinanceAllocationDO>> parentMap = allAllocations.stream()
                .collect(Collectors.groupingBy(a -> a.getParentId() != null ? a.getParentId() : 0L));

        List<FinanceAllocationDO> roots = parentMap.getOrDefault(0L, Collections.emptyList());
        return roots.stream().map(root -> buildTreeNode(root, parentMap)).collect(Collectors.toList());
    }

    @Override
    public List<FinanceAllocationRespVO> getChildAllocations(Long parentId) {
        List<FinanceAllocationDO> children = allocationMapper.selectListByParentId(parentId);
        return children.stream().map(child -> {
            FinanceAllocationRespVO vo = BeanUtils.toBean(child, FinanceAllocationRespVO.class);
            BigDecimal childSum = allocationMapper.sumChildAllocations(child.getId());
            vo.setChildAllocatedAmount(childSum);
            vo.setRemainingAmount(child.getAllocatedAmount().subtract(childSum).max(BigDecimal.ZERO));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<FinanceContractSummaryVO> getContractSummaryList() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return Collections.emptyList();
        }

        List<ContractSummaryDTO> contracts = financeCrmDataApi.getContractListForFinance(userId);
        if (contracts.isEmpty()) {
            return Collections.emptyList();
        }

        List<FinanceContractSummaryVO> result = new ArrayList<>(contracts.size());
        for (ContractSummaryDTO dto : contracts) {
            FinanceContractSummaryVO vo = new FinanceContractSummaryVO();
            vo.setContractId(dto.getId());
            vo.setContractNo(dto.getNo());
            vo.setContractName(dto.getName());
            vo.setAuditStatus(dto.getAuditStatus());
            vo.setStartTime(dto.getStartTime());
            vo.setEndTime(dto.getEndTime());
            vo.setTotalPrice(dto.getTotalPrice() != null ? dto.getTotalPrice() : BigDecimal.ZERO);
            vo.setCustomerName(dto.getCustomerName() != null ? dto.getCustomerName() : "");
            vo.setOwnerUserId(dto.getOwnerUserId());
            vo.setOwnerUserName(dto.getOwnerUserName() != null ? dto.getOwnerUserName() : "");
            vo.setBusinessId(dto.getBusinessId());
            vo.setBusinessName(dto.getBusinessName());
            vo.setBusinessTotalPrice(dto.getBusinessTotalPrice());

            if (dto.getDeptAllocations() != null && !dto.getDeptAllocations().isEmpty()) {
                Set<Long> deptIds = dto.getDeptAllocations().stream()
                        .map(DeptAllocationDTO::getDeptId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                Map<Long, DeptDO> deptMap = deptService.getDeptMap(deptIds);

                Set<Long> leaderUserIds = deptMap.values().stream()
                        .map(DeptDO::getLeaderUserId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                Map<Long, AdminUserDO> leaderMap = adminUserService.getUserMap(leaderUserIds);

                List<FinanceContractSummaryVO.DeptInfo> deptInfoList = new ArrayList<>();
                for (DeptAllocationDTO da : dto.getDeptAllocations()) {
                    FinanceContractSummaryVO.DeptInfo info = new FinanceContractSummaryVO.DeptInfo();
                    info.setDeptId(da.getDeptId());
                    info.setDeptName(da.getDeptName());
                    info.setAmount(da.getAmount());

                    DeptDO dept = deptMap.get(da.getDeptId());
                    if (dept != null && dept.getLeaderUserId() != null) {
                        AdminUserDO leader = leaderMap.get(dept.getLeaderUserId());
                        info.setLeaderName(leader != null ? leader.getNickname() : "");
                    }
                    deptInfoList.add(info);
                }
                vo.setDeptInfoList(deptInfoList);
            }

            BigDecimal allocated = allocationMapper.sumLevel1Allocations(dto.getId());
            vo.setAllocatedAmount(allocated);
            vo.setUnallocatedAmount(vo.getTotalPrice().subtract(allocated).max(BigDecimal.ZERO));

            result.add(vo);
        }
        return result;
    }

    private FinanceAllocationRespVO buildTreeNode(FinanceAllocationDO node, Map<Long, List<FinanceAllocationDO>> parentMap) {
        FinanceAllocationRespVO vo = BeanUtils.toBean(node, FinanceAllocationRespVO.class);

        BigDecimal childSum = allocationMapper.sumChildAllocations(node.getId());
        vo.setChildAllocatedAmount(childSum);
        vo.setRemainingAmount(node.getAllocatedAmount().subtract(childSum).max(BigDecimal.ZERO));

        List<FinanceAllocationDO> children = parentMap.getOrDefault(node.getId(), Collections.emptyList());
        if (!children.isEmpty()) {
            vo.setChildren(children.stream()
                    .map(child -> buildTreeNode(child, parentMap))
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initAllocationsFromContract(FinanceInitAllocationReqVO reqVO) {
        Long contractId = reqVO.getContractId();
        List<FinanceAllocationDO> existing = allocationMapper.selectListByContractIdAndLevel(contractId, 1);
        if (!existing.isEmpty()) {
            throw exception(FINANCE_ALLOCATION_ALREADY_INITIALIZED);
        }

        for (FinanceInitAllocationReqVO.DeptAllocationItem item : reqVO.getItems()) {
            FinanceAllocationDO allocation = new FinanceAllocationDO();
            allocation.setContractId(contractId);
            allocation.setParentId(null);
            allocation.setAllocationLevel(1);
            allocation.setAllocationType("dept");
            allocation.setDeptId(item.getDeptId());
            allocation.setDeptName(item.getDeptName());
            allocation.setName(item.getDeptName());
            allocation.setAllocatedAmount(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
            allocationMapper.insert(allocation);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoInitAllocationsFromContract(Long contractId) {
        List<FinanceAllocationDO> existing = allocationMapper.selectListByContractIdAndLevel(contractId, 1);
        if (!existing.isEmpty()) {
            return;
        }

        ContractAllocationsDTO allocationsDTO = financeCrmDataApi.getContractAllocations(contractId);
        if (allocationsDTO == null || allocationsDTO.getDeptAllocations() == null || allocationsDTO.getDeptAllocations().isEmpty()) {
            return;
        }

        for (DeptAllocationDTO deptAlloc : allocationsDTO.getDeptAllocations()) {
            FinanceAllocationDO allocation = new FinanceAllocationDO();
            allocation.setContractId(contractId);
            allocation.setParentId(null);
            allocation.setAllocationLevel(1);
            allocation.setAllocationType("dept");
            allocation.setDeptId(deptAlloc.getDeptId());
            allocation.setDeptName(deptAlloc.getDeptName());
            allocation.setName(deptAlloc.getDeptName());
            allocation.setAllocatedAmount(deptAlloc.getAmount() != null ? deptAlloc.getAmount() : BigDecimal.ZERO);
            DeptDO dept = deptService.getDept(deptAlloc.getDeptId());
            if (dept != null && dept.getDeptType() != null) {
                allocation.setDeptType(dept.getDeptType());
            }
            allocationMapper.insert(allocation);
        }
    }

    private FinanceAllocationDO validateAllocationExists(Long id) {
        FinanceAllocationDO allocation = allocationMapper.selectById(id);
        if (allocation == null) {
            throw exception(FINANCE_SERVICE_ALLOCATION_NOT_EXISTS);
        }
        return allocation;
    }

}
