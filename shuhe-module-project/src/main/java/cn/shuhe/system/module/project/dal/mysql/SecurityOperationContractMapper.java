package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.controller.admin.vo.SecurityOperationContractPageReqVO;
import cn.shuhe.system.module.project.dal.dataobject.SecurityOperationContractDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 安全运营合同 Mapper
 */
@Mapper
public interface SecurityOperationContractMapper extends BaseMapperX<SecurityOperationContractDO> {

    /**
     * 分页查询
     */
    default PageResult<SecurityOperationContractDO> selectPage(SecurityOperationContractPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SecurityOperationContractDO>()
                .eqIfPresent(SecurityOperationContractDO::getContractId, reqVO.getContractId())
                .likeIfPresent(SecurityOperationContractDO::getContractNo, reqVO.getContractNo())
                .likeIfPresent(SecurityOperationContractDO::getCustomerName, reqVO.getCustomerName())
                .likeIfPresent(SecurityOperationContractDO::getName, reqVO.getName())
                .eqIfPresent(SecurityOperationContractDO::getStatus, reqVO.getStatus())
                .orderByDesc(SecurityOperationContractDO::getId));
    }

    /**
     * 根据合同ID查询
     */
    default SecurityOperationContractDO selectByContractId(Long contractId) {
        return selectOne(SecurityOperationContractDO::getContractId, contractId);
    }

    /**
     * 根据客户ID查询列表
     */
    default List<SecurityOperationContractDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<SecurityOperationContractDO>()
                .eq(SecurityOperationContractDO::getCustomerId, customerId)
                .orderByDesc(SecurityOperationContractDO::getId));
    }

    /**
     * 根据状态查询列表
     */
    default List<SecurityOperationContractDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<SecurityOperationContractDO>()
                .eq(SecurityOperationContractDO::getStatus, status)
                .orderByDesc(SecurityOperationContractDO::getId));
    }

}
