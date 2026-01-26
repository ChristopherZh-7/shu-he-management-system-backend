package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceExecutionPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceExecutionRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceExecutionSaveReqVO;
import cn.shuhe.system.module.project.service.ServiceExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 服务执行申请")
@RestController
@RequestMapping("/project/service-execution")
@Validated
public class ServiceExecutionController {

    @Resource
    private ServiceExecutionService serviceExecutionService;

    @PostMapping("/create")
    @Operation(summary = "创建服务执行申请")
    @PreAuthorize("@ss.hasPermission('project:service-execution:create')")
    public CommonResult<Long> createServiceExecution(@Valid @RequestBody ServiceExecutionSaveReqVO createReqVO) {
        return success(serviceExecutionService.createServiceExecution(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新服务执行申请")
    @PreAuthorize("@ss.hasPermission('project:service-execution:update')")
    public CommonResult<Boolean> updateServiceExecution(@Valid @RequestBody ServiceExecutionSaveReqVO updateReqVO) {
        serviceExecutionService.updateServiceExecution(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-process-instance-id")
    @Operation(summary = "更新流程实例ID")
    @Parameter(name = "id", description = "申请ID", required = true)
    @Parameter(name = "processInstanceId", description = "流程实例ID", required = true)
    public CommonResult<Boolean> updateProcessInstanceId(@RequestParam("id") Long id,
                                                          @RequestParam("processInstanceId") String processInstanceId) {
        serviceExecutionService.updateProcessInstanceId(id, processInstanceId);
        return success(true);
    }

    @PutMapping("/set-executors")
    @Operation(summary = "设置执行人（审批时调用）")
    @Parameter(name = "id", description = "申请ID", required = true)
    @Parameter(name = "executorIds", description = "执行人ID列表", required = true)
    public CommonResult<Boolean> setExecutors(@RequestParam("id") Long id,
                                               @RequestParam("executorIds") List<Long> executorIds) {
        serviceExecutionService.setExecutors(id, executorIds);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除服务执行申请")
    @Parameter(name = "id", description = "申请ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-execution:delete')")
    public CommonResult<Boolean> deleteServiceExecution(@RequestParam("id") Long id) {
        serviceExecutionService.deleteServiceExecution(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取服务执行申请详情")
    @Parameter(name = "id", description = "申请ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:service-execution:query')")
    public CommonResult<ServiceExecutionRespVO> getServiceExecution(@RequestParam("id") Long id) {
        return success(serviceExecutionService.getServiceExecutionDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取服务执行申请分页")
    @PreAuthorize("@ss.hasPermission('project:service-execution:query')")
    public CommonResult<PageResult<ServiceExecutionRespVO>> getServiceExecutionPage(@Valid ServiceExecutionPageReqVO pageReqVO) {
        return success(serviceExecutionService.getServiceExecutionPage(pageReqVO));
    }

    @GetMapping("/my-page")
    @Operation(summary = "获取我发起的服务执行申请分页")
    public CommonResult<PageResult<ServiceExecutionRespVO>> getMyServiceExecutionPage(@Valid ServiceExecutionPageReqVO pageReqVO) {
        return success(serviceExecutionService.getMyServiceExecutionPage(pageReqVO));
    }

}
