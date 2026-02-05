package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.project.dal.dataobject.EmployeeScheduleDO;
import cn.shuhe.system.module.project.service.EmployeeScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

/**
 * 员工工作排期 Controller
 */
@Tag(name = "管理后台 - 员工工作排期")
@RestController
@RequestMapping("/project/employee-schedule")
@Validated
public class EmployeeScheduleController {

    @Resource
    private EmployeeScheduleService employeeScheduleService;

    // ==================== 员工空置查询 ====================

    @GetMapping("/dept-status")
    @Operation(summary = "获取部门员工空置情况")
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:employee-schedule:query')")
    public CommonResult<List<Map<String, Object>>> getDeptEmployeeStatus(@RequestParam("deptId") Long deptId) {
        return success(employeeScheduleService.getDeptEmployeeStatus(deptId));
    }

    @GetMapping("/available-employees")
    @Operation(summary = "获取部门空闲员工列表")
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:employee-schedule:query')")
    public CommonResult<List<Map<String, Object>>> getAvailableEmployees(@RequestParam("deptId") Long deptId) {
        return success(employeeScheduleService.getAvailableEmployees(deptId));
    }

    @GetMapping("/earliest-available")
    @Operation(summary = "获取部门最早可安排时间")
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:employee-schedule:query')")
    public CommonResult<LocalDateTime> getEarliestAvailableTime(@RequestParam("deptId") Long deptId) {
        return success(employeeScheduleService.getEarliestAvailableTime(deptId));
    }

    @GetMapping("/check-available")
    @Operation(summary = "检查员工在指定时间段是否可用")
    @PreAuthorize("@ss.hasPermission('project:employee-schedule:query')")
    public CommonResult<Boolean> checkEmployeeAvailable(
            @RequestParam("userId") Long userId,
            @RequestParam("startTime") LocalDateTime startTime,
            @RequestParam("endTime") LocalDateTime endTime) {
        return success(employeeScheduleService.isEmployeeAvailable(userId, startTime, endTime));
    }

    // ==================== 排期查询 ====================

    @GetMapping("/get")
    @Operation(summary = "获取排期详情")
    @Parameter(name = "id", description = "排期ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:employee-schedule:query')")
    public CommonResult<EmployeeScheduleDO> getSchedule(@RequestParam("id") Long id) {
        return success(employeeScheduleService.getSchedule(id));
    }

    @GetMapping("/list-by-user")
    @Operation(summary = "获取员工的排期列表")
    @Parameter(name = "userId", description = "员工ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:employee-schedule:query')")
    public CommonResult<List<EmployeeScheduleDO>> getSchedulesByUserId(@RequestParam("userId") Long userId) {
        return success(employeeScheduleService.getSchedulesByUserId(userId));
    }

    @GetMapping("/list-by-dept")
    @Operation(summary = "获取部门的排期列表")
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:employee-schedule:query')")
    public CommonResult<List<EmployeeScheduleDO>> getSchedulesByDeptId(@RequestParam("deptId") Long deptId) {
        return success(employeeScheduleService.getSchedulesByDeptId(deptId));
    }

    @GetMapping("/queue-by-dept")
    @Operation(summary = "获取部门排队列表")
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:employee-schedule:query')")
    public CommonResult<List<EmployeeScheduleDO>> getQueueByDeptId(@RequestParam("deptId") Long deptId) {
        return success(employeeScheduleService.getQueueByDeptId(deptId));
    }

}
