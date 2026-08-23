package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteMemberRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteMemberDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.BusinessTimeMapper;
import cn.shuhe.system.module.project.dal.mysql.ContractTimeMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectSiteMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectSiteMemberMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 项目驻场点 Service 实现类
 */
@Service
@Validated
@Slf4j
public class ProjectSiteServiceImpl implements ProjectSiteService {

    @Resource
    private ProjectSiteMapper siteMapper;

    @Resource
    private ProjectSiteMemberMapper memberMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ContractTimeMapper contractTimeMapper;

    @Resource
    private BusinessTimeMapper businessTimeMapper;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSite(ProjectSiteSaveReqVO createReqVO) {
        // 校验项目是否存在
        ProjectDO project = projectMapper.selectById(createReqVO.getProjectId());
        if (project == null) {
            throw exception(PROJECT_NOT_EXISTS);
        }

        // 创建驻场点
        ProjectSiteDO site = BeanUtils.toBean(createReqVO, ProjectSiteDO.class);
        site.setStatus(ProjectSiteDO.STATUS_ENABLED);
        site.setSort(0);

        SiteDates dates = resolveSiteDates(project, site.getStartDate(), site.getEndDate());
        site.setStartDate(dates.startDate());
        site.setEndDate(dates.endDate());
        validateDateRange(site.getStartDate(), site.getEndDate());

        siteMapper.insert(site);

        log.info("[createSite][创建驻场点成功，id={}，projectId={}，name={}]",
                site.getId(), site.getProjectId(), site.getName());
        return site.getId();
    }

    @Override
    public void updateSite(ProjectSiteSaveReqVO updateReqVO) {
        // 校验存在
        ProjectSiteDO existing = validateSiteExists(updateReqVO.getId());
        ProjectDO project = projectMapper.selectById(existing.getProjectId());
        if (project == null) {
            throw exception(PROJECT_NOT_EXISTS);
        }

        ProjectSiteDO updateObj = BeanUtils.toBean(updateReqVO, ProjectSiteDO.class);
        SiteDates dates = resolveSiteDates(project, updateReqVO.getStartDate(), updateReqVO.getEndDate());
        updateObj.setStartDate(dates.startDate());
        updateObj.setEndDate(dates.endDate());
        validateDateRange(updateObj.getStartDate(), updateObj.getEndDate());
        siteMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSite(Long id) {
        // 校验存在
        validateSiteExists(id);

        // 删除驻场点下的所有人员
        memberMapper.deleteBySiteId(id);

        // 删除驻场点
        siteMapper.deleteById(id);
    }

    @Override
    public ProjectSiteDO getSite(Long id) {
        return siteMapper.selectById(id);
    }

    @Override
    public ProjectSiteRespVO getSiteDetail(Long id) {
        ProjectSiteDO site = siteMapper.selectById(id);
        if (site == null) {
            return null;
        }
        return convertToRespVO(site);
    }

    @Override
    public List<ProjectSiteDO> getListByProjectIdAndDeptType(Long projectId, Integer deptType) {
        return siteMapper.selectListByProjectIdAndDeptType(projectId, deptType);
    }

    @Override
    public List<ProjectSiteDO> getListByProjectId(Long projectId) {
        return siteMapper.selectListByProjectId(projectId);
    }

    @Override
    public List<ProjectSiteRespVO> getSiteDetailListByProjectIdAndDeptType(Long projectId, Integer deptType) {
        List<ProjectSiteDO> sites = siteMapper.selectListByProjectIdAndDeptType(projectId, deptType);
        if (CollUtil.isEmpty(sites)) {
            return Collections.emptyList();
        }

        List<ProjectSiteRespVO> result = new ArrayList<>(sites.size());
        for (ProjectSiteDO site : sites) {
            result.add(convertToRespVO(site));
        }
        return result;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        // 校验存在
        validateSiteExists(id);

        // 更新状态
        ProjectSiteDO updateObj = new ProjectSiteDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        siteMapper.updateById(updateObj);
    }

    @Override
    public boolean hasSite(Long projectId, Integer deptType) {
        Long count = siteMapper.selectCountByProjectIdAndDeptType(projectId, deptType);
        return count != null && count > 0;
    }

    /**
     * 将 DO 转换为 RespVO（包含人员列表）
     */
    private ProjectSiteRespVO convertToRespVO(ProjectSiteDO site) {
        ProjectSiteRespVO respVO = BeanUtils.toBean(site, ProjectSiteRespVO.class);
        ProjectDO project = projectMapper.selectById(site.getProjectId());
        SiteDates dates = project == null
                ? new SiteDates(site.getStartDate(), site.getEndDate(), "manual", "手工录入")
                : resolveSiteDates(project, site.getStartDate(), site.getEndDate());
        respVO.setStartDate(dates.startDate());
        respVO.setEndDate(dates.endDate());
        respVO.setDateSource(dates.source());
        respVO.setDateSourceName(dates.sourceName());

        boolean hasOnsiteServiceItem = serviceItemMapper
                .selectListByProjectIdAndDeptType(site.getProjectId(), site.getDeptType()).stream()
                .anyMatch(item -> !Integer.valueOf(4).equals(item.getStatus()) && isOnsiteItem(item));
        respVO.setHasOnsiteServiceItem(hasOnsiteServiceItem);

        // 查询该驻场点的人员列表
        List<ProjectSiteMemberDO> members = memberMapper.selectListBySiteId(site.getId());
        if (CollUtil.isNotEmpty(members)) {
            List<ProjectSiteMemberRespVO> memberVOs = new ArrayList<>(members.size());
            for (ProjectSiteMemberDO member : members) {
                ProjectSiteMemberRespVO memberVO = BeanUtils.toBean(member, ProjectSiteMemberRespVO.class);
                // 设置人员类型名称
                if (member.getMemberType() != null) {
                    memberVO.setMemberTypeName(member.getMemberType() == ProjectSiteMemberDO.MEMBER_TYPE_MANAGEMENT ? "管理人员" : "驻场人员");
                }
                // 设置状态名称
                if (member.getStatus() != null) {
                    switch (member.getStatus()) {
                        case ProjectSiteMemberDO.STATUS_PENDING:
                            memberVO.setStatusName("待入场");
                            break;
                        case ProjectSiteMemberDO.STATUS_ACTIVE:
                            memberVO.setStatusName("在岗");
                            break;
                        case ProjectSiteMemberDO.STATUS_LEFT:
                            memberVO.setStatusName("已离开");
                            break;
                        default:
                            memberVO.setStatusName("未知");
                    }
                }
                memberVOs.add(memberVO);
            }
            respVO.setMembers(memberVOs);
            long activeOnsiteCount = members.stream()
                    .filter(m -> Integer.valueOf(ProjectSiteMemberDO.MEMBER_TYPE_ONSITE).equals(m.getMemberType()))
                    .filter(m -> Integer.valueOf(ProjectSiteMemberDO.STATUS_ACTIVE).equals(m.getStatus()))
                    .count();
            long managementCount = members.stream()
                    .filter(m -> Integer.valueOf(ProjectSiteMemberDO.MEMBER_TYPE_MANAGEMENT).equals(m.getMemberType()))
                    .filter(m -> !Integer.valueOf(ProjectSiteMemberDO.STATUS_LEFT).equals(m.getStatus()))
                    .count();
            respVO.setManagementMemberCount((int) managementCount);
            fillDeliveryStatus(respVO, site, dates, hasOnsiteServiceItem, (int) activeOnsiteCount);
            if (!Integer.valueOf(2).equals(respVO.getDeliveryStatus())) {
                memberVOs.stream()
                        .filter(member -> Integer.valueOf(ProjectSiteMemberDO.MEMBER_TYPE_ONSITE)
                                .equals(member.getMemberType()))
                        .filter(member -> Integer.valueOf(ProjectSiteMemberDO.STATUS_ACTIVE)
                                .equals(member.getStatus()))
                        .forEach(member -> member.setStatusName("待入场"));
            }
        } else {
            respVO.setMembers(Collections.emptyList());
            respVO.setManagementMemberCount(0);
            fillDeliveryStatus(respVO, site, dates, hasOnsiteServiceItem, 0);
        }

        return respVO;
    }

    private void fillDeliveryStatus(ProjectSiteRespVO respVO, ProjectSiteDO site, SiteDates dates,
                                    boolean hasOnsiteServiceItem, int activeOnsiteCount) {
        LocalDate today = LocalDate.now();
        int deliveryStatus;
        String deliveryStatusName;
        if (Integer.valueOf(ProjectSiteDO.STATUS_DISABLED).equals(site.getStatus())
                || dates.endDate() != null && dates.endDate().isBefore(today)) {
            deliveryStatus = 3;
            deliveryStatusName = "已退场";
        } else if (!hasOnsiteServiceItem) {
            deliveryStatus = 0;
            deliveryStatusName = "计划中";
        } else if (dates.startDate() != null && dates.startDate().isAfter(today)
                || activeOnsiteCount == 0) {
            deliveryStatus = 1;
            deliveryStatusName = "待入场";
        } else {
            deliveryStatus = 2;
            deliveryStatusName = "驻场中";
        }
        int actualCount = deliveryStatus == 2 ? activeOnsiteCount : 0;
        int plannedCount = site.getStaffCount() == null ? 0 : Math.max(0, site.getStaffCount());
        respVO.setDeliveryStatus(deliveryStatus);
        respVO.setDeliveryStatusName(deliveryStatusName);
        respVO.setPlannedMemberCount(plannedCount);
        respVO.setActiveOnsiteMemberCount(actualCount);
        respVO.setMemberCount(actualCount); // 兼容旧前端字段，但只表示实际驻场人数
        respVO.setStaffingGap(Math.max(0, plannedCount - actualCount));
    }

    private boolean isOnsiteItem(ServiceItemDO item) {
        return Integer.valueOf(ServiceItemDO.SERVICE_MODE_ONSITE).equals(item.getServiceMode())
                || Integer.valueOf(ServiceItemDO.SERVICE_MEMBER_TYPE_ONSITE).equals(item.getServiceMemberType());
    }

    private SiteDates resolveSiteDates(ProjectDO project, LocalDate manualStart, LocalDate manualEnd) {
        if (project.getContractId() != null) {
            Map<String, LocalDateTime> contractTime = contractTimeMapper.selectContractTime(project.getContractId());
            if (contractTime != null) {
                LocalDateTime start = contractTime.get("startTime");
                LocalDateTime end = contractTime.get("endTime");
                if (start != null || end != null) {
                    return new SiteDates(start == null ? null : start.toLocalDate(),
                            end == null ? null : end.toLocalDate(),
                            "signed_contract", "合同日期");
                }
            }
        }
        if (project.getBusinessId() != null) {
            Map<String, Object> earlyInvestmentTime =
                    businessTimeMapper.selectEarlyInvestmentTime(project.getBusinessId());
            LocalDate earlyStart = toLocalDate(earlyInvestmentTime == null
                    ? null : earlyInvestmentTime.get("startDate"));
            LocalDate earlyEnd = toLocalDate(earlyInvestmentTime == null
                    ? null : earlyInvestmentTime.get("endDate"));
            if (earlyInvestmentTime != null
                    && (earlyStart != null || earlyEnd != null)) {
                return new SiteDates(earlyStart, earlyEnd,
                        "approved_early_investment", "提前投入计划");
            }
        }
        return new SiteDates(manualStart, manualEnd, "manual", "手工录入");
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return null;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw exception(PROJECT_SITE_DATE_INVALID);
        }
    }

    private record SiteDates(LocalDate startDate, LocalDate endDate,
                             String source, String sourceName) {
    }

    /**
     * 校验驻场点是否存在
     */
    private ProjectSiteDO validateSiteExists(Long id) {
        if (id == null) {
            throw exception(PROJECT_SITE_NOT_EXISTS);
        }
        ProjectSiteDO site = siteMapper.selectById(id);
        if (site == null) {
            throw exception(PROJECT_SITE_NOT_EXISTS);
        }
        return site;
    }

}
