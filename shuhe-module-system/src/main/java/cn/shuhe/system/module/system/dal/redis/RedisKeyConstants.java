package cn.shuhe.system.module.system.dal.redis;

import cn.shuhe.system.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;

/**
 * System Redis Key 枚举类
 *
 * @author 芋道源码
 */
public interface RedisKeyConstants {

    /**
     * 指定部门的所有子部门编号数组的缓存
     * <p>
     * KEY 格式：dept_children_ids:{id}
     * VALUE 数据类型：String 子部门编号集合
     */
    String DEPT_CHILDREN_ID_LIST = "dept_children_ids";

    /**
     * 角色的缓存
     * <p>
     * KEY 格式：role:{id}
     * VALUE 数据类型：String 角色信息
     */
    String ROLE = "role";

    /**
     * 用户拥有的角色编号的缓存
     * <p>
     * KEY 格式：user_role_ids:{userId}
     * VALUE 数据类型：String 角色编号集合
     */
    String USER_ROLE_ID_LIST = "user_role_ids";

    /**
     * 拥有指定菜单的角色编号的缓存
     * <p>
     * KEY 格式：user_role_ids:{menuId}
     * VALUE 数据类型：String 角色编号集合
     */
    String MENU_ROLE_ID_LIST = "menu_role_ids";

    /**
     * 拥有权限对应的菜单编号数组的缓存
     * <p>
     * KEY 格式：permission_menu_ids:{permission}
     * VALUE 数据类型：String 菜单编号数组
     */
    String PERMISSION_MENU_ID_LIST = "permission_menu_ids";

    /**
     * OAuth2 客户端的缓存
     * <p>
     * KEY 格式：oauth_client:{id}
     * VALUE 数据类型：String 客户端信息
     */
    String OAUTH_CLIENT = "oauth_client";

    /**
     * 访问令牌的缓存
     * <p>
     * KEY 格式：oauth2_access_token:{token}
     * VALUE 数据类型：String 访问令牌信息 {@link OAuth2AccessTokenDO}
     * <p>
     * 由于动态过期时间，使用 RedisTemplate 操作
     */
    String OAUTH2_ACCESS_TOKEN = "oauth2_access_token:%s";

    /**
     * 站内信模版的缓存
     * <p>
     * KEY 格式：notify_template:{code}
     * VALUE 数据格式：String 模版信息
     */
    String NOTIFY_TEMPLATE = "notify_template";

    /**
     * 邮件账号的缓存
     * <p>
     * KEY 格式：mail_account:{id}
     * VALUE 数据格式：String 账号信息
     */
    String MAIL_ACCOUNT = "mail_account";

    /**
     * 邮件模版的缓存
     * <p>
     * KEY 格式：mail_template:{code}
     * VALUE 数据格式：String 模版信息
     */
    String MAIL_TEMPLATE = "mail_template";

    /**
     * 短信模版的缓存
     * <p>
     * KEY 格式：sms_template:{id}
     * VALUE 数据格式：String 模版信息
     */
    String SMS_TEMPLATE = "sms_template";

    /**
     * 小程序订阅模版的缓存
     *
     * KEY 格式：wxa_subscribe_template:{userType}
     * VALUE 数据格式 String, 模版信息
     */
    String WXA_SUBSCRIBE_TEMPLATE = "wxa_subscribe_template";

    // ========== 经营分析缓存相关 ==========

    /**
     * 经营分析汇总数据缓存
     * <p>
     * KEY 格式：business_analysis:{year}:{cutoffDate}
     * VALUE 数据类型：String 经营分析结果
     * 过期时间：10分钟
     */
    String BUSINESS_ANALYSIS = "business_analysis";

    /**
     * 部门经营分析缓存
     * <p>
     * KEY 格式：dept_analysis:{deptId}:{year}:{cutoffDate}
     * VALUE 数据类型：String 部门分析结果
     */
    String DEPT_ANALYSIS = "dept_analysis";

    /**
     * 员工成本缓存
     * <p>
     * KEY 格式：employee_cost:{userId}:{year}:{month}
     * VALUE 数据类型：BigDecimal 员工成本
     * 过期时间：30分钟
     */
    String EMPLOYEE_COST = "employee_cost";

    /**
     * 仪表板收入统计缓存
     * <p>
     * KEY 格式：dashboard_revenue:{userId}
     * VALUE 数据类型：String 收入统计结果
     * 过期时间：5分钟
     */
    String DASHBOARD_REVENUE = "dashboard_revenue";

    /**
     * 仪表板部门排行缓存
     * <p>
     * KEY 格式：dashboard_dept_ranking
     * VALUE 数据类型：String 部门排行结果
     * 过期时间：10分钟
     */
    String DASHBOARD_DEPT_RANKING = "dashboard_dept_ranking";

}
