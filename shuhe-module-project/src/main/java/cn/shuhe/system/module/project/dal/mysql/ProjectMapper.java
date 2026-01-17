package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectPageReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 项目 Mapper（顶层项目）
 */
@Mapper
public interface ProjectMapper extends BaseMapperX<ProjectDO> {

    default PageResult<ProjectDO> selectPage(ProjectPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProjectDO>()
                .eqIfPresent(ProjectDO::getDeptType, reqVO.getDeptType())
                .eqIfPresent(ProjectDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ProjectDO::getName, reqVO.getName())
                .likeIfPresent(ProjectDO::getCode, reqVO.getCode())
                .likeIfPresent(ProjectDO::getCustomerName, reqVO.getCustomerName())
                .orderByDesc(ProjectDO::getId));
    }

    default List<ProjectDO> selectListByDeptType(Integer deptType) {
        return selectList(new LambdaQueryWrapperX<ProjectDO>()
                .eq(ProjectDO::getDeptType, deptType)
                .orderByDesc(ProjectDO::getId));
    }

    default ProjectDO selectByCode(String code) {
        return selectOne(ProjectDO::getCode, code);
    }

    default ProjectDO selectByContractId(Long contractId) {
        return selectOne(ProjectDO::getContractId, contractId);
    }

    default PageResult<ProjectDO> selectPageByIds(ProjectPageReqVO reqVO, List<Long> projectIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProjectDO>()
                .in(ProjectDO::getId, projectIds)
                .eqIfPresent(ProjectDO::getDeptType, reqVO.getDeptType())
                .eqIfPresent(ProjectDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ProjectDO::getName, reqVO.getName())
                .likeIfPresent(ProjectDO::getCode, reqVO.getCode())
                .likeIfPresent(ProjectDO::getCustomerName, reqVO.getCustomerName())
                .orderByDesc(ProjectDO::getId));
    }

}
