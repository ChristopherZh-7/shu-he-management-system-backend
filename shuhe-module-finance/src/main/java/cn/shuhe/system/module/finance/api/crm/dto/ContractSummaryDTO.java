package cn.shuhe.system.module.finance.api.crm.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContractSummaryDTO {

    private Long id;
    private String no;
    private String name;
    private Integer auditStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal totalPrice;

    private Long customerId;
    private String customerName;

    private Long ownerUserId;
    private String ownerUserName;

    private Long businessId;
    private String businessName;
    private BigDecimal businessTotalPrice;

    private List<DeptAllocationDTO> deptAllocations;
}
