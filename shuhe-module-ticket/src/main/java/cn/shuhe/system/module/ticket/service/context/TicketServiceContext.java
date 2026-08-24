package cn.shuhe.system.module.ticket.service.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 服务工单的权威业务上下文。
 *
 * <p>由项目模块根据服务项解析；工单模块只消费该快照，
 * 不接受前端直接指定项目、客户、服务类型或派单部门。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketServiceContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long serviceItemId;
    private String serviceItemCode;
    private String serviceType;
    private String serviceTypeName;
    private Integer serviceMode;
    private Integer serviceMemberType;
    private Integer deptType;

    private Long projectId;
    private String projectCode;
    private String projectName;
    private Long projectManagerId;
    private String projectManagerName;

    private Long responsibleDeptId;
    private String responsibleDeptName;

    private Long customerId;
    private String customerName;
    private Long contractId;
    private String contractNo;

    /** signed_contract / approved_early_investment */
    private String sourceType;

    /** -1 表示按需不限制。 */
    private Integer remainingCount;

}
