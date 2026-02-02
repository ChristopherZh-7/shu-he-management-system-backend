package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteMemberRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteMemberSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteMemberDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectSiteMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectSiteMemberMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 项目驻场人员 Service 实现类
 */
@Service
@Validated
@Slf4j
public class ProjectSiteMemberServiceImpl implements ProjectSiteMemberService {

    @Resource
    private ProjectSiteMemberMapper memberMapper;

    @Resource
    private ProjectSiteMapper siteMapper;

    @Override
    public Long createMember(ProjectSiteMemberSaveReqVO createReqVO) {
        // 校验驻场点是否存在
        ProjectSiteDO site = siteMapper.selectById(createReqVO.getSiteId());
        if (site == null) {
            throw exception(PROJECT_SITE_NOT_EXISTS);
        }

        // 创建时必须有用户ID
        if (createReqVO.getUserId() == null) {
            throw exception(PROJECT_SITE_MEMBER_USER_REQUIRED);
        }

        ProjectSiteMemberDO member = BeanUtils.toBean(createReqVO, ProjectSiteMemberDO.class);
        // 自动填充项目ID和部门类型（冗余字段）
        member.setProjectId(site.getProjectId());
        member.setDeptType(site.getDeptType());
        // 默认状态：在岗
        if (member.getStatus() == null) {
            member.setStatus(ProjectSiteMemberDO.STATUS_ACTIVE);
        }
        // 默认人员类型：驻场人员
        if (member.getMemberType() == null) {
            member.setMemberType(ProjectSiteMemberDO.MEMBER_TYPE_ONSITE);
        }
        // 默认非负责人
        if (member.getIsLeader() == null) {
            member.setIsLeader(ProjectSiteMemberDO.IS_LEADER_NO);
        }

        memberMapper.insert(member);

        log.info("[createMember][创建驻场人员成功，id={}，siteId={}，userId={}，userName={}]",
                member.getId(), member.getSiteId(), member.getUserId(), member.getUserName());
        return member.getId();
    }

    @Override
    public void updateMember(ProjectSiteMemberSaveReqVO updateReqVO) {
        // 校验存在
        if (updateReqVO.getId() == null) {
            throw exception(PROJECT_SITE_MEMBER_NOT_EXISTS);
        }
        ProjectSiteMemberDO existMember = memberMapper.selectById(updateReqVO.getId());
        if (existMember == null) {
            throw exception(PROJECT_SITE_MEMBER_NOT_EXISTS);
        }

        // 转换并更新
        ProjectSiteMemberDO member = BeanUtils.toBean(updateReqVO, ProjectSiteMemberDO.class);
        memberMapper.updateById(member);

        log.info("[updateMember][更新驻场人员成功，id={}]", updateReqVO.getId());
    }

    @Override
    public void deleteMember(Long id) {
        // 校验存在
        validateMemberExists(id);

        memberMapper.deleteById(id);
        log.info("[deleteMember][删除驻场人员成功，id={}]", id);
    }

    @Override
    public ProjectSiteMemberDO getMember(Long id) {
        return memberMapper.selectById(id);
    }

    @Override
    public ProjectSiteMemberRespVO getMemberDetail(Long id) {
        ProjectSiteMemberDO member = memberMapper.selectById(id);
        if (member == null) {
            return null;
        }
        return convertToRespVO(member);
    }

    @Override
    public List<ProjectSiteMemberRespVO> getListBySiteId(Long siteId) {
        List<ProjectSiteMemberDO> members = memberMapper.selectListBySiteId(siteId);
        return convertToRespVOList(members);
    }

    @Override
    public List<ProjectSiteMemberRespVO> getListByProjectId(Long projectId) {
        List<ProjectSiteMemberDO> members = memberMapper.selectListByProjectId(projectId);
        return convertToRespVOList(members);
    }

    @Override
    public void setMemberLeft(Long id) {
        // 校验存在
        validateMemberExists(id);

        ProjectSiteMemberDO updateObj = new ProjectSiteMemberDO();
        updateObj.setId(id);
        updateObj.setStatus(ProjectSiteMemberDO.STATUS_LEFT);
        updateObj.setEndDate(LocalDate.now());
        memberMapper.updateById(updateObj);

        log.info("[setMemberLeft][标记人员已离开，id={}]", id);
    }

    /**
     * 转换为 RespVO
     */
    private ProjectSiteMemberRespVO convertToRespVO(ProjectSiteMemberDO member) {
        ProjectSiteMemberRespVO respVO = BeanUtils.toBean(member, ProjectSiteMemberRespVO.class);
        // 设置人员类型名称
        if (member.getMemberType() != null) {
            respVO.setMemberTypeName(member.getMemberType() == ProjectSiteMemberDO.MEMBER_TYPE_MANAGEMENT ? "管理人员" : "驻场人员");
        }
        // 设置状态名称
        if (member.getStatus() != null) {
            switch (member.getStatus()) {
                case ProjectSiteMemberDO.STATUS_PENDING:
                    respVO.setStatusName("待入场");
                    break;
                case ProjectSiteMemberDO.STATUS_ACTIVE:
                    respVO.setStatusName("在岗");
                    break;
                case ProjectSiteMemberDO.STATUS_LEFT:
                    respVO.setStatusName("已离开");
                    break;
                default:
                    respVO.setStatusName("未知");
            }
        }
        return respVO;
    }

    /**
     * 批量转换为 RespVO
     */
    private List<ProjectSiteMemberRespVO> convertToRespVOList(List<ProjectSiteMemberDO> members) {
        if (CollUtil.isEmpty(members)) {
            return Collections.emptyList();
        }
        List<ProjectSiteMemberRespVO> result = new ArrayList<>(members.size());
        for (ProjectSiteMemberDO member : members) {
            result.add(convertToRespVO(member));
        }
        return result;
    }

    /**
     * 校验人员是否存在
     */
    private void validateMemberExists(Long id) {
        if (id == null) {
            throw exception(PROJECT_SITE_MEMBER_NOT_EXISTS);
        }
        ProjectSiteMemberDO member = memberMapper.selectById(id);
        if (member == null) {
            throw exception(PROJECT_SITE_MEMBER_NOT_EXISTS);
        }
    }

}
