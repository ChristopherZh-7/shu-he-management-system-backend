package cn.shuhe.system.module.system.controller.admin.dingtalkmapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 钉钉数据映射 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DingtalkMappingRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "5104")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "钉钉配置ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "31125")
    @ExcelProperty("钉钉配置ID")
    private Long configId;

    @Schema(description = "类型（DEPT-部门，USER-用户）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("类型（DEPT-部门，USER-用户）")
    private String type;

    @Schema(description = "本地ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3825")
    @ExcelProperty("本地ID")
    private Long localId;

    @Schema(description = "钉钉ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10379")
    @ExcelProperty("钉钉ID")
    private String dingtalkId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}