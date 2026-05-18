package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 项目维度的部门可见性 DO
 *
 * 用途：部门下所有 user 都能看到该项目（在「项目管理」列表里出现）。
 * 由 createProjectInternally 从 CrmBusinessDO.involvedDeptIds 自动派生写入；
 * 同时项目详情页提供「可见部门」管理 tab 让用户手动增删。
 *
 * 与 project_member 的区别：
 * - project_member：个人维度，每个 user 一行（粒度细，灵活）
 * - project_dept_visibility：部门维度，一个 dept 一行，部门下所有 user 自动可见
 */
@TableName(value = "project_dept_visibility")
@KeySequence("project_dept_visibility_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDeptVisibilityDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 项目 id
     */
    private Long projectId;

    /**
     * 可见部门 id（部门下所有人都能看到该项目）
     */
    private Long deptId;

}
