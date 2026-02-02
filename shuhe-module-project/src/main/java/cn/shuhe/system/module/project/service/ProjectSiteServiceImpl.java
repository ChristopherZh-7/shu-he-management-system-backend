package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteMemberRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteMemberDO;
import cn.shuhe.system.module.project.dal.mysql.ContractTimeMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectSiteMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectSiteMemberMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

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

        // 自动从 CRM 合同获取时间（保持一致性）
        if (project.getContractId() != null) {
            Map<String, LocalDateTime> contractTime = contractTimeMapper.selectContractTime(project.getContractId());
            if (contractTime != null) {
                LocalDateTime startTime = contractTime.get("startTime");
                LocalDateTime endTime = contractTime.get("endTime");
                if (startTime != null) {
                    site.setStartDate(startTime.toLocalDate());
                }
                if (endTime != null) {
                    site.setEndDate(endTime.toLocalDate());
                }
                log.info("[createSite][从CRM合同自动获取时间，contractId={}，startDate={}，endDate={}]",
                        project.getContractId(), site.getStartDate(), site.getEndDate());
            }
        }

        siteMapper.insert(site);

        log.info("[createSite][创建驻场点成功，id={}，projectId={}，name={}]",
                site.getId(), site.getProjectId(), site.getName());
        return site.getId();
    }

    @Override
    public void updateSite(ProjectSiteSaveReqVO updateReqVO) {
        // 校验存在
        validateSiteExists(updateReqVO.getId());

        // 更新（注意：时间字段由 CRM 合同决定，不允许手动修改）
        ProjectSiteDO updateObj = BeanUtils.toBean(updateReqVO, ProjectSiteDO.class);
        // 清除时间字段，保持与 CRM 合同一致
        updateObj.setStartDate(null);
        updateObj.setEndDate(null);
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
    public List<ProjectSiteDO> getListByProjectId(Long projectId) {
        return siteMapper.selectListByProjectId(projectId);
    }

    @Override
    public List<ProjectSiteRespVO> getSiteDetailListByProjectId(Long projectId) {
        List<ProjectSiteDO> sites = siteMapper.selectEnabledListByProjectId(projectId);
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
    public boolean hasSite(Long projectId) {
        Long count = siteMapper.selectCountByProjectId(projectId);
        return count != null && count > 0;
    }

    /**
     * 将 DO 转换为 RespVO（包含人员列表）
     */
    private ProjectSiteRespVO convertToRespVO(ProjectSiteDO site) {
        ProjectSiteRespVO respVO = BeanUtils.toBean(site, ProjectSiteRespVO.class);

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
            // 统计在岗人数
            long activeCount = members.stream()
                    .filter(m -> m.getStatus() != null && m.getStatus() == ProjectSiteMemberDO.STATUS_ACTIVE)
                    .count();
            respVO.setMemberCount((int) activeCount);
        } else {
            respVO.setMembers(Collections.emptyList());
            respVO.setMemberCount(0);
        }

        return respVO;
    }

    /**
     * 校验驻场点是否存在
     */
    private void validateSiteExists(Long id) {
        if (id == null) {
            throw exception(PROJECT_SITE_NOT_EXISTS);
        }
        ProjectSiteDO site = siteMapper.selectById(id);
        if (site == null) {
            throw exception(PROJECT_SITE_NOT_EXISTS);
        }
    }

}
