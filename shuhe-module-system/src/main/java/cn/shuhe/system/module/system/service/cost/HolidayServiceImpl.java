package cn.shuhe.system.module.system.service.cost;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.module.system.dal.dataobject.holiday.HolidayDO;
import cn.shuhe.system.module.system.dal.mysql.holiday.HolidayMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 节假日 Service 实现类
 * 使用 timor.tech API 获取中国节假日数据
 *
 * @author system
 */
@Slf4j
@Service
public class HolidayServiceImpl implements HolidayService {

    /**
     * 节假日API地址
     * 文档：https://timor.tech/api/holiday
     */
    private static final String HOLIDAY_API_URL = "https://timor.tech/api/holiday/year/";

    @Resource
    private HolidayMapper holidayMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncHolidayData(int year) {
        // 先检查是否已有数据
        List<HolidayDO> existing = holidayMapper.selectByYear(year);
        if (CollUtil.isNotEmpty(existing)) {
            log.info("{} 年节假日数据已存在，共 {} 条，跳过同步", year, existing.size());
            return;
        }

        log.info("开始同步 {} 年节假日数据", year);

        try {
            // 调用API获取整年数据
            String url = HOLIDAY_API_URL + year;
            String response = HttpUtil.get(url, 10000);

            JSONObject jsonResponse = JSONUtil.parseObj(response);
            if (jsonResponse.getInt("code") != 0) {
                log.error("获取节假日数据失败: {}", jsonResponse.getStr("msg"));
                // 使用默认数据
                syncDefaultHolidayData(year);
                return;
            }

            JSONObject holidayData = jsonResponse.getJSONObject("holiday");
            if (holidayData == null || holidayData.isEmpty()) {
                log.warn("节假日数据为空，使用默认数据");
                syncDefaultHolidayData(year);
                return;
            }

            // 生成全年的日期数据并插入
            int insertCount = 0;
            for (int month = 1; month <= 12; month++) {
                YearMonth yearMonth = YearMonth.of(year, month);
                int daysInMonth = yearMonth.lengthOfMonth();

                for (int day = 1; day <= daysInMonth; day++) {
                    LocalDate date = LocalDate.of(year, month, day);
                    String dateKey = String.format("%02d-%02d", month, day);

                    HolidayDO holiday = new HolidayDO();
                    holiday.setDate(date);
                    holiday.setYear(year);
                    holiday.setMonth(month);

                    // 检查是否在节假日数据中
                    JSONObject dayInfo = holidayData.getJSONObject(dateKey);
                    if (dayInfo != null) {
                        // API返回的节假日信息
                        boolean isHoliday = dayInfo.getBool("holiday", false);
                        String name = dayInfo.getStr("name", "");

                        holiday.setIsHoliday(isHoliday ? 1 : 0);
                        holiday.setHolidayName(name);
                        // 如果是节假日则不是工作日，如果是调休（holiday=false且在列表中）则是工作日
                        holiday.setIsWorkday(isHoliday ? 0 : 1);
                    } else {
                        // 普通日期
                        boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
                        holiday.setIsHoliday(0);
                        holiday.setHolidayName(null);
                        holiday.setIsWorkday(isWeekend ? 0 : 1);
                    }

                    // 插入或更新
                    insertOrUpdate(holiday);
                    insertCount++;
                }
            }

            log.info("同步 {} 年节假日数据完成，共 {} 条", year, insertCount);

        } catch (Exception e) {
            log.error("同步节假日数据异常，使用默认数据", e);
            syncDefaultHolidayData(year);
        }
    }

    @Override
    public List<HolidayDO> getHolidaysByMonth(int year, int month) {
        List<HolidayDO> holidays = holidayMapper.selectByYearAndMonth(year, month);
        if (CollUtil.isEmpty(holidays)) {
            // 尝试同步数据
            syncHolidayData(year);
            holidays = holidayMapper.selectByYearAndMonth(year, month);
        }
        return holidays;
    }

    @Override
    public List<HolidayDO> getHolidaysByYear(int year) {
        List<HolidayDO> holidays = holidayMapper.selectByYear(year);
        if (CollUtil.isEmpty(holidays)) {
            syncHolidayData(year);
            holidays = holidayMapper.selectByYear(year);
        }
        return holidays;
    }

    @Override
    public int countWorkdaysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return 0;
        }

        // 批量获取日期范围内的所有年份数据，避免 N+1 查询
        int startYear = startDate.getYear();
        int endYear = endDate.getYear();
        
        // 预加载所有相关年份的节假日数据到内存
        Map<LocalDate, HolidayDO> holidayMap = new java.util.HashMap<>();
        for (int year = startYear; year <= endYear; year++) {
            List<HolidayDO> yearHolidays = getHolidaysByYear(year);
            if (yearHolidays != null) {
                for (HolidayDO h : yearHolidays) {
                    holidayMap.put(h.getDate(), h);
                }
            }
        }

        // 在内存中计算工作日数量
        int workingDays = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            HolidayDO holiday = holidayMap.get(current);
            if (holiday != null) {
                if (holiday.getIsWorkday() != null && holiday.getIsWorkday() == 1) {
                    workingDays++;
                }
            } else {
                // 默认按周末判断
                DayOfWeek dayOfWeek = current.getDayOfWeek();
                if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                    workingDays++;
                }
            }
            current = current.plusDays(1);
        }

        return workingDays;
    }

    /**
     * 同步默认节假日数据（只排除周末）
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncDefaultHolidayData(int year) {
        // 先检查是否已有数据
        List<HolidayDO> existing = holidayMapper.selectByYear(year);
        if (CollUtil.isNotEmpty(existing)) {
            log.info("{} 年日历数据已存在，跳过生成", year);
            return;
        }

        log.info("使用默认规则生成 {} 年日历数据（只排除周末）", year);

        int insertCount = 0;
        for (int month = 1; month <= 12; month++) {
            YearMonth yearMonth = YearMonth.of(year, month);
            int daysInMonth = yearMonth.lengthOfMonth();

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = LocalDate.of(year, month, day);
                boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                        || date.getDayOfWeek() == DayOfWeek.SUNDAY;

                HolidayDO holiday = new HolidayDO();
                holiday.setDate(date);
                holiday.setYear(year);
                holiday.setMonth(month);
                holiday.setIsHoliday(0);
                holiday.setHolidayName(null);
                holiday.setIsWorkday(isWeekend ? 0 : 1);

                // 插入或更新
                insertOrUpdate(holiday);
                insertCount++;
            }
        }

        log.info("生成 {} 年默认日历数据完成，共 {} 条", year, insertCount);
    }

    /**
     * 插入或更新节假日数据
     * 注意：由于软删除机制，可能存在 deleted=1 的记录占用唯一键
     */
    private void insertOrUpdate(HolidayDO holiday) {
        // 检查是否已存在（只能查到 deleted=0 的记录）
        HolidayDO existing = holidayMapper.selectByDate(holiday.getDate());
        if (existing != null) {
            // 更新已存在的记录
            holiday.setId(existing.getId());
            holidayMapper.updateById(holiday);
        } else {
            // 尝试插入，如果失败（可能是因为有 deleted=1 的记录）则跳过
            try {
                holidayMapper.insert(holiday);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 可能存在软删除的记录，记录日志并跳过
                log.debug("日期 {} 的节假日数据已存在（可能是软删除记录），跳过插入", holiday.getDate());
            }
        }
    }

}
