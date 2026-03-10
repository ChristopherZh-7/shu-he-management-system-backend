package cn.shuhe.system.module.crm.service.resignation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil;
import cn.shuhe.system.module.crm.controller.admin.resignation.vo.ResignationHandoverExecuteReqVO;
import cn.shuhe.system.module.crm.controller.admin.resignation.vo.ResignationHandoverPreviewRespVO;
import cn.shuhe.system.module.crm.dal.mysql.business.CrmBusinessMapper;
import cn.shuhe.system.module.crm.dal.mysql.clue.CrmClueMapper;
import cn.shuhe.system.module.crm.dal.mysql.contact.CrmContactMapper;
import cn.shuhe.system.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.shuhe.system.module.crm.dal.mysql.customer.CrmCustomerMapper;
import cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO;
import cn.shuhe.system.module.crm.dal.dataobject.clue.CrmClueDO;
import cn.shuhe.system.module.crm.dal.dataobject.contact.CrmContactDO;
import cn.shuhe.system.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.shuhe.system.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum;
import cn.shuhe.system.module.crm.enums.common.CrmBizTypeEnum;
import cn.shuhe.system.module.crm.service.permission.CrmPermissionService;
import cn.shuhe.system.module.crm.service.permission.bo.CrmPermissionTransferReqBO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptServiceDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteMemberDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectDeptServiceMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectSiteMemberMapper;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 离职交接 Service 实现类
 *
 * @author ShuHe
 */
@Slf4j
@Service
public class ResignationHandoverServiceImpl implements ResignationHandoverService {

    /** 项目状态：0-草稿 1-进行中 2-已完成 3-已退场。只转移草稿，不转移进行中/已完成/已退场 */
    private static final int PROJECT_STATUS_DRAFT = 0;
    /** 部门服务单状态：0-待领取 1-待开始 2-进行中 3-已暂停 4-已完成 5-已取消。只转移待领取、待开始 */
    private static final int DEPT_SERVICE_STATUS_CLAIM = 0;
    private static final int DEPT_SERVICE_STATUS_PENDING = 1;
    /** 驻场负责人状态：0-待入场 1-在岗 2-已离开。只转移待入场，不转移在岗/已离开 */
    private static final int SITE_MEMBER_STATUS_PENDING = 0;

    @Resource
    private CrmCustomerMapper customerMapper;
    @Resource
    private CrmContactMapper contactMapper;
    @Resource
    private CrmBusinessMapper businessMapper;
    @Resource
    private CrmContractMapper contractMapper;
    @Resource
    private CrmClueMapper clueMapper;
    @Resource
    private CrmPermissionService permissionService;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProjectDeptServiceMapper deptServiceMapper;
    @Resource
    private ProjectSiteMemberMapper siteMemberMapper;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public ResignationHandoverPreviewRespVO preview(Long resignUserId, Long newOwnerUserId) {
        // 校验用户：离职用户只校验存在（通常已被禁用），接任用户校验存在且启用
        adminUserApi.validateUserListExists(List.of(resignUserId));
        adminUserApi.validateUserList(List.of(newOwnerUserId));
        if (ObjUtil.equal(resignUserId, newOwnerUserId)) {
            throw ServiceExceptionUtil.invalidParamException("离职用户和接任用户不能相同");
        }

        int customerCount = countTransferableCustomers(resignUserId);
        int businessCount = countTransferableBusiness(resignUserId);
        int contractCount = countTransferableContracts(resignUserId);
        int clueCount = clueMapper.selectListByOwnerUserId(resignUserId).size();

        // 项目：只转移草稿，不转移进行中/已完成/已退场
        List<ProjectDO> projects = projectMapper.selectList();
        int projectCount = (int) projects.stream()
                .filter(p -> CollUtil.isNotEmpty(p.getManagerIds()) && p.getManagerIds().contains(resignUserId)
                        && Integer.valueOf(PROJECT_STATUS_DRAFT).equals(p.getStatus()))
                .count();

        // 部门服务单：只转移待领取、待开始，不转移进行中/已暂停/已完成/已取消
        List<ProjectDeptServiceDO> deptServices = deptServiceMapper.selectList();
        int deptServiceCount = (int) deptServices.stream()
                .filter(d -> containsManager(d, resignUserId)
                        && (Integer.valueOf(DEPT_SERVICE_STATUS_CLAIM).equals(d.getStatus())
                                || Integer.valueOf(DEPT_SERVICE_STATUS_PENDING).equals(d.getStatus())))
                .count();

        // 驻场负责人：只转移待入场，不转移在岗/已离开
        int siteLeaderCount = (int) siteMemberMapper.selectListByUserIdAndIsLeader(resignUserId, ProjectSiteMemberDO.IS_LEADER_YES).stream()
                .filter(m -> Integer.valueOf(SITE_MEMBER_STATUS_PENDING).equals(m.getStatus()))
                .count();

        return ResignationHandoverPreviewRespVO.builder()
                .customerCount(customerCount)
                .businessCount(businessCount)
                .contractCount(contractCount)
                .clueCount(clueCount)
                .projectCount(projectCount)
                .deptServiceCount(deptServiceCount)
                .siteLeaderCount(siteLeaderCount)
                .build();
    }

    /** 可转移的客户数（客户都转移） */
    private int countTransferableCustomers(Long resignUserId) {
        return customerMapper.selectListByOwnerUserId(resignUserId).size();
    }

    /** 可转移的商机数：排除进行中（endStatus 为 null） */
    private int countTransferableBusiness(Long resignUserId) {
        return (int) businessMapper.selectListByOwnerUserId(resignUserId).stream()
                .filter(b -> b.getEndStatus() != null)
                .count();
    }

    /** 可转移的合同数：排除审批中、审核通过（进行中） */
    private int countTransferableContracts(Long resignUserId) {
        return (int) contractMapper.selectListByOwnerUserId(resignUserId).stream()
                .filter(c -> !isContractInProgress(c))
                .count();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(ResignationHandoverExecuteReqVO reqVO, Long userId) {
        Long resignUserId = reqVO.getResignUserId();
        Long newOwnerUserId = reqVO.getNewOwnerUserId();

        // 校验：离职用户只校验存在，接任用户校验存在且启用
        adminUserApi.validateUserListExists(List.of(resignUserId));
        adminUserApi.validateUserList(List.of(newOwnerUserId));
        if (ObjUtil.equal(resignUserId, newOwnerUserId)) {
            throw ServiceExceptionUtil.invalidParamException("离职用户和接任用户不能相同");
        }

        log.info("【离职交接】开始执行，离职用户={}, 接任用户={}, 操作人={}", resignUserId, newOwnerUserId, userId);

        // 1. CRM - 客户（含联系人、商机、合同）
        transferCustomers(resignUserId, newOwnerUserId, userId);

        // 2. CRM - 独立商机、合同、联系人、线索
        transferStandaloneBusiness(resignUserId, newOwnerUserId, userId);
        transferStandaloneContracts(resignUserId, newOwnerUserId, userId);
        transferStandaloneContacts(resignUserId, newOwnerUserId, userId);
        transferClues(resignUserId, newOwnerUserId, userId);

        // 3. 项目 - 负责人
        transferProjectManagers(resignUserId, newOwnerUserId);

        // 4. 部门服务单 - 负责人
        transferDeptServiceManagers(resignUserId, newOwnerUserId);

        // 5. 驻场负责人
        transferSiteLeaders(resignUserId, newOwnerUserId);

        log.info("【离职交接】执行完成，离职用户={}, 接任用户={}", resignUserId, newOwnerUserId);
    }

    private void transferCustomers(Long resignUserId, Long newOwnerUserId, Long operatorUserId) {
        List<CrmCustomerDO> customers = customerMapper.selectListByOwnerUserId(resignUserId);
        for (CrmCustomerDO customer : customers) {
            // 更新客户负责人
            customerMapper.updateById(new CrmCustomerDO().setId(customer.getId())
                    .setOwnerUserId(newOwnerUserId).setOwnerTime(LocalDateTime.now()));
            permissionService.transferPermission(new CrmPermissionTransferReqBO(operatorUserId,
                    CrmBizTypeEnum.CRM_CUSTOMER.getType(), customer.getId(), newOwnerUserId, null));

            // 转移该客户下的联系人、商机、合同（商机排除进行中，合同排除审批中/审核通过）
            List<CrmContactDO> contacts = contactMapper.selectListByCustomerIdOwnerUserId(customer.getId(), resignUserId);
            for (CrmContactDO c : contacts) {
                contactMapper.updateById(new CrmContactDO().setId(c.getId()).setOwnerUserId(newOwnerUserId));
                permissionService.transferPermission(new CrmPermissionTransferReqBO(operatorUserId,
                        CrmBizTypeEnum.CRM_CONTACT.getType(), c.getId(), newOwnerUserId, null));
            }
            List<CrmBusinessDO> businesses = businessMapper.selectListByCustomerIdOwnerUserId(customer.getId(), resignUserId);
            for (CrmBusinessDO b : businesses) {
                if (b.getEndStatus() == null) continue; // 进行中不转移
                businessMapper.updateById(new CrmBusinessDO().setId(b.getId()).setOwnerUserId(newOwnerUserId));
                permissionService.transferPermission(new CrmPermissionTransferReqBO(operatorUserId,
                        CrmBizTypeEnum.CRM_BUSINESS.getType(), b.getId(), newOwnerUserId, null));
            }
            List<CrmContractDO> contracts = contractMapper.selectListByCustomerIdOwnerUserId(customer.getId(), resignUserId);
            for (CrmContractDO c : contracts) {
                if (isContractInProgress(c)) continue; // 审批中/审核通过不转移
                contractMapper.updateById(new CrmContractDO().setId(c.getId()).setOwnerUserId(newOwnerUserId));
                permissionService.transferPermission(new CrmPermissionTransferReqBO(operatorUserId,
                        CrmBizTypeEnum.CRM_CONTRACT.getType(), c.getId(), newOwnerUserId, null));
            }
        }
    }

    private boolean isContractInProgress(CrmContractDO c) {
        return CrmAuditStatusEnum.PROCESS.getStatus().equals(c.getAuditStatus())
                || CrmAuditStatusEnum.APPROVE.getStatus().equals(c.getAuditStatus());
    }

    private void transferStandaloneBusiness(Long resignUserId, Long newOwnerUserId, Long operatorUserId) {
        List<CrmBusinessDO> list = businessMapper.selectListByOwnerUserId(resignUserId);
        for (CrmBusinessDO b : list) {
            if (b.getEndStatus() == null) continue; // 进行中不转移
            businessMapper.updateById(new CrmBusinessDO().setId(b.getId()).setOwnerUserId(newOwnerUserId));
            permissionService.transferPermission(new CrmPermissionTransferReqBO(operatorUserId,
                    CrmBizTypeEnum.CRM_BUSINESS.getType(), b.getId(), newOwnerUserId, null));
        }
    }

    private void transferStandaloneContracts(Long resignUserId, Long newOwnerUserId, Long operatorUserId) {
        List<CrmContractDO> list = contractMapper.selectListByOwnerUserId(resignUserId);
        for (CrmContractDO c : list) {
            if (isContractInProgress(c)) continue; // 审批中/审核通过不转移
            contractMapper.updateById(new CrmContractDO().setId(c.getId()).setOwnerUserId(newOwnerUserId));
            permissionService.transferPermission(new CrmPermissionTransferReqBO(operatorUserId,
                    CrmBizTypeEnum.CRM_CONTRACT.getType(), c.getId(), newOwnerUserId, null));
        }
    }

    private void transferStandaloneContacts(Long resignUserId, Long newOwnerUserId, Long operatorUserId) {
        List<CrmContactDO> list = contactMapper.selectList(CrmContactDO::getOwnerUserId, resignUserId);
        for (CrmContactDO c : list) {
            contactMapper.updateById(new CrmContactDO().setId(c.getId()).setOwnerUserId(newOwnerUserId));
            permissionService.transferPermission(new CrmPermissionTransferReqBO(operatorUserId,
                    CrmBizTypeEnum.CRM_CONTACT.getType(), c.getId(), newOwnerUserId, null));
        }
    }

    private void transferClues(Long resignUserId, Long newOwnerUserId, Long operatorUserId) {
        List<CrmClueDO> list = clueMapper.selectListByOwnerUserId(resignUserId);
        for (CrmClueDO c : list) {
            clueMapper.updateById(new CrmClueDO().setId(c.getId()).setOwnerUserId(newOwnerUserId));
            permissionService.transferPermission(new CrmPermissionTransferReqBO(operatorUserId,
                    CrmBizTypeEnum.CRM_CLUE.getType(), c.getId(), newOwnerUserId, null));
        }
    }

    private void transferProjectManagers(Long resignUserId, Long newOwnerUserId) {
        List<ProjectDO> projects = projectMapper.selectList();
        for (ProjectDO p : projects) {
            if (CollUtil.isEmpty(p.getManagerIds()) || !p.getManagerIds().contains(resignUserId)) {
                continue;
            }
            if (!Integer.valueOf(PROJECT_STATUS_DRAFT).equals(p.getStatus())) {
                continue; // 只转移草稿，不转移进行中/已完成/已退场
            }
            List<Long> newIds = replaceInList(p.getManagerIds(), resignUserId, newOwnerUserId);
            List<String> newNames = resolveManagerNames(newIds);
            projectMapper.updateById(new ProjectDO().setId(p.getId()).setManagerIds(newIds).setManagerNames(newNames));
        }
    }

    private void transferDeptServiceManagers(Long resignUserId, Long newOwnerUserId) {
        List<ProjectDeptServiceDO> list = deptServiceMapper.selectList();
        for (ProjectDeptServiceDO d : list) {
            if (!Integer.valueOf(DEPT_SERVICE_STATUS_CLAIM).equals(d.getStatus())
                    && !Integer.valueOf(DEPT_SERVICE_STATUS_PENDING).equals(d.getStatus())) {
                continue; // 只转移待领取、待开始
            }
            boolean updated = false;
            ProjectDeptServiceDO updateObj = new ProjectDeptServiceDO().setId(d.getId());

            if (CollUtil.isNotEmpty(d.getManagerIds()) && d.getManagerIds().contains(resignUserId)) {
                updateObj.setManagerIds(replaceInList(d.getManagerIds(), resignUserId, newOwnerUserId));
                updateObj.setManagerNames(resolveManagerNames(updateObj.getManagerIds()));
                updated = true;
            }
            if (CollUtil.isNotEmpty(d.getOnsiteManagerIds()) && d.getOnsiteManagerIds().contains(resignUserId)) {
                updateObj.setOnsiteManagerIds(replaceInList(d.getOnsiteManagerIds(), resignUserId, newOwnerUserId));
                updateObj.setOnsiteManagerNames(resolveManagerNames(updateObj.getOnsiteManagerIds()));
                updated = true;
            }
            if (CollUtil.isNotEmpty(d.getSecondLineManagerIds()) && d.getSecondLineManagerIds().contains(resignUserId)) {
                updateObj.setSecondLineManagerIds(replaceInList(d.getSecondLineManagerIds(), resignUserId, newOwnerUserId));
                updateObj.setSecondLineManagerNames(resolveManagerNames(updateObj.getSecondLineManagerIds()));
                updated = true;
            }
            if (updated) {
                deptServiceMapper.updateById(updateObj);
            }
        }
    }

    private void transferSiteLeaders(Long resignUserId, Long newOwnerUserId) {
        AdminUserRespDTO newUser = adminUserApi.getUser(newOwnerUserId);
        String newUserName = newUser != null ? newUser.getNickname() : String.valueOf(newOwnerUserId);

        List<ProjectSiteMemberDO> list = siteMemberMapper.selectListByUserIdAndIsLeader(resignUserId, ProjectSiteMemberDO.IS_LEADER_YES);
        for (ProjectSiteMemberDO m : list) {
            if (!Integer.valueOf(SITE_MEMBER_STATUS_PENDING).equals(m.getStatus())) {
                continue; // 只转移待入场，不转移在岗/已离开
            }
            siteMemberMapper.updateById(new ProjectSiteMemberDO().setId(m.getId())
                    .setUserId(newOwnerUserId).setUserName(newUserName));
        }
    }

    private boolean containsManager(ProjectDeptServiceDO d, Long userId) {
        return (CollUtil.isNotEmpty(d.getManagerIds()) && d.getManagerIds().contains(userId))
                || (CollUtil.isNotEmpty(d.getOnsiteManagerIds()) && d.getOnsiteManagerIds().contains(userId))
                || (CollUtil.isNotEmpty(d.getSecondLineManagerIds()) && d.getSecondLineManagerIds().contains(userId));
    }

    private List<Long> replaceInList(List<Long> list, Long oldId, Long newId) {
        if (list == null) return list;
        List<Long> result = new ArrayList<>();
        boolean added = false;
        for (Long id : list) {
            if (ObjUtil.equal(id, oldId)) {
                if (!result.contains(newId)) {
                    result.add(newId);
                    added = true;
                }
            } else {
                result.add(id);
            }
        }
        if (!added && !result.contains(newId)) {
            result.add(newId);
        }
        return result;
    }

    private List<String> resolveManagerNames(List<Long> managerIds) {
        if (CollUtil.isEmpty(managerIds)) return new ArrayList<>();
        return managerIds.stream()
                .map(id -> {
                    AdminUserRespDTO u = adminUserApi.getUser(id);
                    return u != null ? u.getNickname() : String.valueOf(id);
                })
                .collect(Collectors.toList());
    }
}
