package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.project.controller.admin.vo.DailyManagementRecordPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.DailyManagementRecordSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.DailyManagementRecordDO;
import cn.shuhe.system.module.project.dal.mysql.DailyManagementRecordMapper;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 日常管理记录 Service 实现类
 */
@Service
@Validated
@Slf4j
public class DailyManagementRecordServiceImpl implements DailyManagementRecordService {

    @Resource
    private DailyManagementRecordMapper dailyManagementRecordMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Override
    public Long createRecord(DailyManagementRecordSaveReqVO createReqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        
        // 检查是否已存在该周的记录
        DailyManagementRecordDO existRecord = dailyManagementRecordMapper.selectByCreatorAndYearAndWeek(
                String.valueOf(userId), createReqVO.getYear(), createReqVO.getWeekNumber());
        if (existRecord != null) {
            throw exception(DAILY_RECORD_ALREADY_EXISTS);
        }
        
        // 构建记录
        DailyManagementRecordDO record = BeanUtils.toBean(createReqVO, DailyManagementRecordDO.class);
        record.setWeekStartDate(LocalDate.parse(createReqVO.getWeekStartDate()));
        record.setWeekEndDate(LocalDate.parse(createReqVO.getWeekEndDate()));
        
        // 处理附件
        if (CollUtil.isNotEmpty(createReqVO.getAttachments())) {
            record.setAttachments(JSONUtil.toJsonStr(createReqVO.getAttachments()));
        }
        
        // 设置记录人信息
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user != null) {
            record.setCreatorName(user.getNickname());
            record.setDeptId(user.getDeptId());
            if (user.getDeptId() != null) {
                DeptRespDTO dept = deptApi.getDept(user.getDeptId());
                if (dept != null) {
                    record.setDeptName(dept.getName());
                }
            }
        }
        
        dailyManagementRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    public void updateRecord(DailyManagementRecordSaveReqVO updateReqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        
        // 检查记录是否存在
        DailyManagementRecordDO existRecord = dailyManagementRecordMapper.selectById(updateReqVO.getId());
        if (existRecord == null) {
            throw exception(DAILY_RECORD_NOT_EXISTS);
        }
        
        // 检查是否是记录所有者
        if (!String.valueOf(userId).equals(existRecord.getCreator())) {
            throw exception(DAILY_RECORD_UPDATE_NOT_OWNER);
        }
        
        // 更新记录
        DailyManagementRecordDO updateRecord = BeanUtils.toBean(updateReqVO, DailyManagementRecordDO.class);
        updateRecord.setWeekStartDate(LocalDate.parse(updateReqVO.getWeekStartDate()));
        updateRecord.setWeekEndDate(LocalDate.parse(updateReqVO.getWeekEndDate()));
        
        // 处理附件
        if (updateReqVO.getAttachments() != null) {
            updateRecord.setAttachments(JSONUtil.toJsonStr(updateReqVO.getAttachments()));
        }
        
        dailyManagementRecordMapper.updateById(updateRecord);
    }

    @Override
    public void deleteRecord(Long id) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        
        // 检查记录是否存在
        DailyManagementRecordDO existRecord = dailyManagementRecordMapper.selectById(id);
        if (existRecord == null) {
            throw exception(DAILY_RECORD_NOT_EXISTS);
        }
        
        // 检查是否是记录所有者
        if (!String.valueOf(userId).equals(existRecord.getCreator())) {
            throw exception(DAILY_RECORD_DELETE_NOT_OWNER);
        }
        
        dailyManagementRecordMapper.deleteById(id);
    }

    @Override
    public DailyManagementRecordDO getRecord(Long id) {
        return dailyManagementRecordMapper.selectById(id);
    }

    @Override
    public DailyManagementRecordDO getMyRecordByYearAndWeek(Integer year, Integer weekNumber) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return dailyManagementRecordMapper.selectByCreatorAndYearAndWeek(String.valueOf(userId), year, weekNumber);
    }

    @Override
    public PageResult<DailyManagementRecordDO> getRecordPage(DailyManagementRecordPageReqVO pageReqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        
        // 确定查询的部门范围
        Set<Long> deptIds = null;
        if (pageReqVO.getDeptId() != null) {
            deptIds = new HashSet<>();
            deptIds.add(pageReqVO.getDeptId());
            if (Boolean.TRUE.equals(pageReqVO.getIncludeSubDept())) {
                List<DeptRespDTO> childDepts = deptApi.getChildDeptList(pageReqVO.getDeptId());
                if (CollUtil.isNotEmpty(childDepts)) {
                    deptIds.addAll(childDepts.stream().map(DeptRespDTO::getId).collect(Collectors.toSet()));
                }
            }
        } else if (user != null && user.getDeptId() != null && Boolean.TRUE.equals(pageReqVO.getIncludeSubDept())) {
            // 默认查询自己部门及子部门
            deptIds = new HashSet<>();
            deptIds.add(user.getDeptId());
            List<DeptRespDTO> childDepts = deptApi.getChildDeptList(user.getDeptId());
            if (CollUtil.isNotEmpty(childDepts)) {
                deptIds.addAll(childDepts.stream().map(DeptRespDTO::getId).collect(Collectors.toSet()));
            }
        }
        
        return dailyManagementRecordMapper.selectPage(pageReqVO, deptIds);
    }

}
