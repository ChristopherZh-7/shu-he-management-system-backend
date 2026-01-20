package cn.shuhe.system.module.system.service.cost;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.system.controller.admin.cost.vo.PositionLevelHistoryPageReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.PositionLevelHistoryRespVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.PositionLevelHistorySaveReqVO;
import cn.shuhe.system.module.system.dal.dataobject.cost.PositionLevelHistoryDO;

import java.time.LocalDate;
import java.util.List;

/**
 * 职级变更记录 Service
 */
public interface PositionLevelHistoryService {

    /**
     * 创建职级变更记录（手动录入）
     *
     * @param reqVO 创建信息
     * @return 记录ID
     */
    Long createHistory(PositionLevelHistorySaveReqVO reqVO);

    /**
     * 更新职级变更记录
     *
     * @param reqVO 更新信息
     */
    void updateHistory(PositionLevelHistorySaveReqVO reqVO);

    /**
     * 删除职级变更记录
     *
     * @param id 记录ID
     */
    void deleteHistory(Long id);

    /**
     * 自动记录职级变更（钉钉同步时调用）
     *
     * @param userId           用户ID
     * @param oldPositionLevel 变更前职级
     * @param newPositionLevel 变更后职级
     */
    void recordPositionChange(Long userId, String oldPositionLevel, String newPositionLevel);

    /**
     * 获取用户的职级变更记录
     *
     * @param userId 用户ID
     * @return 变更记录列表
     */
    List<PositionLevelHistoryDO> getHistoryByUserId(Long userId);

    /**
     * 获取用户在指定年份的职级变更记录
     *
     * @param userId 用户ID
     * @param year   年份
     * @return 变更记录列表
     */
    List<PositionLevelHistoryDO> getHistoryByUserIdAndYear(Long userId, int year);

    /**
     * 获取用户在指定日期的职级
     * 查找生效日期 <= 指定日期的最新职级记录
     *
     * @param userId 用户ID
     * @param date   日期
     * @return 职级，如果没有记录则返回null
     */
    String getPositionLevelAtDate(Long userId, LocalDate date);

    /**
     * 分页查询职级变更记录
     *
     * @param reqVO 查询条件
     * @return 分页结果
     */
    PageResult<PositionLevelHistoryRespVO> getHistoryPage(PositionLevelHistoryPageReqVO reqVO);

    /**
     * 获取职级变更记录详情
     *
     * @param id 记录ID
     * @return 记录详情
     */
    PositionLevelHistoryRespVO getHistory(Long id);

}
