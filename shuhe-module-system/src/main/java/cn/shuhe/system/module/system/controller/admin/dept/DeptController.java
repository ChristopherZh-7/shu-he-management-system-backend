package cn.shuhe.system.module.system.controller.admin.dept;

import cn.shuhe.system.framework.common.enums.CommonStatusEnum;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.shuhe.system.module.system.controller.admin.dept.vo.dept.DeptRespVO;
import cn.shuhe.system.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import cn.shuhe.system.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.service.dept.DeptService;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 部门")
@RestController
@RequestMapping("/system/dept")
@Validated
public class DeptController {

    @Resource
    private DeptService deptService;

    @Resource
    private AdminUserService adminUserService;

    @PostMapping("create")
    @Operation(summary = "创建部门")
    @PreAuthorize("@ss.hasPermission('system:dept:create')")
    public CommonResult<Long> createDept(@Valid @RequestBody DeptSaveReqVO createReqVO) {
        Long deptId = deptService.createDept(createReqVO);
        return success(deptId);
    }

    @PutMapping("update")
    @Operation(summary = "更新部门")
    @PreAuthorize("@ss.hasPermission('system:dept:update')")
    public CommonResult<Boolean> updateDept(@Valid @RequestBody DeptSaveReqVO updateReqVO) {
        deptService.updateDept(updateReqVO);
        return success(true);
    }

    @DeleteMapping("delete")
    @Operation(summary = "删除部门")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> deleteDept(@RequestParam("id") Long id) {
        deptService.deleteDept(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除部门")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> deleteDeptList(@RequestParam("ids") List<Long> ids) {
        deptService.deleteDeptList(ids);
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获取部门列表")
    @PreAuthorize("@ss.hasPermission('system:dept:query')")
    public CommonResult<List<DeptRespVO>> getDeptList(DeptListReqVO reqVO) {
        List<DeptDO> list = deptService.getDeptList(reqVO);
        return success(BeanUtils.toBean(list, DeptRespVO.class));
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取部门精简信息列表", description = "只包含被开启的部门，主要用于前端的下拉选项，不受数据权限限制")
    public CommonResult<List<DeptSimpleRespVO>> getSimpleDeptList() {
        List<DeptDO> list = deptService.getDeptListForSimpleList(
                new DeptListReqVO().setStatus(CommonStatusEnum.ENABLE.getStatus()));
        List<DeptSimpleRespVO> voList = BeanUtils.toBean(list, DeptSimpleRespVO.class);
        fillLeaderNames(list, voList);
        return success(voList);
    }

    @GetMapping("/business-dept-list")
    @Operation(summary = "获取业务部门列表（商机用）", description = "过滤掉总经办、人事行政部及其子部门，用于商机补充部门分配")
    public CommonResult<List<DeptSimpleRespVO>> getBusinessDeptList() {
        List<DeptDO> allDepts = deptService.getDeptListForSimpleList(
                new DeptListReqVO().setStatus(CommonStatusEnum.ENABLE.getStatus()));

        // 1. 找到"总经办"和"人事行政"部门
        Long zongjingbanId = null;
        Set<Long> renshiIds = new HashSet<>();
        for (DeptDO dept : allDepts) {
            if (dept.getName() == null) continue;
            if (dept.getName().contains("总经办")) {
                zongjingbanId = dept.getId();
            }
            if (dept.getName().contains("人事行政")) {
                renshiIds.add(dept.getId());
            }
        }

        // 2. 递归收集"人事行政"的所有子部门
        boolean changed = true;
        while (changed) {
            changed = false;
            for (DeptDO dept : allDepts) {
                if (!renshiIds.contains(dept.getId()) && dept.getParentId() != null
                        && renshiIds.contains(dept.getParentId())) {
                    renshiIds.add(dept.getId());
                    changed = true;
                }
            }
        }

        // 3. 构建结果：移除总经办自身（子部门提升到总经办的父级）+ 移除人事行政整棵子树
        final Long zjbId = zongjingbanId;
        Long zjbParentId = null;
        if (zongjingbanId != null) {
            for (DeptDO dept : allDepts) {
                if (dept.getId().equals(zongjingbanId)) {
                    zjbParentId = dept.getParentId();
                    break;
                }
            }
        }
        final Long finalZjbParentId = zjbParentId;

        List<DeptDO> filtered = new java.util.ArrayList<>();
        for (DeptDO dept : allDepts) {
            // 排除人事行政及其子部门
            if (renshiIds.contains(dept.getId())) continue;
            // 排除总经办自身
            if (zjbId != null && dept.getId().equals(zjbId)) continue;

            // 总经办的直接子部门，parentId 提升到总经办的父级
            if (zjbId != null && zjbId.equals(dept.getParentId())) {
                DeptDO copy = BeanUtils.toBean(dept, DeptDO.class);
                copy.setParentId(finalZjbParentId);
                filtered.add(copy);
            } else {
                filtered.add(dept);
            }
        }
        List<DeptSimpleRespVO> voList = BeanUtils.toBean(filtered, DeptSimpleRespVO.class);
        fillLeaderNames(filtered, voList);
        return success(voList);
    }

    private void fillLeaderNames(List<DeptDO> deptDOs, List<DeptSimpleRespVO> voList) {
        Set<Long> leaderIds = deptDOs.stream()
                .map(DeptDO::getLeaderUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (leaderIds.isEmpty()) return;
        // 禁用数据权限：getSimpleDeptList 的目的是给前端做下拉/树形展示，
        // 必须能列出全公司所有部门 + 各自负责人；直接调 service 层 getUserMap
        // 会被 @DataPermission 切面按当前登录人 dept 范围裁剪，导致非超管用户
        // 只能看见自己 dept 内 user 的 nickname，其它部门的负责人显示不出来。
        // 通过 DataPermissionUtils.executeIgnore 临时关掉数据权限，与 AdminUserApiImpl.getUserList 一致。
        Map<Long, AdminUserDO> userMap = cn.shuhe.system.framework.datapermission.core.util.DataPermissionUtils
                .executeIgnore(() -> adminUserService.getUserMap(leaderIds));
        for (int i = 0; i < deptDOs.size(); i++) {
            Long leaderId = deptDOs.get(i).getLeaderUserId();
            if (leaderId != null) {
                voList.get(i).setLeaderUserId(leaderId);
                AdminUserDO user = userMap.get(leaderId);
                if (user != null) {
                    voList.get(i).setLeaderName(user.getNickname());
                }
            }
        }
    }

    @GetMapping("/get")
    @Operation(summary = "获得部门信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dept:query')")
    public CommonResult<DeptRespVO> getDept(@RequestParam("id") Long id) {
        DeptDO dept = deptService.getDept(id);
        return success(BeanUtils.toBean(dept, DeptRespVO.class));
    }

}
