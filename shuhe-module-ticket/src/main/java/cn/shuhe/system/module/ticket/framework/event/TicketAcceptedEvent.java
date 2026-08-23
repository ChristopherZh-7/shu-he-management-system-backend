package cn.shuhe.system.module.ticket.framework.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 工单「接单」领域事件。由 {@code TicketServiceImpl.acceptTicket} 在事务提交后通过
 * {@link org.springframework.context.ApplicationEventPublisher} 同步发布；由各业务模块通过
 * {@link org.springframework.context.event.EventListener} 监听并按 {@link #businessType} 路由处理。
 *
 * <p>跨模块解耦的核心：ticket 模块不依赖任何业务模块；业务模块依赖 ticket 模块并实现 listener。
 *
 * <p>事件采用同步发布（默认行为）。listener 抛出的异常**会**回滚 acceptTicket 事务，请在 listener 内部
 * 用 try/catch 包住外部副作用，仅在数据写入失败时才让异常冒泡。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAcceptedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单 ID。 */
    private Long ticketId;

    /** 工单编号（便于业务侧日志检索）。 */
    private String ticketNo;

    /** 工单标题（便于业务侧落业务表标题字段）。 */
    private String title;

    /** 业务类型 {@code shuhe_ticket.business_type}，listener 据此过滤。 */
    private String businessType;

    /** 业务表主键（可选）；若为空表示通用工单。 */
    private Long businessId;

    /** 精确服务项 ID；服务工单驱动器必须按此字段处理，不再模糊反查。 */
    private Long serviceItemId;

    /** 工单归属部门 ID（业务侧若需冗余可用）。 */
    private Long deptId;

    /** 工单提单人 ID（可作为业务侧 requestUserId）。 */
    private Long creatorId;

    /** 工单提单人姓名快照。 */
    private String creatorName;

    /** 接单人（主管）ID。 */
    private Long acceptedBy;

    /** 接单人姓名快照。 */
    private String acceptedByName;

    /** 主管接单时指派的执行人 ID 列表（非空、已去重）。 */
    private List<Long> executorIds;

    /** 接单备注（来自前端 reqVO.remark）。 */
    private String remark;

    /** 工单扩展字段（业务驱动器消费业务参数的载荷）。 */
    private Map<String, Object> extJson;

}
