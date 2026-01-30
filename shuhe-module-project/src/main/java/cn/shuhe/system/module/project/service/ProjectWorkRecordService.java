package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectWorkRecordPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectWorkRecordSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectWorkRecordDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 项目工作记录 Service 接口
 */
public interface ProjectWorkRecordService {

    /**
     * 创建工作记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createWorkRecord(@Valid ProjectWorkRecordSaveReqVO createReqVO);

    /**
     * 更新工作记录
     *
     * @param updateReqVO 更新信息
     */
    void updateWorkRecord(@Valid ProjectWorkRecordSaveReqVO updateReqVO);

    /**
     * 删除工作记录
     *
     * @param id 编号
     */
    void deleteWorkRecord(Long id);

    /**
     * 获取工作记录详情
     *
     * @param id 编号
     * @return 工作记录
     */
    ProjectWorkRecordDO getWorkRecord(Long id);

    /**
     * 分页查询工作记录
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<ProjectWorkRecordDO> getWorkRecordPage(ProjectWorkRecordPageReqVO pageReqVO);

    /**
     * 根据项目ID查询工作记录列表
     *
     * @param projectId 项目ID
     * @return 工作记录列表
     */
    List<ProjectWorkRecordDO> getWorkRecordListByProjectId(Long projectId);

    /**
     * 根据服务项ID查询工作记录列表
     *
     * @param serviceItemId 服务项ID
     * @return 工作记录列表
     */
    List<ProjectWorkRecordDO> getWorkRecordListByServiceItemId(Long serviceItemId);

    /**
     * 获取导出数据
     *
     * @param reqVO 查询条件
     * @return 工作记录列表
     */
    List<ProjectWorkRecordDO> getWorkRecordListForExport(ProjectWorkRecordPageReqVO reqVO);

    /**
     * 获取当前用户可见的项目列表（根据权限）
     * 管理层可以看到部门及下属部门的所有项目
     * 普通员工只能看到自己负责/参与的项目
     *
     * @return 项目列表（项目ID -> 项目名称）
     */
    List<ProjectWorkRecordDO> getMyProjects();

}
