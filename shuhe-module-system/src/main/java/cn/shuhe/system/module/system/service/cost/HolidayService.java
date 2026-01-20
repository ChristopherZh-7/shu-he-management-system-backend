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
     * 判断指定日期是否为工作日
     *
     * @param date 日期
     * @return true=工作日，false=非工作日
     */
    boolean isWorkday(LocalDate date);

    /**
     * 获取指定年份的节假日数据
     *
     * @param year 年份
     * @return 节假日列表
     */
    List<HolidayDO> getHolidaysByYear(int year);

}
