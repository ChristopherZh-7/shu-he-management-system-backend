package cn.shuhe.system.module.ticket.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketCategoryRespVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketCategorySaveReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCategoryDO;
import cn.shuhe.system.module.ticket.service.TicketCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工单分类")
@RestController
@RequestMapping("/ticket/category")
@Validated
public class TicketCategoryController {

    @Resource
    private TicketCategoryService categoryService;

    @PostMapping("/create")
    @Operation(summary = "创建分类")
    @PreAuthorize("@ss.hasPermission('ticket:category:create')")
    public CommonResult<Long> create(@Valid @RequestBody TicketCategorySaveReqVO createReqVO) {
        return success(categoryService.createCategory(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改分类")
    @PreAuthorize("@ss.hasPermission('ticket:category:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody TicketCategorySaveReqVO updateReqVO) {
        categoryService.updateCategory(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除分类")
    @Parameter(name = "id", description = "分类 ID", required = true)
    @PreAuthorize("@ss.hasPermission('ticket:category:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        categoryService.deleteCategory(id);
        return success(true);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取启用的分类树")
    @PreAuthorize("@ss.hasPermission('ticket:category:query')")
    public CommonResult<List<TicketCategoryRespVO>> tree() {
        List<TicketCategoryDO> all = categoryService.getEnabledCategoryList();
        List<TicketCategoryRespVO> flat = BeanUtils.toBean(all, TicketCategoryRespVO.class);
        return success(buildTree(flat));
    }

    /**
     * 由扁平列表构建树形结构；以 parentId=0 为根，未挂上的节点也并到根列表（避免遗失）。
     */
    private List<TicketCategoryRespVO> buildTree(List<TicketCategoryRespVO> flat) {
        if (flat == null || flat.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, TicketCategoryRespVO> map = new HashMap<>();
        for (TicketCategoryRespVO node : flat) {
            map.put(node.getId(), node);
        }
        List<TicketCategoryRespVO> roots = new ArrayList<>();
        for (TicketCategoryRespVO node : flat) {
            Long pid = node.getParentId();
            if (pid == null || pid == 0L) {
                roots.add(node);
                continue;
            }
            TicketCategoryRespVO parent = map.get(pid);
            if (parent != null) {
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }
}
