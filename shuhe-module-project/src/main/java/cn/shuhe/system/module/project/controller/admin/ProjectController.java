package cn.shuhe.system.module.project.controller.admin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.service.ProjectService;
import cn.shuhe.system.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.dept.PostDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.dal.mysql.dept.PostMapper;
import cn.shuhe.system.module.system.service.dept.DeptService;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - 项目管理")
@RestController
@RequestMapping("/project/info")
@Validated
public class ProjectController {

    @Resource
    private ProjectService projectService;

    @Resource
    private AdminUserService adminUserService;

    @Resource
    private PostMapper postMapper;

    @Resource
    private DeptService deptService;

    /**
     * 部门类型对应的岗位code映射
     */
    private static final Map<Integer, List<String>> DEPT_TYPE_POST_CODES = Map.of(
            1, List.of("anquanjishufuwugongchengshi", "anquanjishufuwuzuzhang"), // 安全服务
            2, List.of("anquanyunyingfuwugongchengshi", "anquanyunyingfuwuzuzhang", "anquanyunyingfuwuzhuguan"), // 安全运营
            3, List.of("shujuanquanfuwugongchengshi", "shujuanquanfuwuzhuguan") // 数据安全
    );

    @PostMapping("/create")
    @Operation(summary = "创建项目")
    @PreAuthorize("@ss.hasPermission('project:info:create')")
    public CommonResult<Long> createProject(@Valid @RequestBody ProjectSaveReqVO createReqVO) {
        return success(projectService.createProject(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新项目")
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Boolean> updateProject(@Valid @RequestBody ProjectSaveReqVO updateReqVO) {
        projectService.updateProject(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除项目")
    @Parameter(name = "id", description = "项目编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:delete')")
    public CommonResult<Boolean> deleteProject(@RequestParam("id") Long id) {
        projectService.deleteProject(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得项目详情")
    @Parameter(name = "id", description = "项目编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:query')")
    public CommonResult<ProjectRespVO> getProject(@RequestParam("id") Long id) {
        ProjectDO project = projectService.getProject(id);
        ProjectRespVO respVO = BeanUtils.toBean(project, ProjectRespVO.class);
        // 处理标签
        if (project != null && project.getTags() != null) {
            respVO.setTags(JSONUtil.toList(project.getTags(), String.class));
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得项目分页")
    @PreAuthorize("@ss.hasPermission('project:info:query')")
    public CommonResult<PageResult<ProjectRespVO>> getProjectPage(@Valid ProjectPageReqVO pageReqVO) {
        PageResult<ProjectDO> pageResult = projectService.getProjectPage(pageReqVO);
        PageResult<ProjectRespVO> result = BeanUtils.toBean(pageResult, ProjectRespVO.class);
        // 处理标签
        for (int i = 0; i < pageResult.getList().size(); i++) {
            ProjectDO project = pageResult.getList().get(i);
            if (project.getTags() != null) {
                result.getList().get(i).setTags(JSONUtil.toList(project.getTags(), String.class));
            }
        }
        return success(result);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新项目状态")
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Boolean> updateProjectStatus(@RequestParam("id") Long id,
                                                      @RequestParam("status") Integer status) {
        projectService.updateProjectStatus(id, status);
        return success(true);
    }

    @PutMapping("/update-progress")
    @Operation(summary = "更新项目进度")
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Boolean> updateProjectProgress(@RequestParam("id") Long id,
                                                        @RequestParam("progress") Integer progress) {
        projectService.updateProjectProgress(id, progress);
        return success(true);
    }

    @GetMapping("/user-list-by-dept-type")
    @Operation(summary = "根据部门类型获取可选执行人列表", description = "根据部门类型返回对应岗位的用户列表")
    @Parameter(name = "deptType", description = "部门类型：1安全服务 2安全运营 3数据安全", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:query')")
    public CommonResult<List<UserSimpleRespVO>> getUserListByDeptType(@RequestParam("deptType") Integer deptType) {
        // 获取部门类型对应的岗位code列表
        List<String> postCodes = DEPT_TYPE_POST_CODES.get(deptType);
        if (CollUtil.isEmpty(postCodes)) {
            return success(Collections.emptyList());
        }

        // 根据岗位code查询岗位ID
        List<Long> postIds = new ArrayList<>();
        for (String code : postCodes) {
            PostDO post = postMapper.selectByCode(code);
            if (post != null) {
                postIds.add(post.getId());
            }
        }
        if (CollUtil.isEmpty(postIds)) {
            return success(Collections.emptyList());
        }

        // 根据岗位ID查询用户列表
        List<AdminUserDO> users = adminUserService.getUserListByPostIds(postIds);
        if (CollUtil.isEmpty(users)) {
            return success(Collections.emptyList());
        }

        // 拼接部门信息
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(users, AdminUserDO::getDeptId));

        // 转换为简单响应
        List<UserSimpleRespVO> result = users.stream().map(user -> {
            UserSimpleRespVO vo = new UserSimpleRespVO();
            vo.setId(user.getId());
            vo.setNickname(user.getNickname());
            vo.setDeptId(user.getDeptId());
            DeptDO dept = deptMap.get(user.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            return vo;
        }).collect(Collectors.toList());

        return success(result);
    }

}
