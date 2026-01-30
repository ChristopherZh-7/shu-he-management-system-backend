package cn.shuhe.system.module.project.controller.admin;

import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.DailyManagementRecordPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.DailyManagementRecordRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.DailyManagementRecordSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.DailyManagementRecordDO;
import cn.shuhe.system.module.project.service.DailyManagementRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 日常管理记录")
@RestController
@RequestMapping("/project/daily-management-record")
@Validated
@Slf4j
public class DailyManagementRecordController {

    @Resource
    private DailyManagementRecordService dailyRecordService;

    @PostMapping("/create")
    @Operation(summary = "创建日常管理记录")
    @PreAuthorize("@ss.hasPermission('project:daily-record:create')")
    public CommonResult<Long> createRecord(@Valid @RequestBody DailyManagementRecordSaveReqVO createReqVO) {
        return success(dailyRecordService.createRecord(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新日常管理记录")
    @PreAuthorize("@ss.hasPermission('project:daily-record:update')")
    public CommonResult<Boolean> updateRecord(@Valid @RequestBody DailyManagementRecordSaveReqVO updateReqVO) {
        dailyRecordService.updateRecord(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除日常管理记录")
    @Parameter(name = "id", description = "记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:daily-record:delete')")
    public CommonResult<Boolean> deleteRecord(@RequestParam("id") Long id) {
        dailyRecordService.deleteRecord(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取日常管理记录详情")
    @Parameter(name = "id", description = "记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:daily-record:query')")
    public CommonResult<DailyManagementRecordRespVO> getRecord(@RequestParam("id") Long id) {
        DailyManagementRecordDO record = dailyRecordService.getRecord(id);
        return success(convertToRespVO(record));
    }

    @GetMapping("/get-by-week")
    @Operation(summary = "获取某年某周的日常管理记录")
    @Parameter(name = "year", description = "年份", required = true)
    @Parameter(name = "weekNumber", description = "周数", required = true)
    @PreAuthorize("@ss.hasPermission('project:daily-record:query')")
    public CommonResult<DailyManagementRecordRespVO> getRecordByWeek(
            @RequestParam("year") Integer year,
            @RequestParam("weekNumber") Integer weekNumber) {
        DailyManagementRecordDO record = dailyRecordService.getMyRecordByYearAndWeek(year, weekNumber);
        return success(convertToRespVO(record));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询日常管理记录")
    @PreAuthorize("@ss.hasPermission('project:daily-record:query')")
    public CommonResult<PageResult<DailyManagementRecordRespVO>> getRecordPage(@Valid DailyManagementRecordPageReqVO pageReqVO) {
        PageResult<DailyManagementRecordDO> pageResult = dailyRecordService.getRecordPage(pageReqVO);
        return success(new PageResult<>(
                pageResult.getList().stream().map(this::convertToRespVO).toList(),
                pageResult.getTotal()));
    }

    @GetMapping("/current-week-info")
    @Operation(summary = "获取当前周信息", description = "返回当前的年份、周数、周一和周五日期")
    @PreAuthorize("@ss.hasPermission('project:daily-record:query')")
    public CommonResult<Map<String, Object>> getCurrentWeekInfo() {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        
        int year = today.getYear();
        int weekNumber = today.get(weekFields.weekOfWeekBasedYear());
        
        // 计算本周一和本周五
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate friday = today.with(DayOfWeek.FRIDAY);
        
        // 如果周一在下一年，调整年份
        if (monday.getYear() != year) {
            year = monday.getYear();
            weekNumber = monday.get(weekFields.weekOfWeekBasedYear());
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("weekNumber", weekNumber);
        result.put("weekStartDate", monday.toString());
        result.put("weekEndDate", friday.toString());
        result.put("todayDayOfWeek", today.getDayOfWeek().getValue()); // 1=周一, 5=周五
        
        return success(result);
    }

    /**
     * 转换为响应VO
     */
    private DailyManagementRecordRespVO convertToRespVO(DailyManagementRecordDO record) {
        if (record == null) {
            return null;
        }
        DailyManagementRecordRespVO respVO = BeanUtils.toBean(record, DailyManagementRecordRespVO.class);
        
        // 处理附件（JSON转List）
        if (record.getAttachments() != null && !record.getAttachments().isEmpty()) {
            respVO.setAttachments(JSONUtil.toList(record.getAttachments(), String.class));
        }
        
        return respVO;
    }

}
