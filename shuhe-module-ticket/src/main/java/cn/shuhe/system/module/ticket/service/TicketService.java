package cn.shuhe.system.module.ticket.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketPageReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketSaveReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketLogDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 工单 Service 接口
 */
public interface TicketService {

    /**
     * 创建工单
     *
     * @param createReqVO 创建信息
     * @return 工单ID
     */
    Long createTicket(@Valid TicketSaveReqVO createReqVO);

    /**
     * 更新工单
     *
     * @param updateReqVO 更新信息
     */
    void updateTicket(@Valid TicketSaveReqVO updateReqVO);

    /**
     * 删除工单
     *
     * @param id 工单ID
     */
    void deleteTicket(Long id);

    /**
     * 获得工单
     *
     * @param id 工单ID
     * @return 工单
     */
    TicketDO getTicket(Long id);

    /**
     * 获得工单分页
     *
     * @param pageReqVO 分页查询
     * @return 工单分页
     */
    PageResult<TicketDO> getTicketPage(TicketPageReqVO pageReqVO);

    /**
     * 分配工单
     *
     * @param id 工单ID
     * @param assigneeId 处理人ID
     */
    void assignTicket(Long id, Long assigneeId);

    /**
     * 开始处理工单
     *
     * @param id 工单ID
     */
    void startProcess(Long id);

    /**
     * 完成工单
     *
     * @param id 工单ID
     * @param remark 完成备注
     */
    void finishTicket(Long id, String remark);

    /**
     * 关闭工单
     *
     * @param id 工单ID
     * @param remark 关闭原因
     */
    void closeTicket(Long id, String remark);

    /**
     * 取消工单
     *
     * @param id 工单ID
     * @param remark 取消原因
     */
    void cancelTicket(Long id, String remark);

    /**
     * 获取工单操作日志
     *
     * @param ticketId 工单ID
     * @return 操作日志列表
     */
    List<TicketLogDO> getTicketLogs(Long ticketId);

}
