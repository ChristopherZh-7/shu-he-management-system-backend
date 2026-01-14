package cn.shuhe.system.module.ticket.service;

import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.ticket.controller.admin.vo.CategorySaveReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCategoryDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketCategoryMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.*;

/**
 * 工单分类 Service 实现类
 */
@Slf4j
@Service
@Validated
public class TicketCategoryServiceImpl implements TicketCategoryService {

    @Resource
    private TicketCategoryMapper categoryMapper;

    @Override
    public Long createCategory(CategorySaveReqVO createReqVO) {
        // 默认父分类为0（顶级分类）
        if (createReqVO.getParentId() == null) {
            createReqVO.setParentId(0L);
        }
        // 默认排序为0
        if (createReqVO.getSort() == null) {
            createReqVO.setSort(0);
        }
        // 默认状态为启用
        if (createReqVO.getStatus() == null) {
            createReqVO.setStatus(0);
        }
        
        TicketCategoryDO category = BeanUtils.toBean(createReqVO, TicketCategoryDO.class);
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    public void updateCategory(CategorySaveReqVO updateReqVO) {
        // 校验存在
        validateCategoryExists(updateReqVO.getId());
        
        TicketCategoryDO updateObj = BeanUtils.toBean(updateReqVO, TicketCategoryDO.class);
        categoryMapper.updateById(updateObj);
    }

    @Override
    public void deleteCategory(Long id) {
        // 校验存在
        validateCategoryExists(id);
        // 校验是否有子分类
        List<TicketCategoryDO> children = categoryMapper.selectListByParentId(id);
        if (!children.isEmpty()) {
            throw exception(TICKET_CATEGORY_HAS_CHILDREN);
        }
        // 删除
        categoryMapper.deleteById(id);
    }

    @Override
    public TicketCategoryDO getCategory(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public List<TicketCategoryDO> getCategoryList(String name, Integer status) {
        return categoryMapper.selectList(name, status);
    }

    /**
     * 校验分类是否存在
     */
    private void validateCategoryExists(Long id) {
        TicketCategoryDO category = categoryMapper.selectById(id);
        if (category == null) {
            throw exception(TICKET_CATEGORY_NOT_EXISTS);
        }
    }

}
