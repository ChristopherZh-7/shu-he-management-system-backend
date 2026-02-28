package cn.shuhe.system.module.system.api.dept.dto;

import cn.shuhe.system.framework.common.enums.CommonStatusEnum;
import lombok.Data;

/**
 * 部门 Response DTO
 *
 * @author ShuHe
 */
@Data
public class DeptRespDTO {

    /**
     * 部门编号
     */
    private Long id;
    /**
     * 部门名称
     */
    private String name;
    /**
     * 父部门编号
     */
    private Long parentId;
    /**
     * 负责人的用户编号
     */
    private Long leaderUserId;
    /**
     * 负责人名称（用于显示）
     */
    private String leaderUserName;
    /**
     * 部门状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;

    /**
     * 部门类型
     *
     * 1-安全服务 2-安全运营 3-数据安全
     */
    private Integer deptType;

    /**
     * 工作模式
     * null-继承上级 1-驻场 2-二线
     */
    private Integer workMode;

}
