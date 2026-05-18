package cn.shuhe.system.module.ticket.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工单分类 Mapper（{@code shuhe_ticket_category}）。
 */
@Mapper
public interface TicketCategoryMapper extends BaseMapperX<TicketCategoryDO> {

    /**
     * 同级分类下按名称去重。
     */
    default TicketCategoryDO selectByParentIdAndName(Long parentId, String name) {
        return selectOne(new LambdaQueryWrapperX<TicketCategoryDO>()
                .eq(TicketCategoryDO::getParentId, parentId)
                .eq(TicketCategoryDO::getName, name));
    }

    /**
     * 按 code 全局唯一查询。
     */
    default TicketCategoryDO selectByCode(String code) {
        return selectOne(TicketCategoryDO::getCode, code);
    }

    /**
     * 列出指定父分类下的所有直接子分类。
     */
    default List<TicketCategoryDO> selectListByParentId(Long parentId) {
        return selectList(new LambdaQueryWrapperX<TicketCategoryDO>()
                .eq(TicketCategoryDO::getParentId, parentId)
                .orderByAsc(TicketCategoryDO::getSort)
                .orderByDesc(TicketCategoryDO::getId));
    }

    /**
     * 统计指定父分类下的子分类数量（删除前校验）。
     */
    default Long selectCountByParentId(Long parentId) {
        return selectCount(new LambdaQueryWrapperX<TicketCategoryDO>()
                .eq(TicketCategoryDO::getParentId, parentId));
    }

    /**
     * 列出所有启用的分类（构建前端 tree）。
     */
    default List<TicketCategoryDO> selectListAllEnabled() {
        return selectList(new LambdaQueryWrapperX<TicketCategoryDO>()
                .eq(TicketCategoryDO::getStatus, 0)
                .orderByAsc(TicketCategoryDO::getSort)
                .orderByDesc(TicketCategoryDO::getId));
    }

}
