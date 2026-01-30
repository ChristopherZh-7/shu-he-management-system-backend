package cn.shuhe.system.module.project.dal.mysql;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectPageReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 项目 Mapper（顶层项目）
 */
@Mapper
public interface ProjectMapper extends BaseMapperX<ProjectDO> {

    /**
     * 按状态统计数量（可选按项目ID列表过滤）
     *
     * @param status    状态，null 表示不按状态过滤
     * @param projectIds 项目ID列表，null 表示不过滤（查全部）
     * @return 数量
     */
    default Long selectCountByStatusAndIds(Integer status, List<Long> projectIds) {
        LambdaQueryWrapper<ProjectDO> wrapper = new LambdaQueryWrapper<ProjectDO>()
                .eq(status != null, ProjectDO::getStatus, status)
                .in(CollUtil.isNotEmpty(projectIds), ProjectDO::getId, projectIds);
        return selectCount(wrapper);
    }

    /**
     * 统计指定时间之后创建的数量（可选按项目ID列表过滤）
     */
    default Long selectCountByCreateTimeAfterAndIds(LocalDateTime after, List<Long> projectIds) {
        LambdaQueryWrapper<ProjectDO> wrapper = new LambdaQueryWrapper<ProjectDO>()
                .ge(ProjectDO::getCreateTime, after)
                .in(CollUtil.isNotEmpty(projectIds), ProjectDO::getId, projectIds);
        return selectCount(wrapper);
    }

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

    /**
     * 根据部门类型或项目ID列表查询分页
     * 用于部门负责人查看：可以看到负责部门类型下的所有项目 + 自己参与的项目
     */
    default PageResult<ProjectDO> selectPageByDeptTypesOrIds(ProjectPageReqVO reqVO, Set<Integer> deptTypes, List<Long> projectIds) {
        LambdaQueryWrapperX<ProjectDO> wrapper = new LambdaQueryWrapperX<ProjectDO>()
                .eqIfPresent(ProjectDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ProjectDO::getName, reqVO.getName())
                .likeIfPresent(ProjectDO::getCode, reqVO.getCode())
                .likeIfPresent(ProjectDO::getCustomerName, reqVO.getCustomerName());

        // 条件：部门类型在列表中 OR 项目ID在列表中
        wrapper.and(w -> {
            w.in(ProjectDO::getDeptType, deptTypes);
            if (CollUtil.isNotEmpty(projectIds)) {
                w.or().in(ProjectDO::getId, projectIds);
            }
        });

        // 如果查询条件指定了 deptType，则进一步过滤
        if (reqVO.getDeptType() != null) {
            wrapper.eq(ProjectDO::getDeptType, reqVO.getDeptType());
        }

        wrapper.orderByDesc(ProjectDO::getId);
        return selectPage(reqVO, wrapper);
    }

}
