package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundMemberDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProjectRoundMemberMapper extends BaseMapperX<ProjectRoundMemberDO> {
    default List<ProjectRoundMemberDO> selectListByRoundId(Long roundId) {
        return selectList(new LambdaQueryWrapperX<ProjectRoundMemberDO>()
                .eq(ProjectRoundMemberDO::getRoundId, roundId)
                .orderByAsc(ProjectRoundMemberDO::getId));
    }

    default boolean existsByRoundIdAndUserId(Long roundId, Long userId) {
        return roundId != null && userId != null && selectCount(new LambdaQueryWrapperX<ProjectRoundMemberDO>()
                .eq(ProjectRoundMemberDO::getRoundId, roundId)
                .eq(ProjectRoundMemberDO::getUserId, userId)) > 0;
    }

    default boolean existsByRoundIdAndUserIdAndRole(Long roundId, Long userId, String roleType) {
        return roundId != null && userId != null && roleType != null
                && selectCount(new LambdaQueryWrapperX<ProjectRoundMemberDO>()
                .eq(ProjectRoundMemberDO::getRoundId, roundId)
                .eq(ProjectRoundMemberDO::getUserId, userId)
                .eq(ProjectRoundMemberDO::getRoleType, roleType)) > 0;
    }
}
