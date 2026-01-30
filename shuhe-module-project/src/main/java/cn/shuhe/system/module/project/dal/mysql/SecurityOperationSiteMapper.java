package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.dal.dataobject.SecurityOperationSiteDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 安全运营驻场点 Mapper
 */
@Mapper
public interface SecurityOperationSiteMapper extends BaseMapperX<SecurityOperationSiteDO> {

    /**
     * 根据项目ID查询驻场点列表
     *
     * @param projectId 项目ID
     * @return 驻场点列表
     */
    default List<SecurityOperationSiteDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<SecurityOperationSiteDO>()
                .eq(SecurityOperationSiteDO::getProjectId, projectId)
                .orderByAsc(SecurityOperationSiteDO::getSort)
                .orderByAsc(SecurityOperationSiteDO::getId));
    }

    /**
     * 根据项目ID查询启用状态的驻场点列表
     *
     * @param projectId 项目ID
     * @return 驻场点列表
     */
    default List<SecurityOperationSiteDO> selectEnabledListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<SecurityOperationSiteDO>()
                .eq(SecurityOperationSiteDO::getProjectId, projectId)
                .eq(SecurityOperationSiteDO::getStatus, 1)
                .orderByAsc(SecurityOperationSiteDO::getSort)
                .orderByAsc(SecurityOperationSiteDO::getId));
    }

    /**
     * 统计驻场点数量
     *
     * @param projectId 项目ID
     * @return 数量
     */
    default Long selectCountByProjectId(Long projectId) {
        return selectCount(new LambdaQueryWrapperX<SecurityOperationSiteDO>()
                .eq(SecurityOperationSiteDO::getProjectId, projectId));
    }

}
