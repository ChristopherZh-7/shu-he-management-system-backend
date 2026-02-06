package cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 钉钉通知场景配置新增/修改 Request VO")
@Data
public class DingtalkNotificationConfigSaveReqVO {

    @Schema(description = "配置编号", example = "1")
    private Long id;

    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "合同创建通知")
    @NotBlank(message = "配置名称不能为空")
    private String name;

    @Schema(description = "关联的机器人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "请选择机器人")
    private Long robotId;

    @Schema(description = "事件类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "contract_create")
    @NotBlank(message = "请选择事件类型")
    private String eventType;

    @Schema(description = "事件所属模块", requiredMode = Schema.RequiredMode.REQUIRED, example = "crm")
    @NotBlank(message = "请选择事件模块")
    private String eventModule;

    @Schema(description = "消息类型（text/markdown/link/actionCard）", example = "markdown")
    private String msgType;

    @Schema(description = "标题模板", example = "新合同创建通知")
    private String titleTemplate;

    @Schema(description = "内容模板", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "内容模板不能为空")
    private String contentTemplate;

    @Schema(description = "@类型（0-不@任何人 1-@负责人 2-@创建人 3-@指定人员 4-@所有人）", example = "1")
    private Integer atType;

    @Schema(description = "@的手机号列表（JSON数组）", example = "[\"138xxxx1234\"]")
    private String atMobiles;

    @Schema(description = "@的用户ID列表（JSON数组）", example = "[\"user001\"]")
    private String atUserIds;

    @Schema(description = "状态（0-启用 1-停用）", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "合同创建时自动通知")
    private String remark;

}
