package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.datapermission.core.annotation.DataPermission;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceLaunchMemberRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceLaunchPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceLaunchRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceLaunchSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchMemberDO;
import cn.shuhe.system.module.project.service.EmployeeScheduleService;
import cn.shuhe.system.module.project.service.ServiceLaunchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 统一服务发起")
@RestController
@RequestMapping("/project/service-launch")
@Validated
public class ServiceLaunchController {

    @Resource
    private ServiceLaunchService serviceLaunchService;

    @Resource
    private EmployeeScheduleService employeeScheduleService;

    @PostMapping("/create")
    @Operation(summary = "创建统一服务发起")
    @PreAuthorize("@ss.hasPermission('project:service-launch:create')")
    public CommonResult<Long> createServiceLaunch(@Valid @RequestBody ServiceLaunchSaveReqVO createReqVO) {
        return success(serviceLaunchService.createServiceLaunch(createReqVO));
    }

    @GetMapping("/bpm-enabled")
    @Operation(summary = "查询当前 service-launch 是否启用 BPM 审批",
            description = "测试阶段开关：返回 false 时，前端创建后不应再调用 createProcessInstance。"
                    + "由 shuhe.bpm.service-launch.enabled 配置控制，默认 true（走标准审批流程）。")
    public CommonResult<Boolean> getBpmServiceLaunchEnabled() {
        return success(serviceLaunchService.isBpmServiceLaunchEnabled());
    }

    @PutMapping("/update")
    @Operation(summary = "更新统一服务发起")
    @PreAuthorize("@ss.hasPermission('project:service-launch:update')")
    public CommonResult<Boolean> updateServiceLaunch(@Valid @RequestBody ServiceLaunchSaveReqVO updateReqVO) {
        serviceLaunchService.updateServiceLaunch(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-process-instance-id")
    @Operation(summary = "更新流程实例ID")
    @Parameter(name = "id", description = "发起ID", required = true)
    @Parameter(name = "processInstanceId", description = "流程实例ID", required = true)
    public CommonResult<Boolean> updateProcessInstanceId(@RequestParam("id") Long id,
                                                          @RequestParam("processInstanceId") String processInstanceId) {
        serviceLaunchService.updateProcessInstanceId(id, processInstanceId);
        return success(true);
    }

    @PutMapping("/set-executors")
    @Operation(summary = "设置执行人（审批时调用）")
    @Parameter(name = "id", description = "发起ID", required = true)
    @Parameter(name = "executorIds", description = "执行人ID列表", required = true)
    public CommonResult<Boolean> setExecutors(@RequestParam("id") Long id,
                                               @RequestParam("executorIds") List<Long> executorIds) {
        serviceLaunchService.setExecutors(id, executorIds);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除统一服务发起")
    @Parameter(name = "id", description = "发起ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-launch:delete')")
    public CommonResult<Boolean> deleteServiceLaunch(@RequestParam("id") Long id) {
        serviceLaunchService.deleteServiceLaunch(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取统一服务发起详情")
    @Parameter(name = "id", description = "发起ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-launch:query')")
    public CommonResult<ServiceLaunchRespVO> getServiceLaunch(@RequestParam("id") Long id) {
        return success(serviceLaunchService.getServiceLaunchDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取统一服务发起分页")
    @PreAuthorize("@ss.hasPermission('project:service-launch:query')")
    public CommonResult<PageResult<ServiceLaunchRespVO>> getServiceLaunchPage(@Valid ServiceLaunchPageReqVO pageReqVO) {
        return success(serviceLaunchService.getServiceLaunchPage(pageReqVO));
    }

    @GetMapping("/my-page")
    @Operation(summary = "获取我发起的统一服务发起分页")
    public CommonResult<PageResult<ServiceLaunchRespVO>> getMyServiceLaunchPage(@Valid ServiceLaunchPageReqVO pageReqVO) {
        return success(serviceLaunchService.getMyServiceLaunchPage(pageReqVO));
    }

    @GetMapping("/contract-list")
    @Operation(summary = "获取可发起服务的合同列表")
    public CommonResult<List<Map<String, Object>>> getContractListForLaunch() {
        return success(serviceLaunchService.getContractListForLaunch());
    }

    @GetMapping("/service-item-list")
    @Operation(summary = "根据合同获取服务项列表")
    @Parameter(name = "contractId", description = "合同ID", required = true)
    public CommonResult<List<Map<String, Object>>> getServiceItemListByContract(@RequestParam("contractId") Long contractId) {
        return success(serviceLaunchService.getServiceItemListByContract(contractId));
    }

    @GetMapping("/dept-list")
    @Operation(summary = "获取可选的执行部门列表")
    public CommonResult<List<Map<String, Object>>> getDeptList() {
        return success(serviceLaunchService.getDeptList());
    }

    @GetMapping("/dept-type-list")
    @Operation(summary = "获取借调可选的部门类型列表（安全运营、安全服务、数据安全）")
    public CommonResult<List<Map<String, Object>>> getDeptTypeListForSecondment() {
        return success(serviceLaunchService.getDeptTypeListForSecondment());
    }

    @GetMapping("/dept-info")
    @Operation(summary = "获取部门信息（含负责人，用于借调流程变量）")
    @Parameter(name = "deptId", description = "部门ID", required = true)
    public CommonResult<Map<String, Object>> getDeptInfo(@RequestParam("deptId") Long deptId) {
        return success(serviceLaunchService.getDeptInfo(deptId));
    }

    @GetMapping("/dept-employee-status")
    @Operation(summary = "获取部门员工状态（借调选人用，复用 service-launch:create 权限）")
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-launch:create')")
    @DataPermission(enable = false) // 借调需跨部门选人，不受数据权限限制
    public CommonResult<List<Map<String, Object>>> getDeptEmployeeStatusForSecondment(@RequestParam("deptId") Long deptId) {
        return success(employeeScheduleService.getDeptEmployeeStatus(deptId));
    }

    @GetMapping("/earliest-available")
    @Operation(summary = "获取部门最早可安排时间（借调用，复用 service-launch:create 权限）")
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-launch:create')")
    @DataPermission(enable = false) // 借调需跨部门选人，不受数据权限限制
    public CommonResult<LocalDateTime> getEarliestAvailableForSecondment(@RequestParam("deptId") Long deptId) {
        return success(employeeScheduleService.getEarliestAvailableTime(deptId));
    }

    @GetMapping("/user-list-by-dept")
    @Operation(summary = "根据部门ID获取用户列表（包含子部门）")
    @Parameter(name = "deptId", description = "部门ID", required = true)
    public CommonResult<List<Map<String, Object>>> getUserListByDept(@RequestParam("deptId") Long deptId) {
        return success(serviceLaunchService.getUserListByDept(deptId));
    }

    @GetMapping("/dept-leader-list")
    @Operation(summary = "获取所有安全部门负责人列表（用于代他人发起）")
    public CommonResult<List<Map<String, Object>>> getDeptLeaderList() {
        return success(serviceLaunchService.getDeptLeaderList());
    }

    @GetMapping("/child-dept-list")
    @Operation(summary = "获取指定部门的子部门列表（审批时选择实际执行部门）")
    @Parameter(name = "parentDeptId", description = "父部门ID", required = true)
    public CommonResult<List<Map<String, Object>>> getChildDeptList(@RequestParam("parentDeptId") Long parentDeptId) {
        return success(serviceLaunchService.getChildDeptList(parentDeptId));
    }

    @PutMapping("/set-actual-execute-dept")
    @Operation(summary = "设置实际执行部门（审批时调用）")
    @Parameter(name = "id", description = "发起ID", required = true)
    @Parameter(name = "actualExecuteDeptId", description = "实际执行部门ID", required = true)
    public CommonResult<Boolean> setActualExecuteDept(@RequestParam("id") Long id,
                                                       @RequestParam("actualExecuteDeptId") Long actualExecuteDeptId) {
        serviceLaunchService.setActualExecuteDept(id, actualExecuteDeptId);
        return success(true);
    }

    // ==================== 外出服务相关 ====================

    @GetMapping("/outside-page")
    @Operation(summary = "获取外出服务发起分页")
    @PreAuthorize("@ss.hasPermission('project:service-launch:query')")
    public CommonResult<PageResult<ServiceLaunchRespVO>> getOutsideServiceLaunchPage(@Valid ServiceLaunchPageReqVO pageReqVO) {
        return success(serviceLaunchService.getOutsideServiceLaunchPage(pageReqVO));
    }

    @GetMapping("/members")
    @Operation(summary = "获取服务发起的执行人列表")
    @Parameter(name = "launchId", description = "服务发起ID", required = true)
    public CommonResult<List<ServiceLaunchMemberRespVO>> getLaunchMembers(@RequestParam("launchId") Long launchId) {
        List<ServiceLaunchMemberDO> members = serviceLaunchService.getLaunchMembers(launchId);
        List<ServiceLaunchMemberRespVO> result = members.stream()
                .map(this::convertToMemberRespVO)
                .toList();
        return success(result);
    }

    @GetMapping("/member")
    @Operation(summary = "获取执行人详情")
    @Parameter(name = "memberId", description = "执行人记录ID", required = true)
    public CommonResult<ServiceLaunchMemberRespVO> getLaunchMember(@RequestParam("memberId") Long memberId) {
        ServiceLaunchMemberDO member = serviceLaunchService.getLaunchMember(memberId);
        if (member == null) {
            return success(null);
        }
        return success(convertToMemberRespVO(member));
    }

    @PostMapping("/member/finish")
    @Operation(summary = "完成外出任务")
    @Parameter(name = "memberId", description = "执行人记录ID", required = true)
    @Parameter(name = "hasAttachment", description = "是否有附件")
    @Parameter(name = "attachmentUrl", description = "附件URL")
    @Parameter(name = "remark", description = "备注")
    public CommonResult<Boolean> finishLaunchMember(
            @RequestParam("memberId") Long memberId,
            @RequestParam(value = "hasAttachment", required = false) Boolean hasAttachment,
            @RequestParam(value = "attachmentUrl", required = false) String attachmentUrl,
            @RequestParam(value = "remark", required = false) String remark) {
        serviceLaunchService.finishLaunchMember(memberId, hasAttachment, attachmentUrl, remark);
        return success(true);
    }

    /**
     * 转换为执行人响应VO
     */
    private ServiceLaunchMemberRespVO convertToMemberRespVO(ServiceLaunchMemberDO member) {
        ServiceLaunchMemberRespVO vo = new ServiceLaunchMemberRespVO();
        vo.setId(member.getId());
        vo.setLaunchId(member.getLaunchId());
        vo.setUserId(member.getUserId());
        vo.setUserName(member.getUserName());
        vo.setUserDeptId(member.getUserDeptId());
        vo.setUserDeptName(member.getUserDeptName());
        vo.setConfirmStatus(member.getConfirmStatus());
        vo.setConfirmTime(member.getConfirmTime());
        vo.setOaProcessInstanceId(member.getOaProcessInstanceId());
        vo.setFinishStatus(member.getFinishStatus());
        vo.setFinishTime(member.getFinishTime());
        vo.setAttachmentUrl(member.getAttachmentUrl());
        vo.setFinishRemark(member.getFinishRemark());
        return vo;
    }

}
