package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * 项目驻场人员 DO
 * 
 * 通用的驻场人员管理，支持所有部门类型的项目
 * 人员类型：1-管理人员 2-驻场人员
 */
@TableName("project_site_member")
@KeySequence("project_site_member_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSiteMemberDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 驻场点ID
     */
    private Long siteId;

    /**
     * 项目ID（冗余，便于查询）
     */
    private Long projectId;

    /**
     * 部门类型：1-安全服务 2-安全运营 3-数据安全（冗余，便于查询）
     */
    private Integer deptType;

    // ========== 人员信息 ==========

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户姓名（冗余）
     */
    private String userName;

    // ========== 人员类型 ==========

    /**
     * 人员类型：1-管理人员 2-驻场人员
     */
    private Integer memberType;

    /**
     * 是否项目负责人：0-否 1-是
     */
    private Integer isLeader;

    // ========== 岗位信息 ==========

    /**
     * 岗位代码
     */
    private String positionCode;

    /**
     * 岗位名称
     */
    private String positionName;

    // ========== 时间 ==========

    /**
     * 入场日期
     */
    private LocalDate startDate;

    /**
     * 离开日期
     */
    private LocalDate endDate;

    // ========== 状态 ==========

    /**
     * 状态：0-待入场 1-在岗 2-已离开
     */
    private Integer status;

    // ========== 备注 ==========

    /**
     * 备注
     */
    private String remark;

    // ========== 人员类型常量 ==========

    /**
     * 管理人员
     */
    public static final int MEMBER_TYPE_MANAGEMENT = 1;

    /**
     * 驻场人员
     */
    public static final int MEMBER_TYPE_ONSITE = 2;

    // ========== 状态常量 ==========

    /**
     * 待入场
     */
    public static final int STATUS_PENDING = 0;

    /**
     * 在岗
     */
    public static final int STATUS_ACTIVE = 1;

    /**
     * 已离开
     */
    public static final int STATUS_LEFT = 2;

    // ========== 负责人常量 ==========

    /**
     * 不是项目负责人
     */
    public static final int IS_LEADER_NO = 0;

    /**
     * 是项目负责人
     */
    public static final int IS_LEADER_YES = 1;

}
