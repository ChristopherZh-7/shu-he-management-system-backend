package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundRetestBatchDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProjectRoundRetestBatchMapper extends BaseMapperX<ProjectRoundRetestBatchDO> {
    default List<ProjectRoundRetestBatchDO> selectListByRoundId(Long roundId) {
        return selectList(new LambdaQueryWrapperX<ProjectRoundRetestBatchDO>()
                .eq(ProjectRoundRetestBatchDO::getRoundId, roundId)
                .orderByDesc(ProjectRoundRetestBatchDO::getBatchNo));
    }
    default int nextBatchNo(Long roundId) {
        ProjectRoundRetestBatchDO latest = selectOne(new LambdaQueryWrapperX<ProjectRoundRetestBatchDO>()
                .eq(ProjectRoundRetestBatchDO::getRoundId, roundId)
                .orderByDesc(ProjectRoundRetestBatchDO::getBatchNo)
                .last("LIMIT 1"));
        return latest == null ? 1 : latest.getBatchNo() + 1;
    }
}
