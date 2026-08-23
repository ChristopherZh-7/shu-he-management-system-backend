package cn.shuhe.system.module.project.controller.admin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.datapermission.core.annotation.DataPermission;
import cn.shuhe.system.framework.excel.core.util.ExcelUtils;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectWorkRecordPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectWorkRecordRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectWorkRecordSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectWorkRecordDO;
import cn.shuhe.system.module.project.dal.dataobject.SecurityOperationContractDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.SecurityOperationContractMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.project.service.ProjectWorkRecordService;
import cn.shuhe.system.module.project.service.access.ProjectAccessService;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 项目工作记录")
@RestController
@RequestMapping("/project/work-record")
@Validated
@Slf4j
public class ProjectWorkRecordController {

    @Resource
    private ProjectWorkRecordService workRecordService;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private SecurityOperationContractMapper securityOperationContractMapper;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private cn.shuhe.system.module.project.service.ServiceItemService serviceItemService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Resource
    private ProjectAccessService projectAccessService;

    @PostMapping("/create")
    @Operation(summary = "创建工作记录")
    @PreAuthorize("@ss.hasAnyPermissions('project:work-record:create', 'project:management-record:create', 'project:my-work-record:create')")
    public CommonResult<Long> createWorkRecord(@Valid @RequestBody ProjectWorkRecordSaveReqVO createReqVO) {
        validateWorkRecordTarget(createReqVO.getProjectId(), createReqVO.getServiceItemId());
        return success(workRecordService.createWorkRecord(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工作记录")
    @PreAuthorize("@ss.hasAnyPermissions('project:work-record:update', 'project:management-record:update', 'project:my-work-record:update')")
    public CommonResult<Boolean> updateWorkRecord(@Valid @RequestBody ProjectWorkRecordSaveReqVO updateReqVO) {
        ProjectWorkRecordDO existing = workRecordService.getWorkRecord(updateReqVO.getId());
        validateReadWorkRecord(existing);
        updateReqVO.setProjectId(existing.getProjectId());
        validateWorkRecordTarget(existing.getProjectId(), updateReqVO.getServiceItemId());
        workRecordService.updateWorkRecord(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工作记录")
    @Parameter(name = "id", description = "记录编号", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('project:work-record:delete', 'project:management-record:delete')")
    public CommonResult<Boolean> deleteWorkRecord(@RequestParam("id") Long id) {
        validateReadWorkRecord(workRecordService.getWorkRecord(id));
        workRecordService.deleteWorkRecord(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取工作记录详情")
    @Parameter(name = "id", description = "记录编号", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('project:work-record:query', 'project:management-record:query', 'project:my-work-record:query')")
    public CommonResult<ProjectWorkRecordRespVO> getWorkRecord(@RequestParam("id") Long id) {
        ProjectWorkRecordDO record = workRecordService.getWorkRecord(id);
        validateReadWorkRecord(record);
        return success(convertToRespVO(record));
    }

    @GetMapping("/page")
    @Operation(summary = "获取工作记录分页")
    @PreAuthorize("@ss.hasAnyPermissions('project:work-record:query', 'project:management-record:query', 'project:my-work-record:query')")
    public CommonResult<PageResult<ProjectWorkRecordRespVO>> getWorkRecordPage(@Valid ProjectWorkRecordPageReqVO pageReqVO) {
        if (pageReqVO.getProjectId() != null) {
            projectAccessService.validateViewProject(pageReqVO.getProjectId(), SecurityFrameworkUtils.getLoginUserId());
        }
        PageResult<ProjectWorkRecordDO> pageResult = workRecordService.getWorkRecordPage(pageReqVO);

        // 转换响应
        List<ProjectWorkRecordRespVO> respList = pageResult.getList().stream()
                .filter(this::canReadWorkRecord)
                .map(this::convertToRespVO)
                .collect(Collectors.toList());

        return success(new PageResult<>(respList, (long) respList.size()));
    }

    @GetMapping("/list-by-project")
    @Operation(summary = "根据项目ID获取工作记录列表")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('project:work-record:query', 'project:management-record:query', 'project:my-work-record:query')")
    public CommonResult<List<ProjectWorkRecordRespVO>> getWorkRecordListByProject(@RequestParam("projectId") Long projectId) {
        projectAccessService.validateViewProject(projectId, SecurityFrameworkUtils.getLoginUserId());
        List<ProjectWorkRecordDO> list = workRecordService.getWorkRecordListByProjectId(projectId);
        return success(list.stream().filter(this::canReadWorkRecord)
                .map(this::convertToRespVO).collect(Collectors.toList()));
    }

    @GetMapping("/list-by-service-item")
    @Operation(summary = "根据服务项ID获取工作记录列表")
    @Parameter(name = "serviceItemId", description = "服务项ID", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('project:work-record:query', 'project:management-record:query', 'project:my-work-record:query')")
    public CommonResult<List<ProjectWorkRecordRespVO>> getWorkRecordListByServiceItem(@RequestParam("serviceItemId") Long serviceItemId) {
        validateReadableServiceItem(serviceItemId);
        List<ProjectWorkRecordDO> list = workRecordService.getWorkRecordListByServiceItemId(serviceItemId);
        return success(list.stream().filter(this::canReadWorkRecord)
                .map(this::convertToRespVO).collect(Collectors.toList()));
    }

    @GetMapping("/export")
    @Operation(summary = "导出工作记录Excel")
    @PreAuthorize("@ss.hasAnyPermissions('project:work-record:export', 'project:management-record:export')")
    public void exportWorkRecordExcel(@Valid ProjectWorkRecordPageReqVO reqVO, HttpServletResponse response) throws IOException {
        if (reqVO.getProjectId() != null) {
            projectAccessService.validateViewProject(reqVO.getProjectId(), SecurityFrameworkUtils.getLoginUserId());
        }
        List<ProjectWorkRecordDO> list = workRecordService.getWorkRecordListForExport(reqVO);
        List<ProjectWorkRecordRespVO> respList = list.stream()
                .filter(this::canReadWorkRecord)
                .map(this::convertToRespVO)
                .collect(Collectors.toList());
        ExcelUtils.write(response, "工作记录.xls", "工作记录", ProjectWorkRecordRespVO.class, respList);
    }

    // ========== 获取可选项目和服务项列表 ==========

    @GetMapping("/my-projects")
    @Operation(summary = "获取当前用户可见的项目列表", description = "管理层看部门+子部门的项目，员工看自己负责/参与的项目")
    @PreAuthorize("@ss.hasAnyPermissions('project:work-record:query', 'project:management-record:query', 'project:my-work-record:query')")
    @DataPermission(enable = false)
    public CommonResult<List<Map<String, Object>>> getMyProjects(
            @RequestParam(value = "projectType", required = false) Integer projectType) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (user == null || user.getDeptId() == null) {
            return success(result);
        }
        
        // 获取用户所属的顶级部门类型（用于过滤项目类型）
        Integer userDeptType = getUserDeptType(user.getDeptId());
        log.info("[getMyProjects] 用户ID={}, 部门ID={}, 部门类型={}", userId, user.getDeptId(), userDeptType);
        
        // 通过服务项的 deptType 来过滤项目（与项目列表页逻辑保持一致）
        if (userDeptType != null) {
            // 如果指定了 projectType 参数，使用指定的类型；否则使用用户部门类型
            Integer queryDeptType = (projectType != null) ? projectType : userDeptType;
            
            log.info("[getMyProjects] 查询项目: userDeptType={}, projectType={}, queryDeptType={}", 
                userDeptType, projectType, queryDeptType);
            
            // 1. 先通过服务项的 deptType 获取项目ID列表
            List<Long> projectIds = serviceItemMapper.selectProjectIdsByDeptType(queryDeptType);
            List<Long> visibleProjectIds = projectAccessService.getVisibleProjectIds(userId);
            if (visibleProjectIds != null) {
                projectIds = projectIds.stream().filter(visibleProjectIds::contains).toList();
            }
            log.info("[getMyProjects] 根据服务项deptType={}过滤，找到项目IDs: {}", queryDeptType, projectIds);
            
            if (CollUtil.isNotEmpty(projectIds)) {
                // 2. 查询这些项目
                List<ProjectDO> projects = projectMapper.selectBatchIds(projectIds);
                log.info("[getMyProjects] 查询到项目数量: {}", projects != null ? projects.size() : "null");
                
                for (ProjectDO project : projects) {
                    // 暂时简化：只要是同类型的项目就显示（后续可以加更细的权限控制）
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", project.getId());
                    item.put("name", project.getName());
                    item.put("projectType", queryDeptType); // 使用查询的部门类型
                    item.put("customerName", project.getCustomerName());
                    result.add(item);
                }
            }
        }
        
        log.info("[getMyProjects] 最终返回项目数量: {}", result.size());
        return success(result);
    }
    
    /**
     * 获取用户所属的顶级部门类型
     * 通过部门名称判断：安全技术服务部=1, 安全运营服务部=2, 数据安全服务部=3
     */
    private Integer getUserDeptType(Long deptId) {
        if (deptId == null) {
            log.info("[getUserDeptType] deptId is null");
            return null;
        }
        
        // 向上遍历找到顶级部门
        Long currentDeptId = deptId;
        DeptRespDTO topDept = null;
        
        while (currentDeptId != null && currentDeptId != 0) {
            DeptRespDTO dept = deptApi.getDept(currentDeptId);
            log.info("[getUserDeptType] 遍历部门: id={}, name={}, parentId={}", 
                currentDeptId, dept != null ? dept.getName() : "null", dept != null ? dept.getParentId() : "null");
            if (dept == null) {
                break;
            }
            topDept = dept;
            if (dept.getParentId() == null || dept.getParentId() == 0) {
                break;
            }
            currentDeptId = dept.getParentId();
        }
        
        if (topDept == null || topDept.getName() == null) {
            log.info("[getUserDeptType] topDept is null or name is null");
            return null;
        }
        
        // 根据顶级部门名称判断类型
        // 注意：判断顺序很重要，"安全运营服务部"同时包含"安全服务"和"安全运营"
        String deptName = topDept.getName();
        log.info("[getUserDeptType] 顶级部门: id={}, name={}", topDept.getId(), deptName);
        
        Integer result = null;
        if (deptName.contains("数据安全")) {
            result = 3; // 数据安全
        } else if (deptName.contains("安全运营") || deptName.contains("运营服务")) {
            result = 2; // 安全运营（必须在"安全服务"之前判断）
        } else if (deptName.contains("安全技术") || deptName.contains("安全服务") || deptName.contains("安服")) {
            result = 1; // 安全服务
        }
        
        log.info("[getUserDeptType] 返回部门类型: {}", result);
        return result;
    }

    @GetMapping("/service-items-by-project")
    @Operation(summary = "根据项目ID获取服务项列表")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    @Parameter(name = "projectType", description = "项目类型", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('project:work-record:query', 'project:management-record:query', 'project:my-work-record:query')")
    @DataPermission(enable = false)
    public CommonResult<List<Map<String, Object>>> getServiceItemsByProject(
            @RequestParam("projectId") Long projectId,
            @RequestParam("projectType") Integer projectType) {

        Long userId = SecurityFrameworkUtils.getLoginUserId();
        projectAccessService.validateViewProject(projectId, userId);
        List<Long> readableDeptIds = projectAccessService.getReadableDeptIds(projectId, userId);
        if (readableDeptIds != null && readableDeptIds.isEmpty()) {
            return success(List.of());
        }
        // 统一通过 projectId 查询服务项（服务项表的 project_id 字段关联到 project 表）
        List<ServiceItemDO> serviceItems = readableDeptIds == null
                ? serviceItemMapper.selectListByProjectId(projectId)
                : serviceItemMapper.selectListByProjectIdAndDeptIds(projectId, readableDeptIds);
        
        log.info("[getServiceItemsByProject] projectId={}, projectType={}, 查询到服务项数量={}", 
            projectId, projectType, serviceItems.size());
        
        List<Map<String, Object>> result = serviceItems.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("serviceType", item.getServiceType());
            map.put("serviceTypeName",
                    serviceItemService.resolveServiceTypeLabel(item.getDeptType(), item.getServiceType()));
            return map;
        }).collect(Collectors.toList());
        
        return success(result);
    }

    private void validateWorkRecordTarget(Long projectId, Long serviceItemId) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        projectAccessService.validateViewProject(projectId, userId);
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null || !projectAccessService.canReadDept(projectId, userId, user.getDeptId())) {
            throwForbidden();
        }
        if (serviceItemId != null) {
            ServiceItemDO item = validateReadableServiceItem(serviceItemId);
            if (!projectId.equals(item.getProjectId())) {
                throwForbidden();
            }
        }
    }

    private ServiceItemDO validateReadableServiceItem(Long serviceItemId) {
        ServiceItemDO item = serviceItemMapper.selectById(serviceItemId);
        if (item == null || !projectAccessService.canReadDept(
                item.getProjectId(), SecurityFrameworkUtils.getLoginUserId(), item.getDeptId())) {
            throwForbidden();
        }
        return item;
    }

    private boolean canReadWorkRecord(ProjectWorkRecordDO record) {
        return record != null && projectAccessService.canReadDept(
                record.getProjectId(), SecurityFrameworkUtils.getLoginUserId(), record.getDeptId());
    }

    private void validateReadWorkRecord(ProjectWorkRecordDO record) {
        if (!canReadWorkRecord(record)) {
            throwForbidden();
        }
    }

    private void throwForbidden() {
        throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                cn.shuhe.system.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN);
    }

    /**
     * 转换为响应VO
     */
    private ProjectWorkRecordRespVO convertToRespVO(ProjectWorkRecordDO record) {
        if (record == null) {
            return null;
        }
        ProjectWorkRecordRespVO respVO = BeanUtils.toBean(record, ProjectWorkRecordRespVO.class);
        
        // 处理附件（JSON转List）
        if (record.getAttachments() != null && !record.getAttachments().isEmpty()) {
            respVO.setAttachments(JSONUtil.toList(record.getAttachments(), String.class));
        }
        
        return respVO;
    }

}
