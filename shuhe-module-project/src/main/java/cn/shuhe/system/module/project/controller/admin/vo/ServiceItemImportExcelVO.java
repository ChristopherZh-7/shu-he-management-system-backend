package cn.shuhe.system.module.project.controller.admin.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务项 Excel 导入 VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceItemImportExcelVO {

    @ExcelProperty("服务项名称")
    private String name;

    @ExcelProperty("服务类型")
    private String serviceType;

    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("计划开始时间")
    private String planStartTime;

    @ExcelProperty("计划结束时间")
    private String planEndTime;

    @ExcelProperty("备注")
    private String remark;

}
