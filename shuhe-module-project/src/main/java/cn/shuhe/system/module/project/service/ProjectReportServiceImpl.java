package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectReportPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectReportSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectReportDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectReportMapper;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class ProjectReportServiceImpl implements ProjectReportService {

    @Resource
    private ProjectReportMapper projectReportMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Override
    public Long createReport(ProjectReportSaveReqVO createReqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();

        ProjectReportDO report = BeanUtils.toBean(createReqVO, ProjectReportDO.class);
        report.setWeekStartDate(LocalDate.parse(createReqVO.getWeekStartDate()));
        report.setWeekEndDate(LocalDate.parse(createReqVO.getWeekEndDate()));

        if (CollUtil.isNotEmpty(createReqVO.getAttachments())) {
            report.setAttachments(JSONUtil.toJsonStr(createReqVO.getAttachments()));
        }

        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user != null) {
            report.setCreatorName(user.getNickname());
            report.setDeptId(user.getDeptId());
            if (user.getDeptId() != null) {
                DeptRespDTO dept = deptApi.getDept(user.getDeptId());
                if (dept != null) {
                    report.setDeptName(dept.getName());
                }
            }
        }

        projectReportMapper.insert(report);
        return report.getId();
    }

    @Override
    public void updateReport(ProjectReportSaveReqVO updateReqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();

        ProjectReportDO existReport = projectReportMapper.selectById(updateReqVO.getId());
        if (existReport == null) {
            throw exception(PROJECT_REPORT_NOT_EXISTS);
        }
        if (!String.valueOf(userId).equals(existReport.getCreator())) {
            throw exception(PROJECT_REPORT_UPDATE_NOT_OWNER);
        }

        ProjectReportDO updateReport = BeanUtils.toBean(updateReqVO, ProjectReportDO.class);
        updateReport.setWeekStartDate(LocalDate.parse(updateReqVO.getWeekStartDate()));
        updateReport.setWeekEndDate(LocalDate.parse(updateReqVO.getWeekEndDate()));

        if (updateReqVO.getAttachments() != null) {
            updateReport.setAttachments(JSONUtil.toJsonStr(updateReqVO.getAttachments()));
        }

        projectReportMapper.updateById(updateReport);
    }

    @Override
    public void deleteReport(Long id) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();

        ProjectReportDO existReport = projectReportMapper.selectById(id);
        if (existReport == null) {
            throw exception(PROJECT_REPORT_NOT_EXISTS);
        }
        if (!String.valueOf(userId).equals(existReport.getCreator())) {
            throw exception(PROJECT_REPORT_DELETE_NOT_OWNER);
        }

        projectReportMapper.deleteById(id);
    }

    @Override
    public ProjectReportDO getReport(Long id) {
        return projectReportMapper.selectById(id);
    }

    @Override
    public ProjectReportDO getReportByProjectAndWeek(Long projectId, Integer year, Integer weekNumber) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return projectReportMapper.selectByCreatorAndProjectAndWeek(String.valueOf(userId), projectId, year, weekNumber);
    }

    @Override
    public PageResult<ProjectReportDO> getReportPage(ProjectReportPageReqVO pageReqVO) {
        return projectReportMapper.selectPage(pageReqVO);
    }

}
