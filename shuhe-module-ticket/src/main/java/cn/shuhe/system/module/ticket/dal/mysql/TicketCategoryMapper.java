package cn.shuhe.system.module.ticket.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TicketCategoryMapper extends BaseMapperX<TicketCategoryDO> {

    default List<TicketCategoryDO> selectList(String name, Integer status) {
        return selectList(new LambdaQueryWrapperX<TicketCategoryDO>()
                .likeIfPresent(TicketCategoryDO::getName, name)
                .eqIfPresent(TicketCategoryDO::getStatus, status)
                .orderByAsc(TicketCategoryDO::getSort));
    }

    default List<TicketCategoryDO> selectListByParentId(Long parentId) {
        return selectList(TicketCategoryDO::getParentId, parentId);
    }

}
