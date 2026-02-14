package cn.shuhe.system.module.infra.service.logger;

import cn.shuhe.system.framework.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.common.util.string.StrUtils;


import cn.shuhe.system.module.infra.controller.admin.logger.vo.apierrorlog.ApiErrorLogPageReqVO;
import cn.shuhe.system.module.infra.dal.dataobject.logger.ApiErrorLogDO;
import cn.shuhe.system.module.infra.dal.mysql.logger.ApiErrorLogMapper;
import cn.shuhe.system.module.infra.enums.logger.ApiErrorLogProcessStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.infra.dal.dataobject.logger.ApiErrorLogDO.REQUEST_PARAMS_MAX_LENGTH;
import static cn.shuhe.system.module.infra.enums.ErrorCodeConstants.API_ERROR_LOG_NOT_FOUND;
import static cn.shuhe.system.module.infra.enums.ErrorCodeConstants.API_ERROR_LOG_PROCESSED;

/**
 * API 错误日志 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ApiErrorLogServiceImpl implements ApiErrorLogService {

    @Resource
    private ApiErrorLogMapper apiErrorLogMapper;

    @Override
    public void createApiErrorLog(ApiErrorLogCreateReqDTO createDTO) {
        ApiErrorLogDO apiErrorLog = BeanUtils.toBean(createDTO, ApiErrorLogDO.class)
                .setProcessStatus(ApiErrorLogProcessStatusEnum.INIT.getStatus());
        apiErrorLog.setRequestParams(StrUtils.maxLength(apiErrorLog.getRequestParams(), REQUEST_PARAMS_MAX_LENGTH));
        try {
            apiErrorLogMapper.insert(apiErrorLog);
        } catch (Exception ex) {
            // 兜底处理，目前只有 shuhe-cloud 会发生：https://gitee.com/shuhecode/shuhe-cloud-mini/issues/IC1O0A
            log.error("[createApiErrorLog][记录时({}) 发生异常]", createDTO, ex);
        }
    }

    @Override
    public PageResult<ApiErrorLogDO> getApiErrorLogPage(ApiErrorLogPageReqVO pageReqVO) {
        return apiErrorLogMapper.selectPage(pageReqVO);
    }

    @Override
    public ApiErrorLogDO getApiErrorLog(Long id) {
        return apiErrorLogMapper.selectById(id);
    }

    @Override
    public void updateApiErrorLogProcess(Long id, Integer processStatus, Long processUserId) {
        ApiErrorLogDO errorLog = apiErrorLogMapper.selectById(id);
        if (errorLog == null) {
            throw exception(API_ERROR_LOG_NOT_FOUND);
        }
        if (!ApiErrorLogProcessStatusEnum.INIT.getStatus().equals(errorLog.getProcessStatus())) {
            throw exception(API_ERROR_LOG_PROCESSED);
        }
        // 标记处理
        ApiErrorLogDO updateObj = new ApiErrorLogDO();
        updateObj.setId(id);
        updateObj.setProcessStatus(processStatus);
        updateObj.setProcessUserId(processUserId);
        updateObj.setProcessTime(LocalDateTime.now());
        apiErrorLogMapper.updateById(updateObj);
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public Integer cleanErrorLog(Integer exceedDay, Integer deleteLimit) {
        int count = 0;
        LocalDateTime expireDate = LocalDateTime.now().minusDays(exceedDay);
        // 循环删除，直到没有满足条件的数据
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            int deleteCount = apiErrorLogMapper.deleteByCreateTimeLt(expireDate, deleteLimit);
            count += deleteCount;
            // 达到删除预期条数，说明到底了
            if (deleteCount < deleteLimit) {
                break;
            }
        }
        return count;
    }

}
