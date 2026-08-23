package cn.shuhe.system.module.project.service.access;

import java.util.List;

/**
 * 项目级数据权限服务。
 *
 * <p>返回的部门 id 列表中：{@code null} 表示不限部门，空列表表示无权访问部门内容。</p>
 */
public interface ProjectAccessService {

    boolean canViewProject(Long projectId, Long userId);

    boolean canManageProject(Long projectId, Long userId);

    void validateViewProject(Long projectId, Long userId);

    void validateManageProject(Long projectId, Long userId);

    /**
     * 返回项目中的有效角色。部门普通只读用户不是项目成员，返回 null。
     */
    Integer getEffectiveRole(Long projectId, Long userId);

    List<Long> getReadableDeptIds(Long projectId, Long userId);

    List<Long> getManageableDeptIds(Long projectId, Long userId);

    boolean canReadDept(Long projectId, Long userId, Long deptId);

    boolean canManageDept(Long projectId, Long userId, Long deptId);

    /**
     * 当前用户可见的项目 id。超管返回 null，表示全部。
     */
    List<Long> getVisibleProjectIds(Long userId);

}
