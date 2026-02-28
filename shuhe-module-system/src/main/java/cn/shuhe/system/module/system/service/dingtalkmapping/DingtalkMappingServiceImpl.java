package cn.shuhe.system.module.system.service.dingtalkmapping;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.shuhe.system.module.system.controller.admin.dingtalkmapping.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping.DingtalkMappingDO;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.pojo.PageParam;
import cn.shuhe.system.framework.common.util.object.BeanUtils;

import cn.shuhe.system.module.system.dal.mysql.dingtalkmapping.DingtalkMappingMapper;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.convertList;
import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.diffList;
import static cn.shuhe.system.module.system.enums.ErrorCodeConstants.*;

/**
 * 钉钉数据映射 Service 实现类
 *
 * @author ShuHe
 */
@Service
@Validated
public class DingtalkMappingServiceImpl implements DingtalkMappingService {

    @Resource
    private DingtalkMappingMapper dingtalkMappingMapper;

    @Override
    public Long createDingtalkMapping(DingtalkMappingSaveReqVO createReqVO) {
        // 插入
        DingtalkMappingDO dingtalkMapping = BeanUtils.toBean(createReqVO, DingtalkMappingDO.class);
        dingtalkMappingMapper.insert(dingtalkMapping);

        // 返回
        return dingtalkMapping.getId();
    }

    @Override
    public void updateDingtalkMapping(DingtalkMappingSaveReqVO updateReqVO) {
        // 校验存在
        validateDingtalkMappingExists(updateReqVO.getId());
        // 更新
        DingtalkMappingDO updateObj = BeanUtils.toBean(updateReqVO, DingtalkMappingDO.class);
        dingtalkMappingMapper.updateById(updateObj);
    }

    @Override
    public void deleteDingtalkMapping(Long id) {
        // 校验存在
        validateDingtalkMappingExists(id);
        // 删除
        dingtalkMappingMapper.deleteById(id);
    }

    @Override
        public void deleteDingtalkMappingListByIds(List<Long> ids) {
        // 删除
        dingtalkMappingMapper.deleteByIds(ids);
        }


    private void validateDingtalkMappingExists(Long id) {
        if (dingtalkMappingMapper.selectById(id) == null) {
            throw exception(DINGTALK_MAPPING_NOT_EXISTS);
        }
    }

    @Override
    public DingtalkMappingDO getDingtalkMapping(Long id) {
        return dingtalkMappingMapper.selectById(id);
    }

    @Override
    public PageResult<DingtalkMappingDO> getDingtalkMappingPage(DingtalkMappingPageReqVO pageReqVO) {
        return dingtalkMappingMapper.selectPage(pageReqVO);
    }

}