package cn.shuhe.system.module.project.service;

import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundRetestBatchSaveReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundVulnerabilityRetestSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundRetestBatchDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundVulnerabilityRetestDO;

import java.util.List;

public interface ProjectRoundRetestService {
    Long createBatch(ProjectRoundRetestBatchSaveReqVO reqVO);
    Long saveResult(ProjectRoundVulnerabilityRetestSaveReqVO reqVO);
    void completeBatch(Long batchId, String summary);
    ProjectRoundRetestBatchDO getBatch(Long batchId);
    List<ProjectRoundRetestBatchDO> listBatches(Long roundId);
    List<ProjectRoundVulnerabilityRetestDO> listResults(Long batchId);
}
