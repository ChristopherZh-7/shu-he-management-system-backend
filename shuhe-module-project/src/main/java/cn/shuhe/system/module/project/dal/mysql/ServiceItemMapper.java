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
                .eqIfPresent(ServiceItemDO::getServiceMode, reqVO.getServiceMode())
                .eqIfPresent(ServiceItemDO::getServiceMemberType, reqVO.getServiceMemberType())
                .eqIfPresent(ServiceItemDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ServiceItemDO::getServiceType, reqVO.getServiceType())
                .eqIfPresent(ServiceItemDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ServiceItemDO::getName, reqVO.getName())
                .likeIfPresent(ServiceItemDO::getCode, reqVO.getCode())
                .likeIfPresent(ServiceItemDO::getCustomerName, reqVO.getCustomerName())
                .betweenIfPresent(ServiceItemDO::getCreateTime, reqVO.getCreateTime())
                .eq(ServiceItemDO::getVisible, 1)  // 只返回可见的服务项
                .orderByDesc(ServiceItemDO::getId));
    }

    /**
     * 根据项目ID查询服务项列表（只返回可见的服务项）
     */
    default List<ServiceItemDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getProjectId, projectId)
                .eq(ServiceItemDO::getVisible, 1)  // 只返回可见的服务项
                .orderByDesc(ServiceItemDO::getId));
    }

    /**
     * 根据项目ID和服务模式查询服务项列表
     *
     * @param projectId   项目ID
     * @param serviceMode 服务模式：1-驻场 2-二线
     */
    default List<ServiceItemDO> selectListByProjectIdAndServiceMode(Long projectId, Integer serviceMode) {
        return selectList(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getProjectId, projectId)
                .eq(ServiceItemDO::getServiceMode, serviceMode)
                .eq(ServiceItemDO::getVisible, 1)
                .orderByDesc(ServiceItemDO::getId));
    }

    /**
     * 根据项目ID和部门类型查询服务项列表
     *
     * @param projectId 项目ID
     * @param deptType  部门类型：1-安全服务 2-安全运营 3-数据安全
     */
    default List<ServiceItemDO> selectListByProjectIdAndDeptType(Long projectId, Integer deptType) {
        return selectList(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getProjectId, projectId)
                .eq(ServiceItemDO::getDeptType, deptType)
                .eq(ServiceItemDO::getVisible, 1)
                .orderByDesc(ServiceItemDO::getId));
    }

    /**
     * 根据项目ID、部门类型和服务归属人员类型查询服务项列表
     * 用于安全运营按"驻场人员服务项"和"管理人员服务项"分类查询
     *
     * @param projectId         项目ID
     * @param deptType          部门类型：1-安全服务 2-安全运营 3-数据安全
     * @param serviceMemberType 服务归属人员类型：1-驻场人员 2-管理人员
     */
    default List<ServiceItemDO> selectListByProjectIdAndDeptTypeAndMemberType(Long projectId, Integer deptType, Integer serviceMemberType) {
        return selectList(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getProjectId, projectId)
                .eq(ServiceItemDO::getDeptType, deptType)
                .eqIfPresent(ServiceItemDO::getServiceMemberType, serviceMemberType)
                .eq(ServiceItemDO::getVisible, 1)
                .orderByDesc(ServiceItemDO::getId));
    }

    /**
     * 根据项目ID查询所有服务项列表（包含隐藏的）
     */
    default List<ServiceItemDO> selectAllListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getProjectId, projectId)
                .orderByDesc(ServiceItemDO::getId));
    }

    default List<ServiceItemDO> selectListByProjectIdAndDeptId(Long projectId, Long deptId) {
        return selectList(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getProjectId, projectId)
                .eqIfPresent(ServiceItemDO::getDeptId, deptId)
                .eq(ServiceItemDO::getVisible, 1)  // 只返回可见的服务项
                .orderByDesc(ServiceItemDO::getId));
    }

    default Long selectCountByProjectId(Long projectId) {
        return selectCount(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getProjectId, projectId)
                .eq(ServiceItemDO::getVisible, 1));  // 只统计可见的服务项
    }

    default ServiceItemDO selectByCode(String code) {
        return selectOne(ServiceItemDO::getCode, code);
    }

    default List<ServiceItemDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getContractId, contractId)
                .eq(ServiceItemDO::getVisible, 1)  // 只返回可见的服务项
                .orderByDesc(ServiceItemDO::getId));
    }

    /**
     * 查询外出类型的服务项（根据部门ID，不管 visible）
     * 用于外出请求发起页面选择服务项
     */
    default List<ServiceItemDO> selectOutsideServiceItemListByDeptId(Long deptId) {
        return selectList(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getDeptId, deptId)
                .eq(ServiceItemDO::getServiceType, "outside")
                .orderByDesc(ServiceItemDO::getId));
    }

    /**
     * 查询外出类型的服务项（根据项目ID，不管 visible）
     * 用于外出请求发起页面选择服务项
     */
    default List<ServiceItemDO> selectOutsideServiceItemListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getProjectId, projectId)
                .eq(ServiceItemDO::getServiceType, "outside")
                .orderByDesc(ServiceItemDO::getId));
    }

    /**
     * 根据安全运营合同ID查询服务项列表
     */
    default List<ServiceItemDO> selectListBySoContractId(Long soContractId) {
        return selectList(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getSoContractId, soContractId)
                .eq(ServiceItemDO::getVisible, 1)
                .orderByDesc(ServiceItemDO::getId));
    }

    /**
     * 根据部门类型查询项目ID列表（去重）
     * 用于过滤：只显示有指定deptType服务项的项目
     */
    default List<Long> selectProjectIdsByDeptType(Integer deptType) {
        return selectList(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getDeptType, deptType)
                .eq(ServiceItemDO::getVisible, 1)
                .select(ServiceItemDO::getProjectId))
                .stream()
                .map(ServiceItemDO::getProjectId)
                .distinct()
                .toList();
    }

    /**
     * 根据项目ID和部门类型统计服务项数量
     */
    default Long selectCountByProjectIdAndDeptType(Long projectId, Integer deptType) {
        return selectCount(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getProjectId, projectId)
                .eq(ServiceItemDO::getDeptType, deptType)
                .eq(ServiceItemDO::getVisible, 1));
    }

    /**
     * 根据部门服务单ID统计服务项数量
     */
    default Long selectCountByDeptServiceId(Long deptServiceId) {
        return selectCount(new LambdaQueryWrapperX<ServiceItemDO>()
                .eq(ServiceItemDO::getDeptServiceId, deptServiceId)
                .eq(ServiceItemDO::getVisible, 1));
    }

}
