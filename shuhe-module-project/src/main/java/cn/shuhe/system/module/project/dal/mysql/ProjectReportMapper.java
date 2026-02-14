package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectReportPageReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectReportDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProjectReportMapper extends BaseMapperX<ProjectReportDO> {

    default PageResult<ProjectReportDO> selectPage(ProjectReportPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProjectReportDO>()
                .eqIfPresent(ProjectReportDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(ProjectReportDO::getProjectType, reqVO.getProjectType())
                .eqIfPresent(ProjectReportDO::getYear, reqVO.getYear())
                .eqIfPresent(ProjectReportDO::getWeekNumber, reqVO.getWeekNumber())
                .eqIfPresent(ProjectReportDO::getCreator, reqVO.getCreatorId() != null ? String.valueOf(reqVO.getCreatorId()) : null)
                .eqIfPresent(ProjectReportDO::getDeptId, reqVO.getDeptId())
                .and(reqVO.getKeyword() != null && !reqVO.getKeyword().isEmpty(), wrapper ->
                        wrapper.like(ProjectReportDO::getProgress, reqVO.getKeyword())
                                .or().like(ProjectReportDO::getIssues, reqVO.getKeyword())
                                .or().like(ProjectReportDO::getProjectName, reqVO.getKeyword())
                )
                .orderByDesc(ProjectReportDO::getYear)
                .orderByDesc(ProjectReportDO::getWeekNumber));
    }

    default ProjectReportDO selectByCreatorAndProjectAndWeek(String creatorId, Long projectId, Integer year, Integer weekNumber) {
        return selectOne(new LambdaQueryWrapperX<ProjectReportDO>()
                .eq(ProjectReportDO::getCreator, creatorId)
                .eq(ProjectReportDO::getProjectId, projectId)
                .eq(ProjectReportDO::getYear, year)
                .eq(ProjectReportDO::getWeekNumber, weekNumber));
    }

    default List<ProjectReportDO> selectListByYearAndWeek(Integer year, Integer weekNumber) {
        return selectList(new LambdaQueryWrapperX<ProjectReportDO>()
                .eq(ProjectReportDO::getYear, year)
                .eq(ProjectReportDO::getWeekNumber, weekNumber)
                .orderByAsc(ProjectReportDO::getProjectId));
    }

}
