package cn.shuhe.system.module.project.controller.admin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.enums.CommonStatusEnum;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.datapermission.core.annotation.DataPermission;
import cn.shuhe.system.framework.datapermission.core.util.DataPermissionUtils;
import cn.shuhe.system.framework.excel.core.util.ExcelUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemBatchSaveReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemImportExcelVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemImportRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.service.ServiceItemService;
import cn.shuhe.system.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.shuhe.system.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.dept.PostDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.finance.dal.mysql.cost.ContractInfoMapper;
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
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 服务项管理")
@RestController
@RequestMapping("/project/service-item")
@Validated
public class ServiceItemController {

    @Resource
    private ServiceItemService serviceItemService;

    @Resource
    private cn.shuhe.system.module.project.service.ProjectService projectService;

    @Resource
    private AdminUserService adminUserService;

    @Resource
    private PostMapper postMapper;

    @Resource
    private DeptService deptService;

    @Resource
    private ContractInfoMapper contractInfoMapper;

    /**
     * 部门类型对应的岗位code映射
     */
    private static final Map<Integer, List<String>> DEPT_TYPE_POST_CODES = Map.of(
            1, List.of("anquanjishufuwugongchengshi", "anquanjishufuwuzuzhang"), // 安全服务
            2, List.of("anquanyunyingfuwugongchengshi", "anquanyunyingfuwuzuzhang", "anquanyunyingfuwuzhuguan"), // 安全运营
            3, List.of("shujuanquanfuwugongchengshi", "shujuanquanfuwuzhuguan") // 数据安全
    );

    /**
     * 部门类型对应的管理岗位code映射（组长、主管）
     */
    private static final Map<Integer, List<String>> DEPT_TYPE_MANAGER_POST_CODES = Map.of(
            1, List.of("anquanjishufuwuzuzhang"), // 安全服务管理
            2, List.of("anquanyunyingfuwuzuzhang", "anquanyunyingfuwuzhuguan"), // 安全运营管理
            3, List.of("shujuanquanfuwuzhuguan") // 数据安全管理
    );

    @PostMapping("/create")
    @Operation(summary = "创建服务项")
    @PreAuthorize("@ss.hasPermission('project:service-item:create')")
    public CommonResult<Long> createServiceItem(@Valid @RequestBody ServiceItemSaveReqVO createReqVO) {
        validateManagePermission(createReqVO.getProjectId());
        return success(serviceItemService.createServiceItem(createReqVO));
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建服务项")
    @PreAuthorize("@ss.hasPermission('project:service-item:create')")
    public CommonResult<List<Long>> batchCreateServiceItem(@Valid @RequestBody ServiceItemBatchSaveReqVO batchReqVO) {
        validateManagePermission(batchReqVO.getProjectId());
        return success(serviceItemService.batchCreateServiceItem(batchReqVO, null));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获取导入服务项模板")
    public void getImportTemplate(HttpServletResponse response,
            @RequestParam("deptType") Integer deptType) throws IOException {
        // 手动创建导出 demo（V2026_05_20_02 起不再需要填「服务项名称」）
        List<ServiceItemImportExcelVO> list = Arrays.asList(
                ServiceItemImportExcelVO.builder()
                        .serviceType("penetration_test")
                        .customerName("某银行")
                        .planStartTime("2026-01-20 09:00:00")
                        .planEndTime("2026-01-25 18:00:00")
                        .remark("示例服务项1").build(),
                ServiceItemImportExcelVO.builder()
                        .serviceType("security_assessment")
                        .customerName("某企业")
                        .planStartTime("2026-02-01 09:00:00")
                        .planEndTime("2026-02-10 18:00:00")
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
        // deptId 不在导入时设置，由部门主管后续通过"分配"操作设置
        List<ServiceItemImportExcelVO> list = ExcelUtils.read(file, ServiceItemImportExcelVO.class);
        return success(serviceItemService.importServiceItemList(projectId, deptType, list, null));
    }

    /**
     * 校验当前用户是否具有项目管理权限（roleType=1 项目经理 或 roleType=3 审核人员）
     * 执行人员（roleType=2）不允许执行写操作
     */
    private void validateManagePermission(Long projectId) {
        Long userId = getLoginUserId();
        Integer roleType = projectService.getUserRoleInProject(projectId, userId);
        if (roleType == null || roleType == 2) {
            throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.shuhe.system.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN);
        }
    }

    /**
     * 校验当前用户是否为项目成员（任何角色均可，用于状态/进度等操作性更新）
     */
    private void validateMemberPermission(Long projectId) {
        Long userId = getLoginUserId();
        Integer roleType = projectService.getUserRoleInProject(projectId, userId);
        if (roleType == null) {
            throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.shuhe.system.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN);
        }
    }

    /**
     * 通过服务项ID校验成员权限
     */
    private void validateMemberPermissionByServiceItemId(Long serviceItemId) {
        ServiceItemDO item = serviceItemService.getServiceItem(serviceItemId);
        if (item != null && item.getProjectId() != null) {
            validateMemberPermission(item.getProjectId());
        }
    }

    /**
     * 通过服务项ID校验管理权限
     */
    private void validateManagePermissionByServiceItemId(Long serviceItemId) {
        ServiceItemDO item = serviceItemService.getServiceItem(serviceItemId);
        if (item != null && item.getProjectId() != null) {
            validateManagePermission(item.getProjectId());
        }
    }

    private Long findDeptIdByDeptType(Integer deptType) {
        if (deptType == null) {
            return null;
        }
        // 绕过数据权限查找系统级部门（按 deptType 查找不应受当前用户权限限制）
        return DataPermissionUtils.executeIgnore(() -> {
            DeptDO dept = deptService.getDeptByDeptType(deptType);
            return dept != null ? dept.getId() : null;
        });
    }

    /**
     * 向上查找顶级部门ID
     * 
     * 用于服务项列表过滤：同一顶级部门下的用户能看到相同的服务项
     * 
     * @param deptId 当前部门ID
     * @return 顶级部门ID（公司级部门的直属子部门）
     */
    private Long findTopDeptId(Long deptId) {
        if (deptId == null) {
            return null;
        }
        DeptDO currentDept = deptService.getDept(deptId);
        if (currentDept == null) {
            return deptId;
        }
        // 如果已经是顶级部门（parentId为0或null），直接返回
        if (currentDept.getParentId() == null || currentDept.getParentId() == 0) {
            return deptId;
        }
        // 向上查找，直到找到 parentId=0 的部门的直属子部门
        Long parentId = currentDept.getParentId();
        Long currentId = deptId;
        while (parentId != null && parentId != 0) {
            DeptDO parentDept = deptService.getDept(parentId);
            if (parentDept == null) {
                break;
            }
            if (parentDept.getParentId() == null || parentDept.getParentId() == 0) {
                // 当前 parentDept 是公司级部门，返回 currentId（公司的直属子部门）
                return currentId;
            }
            currentId = parentId;
            parentId = parentDept.getParentId();
        }
        return currentId;
    }

    @PutMapping("/assign-dept")
    @Operation(summary = "分配服务项到具体部门（排/班）")
    @PreAuthorize("@ss.hasPermission('project:service-item:update')")
    @DataPermission(enable = false)
    public CommonResult<Boolean> assignServiceItemToDept(
            @RequestParam("id") Long id,
            @RequestParam("deptId") Long deptId) {
        validateManagePermissionByServiceItemId(id);
        serviceItemService.assignServiceItemToDept(id, deptId);
        return success(true);
    }

    @PutMapping("/batch-assign-dept")
    @Operation(summary = "批量分配服务项到具体部门（排/班）")
    @PreAuthorize("@ss.hasPermission('project:service-item:update')")
    @DataPermission(enable = false)
    public CommonResult<Boolean> batchAssignServiceItemToDept(
            @RequestParam("ids") List<Long> ids,
            @RequestParam("deptId") Long deptId) {
        if (!ids.isEmpty()) {
            validateManagePermissionByServiceItemId(ids.get(0));
        }
        for (Long id : ids) {
            serviceItemService.assignServiceItemToDept(id, deptId);
        }
        return success(true);
    }

    @PutMapping("/update")
    @Operation(summary = "更新服务项")
    @PreAuthorize("@ss.hasPermission('project:service-item:update')")
    @DataPermission(enable = false)
    public CommonResult<Boolean> updateServiceItem(@Valid @RequestBody ServiceItemSaveReqVO updateReqVO) {
        if (updateReqVO.getProjectId() != null) {
            validateManagePermission(updateReqVO.getProjectId());
        } else if (updateReqVO.getId() != null) {
            validateManagePermissionByServiceItemId(updateReqVO.getId());
        }
        serviceItemService.updateServiceItem(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除服务项")
    @Parameter(name = "id", description = "服务项编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-item:delete')")
    @DataPermission(enable = false)
    public CommonResult<Boolean> deleteServiceItem(@RequestParam("id") Long id) {
        validateManagePermissionByServiceItemId(id);
        serviceItemService.deleteServiceItem(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得服务项详情")
    @Parameter(name = "id", description = "服务项编号", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('project:service-item:query', 'project:my-tasks:query')")
    @DataPermission(enable = false)
    public CommonResult<ServiceItemRespVO> getServiceItem(@RequestParam("id") Long id) {
        ServiceItemDO serviceItem = serviceItemService.getServiceItem(id);
        ServiceItemRespVO respVO = BeanUtils.toBean(serviceItem, ServiceItemRespVO.class);
        // 处理标签
        if (serviceItem != null && serviceItem.getTags() != null) {
            respVO.setTags(JSONUtil.toList(serviceItem.getTags(), String.class));
        }
        // 设置实时已执行次数（从数据库查询）
        if (serviceItem != null) {
            respVO.setUsedCount(serviceItemService.getExecutedCount(id));
            // 填充合同名称
            fillContractName(respVO, serviceItem.getContractId());
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得服务项分页")
    @PreAuthorize("@ss.hasPermission('project:service-item:query')")
    @DataPermission(enable = false)
    public CommonResult<PageResult<ServiceItemRespVO>> getServiceItemPage(@Valid ServiceItemPageReqVO pageReqVO) {
        // 项目成员权限校验：只有项目成员才能查看
        Long userId = getLoginUserId();
        Integer roleType = projectService.getUserRoleInProject(pageReqVO.getProjectId(), userId);
        if (roleType == null) {
            return success(new PageResult<>(Collections.emptyList(), 0L));
        }
        // 执行者只能看到分配给自己部门的服务项
        if (roleType == 2 && pageReqVO.getDeptId() == null) {
            AdminUserDO user = adminUserService.getUser(userId);
            if (user != null && user.getDeptId() != null) {
                pageReqVO.setDeptId(user.getDeptId());
            }
        }

        PageResult<ServiceItemDO> pageResult = serviceItemService.getServiceItemPage(pageReqVO);
        PageResult<ServiceItemRespVO> result = BeanUtils.toBean(pageResult, ServiceItemRespVO.class);
        // 处理标签和实时已执行次数
        for (int i = 0; i < pageResult.getList().size(); i++) {
            ServiceItemDO serviceItem = pageResult.getList().get(i);
            ServiceItemRespVO respVO = result.getList().get(i);
            if (serviceItem.getTags() != null) {
                respVO.setTags(JSONUtil.toList(serviceItem.getTags(), String.class));
            }
            // 设置实时已执行次数（从数据库查询）
            respVO.setUsedCount(serviceItemService.getExecutedCount(serviceItem.getId()));
            // 填充合同名称
            fillContractName(respVO, serviceItem.getContractId());
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "获得服务项列表（根据项目ID）")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    @Parameter(name = "deptType", description = "部门类型：1-安全服务 2-安全运营 3-数据安全", required = false)
    @Parameter(name = "serviceMode", description = "服务模式：1-驻场 2-二线", required = false)
    @Parameter(name = "serviceMemberType", description = "服务归属人员类型（安全运营专用）：1-驻场人员 2-管理人员", required = false)
    @PreAuthorize("@ss.hasAnyPermissions('project:service-item:query', 'project:my-tasks:query')")
    @DataPermission(enable = false)
    public CommonResult<List<ServiceItemRespVO>> getServiceItemList(
            @RequestParam("projectId") Long projectId,
            @RequestParam(value = "deptType", required = false) Integer deptType,
            @RequestParam(value = "serviceMode", required = false) Integer serviceMode,
            @RequestParam(value = "serviceMemberType", required = false) Integer serviceMemberType) {
        // 项目成员权限校验
        Long userId = getLoginUserId();
        Integer roleType = projectService.getUserRoleInProject(projectId, userId);
        if (roleType == null) {
            return success(Collections.emptyList());
        }

        List<ServiceItemDO> list;

        // 执行者强制按自己的部门过滤
        if (roleType == 2) {
            AdminUserDO user = adminUserService.getUser(userId);
            Long userDeptId = (user != null) ? user.getDeptId() : null;
            list = serviceItemService.getServiceItemListByProjectIdAndDeptId(projectId, userDeptId);
        } else if (serviceMemberType != null && deptType != null) {
            list = serviceItemService.getServiceItemListByProjectIdAndDeptTypeAndMemberType(projectId, deptType, serviceMemberType);
        } else if (serviceMode != null && deptType != null) {
            list = serviceItemService.getServiceItemListByProjectIdAndServiceMode(projectId, serviceMode);
            list = list.stream().filter(item -> deptType.equals(item.getDeptType())).toList();
        } else if (serviceMode != null) {
            list = serviceItemService.getServiceItemListByProjectIdAndServiceMode(projectId, serviceMode);
        } else if (deptType != null) {
            list = serviceItemService.getServiceItemListByProjectIdAndDeptType(projectId, deptType);
        } else {
            list = serviceItemService.getServiceItemListByProjectId(projectId);
        }

        List<ServiceItemRespVO> result = BeanUtils.toBean(list, ServiceItemRespVO.class);
        // 处理标签和实时已执行次数
        for (int i = 0; i < list.size(); i++) {
            ServiceItemDO serviceItem = list.get(i);
            ServiceItemRespVO respVO = result.get(i);
            if (serviceItem.getTags() != null) {
                respVO.setTags(JSONUtil.toList(serviceItem.getTags(), String.class));
            }
            // 设置实时已执行次数（从数据库查询）
            respVO.setUsedCount(serviceItemService.getExecutedCount(serviceItem.getId()));
            // 填充合同名称
            fillContractName(respVO, serviceItem.getContractId());
        }
        return success(result);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新服务项状态")
    @PreAuthorize("@ss.hasPermission('project:service-item:update')")
    @DataPermission(enable = false)
    public CommonResult<Boolean> updateServiceItemStatus(@RequestParam("id") Long id,
            @RequestParam("status") Integer status) {
        validateMemberPermissionByServiceItemId(id);
        serviceItemService.updateServiceItemStatus(id, status);
        return success(true);
    }

    @PutMapping("/update-progress")
    @Operation(summary = "更新服务项进度")
    @PreAuthorize("@ss.hasPermission('project:service-item:update')")
    @DataPermission(enable = false)
    public CommonResult<Boolean> updateServiceItemProgress(@RequestParam("id") Long id,
            @RequestParam("progress") Integer progress) {
        validateMemberPermissionByServiceItemId(id);
        serviceItemService.updateServiceItemProgress(id, progress);
        return success(true);
    }

    @GetMapping("/user-list-by-dept-type")
    @Operation(summary = "根据部门类型获取可选执行人列表", description = "根据部门类型返回对应岗位的用户列表，同时包含同部门无岗位的用户（如实习生）")
    @Parameter(name = "deptType", description = "部门类型：1安全服务 2安全运营 3数据安全", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-item:query')")
    @DataPermission(enable = false)
    public CommonResult<List<UserSimpleRespVO>> getUserListByDeptType(@RequestParam("deptType") Integer deptType) {
        // 收集所有符合条件的用户（按 userId 去重）
        Map<Long, AdminUserDO> userMap = new LinkedHashMap<>();

        // 1. 按岗位code筛选（原有逻辑）
        List<String> postCodes = DEPT_TYPE_POST_CODES.get(deptType);
        if (CollUtil.isNotEmpty(postCodes)) {
            List<Long> postIds = new ArrayList<>();
            for (String code : postCodes) {
                PostDO post = postMapper.selectByCode(code);
                if (post != null) {
                    postIds.add(post.getId());
                }
            }
            if (CollUtil.isNotEmpty(postIds)) {
                List<AdminUserDO> postUsers = adminUserService.getUserListByPostIds(postIds);
                if (CollUtil.isNotEmpty(postUsers)) {
                    postUsers.forEach(u -> userMap.put(u.getId(), u));
                }
            }
        }

        // 2. 补充同部门用户（覆盖无岗位的实习生等）
        Long deptId = findDeptIdByDeptType(deptType);
        if (deptId != null) {
            Set<Long> deptIds = new HashSet<>();
            deptIds.add(deptId);
            deptIds.addAll(deptService.getChildDeptIdListFromCache(deptId));
            List<AdminUserDO> deptUsers = adminUserService.getUserListByDeptIds(deptIds);
            if (CollUtil.isNotEmpty(deptUsers)) {
                deptUsers.forEach(u -> userMap.putIfAbsent(u.getId(), u));
            }
        }

        if (userMap.isEmpty()) {
            return success(Collections.emptyList());
        }

        Collection<AdminUserDO> users = userMap.values();

        // 拼接部门信息
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(new ArrayList<>(users), AdminUserDO::getDeptId));

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

    @GetMapping("/user-list-for-site-member")
    @Operation(summary = "获取可选驻场人员列表",
            description = "返回公司所有启用用户，排除「总经办」本部门（老板/总经理） + 「人事行政」整棵子树")
    @PreAuthorize("@ss.hasPermission('project:service-item:query')")
    @DataPermission(enable = false)
    public CommonResult<List<UserSimpleRespVO>> getUserListForSiteMember() {
        List<DeptDO> allDepts = deptService.getDeptListForSimpleList(
                new DeptListReqVO().setStatus(CommonStatusEnum.ENABLE.getStatus()));
        if (CollUtil.isEmpty(allDepts)) {
            return success(Collections.emptyList());
        }

        Long zongjingbanId = null;
        Set<Long> renshiRootIds = new HashSet<>();
        for (DeptDO dept : allDepts) {
            if (dept.getName() == null) continue;
            if (dept.getName().contains("总经办")) {
                zongjingbanId = dept.getId();
            }
            if (dept.getName().contains("人事行政")) {
                renshiRootIds.add(dept.getId());
            }
        }

        Set<Long> excludedDeptIds = new HashSet<>();
        if (zongjingbanId != null) {
            excludedDeptIds.add(zongjingbanId);
        }
        for (Long rsId : renshiRootIds) {
            excludedDeptIds.add(rsId);
            excludedDeptIds.addAll(deptService.getChildDeptIdListFromCache(rsId));
        }

        Set<Long> allowedDeptIds = allDepts.stream()
                .map(DeptDO::getId)
                .filter(id -> !excludedDeptIds.contains(id))
                .collect(Collectors.toSet());
        if (allowedDeptIds.isEmpty()) {
            return success(Collections.emptyList());
        }

        List<AdminUserDO> users = adminUserService.getUserListByDeptIds(allowedDeptIds);
        if (CollUtil.isEmpty(users)) {
            return success(Collections.emptyList());
        }

        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(users, AdminUserDO::getDeptId));

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

    @GetMapping("/manager-list-by-dept-type")
    @Operation(summary = "根据部门类型获取管理人员列表", description = "根据部门类型返回管理岗位（组长、主管）的用户列表")
    @Parameter(name = "deptType", description = "部门类型：1安全服务 2安全运营 3数据安全", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-item:query')")
    @DataPermission(enable = false)
    public CommonResult<List<UserSimpleRespVO>> getManagerListByDeptType(@RequestParam("deptType") Integer deptType) {
        // 获取部门类型对应的管理岗位code列表
        List<String> postCodes = DEPT_TYPE_MANAGER_POST_CODES.get(deptType);
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

    @GetMapping("/outside-list")
    @Operation(summary = "获得外出服务项列表（根据项目ID）", description = "用于外出请求发起页面选择服务项，不受可见性过滤")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-item:query')")
    public CommonResult<List<ServiceItemRespVO>> getOutsideServiceItemList(@RequestParam("projectId") Long projectId) {
        List<ServiceItemDO> list = serviceItemService.getOutsideServiceItemListByProjectId(projectId);
        List<ServiceItemRespVO> result = BeanUtils.toBean(list, ServiceItemRespVO.class);
        // 处理标签
        for (int i = 0; i < list.size(); i++) {
            ServiceItemDO serviceItem = list.get(i);
            ServiceItemRespVO respVO = result.get(i);
            if (serviceItem.getTags() != null) {
                respVO.setTags(JSONUtil.toList(serviceItem.getTags(), String.class));
            }
        }
        return success(result);
    }

    @GetMapping("/outside-list-by-dept")
    @Operation(summary = "获得外出服务项列表（根据部门ID）", description = "用于外出请求发起页面，先选目标部门再选服务项")
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-item:query')")
    public CommonResult<List<ServiceItemRespVO>> getOutsideServiceItemListByDept(@RequestParam("deptId") Long deptId) {
        List<ServiceItemDO> list = serviceItemService.getOutsideServiceItemListByDeptId(deptId);
        List<ServiceItemRespVO> result = BeanUtils.toBean(list, ServiceItemRespVO.class);
        // 处理标签和填充项目名称
        for (int i = 0; i < list.size(); i++) {
            ServiceItemDO serviceItem = list.get(i);
            ServiceItemRespVO respVO = result.get(i);
            if (serviceItem.getTags() != null) {
                respVO.setTags(JSONUtil.toList(serviceItem.getTags(), String.class));
            }
        }
        return success(result);
    }

    /**
     * 填充合同名称
     */
    private void fillContractName(ServiceItemRespVO respVO, Long contractId) {
        if (contractId == null) {
            return;
        }
        try {
            Map<String, Object> contractInfo = contractInfoMapper.selectContractInfo(contractId);
            if (contractInfo != null && contractInfo.get("contractName") != null) {
                respVO.setContractName((String) contractInfo.get("contractName"));
            }
        } catch (Exception e) {
            // 忽略合同查询异常，不影响主流程
        }
    }

}
