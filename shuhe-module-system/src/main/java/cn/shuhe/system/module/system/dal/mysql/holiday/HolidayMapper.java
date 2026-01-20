package cn.shuhe.system.module.system.dal.mysql.holiday;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.module.system.dal.dataobject.holiday.HolidayDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * 节假日缓存 Mapper
 *
 * @author system
 */
@Mapper
public interface HolidayMapper extends BaseMapperX<HolidayDO> {

    default List<HolidayDO> selectByYearAndMonth(Integer year, Integer month) {
        return selectList(HolidayDO::getYear, year, HolidayDO::getMonth, month);
    }

    default HolidayDO selectByDate(LocalDate date) {
        return selectOne(HolidayDO::getDate, date);
    }

    default List<HolidayDO> selectByYear(Integer year) {
        return selectList(HolidayDO::getYear, year);
    }

}
