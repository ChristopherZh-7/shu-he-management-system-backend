package cn.shuhe.system.module.crm.controller.admin.product.vo.product;

import cn.shuhe.system.framework.excel.core.annotations.DictFormat;
import cn.shuhe.system.framework.excel.core.convert.DictConvert;
import cn.shuhe.system.module.crm.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.fhs.core.trans.vo.VO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CRM 服务项 Response VO（简化版）")
@Data
@ExcelIgnoreUnannotated
public class CrmProductRespVO implements VO {

    @Schema(description = "服务项编号", example = "20529")
    @ExcelProperty("服务项编号")
    private Long id;

    @Schema(description = "服务项名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "渗透测试")
    @ExcelProperty("服务项名称")
    private String name;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "上架")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.CRM_PRODUCT_STATUS)
    private Integer status;

    @Schema(description = "服务项描述", example = "安全服务描述")
    @ExcelProperty("服务项描述")
    private String description;

    @Schema(description = "负责人的用户编号", example = "31926")
    @Trans(type = TransType.SIMPLE, targetClassName = "cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO",
            fields = "nickname", ref = "ownerUserName")
    private Long ownerUserId;
    @Schema(description = "负责人的用户昵称", example = "戍合")
    @ExcelProperty("负责人")
    private String ownerUserName;

    @Schema(description = "创建人编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @Trans(type = TransType.SIMPLE, targetClassName = "cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO",
            fields = "nickname", ref = "creatorName")
    private String creator;
    @Schema(description = "创建人名字", requiredMode = Schema.RequiredMode.REQUIRED, example = "戍合")
    @ExcelProperty("创建人")
    private String creatorName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

}
