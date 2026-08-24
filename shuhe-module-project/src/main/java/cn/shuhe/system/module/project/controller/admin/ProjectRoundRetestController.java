package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundRetestBatchSaveReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundVulnerabilityRetestSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundRetestBatchDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundVulnerabilityRetestDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMemberMapper;
import cn.shuhe.system.module.project.service.ProjectRoundRetestService;
import cn.shuhe.system.module.project.service.access.ProjectAccessService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.PROJECT_ROUND_NOT_EXISTS;

@RestController
@RequestMapping("/project/round-retest")
public class ProjectRoundRetestController {
    @Resource private ProjectRoundRetestService service;
    @Resource private ProjectRoundMapper roundMapper;
    @Resource private ProjectRoundMemberMapper memberMapper;
    @Resource private ProjectAccessService accessService;

    @PostMapping("/batch/create")
    @PreAuthorize("@ss.hasAnyPermissions('project:info:update', 'project:my-tasks:update')")
    public CommonResult<Long> createBatch(@Valid @RequestBody ProjectRoundRetestBatchSaveReqVO reqVO) {
        validate(reqVO.getRoundId(), true);
        return success(service.createBatch(reqVO));
    }

    @PostMapping("/result/save")
    @PreAuthorize("@ss.hasAnyPermissions('project:info:update', 'project:my-tasks:update')")
    public CommonResult<Long> saveResult(@Valid @RequestBody ProjectRoundVulnerabilityRetestSaveReqVO reqVO) {
        ProjectRoundRetestBatchDO batch = service.getBatch(reqVO.getBatchId());
        validate(batch.getRoundId(), true);
        return success(service.saveResult(reqVO));
    }

    @PutMapping("/batch/complete")
    @PreAuthorize("@ss.hasAnyPermissions('project:info:update', 'project:my-tasks:update')")
    public CommonResult<Boolean> completeBatch(@RequestParam Long batchId,
                                               @RequestParam(required = false) String summary) {
        ProjectRoundRetestBatchDO batch = service.getBatch(batchId);
        validate(batch.getRoundId(), true);
        service.completeBatch(batchId, summary);
        return success(true);
    }

    @GetMapping("/batch/list")
    @PreAuthorize("@ss.hasAnyPermissions('project:info:query', 'project:my-tasks:query')")
    public CommonResult<List<ProjectRoundRetestBatchDO>> listBatches(@RequestParam Long roundId) {
        validate(roundId, false);
        return success(service.listBatches(roundId));
    }

    @GetMapping("/result/list")
    @PreAuthorize("@ss.hasAnyPermissions('project:info:query', 'project:my-tasks:query')")
    public CommonResult<List<ProjectRoundVulnerabilityRetestDO>> listResults(@RequestParam Long batchId) {
        ProjectRoundRetestBatchDO batch = service.getBatch(batchId);
        validate(batch.getRoundId(), false);
        return success(service.listResults(batchId));
    }

    private void validate(Long roundId, boolean write) {
        ProjectRoundDO round = roundMapper.selectById(roundId);
        if (round == null) throw exception(PROJECT_ROUND_NOT_EXISTS);
        if (memberMapper.existsByRoundIdAndUserId(roundId, getLoginUserId())) return;
        if (write) accessService.validateManageProject(round.getProjectId(), getLoginUserId());
        else accessService.validateViewProject(round.getProjectId(), getLoginUserId());
    }
}
