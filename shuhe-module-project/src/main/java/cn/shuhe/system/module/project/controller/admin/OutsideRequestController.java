package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideMemberFinishReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideMemberRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideRequestPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideRequestRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideRequestSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.OutsideMemberDO;
import cn.shuhe.system.module.project.service.OutsideRequestService;
import cn.shuhe.system.module.project.service.ReportGenerateService;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 外出请求")
@RestController
@RequestMapping("/project/outside-request")
@Validated
public class OutsideRequestController {

    @Resource
    private OutsideRequestService outsideRequestService;

    @Resource
    private ReportGenerateService reportGenerateService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建外出请求")
    @PreAuthorize("@ss.hasPermission('project:outside-request:create')")
    public CommonResult<Long> createOutsideRequest(@Valid @RequestBody OutsideRequestSaveReqVO createReqVO) {
        // 设置发起人信息
        Long userId = getLoginUserId();
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        createReqVO.setId(null); // 确保是创建
        
        // 在 Service 中会设置 requestUserId 和 requestDeptId
        OutsideRequestSaveReqVO reqVO = createReqVO;
        
        return success(outsideRequestService.createOutsideRequest(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新外出请求")
    @PreAuthorize("@ss.hasPermission('project:outside-request:update')")
    public CommonResult<Boolean> updateOutsideRequest(@Valid @RequestBody OutsideRequestSaveReqVO updateReqVO) {
        outsideRequestService.updateOutsideRequest(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除外出请求")
    @Parameter(name = "id", description = "外出请求ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:outside-request:delete')")
    public CommonResult<Boolean> deleteOutsideRequest(@RequestParam("id") Long id) {
        outsideRequestService.deleteOutsideRequest(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得外出请求详情")
    @Parameter(name = "id", description = "外出请求ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:outside-request:query')")
    public CommonResult<OutsideRequestRespVO> getOutsideRequest(@RequestParam("id") Long id) {
        return success(outsideRequestService.getOutsideRequestDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得外出请求分页")
    @PreAuthorize("@ss.hasPermission('project:outside-request:query')")
    public CommonResult<PageResult<OutsideRequestRespVO>> getOutsideRequestPage(@Valid OutsideRequestPageReqVO pageReqVO) {
        return success(outsideRequestService.getOutsideRequestPage(pageReqVO));
    }

    @GetMapping("/list-by-project")
    @Operation(summary = "获得项目的外出请求列表")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:outside-request:query')")
    public CommonResult<List<OutsideRequestRespVO>> getOutsideRequestListByProject(@RequestParam("projectId") Long projectId) {
        List<cn.shuhe.system.module.project.dal.dataobject.OutsideRequestDO> list = 
                outsideRequestService.getOutsideRequestListByProjectId(projectId);
        // 简单转换，不填充关联信息
        return success(BeanUtils.toBean(list, OutsideRequestRespVO.class));
    }

    @GetMapping("/list-by-service-item")
    @Operation(summary = "获得服务项的外出请求列表（包含完整关联信息）")
    @Parameter(name = "serviceItemId", description = "服务项ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:outside-request:query')")
    public CommonResult<List<OutsideRequestRespVO>> getOutsideRequestListByServiceItem(@RequestParam("serviceItemId") Long serviceItemId) {
        return success(outsideRequestService.getOutsideRequestListByServiceItemIdWithDetail(serviceItemId));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新外出请求状态")
    @PreAuthorize("@ss.hasPermission('project:outside-request:update')")
    public CommonResult<Boolean> updateOutsideRequestStatus(
            @RequestParam("id") Long id,
            @RequestParam("status") Integer status) {
        outsideRequestService.updateOutsideRequestStatus(id, status);
        return success(true);
    }

    @PostMapping("/set-members")
    @Operation(summary = "设置外出人员（审批时使用）")
    @PreAuthorize("@ss.hasPermission('project:outside-request:update')")
    public CommonResult<Boolean> setOutsideMembers(
            @RequestParam("requestId") Long requestId,
            @RequestBody List<Long> memberUserIds) {
        outsideRequestService.setOutsideMembers(requestId, memberUserIds);
        return success(true);
    }

    @GetMapping("/members")
    @Operation(summary = "获得外出请求的人员列表")
    @Parameter(name = "requestId", description = "外出请求ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:outside-request:query')")
    public CommonResult<List<OutsideMemberRespVO>> getOutsideMembers(@RequestParam("requestId") Long requestId) {
        List<OutsideMemberDO> members = outsideRequestService.getOutsideMembers(requestId);
        return success(BeanUtils.toBean(members, OutsideMemberRespVO.class));
    }

    @PostMapping("/member/finish")
    @Operation(summary = "外出人员确认完成", description = "外出人员回来后确认完成，可选择是否上传附件")
    @PreAuthorize("@ss.hasPermission('project:outside-request:update')")
    public CommonResult<Boolean> finishOutsideMember(@Valid @RequestBody OutsideMemberFinishReqVO reqVO) {
        outsideRequestService.finishOutsideMember(
                reqVO.getMemberId(),
                reqVO.getHasAttachment(),
                reqVO.getAttachmentUrl(),
                reqVO.getFinishRemark()
        );
        return success(true);
    }

    @GetMapping("/member/get")
    @Operation(summary = "获得外出人员记录详情")
    @Parameter(name = "memberId", description = "外出人员记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:outside-request:query')")
    public CommonResult<OutsideMemberRespVO> getOutsideMember(@RequestParam("memberId") Long memberId) {
        OutsideMemberDO member = outsideRequestService.getOutsideMember(memberId);
        return success(BeanUtils.toBean(member, OutsideMemberRespVO.class));
    }

    @GetMapping("/count-by-service-item")
    @Operation(summary = "统计服务项的外出次数")
    @Parameter(name = "serviceItemId", description = "服务项ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:outside-request:query')")
    public CommonResult<Long> getOutsideCountByServiceItem(@RequestParam("serviceItemId") Long serviceItemId) {
        return success(outsideRequestService.getOutsideCountByServiceItemId(serviceItemId));
    }

    @GetMapping("/user-list-by-dept")
    @Operation(summary = "根据部门ID获取可选外出人员列表", description = "包含该部门及其所有子部门的用户")
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:outside-request:query')")
    public CommonResult<List<AdminUserRespDTO>> getUserListByDept(@RequestParam("deptId") Long deptId) {
        // 获取该部门及其所有子部门
        java.util.Set<Long> deptIds = new java.util.HashSet<>();
        deptIds.add(deptId);
        
        // 获取子部门
        List<DeptRespDTO> childDepts = deptApi.getChildDeptList(deptId);
        if (childDepts != null && !childDepts.isEmpty()) {
            for (DeptRespDTO child : childDepts) {
                deptIds.add(child.getId());
            }
        }
        
        // 获取所有相关部门下的用户
        List<AdminUserRespDTO> users = adminUserApi.getUserListByDeptIds(deptIds);
        return success(users);
    }

    @GetMapping("/dept-list")
    @Operation(summary = "获取可选的目标部门列表", description = "返回安全服务、安全运营、数据安全等有部门类型的部门")
    @PreAuthorize("@ss.hasPermission('project:outside-request:query')")
    public CommonResult<List<DeptRespDTO>> getTargetDeptList() {
        // 分别获取三种类型的部门：1-安全服务、2-安全运营、3-数据安全
        java.util.List<DeptRespDTO> allDepts = new java.util.ArrayList<>();
        for (int deptType = 1; deptType <= 3; deptType++) {
            List<DeptRespDTO> depts = deptApi.getDeptListByDeptType(deptType);
            if (depts != null) {
                allDepts.addAll(depts);
            }
        }
        
        // 构建部门ID到部门的映射，用于向上查找父部门
        java.util.Map<Long, DeptRespDTO> deptMap = allDepts.stream()
                .collect(java.util.stream.Collectors.toMap(DeptRespDTO::getId, d -> d, (a, b) -> a));
        
        // 为没有负责人的部门，向上查找父部门负责人
        for (DeptRespDTO dept : allDepts) {
            if (dept.getLeaderUserId() == null) {
                // 向上遍历父部门，找到有负责人的部门
                Long parentId = dept.getParentId();
                while (parentId != null && parentId > 0) {
                    DeptRespDTO parentDept = deptMap.get(parentId);
                    if (parentDept == null) {
                        // 父部门不在当前列表中，从API获取
                        parentDept = deptApi.getDept(parentId);
                    }
                    if (parentDept != null && parentDept.getLeaderUserId() != null) {
                        // 找到有负责人的父部门，使用其负责人
                        dept.setLeaderUserId(parentDept.getLeaderUserId());
                        break;
                    }
                    parentId = parentDept != null ? parentDept.getParentId() : null;
                }
            }
        }
        
        // 获取所有负责人的用户信息，填充负责人名称
        java.util.Set<Long> leaderUserIds = allDepts.stream()
                .map(DeptRespDTO::getLeaderUserId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (!leaderUserIds.isEmpty()) {
            java.util.Map<Long, String> userNameMap = adminUserApi.getUserList(leaderUserIds).stream()
                    .collect(java.util.stream.Collectors.toMap(
                            cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO::getId,
                            cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO::getNickname,
                            (a, b) -> a
                    ));
            // 设置负责人名称
            allDepts.forEach(dept -> {
                if (dept.getLeaderUserId() != null) {
                    dept.setLeaderUserName(userNameMap.get(dept.getLeaderUserId()));
                }
            });
        }
        
        return success(allDepts);
    }

    @GetMapping("/report-templates")
    @Operation(summary = "获取可用的报告模板列表")
    @Parameter(name = "type", description = "模板类型：outside-request, pentest, retest（可选，不传返回所有）", required = false)
    @PreAuthorize("@ss.hasPermission('project:outside-request:query')")
    public CommonResult<List<ReportGenerateService.ReportTemplate>> getReportTemplates(
            @RequestParam(value = "type", required = false) String type) {
        return success(reportGenerateService.getReportTemplates(type));
    }

    @GetMapping("/export-report")
    @Operation(summary = "导出外协请求报告", description = "根据模板生成 Word 报告并下载")
    @Parameter(name = "id", description = "外协请求ID", required = true)
    @Parameter(name = "templateCode", description = "模板编码，默认为 outside-request", required = false)
    @PreAuthorize("@ss.hasPermission('project:outside-request:query')")
    public void exportOutsideRequestReport(
            @RequestParam("id") Long id,
            @RequestParam(value = "templateCode", defaultValue = "outside-request") String templateCode,
            HttpServletResponse response) throws IOException {
        // 生成报告
        byte[] reportData = reportGenerateService.generateOutsideRequestReport(id, templateCode);

        // 获取外协请求信息用于文件名
        OutsideRequestRespVO request = outsideRequestService.getOutsideRequestDetail(id);
        String fileName = String.format("外协请求报告_%s_%s.docx",
                request.getProjectName() != null ? request.getProjectName() : "未知项目",
                request.getId());

        // 设置响应头
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLength(reportData.length);

        // 写入响应
        response.getOutputStream().write(reportData);
        response.getOutputStream().flush();
    }

}
