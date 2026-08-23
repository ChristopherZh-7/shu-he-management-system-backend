package cn.shuhe.system.module.ticket.service.context;

import java.util.List;

/**
 * 跨模块解析“项目 + 服务项 + 负责部门”。
 *
 * <p>接口放在 ticket 模块，由 project 模块实现，避免 ticket 反向依赖 project。</p>
 */
public interface TicketServiceContextResolver {

    /** 解析并校验当前用户可申请的单个服务项。 */
    TicketServiceContext resolve(Long serviceItemId, Long userId);

    /** 列出当前用户可申请的服务项；projectId 可为空。 */
    List<TicketServiceContext> listEligible(Long userId, Long projectId);

}
