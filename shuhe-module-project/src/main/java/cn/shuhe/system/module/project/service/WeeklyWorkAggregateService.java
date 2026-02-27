package cn.shuhe.system.module.project.service;

import cn.shuhe.system.module.project.controller.admin.vo.GlobalOverviewRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.TeamOverviewRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.WeeklyWorkAggregateReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.WeeklyWorkAggregateRespVO;

import java.util.List;
import java.util.Map;

/**
 * 周工作聚合服务接口
 * 聚合日常管理记录和项目管理记录
 */
public interface WeeklyWorkAggregateService {

    /**
     * 获取某周的工作聚合数据
     *
     * @param reqVO 查询条件（年份、周数）
     * @return 周工作聚合数据
     */
    WeeklyWorkAggregateRespVO getWeeklyWorkAggregate(WeeklyWorkAggregateReqVO reqVO);

    /**
     * 获取当前周的工作聚合数据（当前登录用户）
     *
     * @return 周工作聚合数据
     */
    WeeklyWorkAggregateRespVO getCurrentWeekAggregate();

    /**
     * 获取当前用户可查看的员工列表（包括自己和下属）
     * 
     * @return 员工列表，每个员工包含 id, nickname, deptName
     */
    List<Map<String, Object>> getViewableUserList();

    /**
     * 获取团队工作总览（管理层专用）
     * 汇总所有下属在指定周的工作记录
     *
     * @param year 年份
     * @param weekNumber 周数
     * @return 团队工作总览
     */
    TeamOverviewRespVO getTeamOverview(Integer year, Integer weekNumber);

    /**
     * 获取当前用户的工作模式
     *
     * @return 工作模式 1-驻场 2-二线
     */
    Integer getCurrentUserWorkMode();

    /**
     * 获取当前用户的工作模式及驻场项目信息
     *
     * @return 包含 workMode、驻场项目信息的 Map
     */
    Map<String, Object> getCurrentUserWorkModeInfo();

    /**
     * 获取全局总览（总经办专用）
     * 汇总所有部门、所有项目的工作情况
     *
     * @param year 年份
     * @param weekNumber 周数
     * @return 全局总览数据
     */
    GlobalOverviewRespVO getGlobalOverview(Integer year, Integer weekNumber);

}
