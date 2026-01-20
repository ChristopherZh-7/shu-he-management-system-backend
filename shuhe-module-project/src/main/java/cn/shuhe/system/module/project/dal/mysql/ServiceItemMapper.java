package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemPageReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 服务项 Mapper
 */
@Mapper
public interface ServiceItemMapper extends BaseMapperX<ServiceItemDO> {

    default PageResult<ServiceItemDO> selectPage(ServiceItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ServiceItemDO>()
                .eqIfPresent(ServiceItemDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(ServiceItemDO::getDeptType, reqVO.getDeptType())
                .eqIfPresent(ServiceItemDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ServiceItemDO::getServiceType, reqVO.getServiceType())
                .eqIfPresent(ServiceItemDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ServiceItemDO::getPriority, reqVO.getPriority())
                .eqIfPresent(ServiceItemDO::getManagerId, reqVO.getManagerId())
                .likeIfPresent(ServiceItemDO::getName, reqVO.getName())
                .likeIfPresent(ServiceItemDO::getCode, reqVO.getCode())
                .likeIfPresent(ServiceItemDO::getCustomerName, reqVO.getCustomerName())
                .betweenIfPresent(ServiceItemDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ServiceItemDO::getId));
    }

    default List<ServiceItemDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getProjectId, projectId)
                .orderByDesc(ServiceItemDO::getId));
    }

    default List<ServiceItemDO> selectListByProjectIdAndDeptId(Long projectId, Long deptId) {
        return selectList(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getProjectId, projectId)
                .eqIfPresent(ServiceItemDO::getDeptId, deptId)
                .orderByDesc(ServiceItemDO::getId));
    }

    default Long selectCountByProjectId(Long projectId) {
        return selectCount(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getProjectId, projectId));
    }

    default ServiceItemDO selectByCode(String code) {
        return selectOne(ServiceItemDO::getCode, code);
    }

    default List<ServiceItemDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getContractId, contractId)
                .orderByDesc(ServiceItemDO::getId));
    }

}
