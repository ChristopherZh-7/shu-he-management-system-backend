package cn.shuhe.system.module.system.service.cost;

import cn.shuhe.system.module.system.dal.dataobject.holiday.HolidayDO;

import java.time.LocalDate;
import java.util.List;

/**
 * 节假日 Service 接口
 *
 * @author system
 */
public interface HolidayService {

    /**
     * 同步指定年份的节假日数据
     *
     * @param year 年份
     */
    void syncHolidayData(int year);

    /**
     * 获取指定年月的节假日数据
     *
     * @param year 年份
     * @param month 月份
     * @return 节假日列表
     */
    List<HolidayDO> getHolidaysByMonth(int year, int month);

    /**
     * 获取指定年份的节假日数据
     *
     * @param year 年份
     * @return 节假日列表
     */
    List<HolidayDO> getHolidaysByYear(int year);

    /**
     * 计算两个日期之间的工作日数量
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 工作日数量
     */
    int countWorkdaysBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);

}
