package cn.shuhe.system.module.project.service;

import cn.shuhe.system.module.project.dal.dataobject.OutsideMemberDO;
import cn.shuhe.system.module.project.dal.dataobject.OutsideRequestDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.mysql.OutsideMemberMapper;
import cn.shuhe.system.module.project.dal.mysql.OutsideRequestMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.system.service.dingtalk.OutsideConfirmCallback;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 外出确认回调实现类
 * 
 * 实现 OutsideConfirmCallback 接口，供 system 模块跨模块调用
 */
@Service
@Slf4j
public class OutsideConfirmCallbackImpl implements OutsideConfirmCallback {

    @Resource
    private OutsideMemberMapper outsideMemberMapper;

    @Resource
    private OutsideRequestMapper outsideRequestMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Override
    public OutsideMemberInfo getOutsideMemberInfo(Long memberId) {
        if (memberId == null) {
            return null;
        }

        OutsideMemberDO member = outsideMemberMapper.selectById(memberId);
        if (member == null) {
            return null;
        }

        OutsideMemberInfo info = new OutsideMemberInfo();
        info.setId(member.getId());
        info.setRequestId(member.getRequestId());
        info.setUserId(member.getUserId());
        info.setUserName(member.getUserName());
        info.setConfirmStatus(member.getConfirmStatus());
        info.setOaProcessInstanceId(member.getOaProcessInstanceId());
        return info;
    }

    @Override
    public OutsideRequestInfo getOutsideRequestInfo(Long requestId) {
        if (requestId == null) {
            return null;
        }

        OutsideRequestDO request = outsideRequestMapper.selectById(requestId);
        if (request == null) {
            return null;
        }

        OutsideRequestInfo info = new OutsideRequestInfo();
        info.setId(request.getId());
        info.setProjectId(request.getProjectId());
        info.setDestination(request.getDestination());
        info.setReason(request.getReason());
        info.setPlanStartTime(request.getPlanStartTime());
        info.setPlanEndTime(request.getPlanEndTime());

        // 获取项目名称
        if (request.getProjectId() != null) {
            ProjectDO project = projectMapper.selectById(request.getProjectId());
            if (project != null) {
                info.setProjectName(project.getName());
            }
        }

        return info;
    }

    @Override
    public void updateConfirmStatus(Long memberId, Integer confirmStatus, String oaProcessInstanceId) {
        if (memberId == null) {
            return;
        }

        OutsideMemberDO updateObj = new OutsideMemberDO();
        updateObj.setId(memberId);
        updateObj.setConfirmStatus(confirmStatus);
        updateObj.setOaProcessInstanceId(oaProcessInstanceId);
        updateObj.setConfirmTime(LocalDateTime.now());

        outsideMemberMapper.updateById(updateObj);
        log.info("【外出确认】更新确认状态，memberId={}, confirmStatus={}, oaProcessInstanceId={}",
                memberId, confirmStatus, oaProcessInstanceId);
    }

}
