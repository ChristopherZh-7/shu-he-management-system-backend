package cn.shuhe.system.module.system.dal.dataobject.dashboard;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 仪表板配置 DO
 * 
 * 用于存储用户自定义的仪表板布局配置
 */
@TableName("system_dashboard_config")
@KeySequence("system_dashboard_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardConfigDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 页面类型
     * 
     * workspace - 工作台
     * analytics - 分析页
     */
    private String pageType;

    /**
     * 布局配置 JSON
     * 
     * 存储格式：
     * [
     *   { "i": "widget-1", "x": 0, "y": 0, "w": 3, "h": 2, "componentType": "stat-project", "props": {} },
     *   ...
     * ]
     */
    private String layoutConfig;

}
