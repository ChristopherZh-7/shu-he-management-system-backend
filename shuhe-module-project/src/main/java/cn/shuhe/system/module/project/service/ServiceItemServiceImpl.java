package cn.shuhe.system.module.project.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemBatchSaveReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemImportExcelVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemImportRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 服务项 Service 实现类
 */
@Service
@Validated
@Slf4j
public class ServiceItemServiceImpl implements ServiceItemService {

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createServiceItem(ServiceItemSaveReqVO createReqVO) {
        // 1. 生成服务项编号
        String code = generateServiceItemCode(createReqVO.getDeptType());

        // 2. 转换并保存
        ServiceItemDO serviceItem = BeanUtils.toBean(createReqVO, ServiceItemDO.class);
        serviceItem.setCode(code);
        serviceItem.setProgress(0); // 初始进度为0
        if (serviceItem.getStatus() == null) {
            serviceItem.setStatus(0); // 默认草稿状态
        }
        if (serviceItem.getPriority() == null) {
            serviceItem.setPriority(1); // 默认中优先级
        }
        // 如果未提供名称，自动生成
        if (StrUtil.isBlank(serviceItem.getName())) {
            serviceItem.setName(generateServiceItemName(createReqVO, code));
        }
        // 处理标签
        if (createReqVO.getTags() != null && !createReqVO.getTags().isEmpty()) {
            serviceItem.setTags(JSONUtil.toJsonStr(createReqVO.getTags()));
        }

        serviceItemMapper.insert(serviceItem);
        return serviceItem.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateServiceItem(ServiceItemSaveReqVO updateReqVO) {
        // 1. 校验存在
        validateServiceItemExists(updateReqVO.getId());

        // 2. 更新
        ServiceItemDO updateObj = BeanUtils.toBean(updateReqVO, ServiceItemDO.class);
        // 处理标签
        if (updateReqVO.getTags() != null) {
            updateObj.setTags(JSONUtil.toJsonStr(updateReqVO.getTags()));
        }
        serviceItemMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteServiceItem(Long id) {
        // 1. 校验存在
        validateServiceItemExists(id);

        // 2. 删除
        serviceItemMapper.deleteById(id);
    }

    @Override
    public ServiceItemDO getServiceItem(Long id) {
        return serviceItemMapper.selectById(id);
    }

    @Override
    public PageResult<ServiceItemDO> getServiceItemPage(ServiceItemPageReqVO pageReqVO) {
        return serviceItemMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ServiceItemDO> getServiceItemListByProjectId(Long projectId) {
        return serviceItemMapper.selectListByProjectId(projectId);
    }

    @Override
    public List<ServiceItemDO> getServiceItemListByProjectIdAndDeptId(Long projectId, Long deptId) {
        return serviceItemMapper.selectListByProjectIdAndDeptId(projectId, deptId);
    }

    @Override
    public void updateServiceItemStatus(Long id, Integer status) {
        // 1. 校验存在
        ServiceItemDO serviceItem = validateServiceItemExists(id);

        // 2. 更新状态
        ServiceItemDO updateObj = new ServiceItemDO();
        updateObj.setId(id);
        updateObj.setStatus(status);

        // 如果状态变为进行中，且实际开始时间为空，则设置实际开始时间
        if (status == 1 && serviceItem.getActualStartTime() == null) {
            updateObj.setActualStartTime(LocalDateTime.now());
        }
        // 如果状态变为已完成，则设置实际结束时间和进度100%
        if (status == 3) {
            updateObj.setActualEndTime(LocalDateTime.now());
            updateObj.setProgress(100);
        }

        serviceItemMapper.updateById(updateObj);
    }

    @Override
    public void updateServiceItemProgress(Long id, Integer progress) {
        // 1. 校验存在
        validateServiceItemExists(id);

        // 2. 更新进度
        ServiceItemDO updateObj = new ServiceItemDO();
        updateObj.setId(id);
        updateObj.setProgress(progress);
        serviceItemMapper.updateById(updateObj);
    }

    /**
     * 校验服务项是否存在
     */
    private ServiceItemDO validateServiceItemExists(Long id) {
        ServiceItemDO serviceItem = serviceItemMapper.selectById(id);
        if (serviceItem == null) {
            throw exception(SERVICE_ITEM_NOT_EXISTS);
        }
        return serviceItem;
    }

    /**
     * 生成服务项编号
     * 格式：SVC-{部门类型}-{年月日}-{4位随机数}
     * 例如：SVC-1-20260116-0001
     */
    private String generateServiceItemCode(Integer deptType) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return StrUtil.format("SVC-{}-{}-{}", deptType, date, random);
    }

    /**
     * 自动生成服务项名称
     * 格式：{客户名称}-{服务类型}-{编号后4位} 或 服务项-{编号后4位}
     */
    private String generateServiceItemName(ServiceItemSaveReqVO reqVO, String code) {
        StringBuilder name = new StringBuilder();
        // 添加客户名称
        if (StrUtil.isNotBlank(reqVO.getCustomerName())) {
            name.append(reqVO.getCustomerName()).append("-");
        }
        // 添加服务类型
        if (StrUtil.isNotBlank(reqVO.getServiceType())) {
            name.append(reqVO.getServiceType()).append("-");
        }
        // 如果没有任何信息，使用默认前缀
        if (name.isEmpty()) {
            name.append("服务项-");
        }
        // 添加编号后4位作为唯一标识
        name.append(code.substring(code.length() - 4));
        return name.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchCreateServiceItem(ServiceItemBatchSaveReqVO batchReqVO, Long deptId) {
        List<Long> createdIds = new ArrayList<>();

        for (ServiceItemSaveReqVO item : batchReqVO.getItems()) {
            // 设置公共字段
            item.setProjectId(batchReqVO.getProjectId());
            item.setDeptType(batchReqVO.getDeptType());
            item.setDeptId(deptId);

            // 复用单条创建逻辑
            Long id = createServiceItem(item);
            createdIds.add(id);
        }

        return createdIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceItemImportRespVO importServiceItemList(Long projectId, Integer deptType,
            List<ServiceItemImportExcelVO> list, Long deptId) {
        int createCount = 0;
        Map<Integer, String> failureRecords = new LinkedHashMap<>();

        for (int i = 0; i < list.size(); i++) {
            ServiceItemImportExcelVO excelVO = list.get(i);
            int rowNum = i + 2; // Excel行号从2开始（第1行是表头）

            try {
                // 校验必填字段
                if (StrUtil.isBlank(excelVO.getName())) {
                    failureRecords.put(rowNum, "服务项名称不能为空");
                    continue;
                }

                // 构建保存VO
                ServiceItemSaveReqVO saveReqVO = new ServiceItemSaveReqVO();
                saveReqVO.setProjectId(projectId);
                saveReqVO.setDeptType(deptType);
                saveReqVO.setDeptId(deptId);
                saveReqVO.setName(excelVO.getName());
                saveReqVO.setServiceType(excelVO.getServiceType());
                saveReqVO.setCustomerName(excelVO.getCustomerName());
                saveReqVO.setPriority(excelVO.getPriority());
                saveReqVO.setRemark(excelVO.getRemark());

                // 处理时间字段
                if (StrUtil.isNotBlank(excelVO.getPlanStartTime())) {
                    try {
                        saveReqVO.setPlanStartTime(LocalDateTime.parse(excelVO.getPlanStartTime(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    } catch (Exception e) {
                        log.warn("解析计划开始时间失败: {}", excelVO.getPlanStartTime());
                    }
                }
                if (StrUtil.isNotBlank(excelVO.getPlanEndTime())) {
                    try {
                        saveReqVO.setPlanEndTime(LocalDateTime.parse(excelVO.getPlanEndTime(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    } catch (Exception e) {
                        log.warn("解析计划结束时间失败: {}", excelVO.getPlanEndTime());
                    }
                }

                // 创建服务项
                createServiceItem(saveReqVO);
                createCount++;
            } catch (Exception e) {
                failureRecords.put(rowNum, e.getMessage());
                log.error("导入服务项失败，行号: {}, 原因: {}", rowNum, e.getMessage());
            }
        }

        return ServiceItemImportRespVO.builder()
                .createCount(createCount)
                .failureCount(failureRecords.size())
                .failureRecords(failureRecords)
                .build();
    }

}
