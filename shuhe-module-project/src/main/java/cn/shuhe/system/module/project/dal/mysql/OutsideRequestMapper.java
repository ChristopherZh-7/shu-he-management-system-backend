package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideRequestPageReqVO;
import cn.shuhe.system.module.project.dal.dataobject.OutsideRequestDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 外出请求 Mapper
 */
@Mapper
public interface OutsideRequestMapper extends BaseMapperX<OutsideRequestDO> {

    /**
     * 分页查询外出请求
     */
    default PageResult<OutsideRequestDO> selectPage(OutsideRequestPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OutsideRequestDO>()
                .eqIfPresent(OutsideRequestDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(OutsideRequestDO::getServiceItemId, reqVO.getServiceItemId())
                .eqIfPresent(OutsideRequestDO::getRequestUserId, reqVO.getRequestUserId())
                .eqIfPresent(OutsideRequestDO::getTargetDeptId, reqVO.getTargetDeptId())
                .eqIfPresent(OutsideRequestDO::getStatus, reqVO.getStatus())
                .likeIfPresent(OutsideRequestDO::getDestination, reqVO.getDestination())
                .betweenIfPresent(OutsideRequestDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(OutsideRequestDO::getId));
    }

    /**
     * 根据项目ID查询外出请求列表
     */
    default List<OutsideRequestDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<OutsideRequestDO>()
                .eq(OutsideRequestDO::getProjectId, projectId)
                .orderByDesc(OutsideRequestDO::getId));
    }

    /**
     * 根据服务项ID查询外出请求列表
     */
    default List<OutsideRequestDO> selectListByServiceItemId(Long serviceItemId) {
        return selectList(new LambdaQueryWrapperX<OutsideRequestDO>()
                .eq(OutsideRequestDO::getServiceItemId, serviceItemId)
                .orderByDesc(OutsideRequestDO::getId));
    }

    /**
     * 根据流程实例ID查询外出请求
     */
    default OutsideRequestDO selectByProcessInstanceId(String processInstanceId) {
        return selectOne(new LambdaQueryWrapperX<OutsideRequestDO>()
                .eq(OutsideRequestDO::getProcessInstanceId, processInstanceId));
    }

    /**
     * 统计服务项的外出次数（已通过或已完成）
     */
    default Long selectCountByServiceItemId(Long serviceItemId) {
        return selectCount(new LambdaQueryWrapperX<OutsideRequestDO>()
                .eq(OutsideRequestDO::getServiceItemId, serviceItemId)
                .in(OutsideRequestDO::getStatus, 1, 3)); // 已通过、已完成
    }

    /**
     * 统计目标部门的外出请求数量
     */
    default Long selectCountByTargetDeptId(Long targetDeptId) {
        return selectCount(new LambdaQueryWrapperX<OutsideRequestDO>()
                .eq(OutsideRequestDO::getTargetDeptId, targetDeptId));
    }

}
