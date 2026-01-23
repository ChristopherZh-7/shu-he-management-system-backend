package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideMemberRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideRequestPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideRequestRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideRequestSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.OutsideMemberDO;
import cn.shuhe.system.module.project.dal.dataobject.OutsideRequestDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.OutsideMemberMapper;
import cn.shuhe.system.module.project.dal.mysql.OutsideRequestMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 外出请求 Service 实现类
 */
@Service
@Validated
@Slf4j
public class OutsideRequestServiceImpl implements OutsideRequestService {

    @Resource
    private OutsideRequestMapper outsideRequestMapper;

    @Resource
    private OutsideMemberMapper outsideMemberMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOutsideRequest(OutsideRequestSaveReqVO createReqVO) {
        // 校验项目存在
        ProjectDO project = projectMapper.selectById(createReqVO.getProjectId());
        if (project == null) {
            throw exception(PROJECT_NOT_EXISTS);
        }

        // 校验服务项存在（如果指定了）
        if (createReqVO.getServiceItemId() != null) {
            ServiceItemDO serviceItem = serviceItemMapper.selectById(createReqVO.getServiceItemId());
            if (serviceItem == null) {
                throw exception(SERVICE_ITEM_NOT_EXISTS);
            }
        }

        // 校验目标部门存在
        DeptRespDTO targetDept = deptApi.getDept(createReqVO.getTargetDeptId());
        if (targetDept == null) {
            throw exception(OUTSIDE_REQUEST_TARGET_DEPT_NOT_EXISTS);
        }

        // 创建外出请求
        OutsideRequestDO request = BeanUtils.toBean(createReqVO, OutsideRequestDO.class);
        request.setStatus(0); // 待审批

        // 设置发起人信息
        Long currentUserId = getLoginUserId();
        request.setRequestUserId(currentUserId);
        AdminUserRespDTO currentUser = adminUserApi.getUser(currentUserId);
        if (currentUser != null && currentUser.getDeptId() != null) {
            request.setRequestDeptId(currentUser.getDeptId());
        }

        outsideRequestMapper.insert(request);

        log.info("【外出请求】创建外出请求成功，id={}, projectId={}, targetDeptId={}, requestUserId={}",
                request.getId(), request.getProjectId(), request.getTargetDeptId(), currentUserId);

        return request.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOutsideRequest(OutsideRequestSaveReqVO updateReqVO) {
        // 校验存在
        OutsideRequestDO existingRequest = outsideRequestMapper.selectById(updateReqVO.getId());
        if (existingRequest == null) {
            throw exception(OUTSIDE_REQUEST_NOT_EXISTS);
        }

        // 校验状态（待审批状态才能修改）
        if (existingRequest.getStatus() != 0) {
            throw exception(OUTSIDE_REQUEST_CANNOT_UPDATE);
        }

        // 更新
        OutsideRequestDO updateObj = BeanUtils.toBean(updateReqVO, OutsideRequestDO.class);
        outsideRequestMapper.updateById(updateObj);

        log.info("【外出请求】更新外出请求成功，id={}", updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOutsideRequest(Long id) {
        // 校验存在
        OutsideRequestDO existingRequest = outsideRequestMapper.selectById(id);
        if (existingRequest == null) {
            throw exception(OUTSIDE_REQUEST_NOT_EXISTS);
        }

        // 校验状态（待审批状态才能删除）
        if (existingRequest.getStatus() != 0) {
            throw exception(OUTSIDE_REQUEST_CANNOT_DELETE);
        }

        // 删除外出人员
        outsideMemberMapper.deleteByRequestId(id);

        // 删除外出请求
        outsideRequestMapper.deleteById(id);

        log.info("【外出请求】删除外出请求成功，id={}", id);
    }

    @Override
    public OutsideRequestDO getOutsideRequest(Long id) {
        return outsideRequestMapper.selectById(id);
    }

    @Override
    public OutsideRequestRespVO getOutsideRequestDetail(Long id) {
        OutsideRequestDO request = outsideRequestMapper.selectById(id);
        if (request == null) {
            return null;
        }

        return convertToRespVO(request);
    }

    @Override
    public PageResult<OutsideRequestRespVO> getOutsideRequestPage(OutsideRequestPageReqVO pageReqVO) {
        PageResult<OutsideRequestDO> pageResult = outsideRequestMapper.selectPage(pageReqVO);

        // 转换为响应 VO
        List<OutsideRequestRespVO> respVOList = new ArrayList<>();
        for (OutsideRequestDO request : pageResult.getList()) {
            respVOList.add(convertToRespVO(request));
        }

        return new PageResult<>(respVOList, pageResult.getTotal());
    }

    /**
     * 转换为响应 VO，填充关联信息
     */
    private OutsideRequestRespVO convertToRespVO(OutsideRequestDO request) {
        OutsideRequestRespVO respVO = BeanUtils.toBean(request, OutsideRequestRespVO.class);

        // 填充项目名称
        if (request.getProjectId() != null) {
            ProjectDO project = projectMapper.selectById(request.getProjectId());
            if (project != null) {
                respVO.setProjectName(project.getName());
            }
        }

        // 填充服务项信息（名称、服务类型、部门类型）
        if (request.getServiceItemId() != null) {
            ServiceItemDO serviceItem = serviceItemMapper.selectById(request.getServiceItemId());
            if (serviceItem != null) {
                respVO.setServiceItemName(serviceItem.getName());
                respVO.setServiceType(serviceItem.getServiceType());
                respVO.setDeptType(serviceItem.getDeptType());
            }
        }

        // 填充发起人信息
        if (request.getRequestUserId() != null) {
            AdminUserRespDTO user = adminUserApi.getUser(request.getRequestUserId());
            if (user != null) {
                respVO.setRequestUserName(user.getNickname());
            }
        }

        // 填充发起人部门信息
        if (request.getRequestDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(request.getRequestDeptId());
            if (dept != null) {
                respVO.setRequestDeptName(dept.getName());
            }
        }

        // 填充目标部门信息
        if (request.getTargetDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(request.getTargetDeptId());
            if (dept != null) {
                respVO.setTargetDeptName(dept.getName());
            }
        }

        // 填充外出人员列表
        List<OutsideMemberDO> members = outsideMemberMapper.selectListByRequestId(request.getId());
        if (CollUtil.isNotEmpty(members)) {
            respVO.setMembers(BeanUtils.toBean(members, OutsideMemberRespVO.class));
        }

        return respVO;
    }

    @Override
    public List<OutsideRequestDO> getOutsideRequestListByProjectId(Long projectId) {
        return outsideRequestMapper.selectListByProjectId(projectId);
    }

    @Override
    public List<OutsideRequestDO> getOutsideRequestListByServiceItemId(Long serviceItemId) {
        return outsideRequestMapper.selectListByServiceItemId(serviceItemId);
    }

    @Override
    public List<OutsideRequestRespVO> getOutsideRequestListByServiceItemIdWithDetail(Long serviceItemId) {
        List<OutsideRequestDO> list = outsideRequestMapper.selectListByServiceItemId(serviceItemId);
        List<OutsideRequestRespVO> respVOList = new ArrayList<>();
        for (OutsideRequestDO request : list) {
            respVOList.add(convertToRespVO(request));
        }
        return respVOList;
    }

    @Override
    public void updateOutsideRequestStatus(Long id, Integer status) {
        // 校验存在
        OutsideRequestDO existingRequest = outsideRequestMapper.selectById(id);
        if (existingRequest == null) {
            throw exception(OUTSIDE_REQUEST_NOT_EXISTS);
        }

        // 更新状态
        OutsideRequestDO updateObj = new OutsideRequestDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        outsideRequestMapper.updateById(updateObj);

        log.info("【外出请求】更新外出请求状态，id={}, status={}", id, status);
    }

    @Override
    public OutsideRequestDO getOutsideRequestByProcessInstanceId(String processInstanceId) {
        return outsideRequestMapper.selectByProcessInstanceId(processInstanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setOutsideMembers(Long requestId, List<Long> memberUserIds) {
        // 校验外出请求存在
        OutsideRequestDO request = outsideRequestMapper.selectById(requestId);
        if (request == null) {
            throw exception(OUTSIDE_REQUEST_NOT_EXISTS);
        }

        // 删除原有人员
        outsideMemberMapper.deleteByRequestId(requestId);

        // 添加新人员
        if (CollUtil.isNotEmpty(memberUserIds)) {
            // 批量获取用户信息
            List<AdminUserRespDTO> users = adminUserApi.getUserList(memberUserIds);
            Map<Long, AdminUserRespDTO> userMap = users.stream()
                    .collect(Collectors.toMap(AdminUserRespDTO::getId, u -> u));

            // 批量获取部门信息
            List<Long> deptIds = users.stream()
                    .map(AdminUserRespDTO::getDeptId)
                    .filter(deptId -> deptId != null)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(deptIds);

            // 创建人员记录
            for (Long userId : memberUserIds) {
                AdminUserRespDTO user = userMap.get(userId);
                if (user == null) {
                    log.warn("【外出请求】用户不存在，跳过。userId={}", userId);
                    continue;
                }

                OutsideMemberDO member = new OutsideMemberDO();
                member.setRequestId(requestId);
                member.setUserId(userId);
                member.setUserName(user.getNickname());
                member.setUserDeptId(user.getDeptId());

                if (user.getDeptId() != null && deptMap.containsKey(user.getDeptId())) {
                    member.setUserDeptName(deptMap.get(user.getDeptId()).getName());
                }

                outsideMemberMapper.insert(member);
            }

            log.info("【外出请求】设置外出人员成功，requestId={}, memberCount={}", requestId, memberUserIds.size());
        }
    }

    @Override
    public List<OutsideMemberDO> getOutsideMembers(Long requestId) {
        return outsideMemberMapper.selectListByRequestId(requestId);
    }

    @Override
    public Long getOutsideCountByServiceItemId(Long serviceItemId) {
        return outsideRequestMapper.selectCountByServiceItemId(serviceItemId);
    }

}
