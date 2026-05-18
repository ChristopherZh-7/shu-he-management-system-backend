package cn.shuhe.system.module.ticket.service;

import cn.shuhe.system.module.ticket.controller.admin.vo.TicketCategorySaveReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCategoryDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketCategoryMapper;
import cn.shuhe.system.module.ticket.dal.mysql.TicketMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_HAS_CHILDREN;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_HAS_TICKETS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_NAME_DUPLICATE;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_PARENT_INVALID;

/**
 * 工单分类 Service 实现。
 */
@Service
@Validated
@Slf4j
public class TicketCategoryServiceImpl implements TicketCategoryService {

    @Resource
    private TicketCategoryMapper categoryMapper;

    @Resource
    private TicketMapper ticketMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(TicketCategorySaveReqVO createReqVO) {
        Long parentId = createReqVO.getParentId() == null ? 0L : createReqVO.getParentId();
        if (parentId > 0) {
            validateCategoryExists(parentId);
        }
        if (categoryMapper.selectByParentIdAndName(parentId, createReqVO.getName()) != null) {
            throw exception(TICKET_CATEGORY_NAME_DUPLICATE);
        }
        TicketCategoryDO category = new TicketCategoryDO();
        applyForm(category, createReqVO);
        category.setParentId(parentId);
        if (category.getStatus() == null) {
            category.setStatus(0);
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(TicketCategorySaveReqVO updateReqVO) {
        TicketCategoryDO existing = validateCategoryExists(updateReqVO.getId());
        Long newParentId = updateReqVO.getParentId() == null ? 0L : updateReqVO.getParentId();
        if (!Objects.equals(newParentId, existing.getParentId())) {
            if (Objects.equals(newParentId, existing.getId())) {
                throw exception(TICKET_CATEGORY_PARENT_INVALID);
            }
            if (newParentId > 0) {
                validateCategoryExists(newParentId);
                if (isDescendant(existing.getId(), newParentId)) {
                    throw exception(TICKET_CATEGORY_PARENT_INVALID);
                }
            }
        }
        TicketCategoryDO other = categoryMapper.selectByParentIdAndName(newParentId, updateReqVO.getName());
        if (other != null && !Objects.equals(other.getId(), existing.getId())) {
            throw exception(TICKET_CATEGORY_NAME_DUPLICATE);
        }
        TicketCategoryDO updateObj = new TicketCategoryDO();
        applyForm(updateObj, updateReqVO);
        updateObj.setId(existing.getId());
        updateObj.setParentId(newParentId);
        categoryMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        validateCategoryExists(id);
        if (categoryMapper.selectCountByParentId(id) > 0) {
            throw exception(TICKET_CATEGORY_HAS_CHILDREN);
        }
        if (ticketMapper.selectCountByCategoryId(id) > 0) {
            throw exception(TICKET_CATEGORY_HAS_TICKETS);
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public TicketCategoryDO getCategory(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public List<TicketCategoryDO> getEnabledCategoryList() {
        List<TicketCategoryDO> list = categoryMapper.selectListAllEnabled();
        return list == null ? Collections.emptyList() : list;
    }

    // ========== Helpers ==========

    private TicketCategoryDO validateCategoryExists(Long id) {
        if (id == null) {
            throw exception(TICKET_CATEGORY_NOT_EXISTS);
        }
        TicketCategoryDO category = categoryMapper.selectById(id);
        if (category == null) {
            throw exception(TICKET_CATEGORY_NOT_EXISTS);
        }
        return category;
    }

    private void applyForm(TicketCategoryDO target, TicketCategorySaveReqVO form) {
        target.setName(form.getName());
        target.setCode(form.getCode());
        target.setIcon(form.getIcon());
        target.setSort(form.getSort());
        target.setDefaultAssigneeId(form.getDefaultAssigneeId());
        target.setDefaultAssigneeDeptId(form.getDefaultAssigneeDeptId());
        target.setDefaultPriority(form.getDefaultPriority());
        target.setDefaultSlaHours(form.getDefaultSlaHours());
        target.setStatus(form.getStatus());
    }

    /**
     * 判定 {@code candidateParentId} 是否是 {@code categoryId} 的后代（即把自己的子孙设为父会形成环）。
     * 通过自下而上沿 parentId 向上回溯，遇到 categoryId 即说明 candidate 在其子树内。
     */
    private boolean isDescendant(Long categoryId, Long candidateParentId) {
        Set<Long> visited = new HashSet<>();
        Long cursor = candidateParentId;
        while (cursor != null && cursor > 0 && !visited.contains(cursor)) {
            visited.add(cursor);
            if (Objects.equals(cursor, categoryId)) {
                return true;
            }
            TicketCategoryDO node = categoryMapper.selectById(cursor);
            if (node == null) {
                return false;
            }
            cursor = node.getParentId();
        }
        return false;
    }
}
