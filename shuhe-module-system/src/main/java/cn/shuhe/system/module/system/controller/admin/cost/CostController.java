package cn.shuhe.system.module.system.controller.admin.cost;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.system.controller.admin.cost.vo.SalaryRuleRespVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.UserCostPageReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.UserCostRespVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.WorkingDaysRespVO;
import cn.shuhe.system.module.system.service.cost.CostCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.module.system.service.cost.CostCalculationServiceImpl.*;

/**
 * 管理后台 - 成本管理
 */
@Tag(name = "管理后台 - 成本管理")
@RestController
@RequestMapping("/system/cost")
@Validated
public class CostController {

    @Resource
    private CostCalculationService costCalculationService;

    @GetMapping("/user-list")
    @Operation(summary = "获取用户成本列表")
    @PreAuthorize("@ss.hasPermission('system:cost:query')")
    public CommonResult<PageResult<UserCostRespVO>> getUserCostPage(@Valid UserCostPageReqVO reqVO) {
        // 设置默认年月
        if (reqVO.getYear() == null) {
            reqVO.setYear(LocalDate.now().getYear());
        }
        if (reqVO.getMonth() == null) {
            reqVO.setMonth(LocalDate.now().getMonthValue());
        }
        return success(costCalculationService.getUserCostPage(reqVO));
    }

    @GetMapping("/user/{id}")
    @Operation(summary = "获取单个用户成本详情")
    @Parameters({
            @Parameter(name = "id", description = "用户编号", required = true, example = "1"),
            @Parameter(name = "year", description = "年份", example = "2026"),
            @Parameter(name = "month", description = "月份", example = "1")
    })
    @PreAuthorize("@ss.hasPermission('system:cost:query')")
    public CommonResult<UserCostRespVO> getUserCost(
            @PathVariable("id") Long userId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        if (month == null) {
            month = LocalDate.now().getMonthValue();
        }
        return success(costCalculationService.getUserCost(userId, year, month));
    }

    @GetMapping("/working-days")
    @Operation(summary = "获取指定月份工作日信息")
    @Parameters({
            @Parameter(name = "year", description = "年份", required = true, example = "2026"),
            @Parameter(name = "month", description = "月份", required = true, example = "1")
    })
    @PreAuthorize("@ss.hasPermission('system:cost:query')")
    public CommonResult<WorkingDaysRespVO> getWorkingDays(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        return success(costCalculationService.getWorkingDaysInfo(year, month));
    }

    @GetMapping("/salary-rules")
    @Operation(summary = "获取工资规则配置")
    @PreAuthorize("@ss.hasPermission('system:cost:query')")
    public CommonResult<SalaryRuleRespVO> getSalaryRules() {
        Map<String, BigDecimal> rules = costCalculationService.getSalaryRules();

        SalaryRuleRespVO respVO = new SalaryRuleRespVO();
        List<SalaryRuleRespVO.DeptTypeSalaryRule> deptTypeRules = new ArrayList<>();

        // 部门类型列表
        int[] deptTypes = {DEPT_TYPE_SECURITY_SERVICE, DEPT_TYPE_SECURITY_OPERATION, DEPT_TYPE_DATA_SECURITY};
        String[] deptTypeNames = {"安全服务", "安全运营", "数据安全"};
        String[] positionLevels = {"P1-1", "P1-2", "P1-3", "P2-1", "P2-2", "P2-3", "P3-1", "P3-2", "P3-3"};

        for (int i = 0; i < deptTypes.length; i++) {
            SalaryRuleRespVO.DeptTypeSalaryRule deptRule = new SalaryRuleRespVO.DeptTypeSalaryRule();
            deptRule.setDeptType(deptTypes[i]);
            deptRule.setDeptTypeName(deptTypeNames[i]);

            List<SalaryRuleRespVO.PositionLevelSalary> salaries = new ArrayList<>();
            for (String level : positionLevels) {
                String key = deptTypes[i] + "-" + level;
                BigDecimal salary = rules.get(key);
                if (salary != null) {
                    SalaryRuleRespVO.PositionLevelSalary levelSalary = new SalaryRuleRespVO.PositionLevelSalary();
                    levelSalary.setPositionLevel(level);
                    levelSalary.setSalary(salary);
                    levelSalary.setMonthlyCost(costCalculationService.calculateMonthlyCost(salary));
                    salaries.add(levelSalary);
                }
            }
            deptRule.setSalaries(salaries);
            deptTypeRules.add(deptRule);
        }

        respVO.setDeptTypeRules(deptTypeRules);
        return success(respVO);
    }

}
