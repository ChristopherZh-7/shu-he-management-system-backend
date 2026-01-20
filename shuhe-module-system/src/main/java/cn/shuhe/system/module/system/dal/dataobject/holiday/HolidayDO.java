package cn.shuhe.system.module.system.dal.dataobject.holiday;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 节假日缓存表
 *
 * @author system
 */
@TableName("system_holiday")
@KeySequence("system_holiday_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HolidayDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 日期
     */
    private LocalDate date;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 是否节假日：1是 0否
     */
    private Integer isHoliday;

    /**
     * 节假日名称
     */
    private String holidayName;

    /**
     * 是否工作日（含调休）：1是 0否
     */
    private Integer isWorkday;

}
