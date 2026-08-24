package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectWorkRecordPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectWorkRecordSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectWorkRecordDO;
import cn.shuhe.system.module.project.dal.dataobject.SecurityOperationContractDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMemberMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectWorkRecordMapper;
import cn.shuhe.system.module.project.dal.mysql.SecurityOperationContractMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketMapper;
import cn.shuhe.system.module.ticket.dal.mysql.TicketExecutorMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 项目工作记录 Service 实现
 */
@Service
@Validated
@Slf4j
public class ProjectWorkRecordServiceImpl implements ProjectWorkRecordService {

    @Resource
    private ProjectWorkRecordMapper workRecordMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private ProjectRoundMapper projectRoundMapper;

    @Resource
    private ProjectRoundMemberMapper projectRoundMemberMapper;

    @Resource
    private TicketMapper ticketMapper;

    @Resource
    private TicketExecutorMapper ticketExecutorMapper;

    @Resource
    @org.springframework.context.annotation.Lazy
    private ServiceItemService serviceItemService;

    @Resource
    private SecurityOperationContractMapper securityOperationContractMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Override
    public Long createWorkRecord(ProjectWorkRecordSaveReqVO createReqVO) {
        // 1. 获取当前用户信息
        Long userId = getLoginUserId();
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        
        // 2. 构建实体
        ProjectWorkRecordDO record = BeanUtils.toBean(createReqVO, ProjectWorkRecordDO.class);
        
        // 3. 设置记录人信息（creator 由 MyBatis Plus 自动填充）
        if (user != null) {
            record.setCreatorName(user.getNickname());
            record.setDeptId(user.getDeptId());
            // 获取部门名称
            if (user.getDeptId() != null) {
                DeptRespDTO dept = deptApi.getDept(user.getDeptId());
                if (dept != null) {
                    record.setDeptName(dept.getName());
                }
            }
        }
        
        // 4. 填充项目名称（如果没传）
        if (record.getProjectName() == null && record.getProjectId() != null) {
            fillProjectName(record);
        }
        
        // 5. 填充服务项的服务类型（冗余存储，便于按类型统计；服务项名称字段已废弃）
        if (record.getServiceItemId() != null) {
            ServiceItemDO serviceItem = serviceItemMapper.selectById(record.getServiceItemId());
            if (serviceItem != null) {
                record.setServiceType(serviceItem.getServiceType());
            }
        }
        
        // 6. 来源快照与核验状态只能由后端生成，不能信任客户端
        fillSourceMetadata(record);

        // 7. 处理附件（JSON存储）
        if (createReqVO.getAttachments() != null && !createReqVO.getAttachments().isEmpty()) {
            record.setAttachments(JSONUtil.toJsonStr(createReqVO.getAttachments()));
        }
        
        // 8. 插入数据库
        workRecordMapper.insert(record);
        
        return record.getId();
    }

    /**
     * 填充项目名称
     */
    private void fillProjectName(ProjectWorkRecordDO record) {
        if (record.getProjectType() == null) {
            return;
        }
        
        // 现行服务事项统一关联 project 表，优先从统一项目取名称。
        ProjectDO project = projectMapper.selectById(record.getProjectId());
        if (project != null) {
            record.setProjectName(project.getName());
            return;
        }
        // 兼容历史安全运营记录仍使用合同主键的情况。
        if (record.getProjectType() == 2) {
            SecurityOperationContractDO contract = securityOperationContractMapper.selectById(record.getProjectId());
            if (contract != null) {
                record.setProjectName(contract.getName());
            }
        }
    }

    @Override
    public void updateWorkRecord(ProjectWorkRecordSaveReqVO updateReqVO) {
        // 1. 校验存在
        ProjectWorkRecordDO existRecord = validateWorkRecordExists(updateReqVO.getId());
        
        // 2. 校验是否为本人创建（只能修改自己的记录）
        Long userId = getLoginUserId();
        if (!String.valueOf(userId).equals(existRecord.getCreator())) {
            throw exception(WORK_RECORD_UPDATE_NOT_OWNER);
        }
        
        // 3. 更新
        ProjectWorkRecordDO updateRecord = BeanUtils.toBean(updateReqVO, ProjectWorkRecordDO.class);
        
        // 处理附件
        if (updateReqVO.getAttachments() != null) {
            updateRecord.setAttachments(JSONUtil.toJsonStr(updateReqVO.getAttachments()));
        }
        
        // 填充项目名称
        if (updateRecord.getProjectName() == null && updateRecord.getProjectId() != null) {
            fillProjectName(updateRecord);
        }
        
        // 填充服务项的服务类型（冗余存储；服务项名称字段已废弃）
        if (updateRecord.getServiceItemId() != null) {
            ServiceItemDO serviceItem = serviceItemMapper.selectById(updateRecord.getServiceItemId());
            if (serviceItem != null) {
                updateRecord.setServiceType(serviceItem.getServiceType());
            }
        }

        fillSourceMetadata(updateRecord);
        
        workRecordMapper.updateById(updateRecord);
    }

    private void fillSourceMetadata(ProjectWorkRecordDO record) {
        String sourceType = record.getSourceType();
        if (sourceType == null || sourceType.isBlank()) {
            sourceType = ProjectWorkRecordDO.SOURCE_MANUAL;
        }
        record.setSourceType(sourceType);
        record.setSourceName(null);
        record.setVerificationStatus(ProjectWorkRecordDO.VERIFICATION_SELF_REPORTED);

        if (ProjectWorkRecordDO.SOURCE_INTERNAL.equals(sourceType)) {
            record.setProjectId(null);
            record.setProjectType(null);
            record.setProjectName(null);
            record.setServiceItemId(null);
            record.setServiceType(null);
            record.setSourceId(null);
            record.setSourceName("内部工作");
        } else if (ProjectWorkRecordDO.SOURCE_SERVICE_ITEM.equals(sourceType)
                && record.getServiceItemId() != null) {
            ServiceItemDO serviceItem = serviceItemMapper.selectById(record.getServiceItemId());
            if (serviceItem != null) {
                record.setSourceId(serviceItem.getId());
                record.setSourceName(serviceItemService.resolveServiceTypeLabel(
                        serviceItem.getDeptType(), serviceItem.getServiceType()));
                record.setVerificationStatus(ProjectWorkRecordDO.VERIFICATION_LINKED);
            }
        } else if (ProjectWorkRecordDO.SOURCE_ROUND.equals(sourceType) && record.getSourceId() != null) {
            ProjectRoundDO round = projectRoundMapper.selectById(record.getSourceId());
            if (round != null) {
                validateRoundWorkRecordMembership(round.getId());
                record.setSourceName(round.getName());
                record.setVerificationStatus(ProjectWorkRecordDO.VERIFICATION_LINKED);
            }
        } else if (ProjectWorkRecordDO.SOURCE_TICKET.equals(sourceType) && record.getSourceId() != null) {
            TicketDO ticket = ticketMapper.selectById(record.getSourceId());
            if (ticket != null) {
                validateTicketWorkRecordMembership(ticket);
                String ticketName = ticket.getTicketNo();
                if (ticket.getTitle() != null && !ticket.getTitle().isBlank()) {
                    ticketName = ticketName + " " + ticket.getTitle();
                }
                record.setSourceName(ticketName);
                record.setVerificationStatus(ProjectWorkRecordDO.VERIFICATION_LINKED);
            }
        } else {
            record.setSourceId(null);
        }
        if (record.getCompletionPercent() == null) {
            record.setCompletionPercent(100);
        }
    }

    private void validateRoundWorkRecordMembership(Long roundId) {
        Long userId = getLoginUserId();
        if (!projectRoundMemberMapper.existsByRoundIdAndUserId(roundId, userId)) {
            throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.shuhe.system.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN);
        }
    }

    private void validateTicketWorkRecordMembership(TicketDO ticket) {
        Long userId = getLoginUserId();
        boolean related = Objects.equals(ticket.getCreatorId(), userId)
                || Objects.equals(ticket.getProjectManagerId(), userId)
                || Objects.equals(ticket.getAssigneeId(), userId)
                || Objects.equals(ticket.getPrimaryExecutorId(), userId)
                || Objects.equals(ticket.getTechReviewerId(), userId)
                || ticketExecutorMapper.existsByTicketIdAndUserId(ticket.getId(), userId);
        if (!related) {
            throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.shuhe.system.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN);
        }
    }

    @Override
    public void deleteWorkRecord(Long id) {
        // 1. 校验存在
        ProjectWorkRecordDO existRecord = validateWorkRecordExists(id);
        
        // 2. 校验是否为本人创建（只能删除自己的记录）
        Long userId = getLoginUserId();
        if (!String.valueOf(userId).equals(existRecord.getCreator())) {
            throw exception(WORK_RECORD_DELETE_NOT_OWNER);
        }
        
        // 3. 删除
        workRecordMapper.deleteById(id);
    }

    @Override
    public ProjectWorkRecordDO getWorkRecord(Long id) {
        return workRecordMapper.selectById(id);
    }

    @Override
    public PageResult<ProjectWorkRecordDO> getWorkRecordPage(ProjectWorkRecordPageReqVO pageReqVO) {
        // 获取部门ID列表（根据权限）
        Collection<Long> deptIds = getDeptIdsForQuery(pageReqVO);
        
        return workRecordMapper.selectPage(pageReqVO, deptIds);
    }

    @Override
    public List<ProjectWorkRecordDO> getWorkRecordListByProjectId(Long projectId) {
        return workRecordMapper.selectListByProjectId(projectId);
    }

    @Override
    public List<ProjectWorkRecordDO> getWorkRecordListByServiceItemId(Long serviceItemId) {
        return workRecordMapper.selectListByServiceItemId(serviceItemId);
    }

    @Override
    public List<ProjectWorkRecordDO> getWorkRecordListForExport(ProjectWorkRecordPageReqVO reqVO) {
        Collection<Long> deptIds = getDeptIdsForQuery(reqVO);
        return workRecordMapper.selectListForExport(reqVO, deptIds);
    }

    @Override
    public List<ProjectWorkRecordDO> getMyProjects() {
        // TODO: 实现获取当前用户可见的项目列表
        // 这个方法需要根据用户权限返回项目列表
        // 暂时返回空列表，由Controller层处理
        return Collections.emptyList();
    }

    /**
     * 校验工作记录是否存在
     */
    private ProjectWorkRecordDO validateWorkRecordExists(Long id) {
        ProjectWorkRecordDO record = workRecordMapper.selectById(id);
        if (record == null) {
            throw exception(WORK_RECORD_NOT_EXISTS);
        }
        return record;
    }

    /**
     * 根据查询条件获取部门ID列表
     * 如果指定了部门且需要包含下属，则返回该部门及所有子部门的ID
     * 如果没有指定部门，则返回当前用户部门及子部门的ID
     */
    private Collection<Long> getDeptIdsForQuery(ProjectWorkRecordPageReqVO pageReqVO) {
        // 如果指定了部门ID
        if (pageReqVO.getDeptId() != null) {
            if (Boolean.TRUE.equals(pageReqVO.getIncludeSubDept())) {
                // 获取该部门及所有子部门
                return getChildDeptIds(pageReqVO.getDeptId());
            }
            return Collections.singleton(pageReqVO.getDeptId());
        }
        
        // 如果没有指定部门，默认获取当前用户部门及子部门
        Long userId = getLoginUserId();
        if (userId == null) {
            return null;
        }
        
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null || user.getDeptId() == null) {
            return null;
        }
        
        // 默认包含子部门
        return getChildDeptIds(user.getDeptId());
    }

    /**
     * 获取部门及所有子部门的ID
     */
    private Collection<Long> getChildDeptIds(Long deptId) {
        Set<Long> deptIds = new HashSet<>();
        deptIds.add(deptId);
        
        // 获取所有子部门
        List<DeptRespDTO> childDepts = deptApi.getChildDeptList(deptId);
        if (CollUtil.isNotEmpty(childDepts)) {
            deptIds.addAll(childDepts.stream()
                    .map(DeptRespDTO::getId)
                    .collect(Collectors.toSet()));
        }
        
        return deptIds;
    }

}
