package cn.shuhe.system.module.project.service;

import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.framework.common.biz.system.dict.dto.DictDataRespDTO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchMemberDO;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceLaunchMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceLaunchMemberMapper;
import cn.shuhe.system.module.system.api.dict.DictDataApi;
import cn.shuhe.system.module.system.api.dingtalk.DingtalkNotifyApi;
import cn.shuhe.system.module.system.service.dingtalk.ServiceLaunchConfirmCallback;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务发起确认回调实现
 */
@Service
@Slf4j
public class ServiceLaunchConfirmCallbackImpl implements ServiceLaunchConfirmCallback {

    @Resource
    private ServiceLaunchMemberMapper serviceLaunchMemberMapper;

    @Resource
    private ServiceLaunchMapper serviceLaunchMapper;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private DingtalkNotifyApi dingtalkNotifyApi;

    @Resource
    private DictDataApi dictDataApi;

    @Override
    public MemberInfo getMemberInfo(Long memberId) {
        ServiceLaunchMemberDO member = serviceLaunchMemberMapper.selectById(memberId);
        if (member == null) {
            return null;
        }
        
        MemberInfo info = new MemberInfo();
        info.setId(member.getId());
        info.setLaunchId(member.getLaunchId());
        info.setUserId(member.getUserId());
        info.setUserName(member.getUserName());
        info.setConfirmStatus(member.getConfirmStatus());
        info.setOaProcessInstanceId(member.getOaProcessInstanceId());
        return info;
    }

    @Override
    public LaunchInfo getLaunchInfo(Long launchId) {
        ServiceLaunchDO launch = serviceLaunchMapper.selectById(launchId);
        if (launch == null) {
            return null;
        }
        
        LaunchInfo info = new LaunchInfo();
        info.setId(launch.getId());
        info.setServiceItemId(launch.getServiceItemId());
        info.setDestination(launch.getDestination());
        info.setReason(launch.getReason());
        info.setPlanStartTime(launch.getPlanStartTime());
        info.setPlanEndTime(launch.getPlanEndTime());
        
        // 获取服务项名称和客户名称
        if (launch.getServiceItemId() != null) {
            ServiceItemDO serviceItem = serviceItemMapper.selectById(launch.getServiceItemId());
            if (serviceItem != null) {
                info.setServiceItemName(serviceItem.getName());
                info.setCustomerName(serviceItem.getCustomerName());
                info.setServiceType(serviceItem.getServiceType());
                // 获取服务类型的中文名称（从字典，需要根据部门类型选择正确的字典）
                info.setServiceTypeName(getServiceTypeLabel(serviceItem.getServiceType(), serviceItem.getDeptType()));
            }
        }
        
        return info;
    }

    /**
     * 部门类型对应的服务类型字典映射
     * 1 - 安全服务
     * 2 - 安全运营
     * 3 - 数据安全
     */
    private static final java.util.Map<Integer, String> DEPT_TYPE_SERVICE_DICT_MAP = java.util.Map.of(
            1, "project_service_type_security",
            2, "project_service_type_operation",
            3, "project_service_type_data"
    );

    /**
     * 获取服务类型的中文名称（从字典）
     * 
     * @param serviceType 服务类型代码（如 penetration_test）
     * @param deptType 部门类型（1-安全服务, 2-安全运营, 3-数据安全）
     * @return 服务类型中文名称（如 渗透测试）
     */
    private String getServiceTypeLabel(String serviceType, Integer deptType) {
        if (StrUtil.isEmpty(serviceType)) {
            return null;
        }
        
        // 根据部门类型获取对应的字典类型
        String dictType = deptType != null ? DEPT_TYPE_SERVICE_DICT_MAP.get(deptType) : null;
        if (StrUtil.isEmpty(dictType)) {
            log.warn("【服务发起确认回调】未知的部门类型，无法获取服务类型字典。deptType={}", deptType);
            return serviceType;
        }
        
        try {
            List<DictDataRespDTO> dictDataList = dictDataApi.getDictDataList(dictType);
            if (dictDataList != null) {
                for (DictDataRespDTO dictData : dictDataList) {
                    if (serviceType.equals(dictData.getValue())) {
                        return dictData.getLabel();
                    }
                }
            }
            log.warn("【服务发起确认回调】在字典 {} 中未找到服务类型 {}", dictType, serviceType);
        } catch (Exception e) {
            log.warn("【服务发起确认回调】获取服务类型中文名称失败，serviceType={}, dictType={}, error={}", 
                    serviceType, dictType, e.getMessage());
        }
        return serviceType; // 找不到则返回原值
    }

    @Override
    public void updateConfirmStatus(Long memberId, Integer confirmStatus, String oaProcessInstanceId) {
        ServiceLaunchMemberDO updateObj = new ServiceLaunchMemberDO();
        updateObj.setId(memberId);
        updateObj.setConfirmStatus(confirmStatus);
        updateObj.setConfirmTime(LocalDateTime.now());
        if (oaProcessInstanceId != null) {
            updateObj.setOaProcessInstanceId(oaProcessInstanceId);
        }
        serviceLaunchMemberMapper.updateById(updateObj);
        log.info("【服务发起确认回调】更新确认状态，memberId={}, confirmStatus={}", memberId, confirmStatus);
    }

    @Override
    public String getDingtalkUserId(Long userId) {
        return dingtalkNotifyApi.getDingtalkUserIdByLocalUserId(userId);
    }

}
