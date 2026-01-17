package cn.shuhe.system.module.project.controller.admin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.excel.core.util.ExcelUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemBatchSaveReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemImportExcelVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemImportRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.service.ServiceItemService;
import cn.shuhe.system.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.dept.PostDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.dal.mysql.dept.PostMapper;
import cn.shuhe.system.module.system.service.dept.DeptService;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - 服务项管理")
@RestController
@RequestMapping("/project/service-item")
@Validated
public class ServiceItemController {

    @Resource
    private ServiceItemService serviceItemService;

    @Resource
    private AdminUserService adminUserService;

    @Resource
    private PostMapper postMapper;

    @Resource
    private DeptService deptService;

    /**
     * 部门类型对应的岗位code映射
     */
    private static final Map<Integer, List<String>> DEPT_TYPE_POST_CODES = Map.of(
            1, List.of("anquanjishufuwugongchengshi", "anquanjishufuwuzuzhang"), // 安全服务
            2, List.of("anquanyunyingfuwugongchengshi", "anquanyunyingfuwuzuzhang", "anquanyunyingfuwuzhuguan"), // 安全运营
            3, List.of("shujuanquanfuwugongchengshi", "shujuanquanfuwuzhuguan") // 数据安全
    );

    @PostMapping("/create")
    @Operation(summary = "创建服务项")
    @PreAuthorize("@ss.hasPermission('project:service-item:create')")
    public CommonResult<Long> createServiceItem(@Valid @RequestBody ServiceItemSaveReqVO createReqVO) {
        // 根据当前用户的部门，向上遍历找到顶级父部门
        if (createReqVO.getDeptId() == null) {
            Long loginUserId = cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId != null) {
                AdminUserDO user = adminUserService.getUser(loginUserId);
                if (user != null && user.getDeptId() != null) {
                    // 向上遍历找到顶级父部门
                    Long topDeptId = findTopDeptId(user.getDeptId());
                    createReqVO.setDeptId(topDeptId);
                }
            }
        }
        // 校验 deptId 不能为空
        if (createReqVO.getDeptId() == null) {
            throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.shuhe.system.module.project.enums.ErrorCodeConstants.SERVICE_ITEM_DEPT_NOT_SET);
        }
        return success(serviceItemService.createServiceItem(createReqVO));
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建服务项")
    @PreAuthorize("@ss.hasPermission('project:service-item:create')")
    public CommonResult<List<Long>> batchCreateServiceItem(@Valid @RequestBody ServiceItemBatchSaveReqVO batchReqVO) {
        // 获取当前用户的顶级部门ID
        Long topDeptId = getLoginUserTopDeptId();
        if (topDeptId == null) {
            throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.shuhe.system.module.project.enums.ErrorCodeConstants.SERVICE_ITEM_DEPT_NOT_SET);
        }
        return success(serviceItemService.batchCreateServiceItem(batchReqVO, topDeptId));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获取导入服务项模板")
    public void getImportTemplate(HttpServletResponse response,
            @RequestParam("deptType") Integer deptType) throws IOException {
        // 手动创建导出 demo
        List<ServiceItemImportExcelVO> list = Arrays.asList(
                ServiceItemImportExcelVO.builder()
                        .name("某银行渗透测试")
                        .serviceType("penetration_test")
                        .customerName("某银行")
                        .planStartTime("2026-01-20 09:00:00")
                        .planEndTime("2026-01-25 18:00:00")
                        .priority(1)
                        .remark("示例服务项1").build(),
                ServiceItemImportExcelVO.builder()
                        .name("某企业安全评估")
                        .serviceType("security_assessment")
                        .customerName("某企业")
                        .planStartTime("2026-02-01 09:00:00")
                        .planEndTime("2026-02-10 18:00:00")
                        .priority(2)
                        .remark("示例服务项2").build());
        // 输出
        ExcelUtils.write(response, "服务项导入模板.xls", "服务项列表", ServiceItemImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入服务项")
    @PreAuthorize("@ss.hasPermission('project:service-item:create')")
    public CommonResult<ServiceItemImportRespVO> importServiceItem(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Long projectId,
            @RequestParam("deptType") Integer deptType) throws Exception {
        // 获取当前用户的顶级部门ID
        Long topDeptId = getLoginUserTopDeptId();
        if (topDeptId == null) {
            throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.shuhe.system.module.project.enums.ErrorCodeConstants.SERVICE_ITEM_DEPT_NOT_SET);
        }
        List<ServiceItemImportExcelVO> list = ExcelUtils.read(file, ServiceItemImportExcelVO.class);
        return success(serviceItemService.importServiceItemList(projectId, deptType, list, topDeptId));
    }

    /**
     * 获取当前登录用户的顶级部门ID
     */
    private Long getLoginUserTopDeptId() {
        Long loginUserId = cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId != null) {
            AdminUserDO user = adminUserService.getUser(loginUserId);
            if (user != null && user.getDeptId() != null) {
                return findTopDeptId(user.getDeptId());
            }
        }
        return null;
    }

    /**
     * 向上遍历找到顶级父部门ID
     * 顶级部门的 parentId 为 0 或 null
     */
    private Long findTopDeptId(Long deptId) {
        DeptDO dept = deptService.getDept(deptId);
        if (dept == null) {
            return deptId;
        }
        // 如果 parentId 为 0 或 null，说明已经是顶级部门
        if (dept.getParentId() == null || dept.getParentId() == 0L) {
            return dept.getId();
        }
        // 递归向上查找
        return findTopDeptId(dept.getParentId());
    }

    @PutMapping("/update")
    @Operation(summary = "更新服务项")
    @PreAuthorize("@ss.hasPermission('project:service-item:update')")
    public CommonResult<Boolean> updateServiceItem(@Valid @RequestBody ServiceItemSaveReqVO updateReqVO) {
        serviceItemService.updateServiceItem(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除服务项")
    @Parameter(name = "id", description = "服务项编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-item:delete')")
    public CommonResult<Boolean> deleteServiceItem(@RequestParam("id") Long id) {
        serviceItemService.deleteServiceItem(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得服务项详情")
    @Parameter(name = "id", description = "服务项编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-item:query')")
    public CommonResult<ServiceItemRespVO> getServiceItem(@RequestParam("id") Long id) {
        ServiceItemDO serviceItem = serviceItemService.getServiceItem(id);
        ServiceItemRespVO respVO = BeanUtils.toBean(serviceItem, ServiceItemRespVO.class);
        // 处理标签
        if (serviceItem != null && serviceItem.getTags() != null) {
            respVO.setTags(JSONUtil.toList(serviceItem.getTags(), String.class));
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得服务项分页")
    @PreAuthorize("@ss.hasPermission('project:service-item:query')")
    public CommonResult<PageResult<ServiceItemRespVO>> getServiceItemPage(@Valid ServiceItemPageReqVO pageReqVO) {
        // 自动设置当前用户的顶级部门ID，实现同一顶级部门下的用户能看到相同的服务项
        Long loginUserId = cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId != null) {
            AdminUserDO user = adminUserService.getUser(loginUserId);
            if (user != null && user.getDeptId() != null) {
                // 向上查找顶级部门
                Long topDeptId = findTopDeptId(user.getDeptId());
                pageReqVO.setDeptId(topDeptId);
            }
        }

        PageResult<ServiceItemDO> pageResult = serviceItemService.getServiceItemPage(pageReqVO);
        PageResult<ServiceItemRespVO> result = BeanUtils.toBean(pageResult, ServiceItemRespVO.class);
        // 处理标签
        for (int i = 0; i < pageResult.getList().size(); i++) {
            ServiceItemDO serviceItem = pageResult.getList().get(i);
            if (serviceItem.getTags() != null) {
                result.getList().get(i).setTags(JSONUtil.toList(serviceItem.getTags(), String.class));
            }
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "获得服务项列表（根据项目ID）")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-item:query')")
    public CommonResult<List<ServiceItemRespVO>> getServiceItemList(@RequestParam("projectId") Long projectId) {
        // 获取当前用户的顶级部门ID
        Long deptId = null;
        Long loginUserId = cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId != null) {
            AdminUserDO user = adminUserService.getUser(loginUserId);
            if (user != null && user.getDeptId() != null) {
                // 向上查找顶级部门
                deptId = findTopDeptId(user.getDeptId());
            }
        }

        // 按项目ID和顶级部门ID过滤
        List<ServiceItemDO> list = serviceItemService.getServiceItemListByProjectIdAndDeptId(projectId, deptId);
        List<ServiceItemRespVO> result = BeanUtils.toBean(list, ServiceItemRespVO.class);
        // 处理标签
        for (int i = 0; i < list.size(); i++) {
            ServiceItemDO serviceItem = list.get(i);
            if (serviceItem.getTags() != null) {
                result.get(i).setTags(JSONUtil.toList(serviceItem.getTags(), String.class));
            }
        }
        return success(result);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新服务项状态")
    @PreAuthorize("@ss.hasPermission('project:service-item:update')")
    public CommonResult<Boolean> updateServiceItemStatus(@RequestParam("id") Long id,
            @RequestParam("status") Integer status) {
        serviceItemService.updateServiceItemStatus(id, status);
        return success(true);
    }

    @PutMapping("/update-progress")
    @Operation(summary = "更新服务项进度")
    @PreAuthorize("@ss.hasPermission('project:service-item:update')")
    public CommonResult<Boolean> updateServiceItemProgress(@RequestParam("id") Long id,
            @RequestParam("progress") Integer progress) {
        serviceItemService.updateServiceItemProgress(id, progress);
        return success(true);
    }

    @GetMapping("/user-list-by-dept-type")
    @Operation(summary = "根据部门类型获取可选执行人列表", description = "根据部门类型返回对应岗位的用户列表")
    @Parameter(name = "deptType", description = "部门类型：1安全服务 2安全运营 3数据安全", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-item:query')")
    public CommonResult<List<UserSimpleRespVO>> getUserListByDeptType(@RequestParam("deptType") Integer deptType) {
        // 获取部门类型对应的岗位code列表
        List<String> postCodes = DEPT_TYPE_POST_CODES.get(deptType);
        if (CollUtil.isEmpty(postCodes)) {
            return success(Collections.emptyList());
        }

        // 根据岗位code查询岗位ID
        List<Long> postIds = new ArrayList<>();
        for (String code : postCodes) {
            PostDO post = postMapper.selectByCode(code);
            if (post != null) {
                postIds.add(post.getId());
            }
        }
        if (CollUtil.isEmpty(postIds)) {
            return success(Collections.emptyList());
        }

        // 根据岗位ID查询用户列表
        List<AdminUserDO> users = adminUserService.getUserListByPostIds(postIds);
        if (CollUtil.isEmpty(users)) {
            return success(Collections.emptyList());
        }

        // 拼接部门信息
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(users, AdminUserDO::getDeptId));

        // 转换为简单响应
        List<UserSimpleRespVO> result = users.stream().map(user -> {
            UserSimpleRespVO vo = new UserSimpleRespVO();
            vo.setId(user.getId());
            vo.setNickname(user.getNickname());
            vo.setDeptId(user.getDeptId());
            DeptDO dept = deptMap.get(user.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            return vo;
        }).collect(Collectors.toList());

        return success(result);
    }

}
