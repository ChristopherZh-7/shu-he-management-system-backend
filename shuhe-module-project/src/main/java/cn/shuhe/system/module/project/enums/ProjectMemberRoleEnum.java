package cn.shuhe.system.module.project.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目成员与派生访问角色。
 *
 * <p>1/2/3 会持久化到 project_member。4 只是根据部门管理链实时计算的只读角色，
 * 不写入项目成员表，避免人员调岗后遗留管理权限。</p>
 */
@Getter
@AllArgsConstructor
public enum ProjectMemberRoleEnum {

    MANAGER(1, "项目经理"),
    EXECUTOR(2, "执行人员"),
    DEPT_LEADER(3, "参与部门负责人"),
    UPPER_LEADER(4, "上级负责人（只读）");

    private final Integer value;
    private final String name;

}
