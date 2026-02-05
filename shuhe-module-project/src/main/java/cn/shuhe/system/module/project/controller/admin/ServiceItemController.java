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
import cn.shuhe.system.module.system.dal.mysql.cost.ContractInfoMapper;
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
        // 根据 deptType 找到对应的部门ID（在哪个页面创建就属于哪个部门）
        if (createReqVO.getDeptId() == null && createReqVO.getDeptType() != null) {
            Long deptId = findDeptIdByDeptType(createReqVO.getDeptType());
            createReqVO.setDeptId(deptId);
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
        // 根据 deptType 找到对应的部门ID（在哪个页面创建就属于哪个部门）
        Long deptId = findDeptIdByDeptType(batchReqVO.getDeptType());
        if (deptId == null) {
            throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.shuhe.system.module.project.enums.ErrorCodeConstants.SERVICE_ITEM_DEPT_NOT_SET);
        }
        return success(serviceItemService.batchCreateServiceItem(batchReqVO, deptId));
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
                        .remark("示例服务项1").build(),
                ServiceItemImportExcelVO.builder()
                        .name("某企业安全评估")
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
        // 根据 deptType 找到对应的部门ID（在哪个页面导入就属于哪个部门）
        Long deptId = findDeptIdByDeptType(deptType);
        if (deptId == null) {
            throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.shuhe.system.module.project.enums.ErrorCodeConstants.SERVICE_ITEM_DEPT_NOT_SET);
        }
        List<ServiceItemImportExcelVO> list = ExcelUtils.read(file, ServiceItemImportExcelVO.class);
        return success(serviceItemService.importServiceItemList(projectId, deptType, list, deptId));
    }

    /**
     * 根据部门类型找到对应的部门ID
     * 
     * 逻辑：在哪个页面（deptType）创建服务项，就属于哪个部门
     * 例如：在安全服务页面（deptType=1）创建，就属于安全技术服务部
     * 
     * @param deptType 部门类型：1安全服务 2安全运营 3数据安全
     * @return 对应的部门ID
     */
    private Long findDeptIdByDeptType(Integer deptType) {
        if (deptType == null) {
            return null;
        }
        // 查找有对应 deptType 的部门
        DeptDO dept = deptService.getDeptByDeptType(deptType);
        return dept != null ? dept.getId() : null;
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
    public CommonResult<PageResult<ServiceItemRespVO>> getServiceItemPage(@Valid ServiceItemPageReqVO pageReqVO) {
        // 根据 deptType 找到对应的部门ID进行过滤，保证与创建时使用相同的部门ID
        if (pageReqVO.getDeptType() != null) {
            Long deptId = findDeptIdByDeptType(pageReqVO.getDeptType());
            pageReqVO.setDeptId(deptId);
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
    @PreAuthorize("@ss.hasPermission('project:service-item:query')")
    public CommonResult<List<ServiceItemRespVO>> getServiceItemList(
            @RequestParam("projectId") Long projectId,
            @RequestParam(value = "deptType", required = false) Integer deptType,
            @RequestParam(value = "serviceMode", required = false) Integer serviceMode,
            @RequestParam(value = "serviceMemberType", required = false) Integer serviceMemberType) {
        List<ServiceItemDO> list;

        // 根据传入的参数组合进行过滤
        if (serviceMemberType != null && deptType != null) {
            // 安全运营按服务归属人员类型过滤（驻场人员服务项 / 管理人员服务项）
            list = serviceItemService.getServiceItemListByProjectIdAndDeptTypeAndMemberType(projectId, deptType, serviceMemberType);
        } else if (serviceMode != null && deptType != null) {
            // 同时按部门类型和服务模式过滤（安全服务驻场详情页会用到）
            list = serviceItemService.getServiceItemListByProjectIdAndServiceMode(projectId, serviceMode);
            // 再按 deptType 过滤
            list = list.stream().filter(item -> deptType.equals(item.getDeptType())).toList();
        } else if (serviceMode != null) {
            // 只按服务模式过滤
            list = serviceItemService.getServiceItemListByProjectIdAndServiceMode(projectId, serviceMode);
        } else if (deptType != null) {
            // 只按部门类型过滤（安全运营详情页会用到）
            list = serviceItemService.getServiceItemListByProjectIdAndDeptType(projectId, deptType);
        } else {
            // 无过滤条件，返回所有服务项
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

    @GetMapping("/manager-list-by-dept-type")
    @Operation(summary = "根据部门类型获取管理人员列表", description = "根据部门类型返回管理岗位（组长、主管）的用户列表")
    @Parameter(name = "deptType", description = "部门类型：1安全服务 2安全运营 3数据安全", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-item:query')")
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
