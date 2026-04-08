package cn.shuhe.system.module.finance.service;

import cn.shuhe.system.module.finance.controller.admin.vo.FinanceAllocationRespVO;
import cn.shuhe.system.module.finance.controller.admin.vo.FinanceAllocationSaveReqVO;
import cn.shuhe.system.module.finance.controller.admin.vo.FinanceContractSummaryVO;
import cn.shuhe.system.module.finance.controller.admin.vo.FinanceInitAllocationReqVO;
import cn.shuhe.system.module.finance.dal.dataobject.FinanceAllocationDO;

import java.util.List;

public interface FinanceAllocationService {

    Long createAllocation(FinanceAllocationSaveReqVO createReqVO);

    void updateAllocation(FinanceAllocationSaveReqVO updateReqVO);

    void deleteAllocation(Long id);

    FinanceAllocationDO getAllocation(Long id);

    List<FinanceAllocationRespVO> getAllocationTree(Long contractId);

    List<FinanceAllocationRespVO> getChildAllocations(Long parentId);

    List<FinanceContractSummaryVO> getContractSummaryList();

    void initAllocationsFromContract(FinanceInitAllocationReqVO reqVO);

    void autoInitAllocationsFromContract(Long contractId);

}
