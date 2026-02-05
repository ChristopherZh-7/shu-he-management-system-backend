package cn.shuhe.system.module.system.service.cost;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.system.controller.admin.cost.vo.UserCostPageReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.UserCostRespVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.WorkingDaysRespVO;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * 成本计算 Service 接口
 *
 * @author system
 */
public interface CostCalculationService {

    /**
     * 根据部门类型和职级计算基础工资
     *
     * @param deptType 部门类型：1安全服务 2安全运营 3数据安全
     * @param positionLevel 职级，如 "P1-1", "P2-2", "P3-3" 等
     * @return 基础工资
     */
    BigDecimal calculateBaseSalary(Integer deptType, String positionLevel);

    /**
     * 计算月成本 = 工资 * 1.3 + 3000
     *
     * @param salary 基础工资
     * @return 月成本
     */
    BigDecimal calculateMonthlyCost(BigDecimal salary);

    /**
     * 获取指定月份的工作日数（排除周末和节假日）
     *
     * @param year 年份
     * @param month 月份
     * @return 工作日数
     */
    int getWorkingDays(int year, int month);

    /**
     * 计算日成本
     *
     * @param monthlyCost 月成本
     * @param year 年份
     * @param month 月份
     * @return 日成本
     */
    BigDecimal calculateDailyCost(BigDecimal monthlyCost, int year, int month);

    /**
     * 获取用户成本列表（分页）
     *
     * @param reqVO 查询参数
     * @return 用户成本分页列表
     */
    PageResult<UserCostRespVO> getUserCostPage(UserCostPageReqVO reqVO);

    /**
     * 获取单个用户成本详情
     *
     * @param userId 用户ID
     * @param year 年份
     * @param month 月份
     * @return 用户成本详情
     */
    UserCostRespVO getUserCost(Long userId, int year, int month);

    /**
     * 获取指定月份的工作日信息
     *
     * @param year 年份
     * @param month 月份
     * @return 工作日信息
     */
    WorkingDaysRespVO getWorkingDaysInfo(int year, int month);

    /**
     * 获取工资规则配置
     *
     * @return 工资规则 Map，key为"部门类型-职级"，value为工资
     */
    Map<String, BigDecimal> getSalaryRules();

    /**
     * 批量获取用户年度累计成本
     * 【性能优化】避免循环调用 getUserCost 产生的 N+1 查询问题
     *
     * @param userIds 用户ID集合
     * @param year    年份
     * @param month   截止月份
     * @return 用户ID -> 年度累计成本 的映射
     */
    Map<Long, BigDecimal> batchGetUserYearToDateCost(Collection<Long> userIds, int year, int month);

}
