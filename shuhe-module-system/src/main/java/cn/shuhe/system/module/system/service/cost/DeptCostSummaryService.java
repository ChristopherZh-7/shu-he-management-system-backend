package cn.shuhe.system.module.system.service.cost;

import cn.shuhe.system.module.system.controller.admin.cost.vo.DeptCostSummaryReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.DeptCostSummaryRespVO;

/**
 * 部门费用汇总 Service 接口
 *
 * @author system
 */
public interface DeptCostSummaryService {

    /**
     * 获取部门费用汇总
     *
     * @param reqVO 请求参数（截止日期、年份、部门ID）
     * @return 部门费用汇总数据
     */
    DeptCostSummaryRespVO getDeptCostSummary(DeptCostSummaryReqVO reqVO);

    /**
     * 获取单个部门的费用汇总
     *
     * @param deptId 部门ID
     * @param year 年份
     * @param cutoffDate 截止日期
     * @return 部门汇总数据
     */
    DeptCostSummaryRespVO.DeptSummary getDeptSummary(Long deptId, int year, java.time.LocalDate cutoffDate);

}
