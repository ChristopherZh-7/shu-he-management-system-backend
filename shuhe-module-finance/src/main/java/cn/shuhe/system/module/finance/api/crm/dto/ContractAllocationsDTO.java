package cn.shuhe.system.module.finance.api.crm.dto;

import lombok.Data;

import java.util.List;

@Data
public class ContractAllocationsDTO {

    private Long contractId;
    private List<DeptAllocationDTO> deptAllocations;
}
