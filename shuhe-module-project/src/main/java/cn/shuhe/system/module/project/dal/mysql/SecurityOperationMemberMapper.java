package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.dal.dataobject.SecurityOperationMemberDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 安全运营人员 Mapper
 */
@Mapper
public interface SecurityOperationMemberMapper extends BaseMapperX<SecurityOperationMemberDO> {

    /**
     * 根据安全运营合同ID查询人员列表
     */
    default List<SecurityOperationMemberDO> selectListBySoContractId(Long soContractId) {
        return selectList(new LambdaQueryWrapperX<SecurityOperationMemberDO>()
                .eq(SecurityOperationMemberDO::getSoContractId, soContractId)
                .orderByAsc(SecurityOperationMemberDO::getMemberType)
                .orderByAsc(SecurityOperationMemberDO::getId));
    }

    /**
     * 根据安全运营合同ID和人员类型查询
     */
    default List<SecurityOperationMemberDO> selectListBySoContractIdAndType(Long soContractId, Integer memberType) {
        return selectList(new LambdaQueryWrapperX<SecurityOperationMemberDO>()
                .eq(SecurityOperationMemberDO::getSoContractId, soContractId)
                .eq(SecurityOperationMemberDO::getMemberType, memberType)
                .orderByAsc(SecurityOperationMemberDO::getId));
    }

    /**
     * 根据用户ID查询参与的安全运营项目
     */
    default List<SecurityOperationMemberDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<SecurityOperationMemberDO>()
                .eq(SecurityOperationMemberDO::getUserId, userId)
                .orderByDesc(SecurityOperationMemberDO::getId));
    }

    /**
     * 统计管理人员数量
     */
    default Long countManagementBySoContractId(Long soContractId) {
        return selectCount(new LambdaQueryWrapperX<SecurityOperationMemberDO>()
                .eq(SecurityOperationMemberDO::getSoContractId, soContractId)
                .eq(SecurityOperationMemberDO::getMemberType, SecurityOperationMemberDO.MEMBER_TYPE_MANAGEMENT)
                .eq(SecurityOperationMemberDO::getStatus, SecurityOperationMemberDO.STATUS_ACTIVE));
    }

    /**
     * 统计驻场人员数量
     */
    default Long countOnsiteBySoContractId(Long soContractId) {
        return selectCount(new LambdaQueryWrapperX<SecurityOperationMemberDO>()
                .eq(SecurityOperationMemberDO::getSoContractId, soContractId)
                .eq(SecurityOperationMemberDO::getMemberType, SecurityOperationMemberDO.MEMBER_TYPE_ONSITE)
                .eq(SecurityOperationMemberDO::getStatus, SecurityOperationMemberDO.STATUS_ACTIVE));
    }

    /**
     * 删除安全运营合同下的所有人员
     */
    default void deleteBySoContractId(Long soContractId) {
        delete(new LambdaQueryWrapperX<SecurityOperationMemberDO>()
                .eq(SecurityOperationMemberDO::getSoContractId, soContractId));
    }

    /**
     * 根据驻场点ID查询人员列表
     */
    default List<SecurityOperationMemberDO> selectListBySiteId(Long siteId) {
        return selectList(new LambdaQueryWrapperX<SecurityOperationMemberDO>()
                .eq(SecurityOperationMemberDO::getSiteId, siteId)
                .orderByAsc(SecurityOperationMemberDO::getId));
    }

    /**
     * 统计驻场点的人员数量
     */
    default Long countBySiteId(Long siteId) {
        return selectCount(new LambdaQueryWrapperX<SecurityOperationMemberDO>()
                .eq(SecurityOperationMemberDO::getSiteId, siteId)
                .eq(SecurityOperationMemberDO::getStatus, SecurityOperationMemberDO.STATUS_ACTIVE));
    }

}
