package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.project.controller.admin.vo.DailyManagementRecordPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.DailyManagementRecordSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.DailyManagementRecordDO;

import jakarta.validation.Valid;

/**
 * 日常管理记录 Service 接口
 */
public interface DailyManagementRecordService {

    /**
     * 创建日常管理记录
     *
     * @param createReqVO 创建信息
     * @return 记录ID
     */
    Long createRecord(@Valid DailyManagementRecordSaveReqVO createReqVO);

    /**
     * 更新日常管理记录
     *
     * @param updateReqVO 更新信息
     */
    void updateRecord(@Valid DailyManagementRecordSaveReqVO updateReqVO);

    /**
     * 删除日常管理记录
     *
     * @param id 记录ID
     */
    void deleteRecord(Long id);

    /**
     * 获取日常管理记录
     *
     * @param id 记录ID
     * @return 记录信息
     */
    DailyManagementRecordDO getRecord(Long id);

    /**
     * 获取某人某年某周的记录
     *
     * @param year 年份
     * @param weekNumber 周数
     * @return 记录信息
     */
    DailyManagementRecordDO getMyRecordByYearAndWeek(Integer year, Integer weekNumber);

    /**
     * 分页查询日常管理记录
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<DailyManagementRecordDO> getRecordPage(DailyManagementRecordPageReqVO pageReqVO);

}
