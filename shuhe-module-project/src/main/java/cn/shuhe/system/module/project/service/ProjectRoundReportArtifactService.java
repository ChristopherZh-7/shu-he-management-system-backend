package cn.shuhe.system.module.project.service;

import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundReportArtifactDO;

import java.util.List;

public interface ProjectRoundReportArtifactService {
    Long recordGenerated(Long roundId, String reportType, String templateCode, String fileName, String fileHash);
    void submitLatest(Long roundId);
    void approveLatest(Long roundId, String comment);
    void rejectLatest(Long roundId, String comment);
    void deliverLatest(Long roundId, String receiver);
    List<ProjectRoundReportArtifactDO> listByRoundId(Long roundId);
}
