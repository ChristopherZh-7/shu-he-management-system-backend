package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * 安全运营驻场点 DO
 * 
 * 一个安全运营合同可以有多个驻场点
 * 每个驻场点有自己的地址、联系人、服务要求等
 */
@TableName("security_operation_site")
@KeySequence("security_operation_site_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityOperationSiteDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

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

}
