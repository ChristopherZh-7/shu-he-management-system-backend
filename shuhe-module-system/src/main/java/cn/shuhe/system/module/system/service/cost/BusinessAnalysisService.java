package cn.shuhe.system.module.system.service.cost;

import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisRespVO;

/**
 * 经营分析 Service 接口
 */
public interface BusinessAnalysisService {

    /**
     * 获取经营分析数据
     *
     * @param reqVO 请求参数
     * @param currentUserId 当前用户ID（用于权限判断）
     * @return 经营分析数据
     */
    BusinessAnalysisRespVO getBusinessAnalysis(BusinessAnalysisReqVO reqVO, Long currentUserId);

    /**
     * 获取部门下的子部门/班级分析
     *
     * @param deptId 父部门ID
     * @param year 年份
     * @param cutoffDate 截止日期
     * @return 子部门分析列表
     */
    BusinessAnalysisRespVO.DeptAnalysis getDeptDetail(Long deptId, int year, java.time.LocalDate cutoffDate);

    /**
     * 获取员工个人分析
     *
     * @param userId 用户ID
     * @param year 年份
     * @param cutoffDate 截止日期
     * @return 员工分析
     */
    BusinessAnalysisRespVO.EmployeeAnalysis getEmployeeAnalysis(Long userId, int year, java.time.LocalDate cutoffDate);

}
