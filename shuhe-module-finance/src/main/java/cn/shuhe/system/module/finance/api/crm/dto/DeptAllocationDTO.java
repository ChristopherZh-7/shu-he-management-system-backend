package cn.shuhe.system.module.finance.api.crm.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeptAllocationDTO {

    private Long deptId;
    private String deptName;
    private BigDecimal amount;
}
