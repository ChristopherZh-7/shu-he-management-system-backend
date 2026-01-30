package cn.shuhe.system.module.project.service;

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

}
