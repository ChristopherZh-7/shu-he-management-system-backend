package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectReportPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectReportSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectReportDO;
import jakarta.validation.Valid;

public interface ProjectReportService {

    Long createReport(@Valid ProjectReportSaveReqVO createReqVO);

    void updateReport(@Valid ProjectReportSaveReqVO updateReqVO);

    void deleteReport(Long id);

    ProjectReportDO getReport(Long id);

    ProjectReportDO getReportByProjectAndWeek(Long projectId, Integer year, Integer weekNumber);

    PageResult<ProjectReportDO> getReportPage(ProjectReportPageReqVO pageReqVO);

}
