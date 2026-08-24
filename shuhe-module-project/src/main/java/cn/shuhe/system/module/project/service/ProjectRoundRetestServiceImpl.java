package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundRetestBatchSaveReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundVulnerabilityRetestSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundRetestBatchDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundVulnerabilityDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundVulnerabilityRetestDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundRetestBatchMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundVulnerabilityMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundVulnerabilityRetestMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.PROJECT_ROUND_RETEST_BATCH_NOT_EXISTS;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.PROJECT_ROUND_RETEST_VULNERABILITY_MISMATCH;

@Service
public class ProjectRoundRetestServiceImpl implements ProjectRoundRetestService {
    @Resource private ProjectRoundRetestBatchMapper batchMapper;
    @Resource private ProjectRoundVulnerabilityRetestMapper resultMapper;
    @Resource private ProjectRoundVulnerabilityMapper vulnerabilityMapper;

    @Override
    public Long createBatch(ProjectRoundRetestBatchSaveReqVO reqVO) {
        ProjectRoundRetestBatchDO batch = ProjectRoundRetestBatchDO.builder()
                .roundId(reqVO.getRoundId())
                .batchNo(batchMapper.nextBatchNo(reqVO.getRoundId()))
                .executorId(reqVO.getExecutorId())
                .executorName(reqVO.getExecutorName())
                .plannedTime(reqVO.getPlannedTime())
                .status("pending")
                .summary(reqVO.getSummary())
                .build();
        batchMapper.insert(batch);
        return batch.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveResult(ProjectRoundVulnerabilityRetestSaveReqVO reqVO) {
        ProjectRoundRetestBatchDO batch = mustBatch(reqVO.getBatchId());
        ProjectRoundVulnerabilityDO vulnerability = vulnerabilityMapper.selectById(reqVO.getVulnerabilityId());
        if (vulnerability == null || !batch.getRoundId().equals(vulnerability.getRoundId())) {
            throw exception(PROJECT_ROUND_RETEST_VULNERABILITY_MISMATCH);
        }
        ProjectRoundVulnerabilityRetestDO existing = resultMapper.selectOne(
                new LambdaQueryWrapperX<ProjectRoundVulnerabilityRetestDO>()
                        .eq(ProjectRoundVulnerabilityRetestDO::getBatchId, reqVO.getBatchId())
                        .eq(ProjectRoundVulnerabilityRetestDO::getVulnerabilityId, reqVO.getVulnerabilityId()));
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userName = SecurityFrameworkUtils.getLoginUserNickname();
        ProjectRoundVulnerabilityRetestDO result = ProjectRoundVulnerabilityRetestDO.builder()
                .id(existing == null ? null : existing.getId())
                .batchId(reqVO.getBatchId())
                .vulnerabilityId(reqVO.getVulnerabilityId())
                .result(reqVO.getResult())
                .evidence(reqVO.getEvidence())
                .retestedBy(userId)
                .retestedByName(userName)
                .retestedAt(LocalDateTime.now())
                .remark(reqVO.getRemark())
                .build();
        if (existing == null) {
            resultMapper.insert(result);
        } else {
            resultMapper.updateById(result);
        }
        // 兼容历史报告取最新复测结果，完整历史仍以批次表为准。
        ProjectRoundVulnerabilityDO latest = new ProjectRoundVulnerabilityDO();
        latest.setId(vulnerability.getId());
        latest.setRetestStatus(reqVO.getResult());
        latest.setRetestReport(reqVO.getEvidence());
        latest.setRetestDate(LocalDateTime.now().toLocalDate());
        latest.setRetestTime(LocalDateTime.now());
        vulnerabilityMapper.updateById(latest);
        return result.getId();
    }

    @Override
    public void completeBatch(Long batchId, String summary) {
        ProjectRoundRetestBatchDO batch = mustBatch(batchId);
        ProjectRoundRetestBatchDO update = new ProjectRoundRetestBatchDO();
        update.setId(batch.getId());
        update.setStatus("completed");
        update.setSummary(summary);
        update.setCompletedAt(LocalDateTime.now());
        batchMapper.updateById(update);
    }

    private ProjectRoundRetestBatchDO mustBatch(Long id) {
        ProjectRoundRetestBatchDO batch = batchMapper.selectById(id);
        if (batch == null) throw exception(PROJECT_ROUND_RETEST_BATCH_NOT_EXISTS);
        return batch;
    }

    @Override public ProjectRoundRetestBatchDO getBatch(Long batchId) { return mustBatch(batchId); }
    @Override public List<ProjectRoundRetestBatchDO> listBatches(Long roundId) { return batchMapper.selectListByRoundId(roundId); }
    @Override public List<ProjectRoundVulnerabilityRetestDO> listResults(Long batchId) {
        mustBatch(batchId);
        return resultMapper.selectListByBatchId(batchId);
    }
}
