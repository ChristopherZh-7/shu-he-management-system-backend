package cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 钉钉群机器人 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DingtalkRobotRespVO {

    @Schema(description = "机器人编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("机器人编号")
    private Long id;

    @Schema(description = "机器人名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "项目通知机器人")
    @ExcelProperty("机器人名称")
    private String name;

    @Schema(description = "Webhook地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("Webhook地址")
    private String webhookUrl;

    @Schema(description = "加签密钥")
    private String secret;

    @Schema(description = "安全类型（1-关键词 2-加签 3-IP白名单）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("安全类型")
    private Integer securityType;

    @Schema(description = "关键词列表（JSON数组）")
    private String keywords;

    @Schema(description = "状态（0-正常 1-停用）", example = "0")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "用于项目进度通知")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
