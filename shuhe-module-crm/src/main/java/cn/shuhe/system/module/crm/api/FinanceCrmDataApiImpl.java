package cn.shuhe.system.module.crm.api;

import cn.shuhe.system.framework.common.pojo.PageParam;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.crm.controller.admin.contract.vo.contract.CrmContractPageReqVO;
import cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO;
import cn.shuhe.system.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.shuhe.system.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.shuhe.system.module.crm.service.business.CrmBusinessService;
import cn.shuhe.system.module.crm.service.contract.CrmContractService;
import cn.shuhe.system.module.crm.service.customer.CrmCustomerService;
import cn.shuhe.system.module.finance.api.crm.FinanceCrmDataApi;
import cn.shuhe.system.module.finance.api.crm.dto.ContractAllocationsDTO;
import cn.shuhe.system.module.finance.api.crm.dto.ContractSummaryDTO;
import cn.shuhe.system.module.finance.api.crm.dto.DeptAllocationDTO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinanceCrmDataApiImpl implements FinanceCrmDataApi {

    @Resource
    private CrmContractService contractService;

    @Resource
    private CrmCustomerService customerService;

    @Resource
    @Lazy
    private CrmBusinessService businessService;

    @Resource
    private AdminUserService adminUserService;

    @Override
    public List<ContractSummaryDTO> getContractListForFinance(Long userId) {
        CrmContractPageReqVO pageReqVO = new CrmContractPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<CrmContractDO> pageResult = contractService.getContractPage(pageReqVO, userId);
        List<CrmContractDO> contracts = pageResult.getList();
        if (contracts.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> customerIds = contracts.stream()
                .map(CrmContractDO::getCustomerId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CrmCustomerDO> customerMap = customerService.getCustomerMap(customerIds);

        Set<Long> ownerUserIds = contracts.stream()
                .map(CrmContractDO::getOwnerUserId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserDO> userMap = adminUserService.getUserMap(ownerUserIds);

        Set<Long> businessIds = contracts.stream()
                .map(CrmContractDO::getBusinessId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CrmBusinessDO> businessMap = businessService.getBusinessMap(businessIds);

        List<ContractSummaryDTO> result = new ArrayList<>(contracts.size());
        for (CrmContractDO contract : contracts) {
            ContractSummaryDTO dto = new ContractSummaryDTO();
            dto.setId(contract.getId());
            dto.setNo(contract.getNo());
            dto.setName(contract.getName());
            dto.setAuditStatus(contract.getAuditStatus());
            dto.setStartTime(contract.getStartTime());
            dto.setEndTime(contract.getEndTime());
            dto.setTotalPrice(contract.getTotalPrice() != null ? contract.getTotalPrice() : BigDecimal.ZERO);

            CrmCustomerDO customer = customerMap.get(contract.getCustomerId());
            dto.setCustomerId(contract.getCustomerId());
            dto.setCustomerName(customer != null ? customer.getName() : "");

            dto.setOwnerUserId(contract.getOwnerUserId());
            AdminUserDO ownerUser = userMap.get(contract.getOwnerUserId());
            dto.setOwnerUserName(ownerUser != null ? ownerUser.getNickname() : "");

            dto.setBusinessId(contract.getBusinessId());
            CrmBusinessDO business = businessMap.get(contract.getBusinessId());
            if (business != null) {
                dto.setBusinessName(business.getName());
                dto.setBusinessTotalPrice(business.getTotalPrice());
            }

            if (contract.getDeptAllocations() != null) {
                dto.setDeptAllocations(convertDeptAllocations(contract.getDeptAllocations()));
            }

            result.add(dto);
        }
        return result;
    }

    @Override
    public ContractAllocationsDTO getContractAllocations(Long contractId) {
        CrmContractDO contract = contractService.getContract(contractId);
        if (contract == null) {
            return null;
        }
        ContractAllocationsDTO dto = new ContractAllocationsDTO();
        dto.setContractId(contract.getId());
        if (contract.getDeptAllocations() != null) {
            dto.setDeptAllocations(convertDeptAllocations(contract.getDeptAllocations()));
        }
        return dto;
    }

    private List<DeptAllocationDTO> convertDeptAllocations(List<CrmBusinessDO.DeptAllocation> allocations) {
        return allocations.stream().map(da -> {
            DeptAllocationDTO dto = new DeptAllocationDTO();
            dto.setDeptId(da.getDeptId());
            dto.setDeptName(da.getDeptName());
            dto.setAmount(da.getAmount());
            return dto;
        }).collect(Collectors.toList());
    }
}
