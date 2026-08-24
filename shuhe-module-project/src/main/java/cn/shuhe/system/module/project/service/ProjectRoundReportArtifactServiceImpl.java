package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundReportArtifactDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundReportArtifactMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.PROJECT_ROUND_REPORT_REQUIRED;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.PROJECT_ROUND_REPORT_STATUS_INVALID;

@Service
public class ProjectRoundReportArtifactServiceImpl implements ProjectRoundReportArtifactService {
    @Resource
    private ProjectRoundReportArtifactMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordGenerated(Long roundId, String reportType, String templateCode,
                                String fileName, String fileHash) {
        ProjectRoundReportArtifactDO latest = mapper.selectLatest(roundId);
        int version = latest == null ? 1 : latest.getVersionNo() + 1;
        ProjectRoundReportArtifactDO artifact = ProjectRoundReportArtifactDO.builder()
                .roundId(roundId)
                .reportType(reportType)
                .versionNo(version)
                .templateCode(templateCode)
                .fileName(fileName)
                .fileHash(fileHash)
                .status("draft")
                .createdBy(SecurityFrameworkUtils.getLoginUserId())
                .build();
        mapper.insert(artifact);
        return artifact.getId();
    }

    @Override
    public void submitLatest(Long roundId) {
        updateLatest(roundId, "draft", "pending_review", null, false, false);
    }

    @Override
    public void approveLatest(Long roundId, String comment) {
        updateLatest(roundId, "pending_review", "approved", comment, true, false);
    }

    @Override
    public void rejectLatest(Long roundId, String comment) {
        updateLatest(roundId, "pending_review", "draft", comment, true, false);
    }

    @Override
    public void deliverLatest(Long roundId, String receiver) {
        updateLatest(roundId, "approved", "delivered", receiver, false, true);
    }

    private void updateLatest(Long roundId, String expected, String target, String comment,
                              boolean review, boolean deliver) {
        ProjectRoundReportArtifactDO latest = mapper.selectLatest(roundId);
        if (latest == null) {
            throw exception(PROJECT_ROUND_REPORT_REQUIRED);
        }
        if (!expected.equals(latest.getStatus())) {
            throw exception(PROJECT_ROUND_REPORT_STATUS_INVALID);
        }
        LambdaUpdateWrapper<ProjectRoundReportArtifactDO> update =
                new LambdaUpdateWrapper<ProjectRoundReportArtifactDO>()
                        .eq(ProjectRoundReportArtifactDO::getId, latest.getId())
                        .set(ProjectRoundReportArtifactDO::getStatus, target);
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (review) {
            update.set(ProjectRoundReportArtifactDO::getReviewedBy, userId)
                    .set(ProjectRoundReportArtifactDO::getReviewedAt, LocalDateTime.now())
                    .set(ProjectRoundReportArtifactDO::getReviewComment, comment);
        }
        if (deliver) {
            update.set(ProjectRoundReportArtifactDO::getDeliveredBy, userId)
                    .set(ProjectRoundReportArtifactDO::getDeliveredAt, LocalDateTime.now())
                    .set(ProjectRoundReportArtifactDO::getReceiver, comment);
        }
        mapper.update(null, update);
    }

    @Override
    public List<ProjectRoundReportArtifactDO> listByRoundId(Long roundId) {
        return mapper.selectListByRoundId(roundId);
    }
}
