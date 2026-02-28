package cn.shuhe.system.module.system.service.dingtalkmapping;

import java.util.*;
import jakarta.validation.*;
import cn.shuhe.system.module.system.controller.admin.dingtalkmapping.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping.DingtalkMappingDO;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.pojo.PageParam;

/**
 * 钉钉数据映射 Service 接口
 *
 * @author ShuHe
 */
public interface DingtalkMappingService {

    /**
     * 创建钉钉数据映射
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDingtalkMapping(@Valid DingtalkMappingSaveReqVO createReqVO);

    /**
     * 更新钉钉数据映射
     *
     * @param updateReqVO 更新信息
     */
    void updateDingtalkMapping(@Valid DingtalkMappingSaveReqVO updateReqVO);

    /**
     * 删除钉钉数据映射
     *
     * @param id 编号
     */
    void deleteDingtalkMapping(Long id);

    /**
    * 批量删除钉钉数据映射
    *
    * @param ids 编号
    */
    void deleteDingtalkMappingListByIds(List<Long> ids);

    /**
     * 获得钉钉数据映射
     *
     * @param id 编号
     * @return 钉钉数据映射
     */
    DingtalkMappingDO getDingtalkMapping(Long id);

    /**
     * 获得钉钉数据映射分页
     *
     * @param pageReqVO 分页查询
     * @return 钉钉数据映射分页
     */
    PageResult<DingtalkMappingDO> getDingtalkMappingPage(DingtalkMappingPageReqVO pageReqVO);

}