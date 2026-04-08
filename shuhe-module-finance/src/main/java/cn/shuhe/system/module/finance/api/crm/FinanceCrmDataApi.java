package cn.shuhe.system.module.finance.api.crm;

import cn.shuhe.system.module.finance.api.crm.dto.ContractAllocationsDTO;
import cn.shuhe.system.module.finance.api.crm.dto.ContractSummaryDTO;

import java.util.List;

/**
 * Finance → CRM 数据 API
 *
 * 用于解耦 Finance 模块对 CRM 模块的直接依赖。
 * 接口定义在 finance 模块，实现在 crm 模块。
 */
public interface FinanceCrmDataApi {

    /**
     * 获取当前用户可见的合同列表（含客户名、商机名、负责人等关联信息）
     */
    List<ContractSummaryDTO> getContractListForFinance(Long userId);

    /**
     * 获取合同的部门分配信息（用于自动初始化财务分配）
     */
    ContractAllocationsDTO getContractAllocations(Long contractId);
}
