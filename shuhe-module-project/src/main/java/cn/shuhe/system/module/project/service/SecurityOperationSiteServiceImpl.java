package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.SecurityOperationMemberRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.SecurityOperationSiteRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.SecurityOperationSiteSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.SecurityOperationMemberDO;
import cn.shuhe.system.module.project.dal.dataobject.SecurityOperationSiteDO;
import cn.shuhe.system.module.project.dal.mysql.ContractTimeMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.SecurityOperationMemberMapper;
import cn.shuhe.system.module.project.dal.mysql.SecurityOperationSiteMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 安全运营驻场点 Service 实现类
 */
@Service
@Validated
@Slf4j
public class SecurityOperationSiteServiceImpl implements SecurityOperationSiteService {

    @Resource
    private SecurityOperationSiteMapper siteMapper;

    @Resource
    private SecurityOperationMemberMapper memberMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ContractTimeMapper contractTimeMapper;

    @Override
    public Long createSite(SecurityOperationSiteSaveReqVO createReqVO) {
        // 校验项目是否存在，并获取项目信息
        ProjectDO project = projectMapper.selectById(createReqVO.getProjectId());
        if (project == null) {
            throw exception(PROJECT_NOT_EXISTS);
        }

        // 创建驻场点
        SecurityOperationSiteDO site = BeanUtils.toBean(createReqVO, SecurityOperationSiteDO.class);
        site.setStatus(1); // 默认启用
        site.setSort(0);
        
        // 自动从 CRM 合同获取时间（保持一致性）
        if (project.getContractId() != null) {
            Map<String, LocalDateTime> contractTime = contractTimeMapper.selectContractTime(project.getContractId());
            if (contractTime != null) {
                LocalDateTime startTime = contractTime.get("startTime");
                LocalDateTime endTime = contractTime.get("endTime");
                // LocalDateTime 转 LocalDate
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
    public void updateSite(SecurityOperationSiteSaveReqVO updateReqVO) {
        // 校验存在
        validateSiteExists(updateReqVO.getId());

        // 更新（注意：时间字段由 CRM 合同决定，不允许手动修改）
        SecurityOperationSiteDO updateObj = BeanUtils.toBean(updateReqVO, SecurityOperationSiteDO.class);
        // 清除时间字段，保持与 CRM 合同一致
        updateObj.setStartDate(null);
        updateObj.setEndDate(null);
        siteMapper.updateById(updateObj);
    }

    @Override
    public void deleteSite(Long id) {
        // 校验存在
        validateSiteExists(id);

        // 删除驻场点
        siteMapper.deleteById(id);
    }

    @Override
    public SecurityOperationSiteDO getSite(Long id) {
        return siteMapper.selectById(id);
    }

    @Override
    public SecurityOperationSiteRespVO getSiteDetail(Long id) {
        SecurityOperationSiteDO site = siteMapper.selectById(id);
        if (site == null) {
            return null;
        }
        return convertToRespVO(site);
    }

    @Override
    public List<SecurityOperationSiteDO> getListByProjectId(Long projectId) {
        return siteMapper.selectListByProjectId(projectId);
    }

    @Override
    public List<SecurityOperationSiteRespVO> getSiteDetailListByProjectId(Long projectId) {
        List<SecurityOperationSiteDO> sites = siteMapper.selectEnabledListByProjectId(projectId);
        if (CollUtil.isEmpty(sites)) {
            return Collections.emptyList();
        }

        List<SecurityOperationSiteRespVO> result = new ArrayList<>(sites.size());
        for (SecurityOperationSiteDO site : sites) {
            result.add(convertToRespVO(site));
        }
        return result;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        // 校验存在
        validateSiteExists(id);

        // 更新状态
        SecurityOperationSiteDO updateObj = new SecurityOperationSiteDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        siteMapper.updateById(updateObj);
    }

    /**
     * 将 DO 转换为 RespVO（包含人员列表）
     */
    private SecurityOperationSiteRespVO convertToRespVO(SecurityOperationSiteDO site) {
        SecurityOperationSiteRespVO respVO = BeanUtils.toBean(site, SecurityOperationSiteRespVO.class);

        // 查询该驻场点的人员列表
        List<SecurityOperationMemberDO> members = memberMapper.selectListBySiteId(site.getId());
        if (CollUtil.isNotEmpty(members)) {
            respVO.setMembers(BeanUtils.toBean(members, SecurityOperationMemberRespVO.class));
            // 统计在岗人数
            long activeCount = members.stream()
                    .filter(m -> m.getStatus() != null && m.getStatus() == SecurityOperationMemberDO.STATUS_ACTIVE)
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
            throw exception(SECURITY_OPERATION_SITE_NOT_EXISTS);
        }
        SecurityOperationSiteDO site = siteMapper.selectById(id);
        if (site == null) {
            throw exception(SECURITY_OPERATION_SITE_NOT_EXISTS);
        }
    }

    /**
     * 校验项目是否存在
     */
    private void validateProjectExists(Long projectId) {
        if (projectId == null) {
            throw exception(PROJECT_NOT_EXISTS);
        }
        ProjectDO project = projectMapper.selectById(projectId);
        if (project == null) {
            throw exception(PROJECT_NOT_EXISTS);
        }
    }

}
