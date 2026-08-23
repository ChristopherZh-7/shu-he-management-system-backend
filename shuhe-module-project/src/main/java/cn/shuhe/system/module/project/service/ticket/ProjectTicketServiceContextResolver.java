package cn.shuhe.system.module.project.service.ticket;

import cn.shuhe.system.framework.datapermission.core.util.DataPermissionUtils;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptServiceDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectDeptServiceMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.project.service.ServiceItemService;
import cn.shuhe.system.module.project.service.access.ProjectAccessService;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.ticket.service.context.TicketServiceContext;
import cn.shuhe.system.module.ticket.service.context.TicketServiceContextResolver;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_SERVICE_ITEM_NOT_AVAILABLE;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_SERVICE_ITEM_REQUIRED;

/**
 * 把项目服务项解析为工单的权威路由上下文。
 */
@Component
public class ProjectTicketServiceContextResolver implements TicketServiceContextResolver {

    private static final int PROJECT_ACTIVE = 1;
    private static final int SERVICE_ITEM_ACTIVE = 1;
    private static final int VISIBLE = 1;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectDeptServiceMapper projectDeptServiceMapper;

    @Resource
    private ProjectAccessService projectAccessService;

    @Resource
    private ServiceItemService serviceItemService;

    @Resource
    private DeptApi deptApi;

    @Override
    public TicketServiceContext resolve(Long serviceItemId, Long userId) {
        if (serviceItemId == null || userId == null) {
            throw exception(TICKET_SERVICE_ITEM_REQUIRED);
        }
        ServiceItemDO item = DataPermissionUtils.executeIgnore(
                () -> serviceItemMapper.selectById(serviceItemId));
        if (item == null) {
            throw exception(TICKET_SERVICE_ITEM_NOT_AVAILABLE);
        }
        ProjectDO project = DataPermissionUtils.executeIgnore(
                () -> projectMapper.selectById(item.getProjectId()));
        ProjectDeptServiceDO deptService = item.getDeptServiceId() == null ? null
                : DataPermissionUtils.executeIgnore(
                        () -> projectDeptServiceMapper.selectById(item.getDeptServiceId()));
        validateAvailable(item, project, deptService, userId);
        return buildContext(item, project, deptService);
    }

    @Override
    public List<TicketServiceContext> listEligible(Long userId, Long projectId) {
        if (userId == null) {
            return List.of();
        }
        if (projectId != null && !projectAccessService.canViewProject(projectId, userId)) {
            return List.of();
        }
        List<Long> visibleProjectIds = projectAccessService.getVisibleProjectIds(userId);
        if (projectId != null && visibleProjectIds != null && !visibleProjectIds.contains(projectId)) {
            return List.of();
        }
        List<ServiceItemDO> items = DataPermissionUtils.executeIgnore(
                () -> serviceItemMapper.selectEligibleTicketItems(projectId, visibleProjectIds));
        List<TicketServiceContext> result = new ArrayList<>();
        for (ServiceItemDO item : items) {
            ProjectDO project = DataPermissionUtils.executeIgnore(
                    () -> projectMapper.selectById(item.getProjectId()));
            ProjectDeptServiceDO deptService = item.getDeptServiceId() == null ? null
                    : DataPermissionUtils.executeIgnore(
                            () -> projectDeptServiceMapper.selectById(item.getDeptServiceId()));
            if (!isAvailable(item, project, deptService, userId)) {
                continue;
            }
            result.add(buildContext(item, project, deptService));
        }
        return result;
    }

    private void validateAvailable(ServiceItemDO item, ProjectDO project,
                                   ProjectDeptServiceDO deptService, Long userId) {
        if (!isAvailable(item, project, deptService, userId)) {
            throw exception(TICKET_SERVICE_ITEM_NOT_AVAILABLE);
        }
    }

    private boolean isAvailable(ServiceItemDO item, ProjectDO project,
                                ProjectDeptServiceDO deptService, Long userId) {
        if (item == null || project == null || deptService == null
                || item.getStatus() == null || item.getStatus() != SERVICE_ITEM_ACTIVE
                || item.getVisible() == null || item.getVisible() != VISIBLE
                || project.getStatus() == null || project.getStatus() != PROJECT_ACTIVE
                || !projectAccessService.canViewProject(project.getId(), userId)) {
            return false;
        }
        Long responsibleDeptId = resolveResponsibleDeptId(item, deptService);
        if (!projectAccessService.canReadDept(project.getId(), userId, responsibleDeptId)) {
            return false;
        }
        return serviceItemService.canStartExecution(item.getId());
    }

    private TicketServiceContext buildContext(ServiceItemDO item, ProjectDO project,
                                              ProjectDeptServiceDO deptService) {
        Long responsibleDeptId = resolveResponsibleDeptId(item, deptService);
        String responsibleDeptName = deptService.getDeptName();
        if (responsibleDeptId != null && !responsibleDeptId.equals(deptService.getDeptId())) {
            DeptRespDTO dept = deptApi.getDept(responsibleDeptId);
            if (dept != null) {
                responsibleDeptName = dept.getName();
            }
        }
        Long contractId = item.getContractId() != null
                ? item.getContractId() : project.getContractId();
        String contractNo = item.getContractNo() != null
                ? item.getContractNo() : project.getContractNo();
        return TicketServiceContext.builder()
                .serviceItemId(item.getId())
                .serviceItemCode(item.getCode())
                .serviceType(item.getServiceType())
                .serviceTypeName(serviceItemService.resolveServiceTypeLabel(
                        item.getDeptType(), item.getServiceType()))
                .serviceMode(item.getServiceMode())
                .serviceMemberType(item.getServiceMemberType())
                .deptType(item.getDeptType())
                .projectId(project.getId())
                .projectCode(project.getCode())
                .projectName(project.getName())
                .responsibleDeptId(responsibleDeptId)
                .responsibleDeptName(responsibleDeptName)
                .customerId(item.getCustomerId() != null ? item.getCustomerId() : project.getCustomerId())
                .customerName(item.getCustomerName() != null ? item.getCustomerName() : project.getCustomerName())
                .contractId(contractId)
                .contractNo(contractNo)
                .sourceType(contractId == null ? "approved_early_investment" : "signed_contract")
                .remainingCount(serviceItemService.getRemainingCount(item.getId()))
                .build();
    }

    private Long resolveResponsibleDeptId(ServiceItemDO item, ProjectDeptServiceDO deptService) {
        return item.getDeptId() != null ? item.getDeptId() : deptService.getDeptId();
    }

}
