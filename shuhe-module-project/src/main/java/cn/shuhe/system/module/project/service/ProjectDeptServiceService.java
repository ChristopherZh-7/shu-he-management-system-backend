package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectDeptServicePageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectDeptServiceSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptServiceDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 项目-部门服务单 Service 接口
 * 
 * 用于解决一个项目被多个部门服务时，负责人、状态、进度等字段冲突的问题。
 */
public interface ProjectDeptServiceService {

    /**
     * 创建部门服务单
     *
     * @param createReqVO 创建信息
     * @return 部门服务单ID
     */
    Long createDeptService(@Valid ProjectDeptServiceSaveReqVO createReqVO);

    /**
     * 更新部门服务单
     *
     * @param updateReqVO 更新信息
     */
    void updateDeptService(@Valid ProjectDeptServiceSaveReqVO updateReqVO);

    /**
     * 删除部门服务单
     *
     * @param id 部门服务单ID
     */
    void deleteDeptService(Long id);

    /**
     * 获得部门服务单
     *
     * @param id 部门服务单ID
     * @return 部门服务单
     */
    ProjectDeptServiceDO getDeptService(Long id);

    /**
     * 获得部门服务单分页
     *
     * @param pageReqVO 分页查询
     * @return 部门服务单分页
     */
    PageResult<ProjectDeptServiceDO> getDeptServicePage(ProjectDeptServicePageReqVO pageReqVO);

    /**
     * 根据项目ID获取部门服务单列表
     *
     * @param projectId 项目ID
     * @return 部门服务单列表
     */
    List<ProjectDeptServiceDO> getDeptServiceListByProjectId(Long projectId);

    /**
     * 根据项目ID和部门类型获取部门服务单
     *
     * @param projectId 项目ID
     * @param deptType  部门类型
     * @return 部门服务单
     */
    ProjectDeptServiceDO getDeptServiceByProjectIdAndDeptType(Long projectId, Integer deptType);

    /**
     * 根据合同ID获取部门服务单列表
     *
     * @param contractId 合同ID
     * @return 部门服务单列表
     */
    List<ProjectDeptServiceDO> getDeptServiceListByContractId(Long contractId);

    /**
     * 根据合同ID和部门类型获取部门服务单
     *
     * @param contractId 合同ID
     * @param deptType   部门类型
     * @return 部门服务单
     */
    ProjectDeptServiceDO getDeptServiceByContractIdAndDeptType(Long contractId, Integer deptType);

    /**
     * 更新部门服务单状态
     *
     * @param id     部门服务单ID
     * @param status 状态
     */
    void updateDeptServiceStatus(Long id, Integer status);

    /**
     * 设置部门服务单负责人
     *
     * @param id           部门服务单ID
     * @param managerIds   负责人ID列表
     * @param managerNames 负责人姓名列表
     */
    void setDeptServiceManagers(Long id, List<Long> managerIds, List<String> managerNames);

    /**
     * 领取部门服务单
     *
     * @param id       部门服务单ID
     * @param deptId   部门ID
     * @param deptName 部门名称
     * @param userId   领取人ID
     * @param userName 领取人姓名
     */
    void claimDeptService(Long id, Long deptId, String deptName, Long userId, String userName);

    /**
     * 批量创建部门服务单（合同审批通过时使用）
     *
     * @param projectId    项目ID
     * @param contractId   合同ID
     * @param contractNo   合同编号
     * @param customerId   客户ID
     * @param customerName 客户名称
     * @param deptTypes    部门类型列表
     * @return 创建的部门服务单列表
     */
    List<ProjectDeptServiceDO> batchCreateDeptService(Long projectId, Long contractId, String contractNo,
                                                       Long customerId, String customerName, List<Integer> deptTypes);

}
