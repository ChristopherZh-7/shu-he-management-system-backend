package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectWorkRecordPageReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectWorkRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 项目工作记录 Mapper
 */
@Mapper
public interface ProjectWorkRecordMapper extends BaseMapperX<ProjectWorkRecordDO> {

    /**
     * 分页查询工作记录
     *
     * @param reqVO 查询条件
     * @param deptIds 部门ID列表（包含下属部门时使用）
     * @return 分页结果
     */
    default PageResult<ProjectWorkRecordDO> selectPage(ProjectWorkRecordPageReqVO reqVO, Collection<Long> deptIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProjectWorkRecordDO>()
                .eqIfPresent(ProjectWorkRecordDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(ProjectWorkRecordDO::getProjectType, reqVO.getProjectType())
                .eqIfPresent(ProjectWorkRecordDO::getServiceItemId, reqVO.getServiceItemId())
                .eqIfPresent(ProjectWorkRecordDO::getWorkType, reqVO.getWorkType())
                .eqIfPresent(ProjectWorkRecordDO::getCreator, reqVO.getCreatorId() != null ? String.valueOf(reqVO.getCreatorId()) : null)
                .inIfPresent(ProjectWorkRecordDO::getDeptId, deptIds)
                .geIfPresent(ProjectWorkRecordDO::getRecordDate, reqVO.getRecordDateStart())
                .leIfPresent(ProjectWorkRecordDO::getRecordDate, reqVO.getRecordDateEnd())
                .and(reqVO.getKeyword() != null && !reqVO.getKeyword().isEmpty(), 
                    wrapper -> wrapper
                        .like(ProjectWorkRecordDO::getProjectName, reqVO.getKeyword())
                        .or()
                        .like(ProjectWorkRecordDO::getWorkContent, reqVO.getKeyword())
                        .or()
                        .like(ProjectWorkRecordDO::getServiceItemName, reqVO.getKeyword())
                )
                .orderByDesc(ProjectWorkRecordDO::getRecordDate)
                .orderByDesc(ProjectWorkRecordDO::getId));
    }

    /**
     * 根据项目ID查询工作记录列表
     *
     * @param projectId 项目ID
     * @return 工作记录列表
     */
    default List<ProjectWorkRecordDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<ProjectWorkRecordDO>()
                .eq(ProjectWorkRecordDO::getProjectId, projectId)
                .orderByDesc(ProjectWorkRecordDO::getRecordDate)
                .orderByDesc(ProjectWorkRecordDO::getId));
    }

    /**
     * 根据服务项ID查询工作记录列表
     *
     * @param serviceItemId 服务项ID
     * @return 工作记录列表
     */
    default List<ProjectWorkRecordDO> selectListByServiceItemId(Long serviceItemId) {
        return selectList(new LambdaQueryWrapperX<ProjectWorkRecordDO>()
                .eq(ProjectWorkRecordDO::getServiceItemId, serviceItemId)
                .orderByDesc(ProjectWorkRecordDO::getRecordDate)
                .orderByDesc(ProjectWorkRecordDO::getId));
    }

    /**
     * 根据记录人查询工作记录
     *
     * @param creatorId 记录人ID
     * @return 工作记录列表
     */
    default List<ProjectWorkRecordDO> selectListByCreator(Long creatorId) {
        return selectList(new LambdaQueryWrapperX<ProjectWorkRecordDO>()
                .eq(ProjectWorkRecordDO::getCreator, String.valueOf(creatorId))
                .orderByDesc(ProjectWorkRecordDO::getRecordDate)
                .orderByDesc(ProjectWorkRecordDO::getId));
    }

    /**
     * 统计项目的工作记录数量
     *
     * @param projectId 项目ID
     * @return 数量
     */
    default Long selectCountByProjectId(Long projectId) {
        return selectCount(new LambdaQueryWrapperX<ProjectWorkRecordDO>()
                .eq(ProjectWorkRecordDO::getProjectId, projectId));
    }

    /**
     * 统计指定日期范围内的工作记录数量
     *
     * @param creatorId 记录人ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 数量
     */
    default Long selectCountByCreatorAndDateRange(Long creatorId, LocalDate startDate, LocalDate endDate) {
        return selectCount(new LambdaQueryWrapperX<ProjectWorkRecordDO>()
                .eq(ProjectWorkRecordDO::getCreator, String.valueOf(creatorId))
                .ge(ProjectWorkRecordDO::getRecordDate, startDate)
                .le(ProjectWorkRecordDO::getRecordDate, endDate));
    }

    /**
     * 根据记录人和日期范围查询工作记录（用于周聚合查询）
     *
     * @param creatorId 记录人ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 工作记录列表
     */
    default List<ProjectWorkRecordDO> selectListByCreatorAndDateRange(Long creatorId, LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<ProjectWorkRecordDO>()
                .eq(ProjectWorkRecordDO::getCreator, String.valueOf(creatorId))
                .ge(ProjectWorkRecordDO::getRecordDate, startDate)
                .le(ProjectWorkRecordDO::getRecordDate, endDate)
                .orderByAsc(ProjectWorkRecordDO::getRecordDate)
                .orderByDesc(ProjectWorkRecordDO::getId));
    }

    /**
     * 导出查询
     *
     * @param reqVO 查询条件
     * @param deptIds 部门ID列表
     * @return 工作记录列表
     */
    default List<ProjectWorkRecordDO> selectListForExport(ProjectWorkRecordPageReqVO reqVO, Collection<Long> deptIds) {
        return selectList(new LambdaQueryWrapperX<ProjectWorkRecordDO>()
                .eqIfPresent(ProjectWorkRecordDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(ProjectWorkRecordDO::getProjectType, reqVO.getProjectType())
                .eqIfPresent(ProjectWorkRecordDO::getServiceItemId, reqVO.getServiceItemId())
                .eqIfPresent(ProjectWorkRecordDO::getWorkType, reqVO.getWorkType())
                .eqIfPresent(ProjectWorkRecordDO::getCreator, reqVO.getCreatorId() != null ? String.valueOf(reqVO.getCreatorId()) : null)
                .inIfPresent(ProjectWorkRecordDO::getDeptId, deptIds)
                .geIfPresent(ProjectWorkRecordDO::getRecordDate, reqVO.getRecordDateStart())
                .leIfPresent(ProjectWorkRecordDO::getRecordDate, reqVO.getRecordDateEnd())
                .and(reqVO.getKeyword() != null && !reqVO.getKeyword().isEmpty(),
                    wrapper -> wrapper
                        .like(ProjectWorkRecordDO::getProjectName, reqVO.getKeyword())
                        .or()
                        .like(ProjectWorkRecordDO::getWorkContent, reqVO.getKeyword())
                )
                .orderByDesc(ProjectWorkRecordDO::getRecordDate)
                .orderByDesc(ProjectWorkRecordDO::getId));
    }

}
