package cn.shuhe.system.module.project.framework.datapermission.config;

import cn.shuhe.system.module.project.dal.dataobject.DailyManagementRecordDO;
import cn.shuhe.system.module.project.dal.dataobject.EmployeeScheduleDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptServiceDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectWorkRecordDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchDO;
import cn.shuhe.system.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * project 模块的数据权限 Configuration
 * 
 * 配置各业务表的数据权限规则：
 * - addDeptColumn: 基于部门ID过滤数据
 * - addUserColumn: 基于用户ID过滤数据（仅本人）
 */
@Configuration(value = "projectDataPermissionConfiguration", proxyBeanMethods = false)
public class DataPermissionConfiguration {

    @Bean
    public DeptDataPermissionRuleCustomizer projectDeptDataPermissionRuleCustomizer() {
        return rule -> {
            // ========== 项目管理相关表 ==========
            // 服务项表 - 基于 deptId 过滤（部门数据权限）
            rule.addDeptColumn(ServiceItemDO.class);
            
            // 部门服务表 - 基于 deptId 过滤
            rule.addDeptColumn(ProjectDeptServiceDO.class);
            
            // ========== 工作记录相关表 ==========
            // 项目管理记录 - 基于 deptId 过滤 + creator 过滤（仅本人）
            rule.addDeptColumn(ProjectWorkRecordDO.class);
            rule.addUserColumn(ProjectWorkRecordDO.class, "creator");
            
            // 日常管理记录 - 基于 deptId 过滤 + creator 过滤（仅本人）
            rule.addDeptColumn(DailyManagementRecordDO.class);
            rule.addUserColumn(DailyManagementRecordDO.class, "creator");
            
            // ========== 员工排班表 ==========
            // 员工排班 - 基于 deptId 过滤 + userId 过滤（仅本人）
            rule.addDeptColumn(EmployeeScheduleDO.class);
            rule.addUserColumn(EmployeeScheduleDO.class, "userId");
            
            // ========== 服务发起相关表 ==========
            // 服务发起 - 基于执行部门过滤
            rule.addDeptColumn(ServiceLaunchDO.class, "executeDeptId");
            // 服务发起 - 基于申请人过滤（仅本人）
            rule.addUserColumn(ServiceLaunchDO.class, "requestUserId");
        };
    }

}
