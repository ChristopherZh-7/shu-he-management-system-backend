package cn.shuhe.system.module.system.service.dingtalkconfig;

import java.util.*;
import jakarta.validation.*;
import cn.shuhe.system.module.system.controller.admin.dingtalkconfig.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.pojo.PageParam;

/**
 * 钉钉配置 Service 接口
 *
 * @author 芋道源码
 */
public interface DingtalkConfigService {

    /**
     * 创建钉钉配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDingtalkConfig(@Valid DingtalkConfigSaveReqVO createReqVO);

    /**
     * 更新钉钉配置
     *
     * @param updateReqVO 更新信息
     */
    void updateDingtalkConfig(@Valid DingtalkConfigSaveReqVO updateReqVO);

    /**
     * 删除钉钉配置
     *
     * @param id 编号
     */
    void deleteDingtalkConfig(Long id);

    /**
    * 批量删除钉钉配置
    *
    * @param ids 编号
    */
    void deleteDingtalkConfigListByIds(List<Long> ids);

    /**
     * 获得钉钉配置
     *
     * @param id 编号
     * @return 钉钉配置
     */
    DingtalkConfigDO getDingtalkConfig(Long id);

    /**
     * 获得钉钉配置分页
     *
     * @param pageReqVO 分页查询
     * @return 钉钉配置分页
     */
    PageResult<DingtalkConfigDO> getDingtalkConfigPage(DingtalkConfigPageReqVO pageReqVO);

    /**
     * 获取启用状态的钉钉配置列表
     *
     * @return 启用的钉钉配置列表
     */
    List<DingtalkConfigDO> getEnabledDingtalkConfigList();

    /**
     * 同步钉钉部门
     *
     * @param configId 配置编号
     */
    void syncDingtalkDept(Long configId);

    /**
     * 测试钉钉API - 获取部门列表数据（仅查看，不同步）
     *
     * @param configId 配置编号
     * @return 钉钉部门列表
     */
    Object testDingtalkApi(Long configId);

    /**
     * 同步钉钉用户
     *
     * @param configId 配置编号
     */
    void syncDingtalkUser(Long configId);

}