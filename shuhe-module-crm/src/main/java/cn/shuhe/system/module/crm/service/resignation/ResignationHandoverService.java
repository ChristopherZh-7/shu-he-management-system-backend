package cn.shuhe.system.module.crm.service.resignation;

import cn.shuhe.system.module.crm.controller.admin.resignation.vo.ResignationHandoverExecuteReqVO;
import cn.shuhe.system.module.crm.controller.admin.resignation.vo.ResignationHandoverPreviewRespVO;

/**
 * 离职交接 Service 接口
 *
 * @author ShuHe
 */
public interface ResignationHandoverService {

    /**
     * 预览离职交接数据
     *
     * @param resignUserId   离职用户ID
     * @param newOwnerUserId 接任用户ID
     * @return 各模块待交接数量
     */
    ResignationHandoverPreviewRespVO preview(Long resignUserId, Long newOwnerUserId);

    /**
     * 执行离职交接
     *
     * @param reqVO  执行请求
     * @param userId 当前操作人（HR/管理员）
     */
    void execute(ResignationHandoverExecuteReqVO reqVO, Long userId);
}
