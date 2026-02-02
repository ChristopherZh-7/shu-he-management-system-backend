package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * 项目驻场点 DO
 * 
 * 通用的驻场点管理，支持所有部门类型的项目：
 * - 安全服务（deptType=1）
 * - 安全运营（deptType=2）
 * - 数据安全（deptType=3）
 * 
 * 一个项目可以有多个驻场点，每个驻场点有自己的地址、联系人、服务要求等
 */
@TableName("project_site")
@KeySequence("project_site_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSiteDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 部门类型：1-安全服务 2-安全运营 3-数据安全
     * 
     * 同一个项目可能有多个部门同时驻场，每个部门独立管理自己的驻场点
     */
    private Integer deptType;

    // ========== 驻场点基本信息 ==========

    /**
     * 驻场点名称（如：客户总部、分公司A）
     */
    private String name;

    /**
     * 详细地址
     */
    private String address;

    // ========== 联系信息 ==========

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    // ========== 服务配置 ==========

    /**
     * 服务要求（如：24小时值班、门禁管理）
     */
    private String serviceRequirement;

    /**
     * 人员配置（需要驻场人数）
     */
    private Integer staffCount;

    // ========== 时间 ==========

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    // ========== 状态 ==========

    /**
     * 状态：0-停用 1-启用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序
     */
    private Integer sort;

    // ========== 状态常量 ==========

    /**
     * 停用
     */
    public static final int STATUS_DISABLED = 0;

    /**
     * 启用
     */
    public static final int STATUS_ENABLED = 1;

}
