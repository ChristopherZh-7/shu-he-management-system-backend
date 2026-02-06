package cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 钉钉通知场景配置 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DingtalkNotificationConfigRespVO {

    @Schema(description = "配置编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("配置编号")
    private Long id;

    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "合同创建通知")
    @ExcelProperty("配置名称")
    private String name;

    @Schema(description = "关联的机器人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long robotId;

    @Schema(description = "机器人名称")
    @ExcelProperty("机器人名称")
    private String robotName;

    @Schema(description = "事件类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "contract_create")
    @ExcelProperty("事件类型")
    private String eventType;

    @Schema(description = "事件所属模块", requiredMode = Schema.RequiredMode.REQUIRED, example = "crm")
    @ExcelProperty("事件模块")
    private String eventModule;

    @Schema(description = "消息类型", example = "markdown")
    @ExcelProperty("消息类型")
    private String msgType;

    @Schema(description = "标题模板", example = "新合同创建通知")
    private String titleTemplate;

    @Schema(description = "内容模板")
    private String contentTemplate;

    @Schema(description = "@类型", example = "1")
    @ExcelProperty("@类型")
    private Integer atType;

    @Schema(description = "@的手机号列表")
    private String atMobiles;

    @Schema(description = "@的用户ID列表")
    private String atUserIds;

    @Schema(description = "状态（0-启用 1-停用）", example = "0")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "合同创建时自动通知")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
