package cn.shuhe.system.module.crm.controller.admin.business;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.framework.apilog.core.annotation.ApiAccessLog;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.collection.MapUtils;
import cn.shuhe.system.framework.common.util.number.NumberUtils;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.excel.core.util.ExcelUtils;
import cn.shuhe.system.module.crm.controller.admin.business.vo.business.*;
import cn.shuhe.system.module.crm.controller.admin.business.vo.business.CrmBusinessEarlyInvestmentSubmitReqVO;
import cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO;
import cn.shuhe.system.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.shuhe.system.module.crm.service.business.CrmBusinessService;
import cn.shuhe.system.module.crm.service.customer.CrmCustomerService;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static cn.shuhe.system.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.framework.common.pojo.PageParam.PAGE_SIZE_NONE;
import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.*;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.shuhe.system.module.crm.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;

@Tag(name = "管理后台 - CRM 商机")
@RestController
@RequestMapping("/crm/business")
@Validated
public class CrmBusinessController {

    @Resource
    private CrmBusinessService businessService;
    @Resource
    private CrmCustomerService customerService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建商机")
    @PreAuthorize("@ss.hasPermission('crm:business:create')")
    public CommonResult<Long> createBusiness(@Valid @RequestBody CrmBusinessSaveReqVO createReqVO) {
        return success(businessService.createBusiness(createReqVO, getLoginUserId()));
    }

    @PutMapping("/update")
    @Operation(summary = "更新商机")
    @PreAuthorize("@ss.hasPermission('crm:business:update')")
    public CommonResult<Boolean> updateBusiness(@Valid @RequestBody CrmBusinessSaveReqVO updateReqVO) {
        businessService.updateBusiness(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit-audit")
    @Operation(summary = "提交商机审核")
    @Parameter(name = "id", description = "商机编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:business:update')")
    public CommonResult<Boolean> submitBusinessAudit(@RequestParam("id") Long id) {
        businessService.submitBusinessAudit(id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/submit-early-investment")
    @Operation(summary = "提交提前投入审批（填写申请详情后发起，审批通过后自动创建项目）")
    @PreAuthorize("@ss.hasPermission('crm:business:update')")
    public CommonResult<Boolean> submitEarlyInvestment(
            @Valid @RequestBody CrmBusinessEarlyInvestmentSubmitReqVO reqVO) {
        businessService.submitEarlyInvestment(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新商机状态")
    @PreAuthorize("@ss.hasPermission('crm:business:update')")
    public CommonResult<Boolean> updateBusinessStatus(@Valid @RequestBody CrmBusinessUpdateStatusReqVO updateStatusReqVO) {
        businessService.updateBusinessStatus(updateStatusReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除商机")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:business:delete')")
    public CommonResult<Boolean> deleteBusiness(@RequestParam("id") Long id) {
        businessService.deleteBusiness(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得商机")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:business:query')")
    public CommonResult<CrmBusinessRespVO> getBusiness(@RequestParam("id") Long id) {
        CrmBusinessDO business = businessService.getBusiness(id);
        return success(buildBusinessDetail(business));
    }

    private CrmBusinessRespVO buildBusinessDetail(CrmBusinessDO business) {
        if (business == null) {
            return null;
        }
        return buildBusinessDetailList(Collections.singletonList(business)).get(0);
    }

    @GetMapping("/simple-all-list")
    @Operation(summary = "获得商机的精简列表")
    @PreAuthorize("@ss.hasPermission('crm:business:query')")
    public CommonResult<List<CrmBusinessRespVO>> getSimpleContactList() {
        CrmBusinessPageReqVO reqVO = new CrmBusinessPageReqVO();
        reqVO.setPageSize(PAGE_SIZE_NONE); // 不分页
        PageResult<CrmBusinessDO> pageResult = businessService.getBusinessPage(reqVO, getLoginUserId());
        return success(convertList(pageResult.getList(), business -> // 只返回 id、name 字段
                new CrmBusinessRespVO().setId(business.getId()).setName(business.getName())
                        .setCustomerId(business.getCustomerId())));
    }

    @GetMapping("/page")
    @Operation(summary = "获得商机分页")
    @PreAuthorize("@ss.hasPermission('crm:business:query')")
    public CommonResult<PageResult<CrmBusinessRespVO>> getBusinessPage(@Valid CrmBusinessPageReqVO pageVO) {
        PageResult<CrmBusinessDO> pageResult = businessService.getBusinessPage(pageVO, getLoginUserId());
        return success(new PageResult<>(buildBusinessDetailList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/page-by-customer")
    @Operation(summary = "获得商机分页，基于指定客户")
    public CommonResult<PageResult<CrmBusinessRespVO>> getBusinessPageByCustomer(@Valid CrmBusinessPageReqVO pageReqVO) {
        if (pageReqVO.getCustomerId() == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        PageResult<CrmBusinessDO> pageResult = businessService.getBusinessPageByCustomerId(pageReqVO);
        return success(new PageResult<>(buildBusinessDetailList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/page-by-contact")
    @Operation(summary = "获得联系人的商机分页")
    @PreAuthorize("@ss.hasPermission('crm:business:query')")
    public CommonResult<PageResult<CrmBusinessRespVO>> getBusinessContactPage(@Valid CrmBusinessPageReqVO pageReqVO) {
        PageResult<CrmBusinessDO> pageResult = businessService.getBusinessPageByContact(pageReqVO);
        return success(new PageResult<>(buildBusinessDetailList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出商机 Excel")
    @PreAuthorize("@ss.hasPermission('crm:business:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportBusinessExcel(@Valid CrmBusinessPageReqVO exportReqVO,
                                    HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PAGE_SIZE_NONE);
        List<CrmBusinessDO> list = businessService.getBusinessPage(exportReqVO, getLoginUserId()).getList();
        // 导出 Excel
        ExcelUtils.write(response, "商机.xls", "数据", CrmBusinessRespVO.class,
                buildBusinessDetailList(list));
    }

    public List<CrmBusinessRespVO> buildBusinessDetailList(List<CrmBusinessDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 1.1 获取最终客户 + 合作商（合并为一次查询）
        Set<Long> allCustomerIds = new java.util.HashSet<>();
        list.forEach(b -> {
            if (b.getCustomerId() != null) allCustomerIds.add(b.getCustomerId());
            if (b.getIntermediaryId() != null) allCustomerIds.add(b.getIntermediaryId());
        });
        Map<Long, CrmCustomerDO> customerMap = customerService.getCustomerMap(allCustomerIds);
        // 1.2 获取创建人、负责人列表
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(list,
                contact -> Stream.of(NumberUtils.parseLong(contact.getCreator()), contact.getOwnerUserId())));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(userMap.values(), AdminUserRespDTO::getDeptId));
        // 1.3 收集所有部门分配中的部门 ID，批量查询部门及其负责人
        Set<Long> allAllocDeptIds = new java.util.HashSet<>();
        list.forEach(b -> {
            if (b.getDeptAllocations() != null) {
                b.getDeptAllocations().forEach(a -> { if (a.getDeptId() != null) allAllocDeptIds.add(a.getDeptId()); });
            }
        });
        Map<Long, DeptRespDTO> allocDeptMap = CollUtil.isEmpty(allAllocDeptIds)
                ? Collections.emptyMap() : deptApi.getDeptMap(allAllocDeptIds);
        Set<Long> allocLeaderIds = convertSet(allocDeptMap.values(),
                d -> d.getLeaderUserId() != null ? d.getLeaderUserId() : null);
        allocLeaderIds.remove(null);
        Map<Long, AdminUserRespDTO> allocLeaderMap = CollUtil.isEmpty(allocLeaderIds)
                ? Collections.emptyMap() : adminUserApi.getUserMap(allocLeaderIds);
        // 2. 转换并拼接数据
        List<CrmBusinessRespVO> voList = BeanUtils.toBean(list, CrmBusinessRespVO.class);
        for (int i = 0; i < list.size(); i++) {
            CrmBusinessDO business = list.get(i);
            CrmBusinessRespVO businessVO = voList.get(i);
            // 2.1 设置最终客户名称
            MapUtils.findAndThen(customerMap, businessVO.getCustomerId(), customer -> businessVO.setCustomerName(customer.getName()));
            // 2.2 设置合作商名称
            MapUtils.findAndThen(customerMap, businessVO.getIntermediaryId(), intermediary -> businessVO.setIntermediaryName(intermediary.getName()));
            // 2.3 设置创建人、负责人名称
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(businessVO.getCreator()),
                    user -> businessVO.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, businessVO.getOwnerUserId(), user -> {
                businessVO.setOwnerUserName(user.getNickname());
                MapUtils.findAndThen(deptMap, user.getDeptId(), dept -> businessVO.setOwnerUserDeptName(dept.getName()));
            });
            // 2.4 将部门分配列表转换为 DeptAllocationVO（含部门负责人姓名）
            if (business.getDeptAllocations() != null) {
                List<CrmBusinessRespVO.DeptAllocationVO> allocVOs = convertList(business.getDeptAllocations(), alloc -> {
                    CrmBusinessRespVO.DeptAllocationVO vo = new CrmBusinessRespVO.DeptAllocationVO();
                    vo.setDeptId(alloc.getDeptId());
                    vo.setDeptName(alloc.getDeptName());
                    vo.setAmount(alloc.getAmount());
                    if (alloc.getDeptId() != null) {
                        DeptRespDTO allocDept = allocDeptMap.get(alloc.getDeptId());
                        if (allocDept != null && allocDept.getLeaderUserId() != null) {
                            AdminUserRespDTO leader = allocLeaderMap.get(allocDept.getLeaderUserId());
                            if (leader != null) {
                                vo.setDeptLeaderName(leader.getNickname());
                            }
                        }
                    }
                    return vo;
                });
                businessVO.setDeptAllocations(allocVOs);
            }
        }
        return voList;
    }

    @PutMapping("/transfer")
    @Operation(summary = "商机转移")
    @PreAuthorize("@ss.hasPermission('crm:business:update')")
    public CommonResult<Boolean> transferBusiness(@Valid @RequestBody CrmBusinessTransferReqVO reqVO) {
        businessService.transferBusiness(reqVO, getLoginUserId());
        return success(true);
    }

}
