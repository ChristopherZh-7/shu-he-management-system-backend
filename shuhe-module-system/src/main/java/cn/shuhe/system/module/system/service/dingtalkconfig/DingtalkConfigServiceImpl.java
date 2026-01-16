package cn.shuhe.system.module.system.service.dingtalkconfig;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.framework.common.enums.CommonStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import cn.hutool.core.util.StrUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.HashSet;

import cn.shuhe.system.module.system.controller.admin.dingtalkconfig.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping.DingtalkMappingDO;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.pojo.PageParam;
import cn.shuhe.system.framework.common.util.object.BeanUtils;

import cn.shuhe.system.module.system.dal.mysql.dingtalkconfig.DingtalkConfigMapper;
import cn.shuhe.system.module.system.dal.mysql.dept.DeptMapper;
import cn.shuhe.system.module.system.dal.mysql.dept.PostMapper;
import cn.shuhe.system.module.system.dal.mysql.dingtalkmapping.DingtalkMappingMapper;
import cn.shuhe.system.module.system.dal.mysql.user.AdminUserMapper;
import cn.shuhe.system.module.system.dal.mysql.dept.UserPostMapper;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.dal.dataobject.dept.PostDO;
import cn.shuhe.system.module.system.dal.dataobject.dept.UserPostDO;
import org.springframework.security.crypto.password.PasswordEncoder;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.convertList;
import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.diffList;
import static cn.shuhe.system.module.system.enums.ErrorCodeConstants.*;

/**
 * 钉钉配置 Service 实现类
 *
 * @author 芋道源码
 */
@Slf4j
@Service
@Validated
public class DingtalkConfigServiceImpl implements DingtalkConfigService {

    private static final String MAPPING_TYPE_DEPT = "DEPT";
    private static final String MAPPING_TYPE_USER = "USER";
    
    /**
     * 标准化手机号格式
     * 钉钉返回的手机号可能带有国际区号前缀（如 +86-17683991468）
     * 需要去掉前缀，只保留纯手机号
     */
    private String normalizeMobile(String mobile) {
        if (StrUtil.isEmpty(mobile)) {
            return mobile;
        }
        // 去掉 +86- 或 +86 前缀
        if (mobile.startsWith("+86-")) {
            return mobile.substring(4);
        }
        if (mobile.startsWith("+86")) {
            return mobile.substring(3);
        }
        // 去掉其他国际区号格式（如 86-）
        if (mobile.startsWith("86-")) {
            return mobile.substring(3);
        }
        return mobile;
    }
    
    /** 在职状态: 1-在职, 2-离职 */
    private static final Integer EMPLOYEE_STATUS_ON_JOB = 1;
    private static final Integer EMPLOYEE_STATUS_DIMISSION = 2;

    @Resource
    private DingtalkConfigMapper dingtalkConfigMapper;
    
    @Resource
    private DeptMapper deptMapper;
    
    @Resource
    private DingtalkMappingMapper dingtalkMappingMapper;
    
    @Resource
    private AdminUserMapper adminUserMapper;
    
    @Resource
    private PostMapper postMapper;

    @Resource
    private UserPostMapper userPostMapper;
    
    @Resource
    private PasswordEncoder passwordEncoder;
    
    @Resource
    private DingtalkApiService dingtalkApiService;

    @Override
    public Long createDingtalkConfig(DingtalkConfigSaveReqVO createReqVO) {
        // 插入
        DingtalkConfigDO dingtalkConfig = BeanUtils.toBean(createReqVO, DingtalkConfigDO.class);
        dingtalkConfigMapper.insert(dingtalkConfig);

        // 返回
        return dingtalkConfig.getId();
    }

    @Override
    public void updateDingtalkConfig(DingtalkConfigSaveReqVO updateReqVO) {
        // 校验存在
        validateDingtalkConfigExists(updateReqVO.getId());
        // 更新
        DingtalkConfigDO updateObj = BeanUtils.toBean(updateReqVO, DingtalkConfigDO.class);
        dingtalkConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteDingtalkConfig(Long id) {
        // 校验存在
        validateDingtalkConfigExists(id);
        // 删除
        dingtalkConfigMapper.deleteById(id);
    }

    @Override
        public void deleteDingtalkConfigListByIds(List<Long> ids) {
        // 删除
        dingtalkConfigMapper.deleteByIds(ids);
        }


    @Override
    public DingtalkConfigDO getDingtalkConfig(Long id) {
        return dingtalkConfigMapper.selectById(id);
    }

    @Override
    public PageResult<DingtalkConfigDO> getDingtalkConfigPage(DingtalkConfigPageReqVO pageReqVO) {
        return dingtalkConfigMapper.selectPage(pageReqVO);
    }

    @Override
    public List<DingtalkConfigDO> getEnabledDingtalkConfigList() {
        return dingtalkConfigMapper.selectList(DingtalkConfigDO::getStatus, 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncDingtalkDept(Long configId) {
        // 1. 校验配置存在
        DingtalkConfigDO config = validateDingtalkConfigExists(configId);
        log.info("开始同步钉钉部门，配置ID: {}, 配置名称: {}", configId, config.getName());
        
        try {
            // 2. 获取钉钉access_token
            String accessToken = dingtalkApiService.getAccessToken(config);
            log.info("获取钉钉access_token成功");
            
            // 3. 获取钉钉部门列表（从根部门1开始递归获取）
            List<DingtalkApiService.DingtalkDept> dingtalkDepts = dingtalkApiService.getDeptList(accessToken, 1L);
            log.info("获取钉钉部门列表成功，共 {} 个部门", dingtalkDepts.size());
            
            // 4. 获取当前配置的所有部门映射
            List<DingtalkMappingDO> existingMappings = dingtalkMappingMapper.selectList(
                    DingtalkMappingDO::getConfigId, configId,
                    DingtalkMappingDO::getType, MAPPING_TYPE_DEPT
            );
            Map<String, DingtalkMappingDO> mappingByDingtalkId = existingMappings.stream()
                    .collect(Collectors.toMap(DingtalkMappingDO::getDingtalkId, m -> m));
            
            // 5. 存储钉钉部门ID到本地部门ID的映射（用于处理父部门ID）
            Map<Long, Long> dingtalkIdToLocalId = new HashMap<>();
            
            // 先处理根部门映射（钉钉根部门ID为1，对应本地根部门ID为0或1）
            // 假设本地根部门ID为0
            dingtalkIdToLocalId.put(1L, 0L);
            
            // 6. 按层级同步部门（先同步父部门，再同步子部门）
            // 按父部门ID排序，确保父部门先被创建
            dingtalkDepts.sort(Comparator.comparingLong(DingtalkApiService.DingtalkDept::getParentId));
            
            int createCount = 0;
            int updateCount = 0;
            
            for (DingtalkApiService.DingtalkDept dingtalkDept : dingtalkDepts) {
                String dingtalkDeptId = String.valueOf(dingtalkDept.getDeptId());
                DingtalkMappingDO mapping = mappingByDingtalkId.get(dingtalkDeptId);
                
                // 获取本地父部门ID
                Long localParentId = dingtalkIdToLocalId.getOrDefault(dingtalkDept.getParentId(), 0L);
                
                if (mapping != null) {
                    // 更新已有部门
                    DeptDO existingDept = deptMapper.selectById(mapping.getLocalId());
                    if (existingDept != null) {
                        existingDept.setName(dingtalkDept.getName());
                        existingDept.setParentId(localParentId);
                        existingDept.setSort(dingtalkDept.getOrder());
                        deptMapper.updateById(existingDept);
                        dingtalkIdToLocalId.put(dingtalkDept.getDeptId(), existingDept.getId());
                        updateCount++;
                    }
                } else {
                    // 创建新部门
                    DeptDO newDept = new DeptDO();
                    newDept.setName(dingtalkDept.getName());
                    newDept.setParentId(localParentId);
                    newDept.setSort(dingtalkDept.getOrder());
                    newDept.setStatus(CommonStatusEnum.ENABLE.getStatus());
                    deptMapper.insert(newDept);
                    
                    // 创建映射关系
                    DingtalkMappingDO newMapping = DingtalkMappingDO.builder()
                            .configId(configId)
                            .type(MAPPING_TYPE_DEPT)
                            .localId(newDept.getId())
                            .dingtalkId(dingtalkDeptId)
                            .build();
                    dingtalkMappingMapper.insert(newMapping);
                    
                    dingtalkIdToLocalId.put(dingtalkDept.getDeptId(), newDept.getId());
                    createCount++;
                }
            }
            
            // 7. 更新配置的最后同步时间和结果
            config.setLastSyncTime(LocalDateTime.now());
            config.setLastSyncResult(String.format("同步成功：新增 %d 个部门，更新 %d 个部门", createCount, updateCount));
            dingtalkConfigMapper.updateById(config);
            
            log.info("钉钉部门同步完成：新增 {} 个，更新 {} 个", createCount, updateCount);
            
        } catch (Exception e) {
            log.error("钉钉部门同步失败", e);
            // 更新同步失败结果
            config.setLastSyncTime(LocalDateTime.now());
            config.setLastSyncResult("同步失败：" + e.getMessage());
            dingtalkConfigMapper.updateById(config);
            throw new RuntimeException("钉钉部门同步失败：" + e.getMessage(), e);
        }
    }

    private DingtalkConfigDO validateDingtalkConfigExists(Long id) {
        DingtalkConfigDO config = dingtalkConfigMapper.selectById(id);
        if (config == null) {
            throw exception(DINGTALK_CONFIG_NOT_EXISTS);
        }
        return config;
    }

    @Override
    public Object testDingtalkApi(Long configId) {
        // 校验配置存在
        DingtalkConfigDO config = validateDingtalkConfigExists(configId);
        
        // 获取钉钉access_token
        String accessToken = dingtalkApiService.getAccessToken(config);
        
        // 获取钉钉部门列表
        List<DingtalkApiService.DingtalkDept> depts = dingtalkApiService.getDeptList(accessToken, 1L);
        
        // 返回测试结果
        Map<String, Object> result = new HashMap<>();
        result.put("configName", config.getName());
        result.put("accessToken", accessToken.substring(0, 10) + "***"); // 隐藏部分token
        result.put("deptCount", depts.size());
        result.put("depts", depts);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncDingtalkUser(Long configId) {
        // 1. 校验配置存在
        DingtalkConfigDO config = validateDingtalkConfigExists(configId);
        log.info("开始同步钉钉用户，配置ID: {}, 配置名称: {}", configId, config.getName());
        
        try {
            // 2. 获取钉钉access_token
            String accessToken = dingtalkApiService.getAccessToken(config);
            log.info("获取钉钉access_token成功");
            
            // 3. 获取钉钉部门列表（需要遍历每个部门获取用户）
            List<DingtalkApiService.DingtalkDept> depts = dingtalkApiService.getDeptList(accessToken, 1L);
            
            // 4. 获取当前配置的所有用户映射
            List<DingtalkMappingDO> existingMappings = dingtalkMappingMapper.selectList(
                    DingtalkMappingDO::getConfigId, configId,
                    DingtalkMappingDO::getType, MAPPING_TYPE_USER
            );
            Map<String, DingtalkMappingDO> mappingByDingtalkId = existingMappings.stream()
                    .collect(Collectors.toMap(DingtalkMappingDO::getDingtalkId, m -> m));
            
            // 5. 获取部门映射（钉钉部门ID -> 本地部门ID）
            List<DingtalkMappingDO> deptMappings = dingtalkMappingMapper.selectList(
                    DingtalkMappingDO::getConfigId, configId,
                    DingtalkMappingDO::getType, MAPPING_TYPE_DEPT
            );
            Map<String, Long> deptIdMapping = deptMappings.stream()
                    .collect(Collectors.toMap(DingtalkMappingDO::getDingtalkId, DingtalkMappingDO::getLocalId));
            
            int createCount = 0;
            int updateCount = 0;
            int dimissionCreateCount = 0;
            int dimissionUpdateCount = 0;
            Set<String> processedUserIds = new HashSet<>();
            
            // 6. 遍历每个部门获取在职用户
            for (DingtalkApiService.DingtalkDept dept : depts) {
                List<DingtalkApiService.DingtalkUser> users = dingtalkApiService.getUserList(accessToken, dept.getDeptId());
                
                for (DingtalkApiService.DingtalkUser dingtalkUser : users) {
                    // 避免重复处理（用户可能属于多个部门）
                    if (processedUserIds.contains(dingtalkUser.getUserid())) {
                        continue;
                    }
                    processedUserIds.add(dingtalkUser.getUserid());
                    
                    DingtalkMappingDO mapping = mappingByDingtalkId.get(dingtalkUser.getUserid());
                    
                    // 获取用户的第一个部门的本地ID
                    Long localDeptId = null;
                    if (dingtalkUser.getDeptIdList() != null && !dingtalkUser.getDeptIdList().isEmpty()) {
                        String firstDeptId = String.valueOf(dingtalkUser.getDeptIdList().get(0));
                        localDeptId = deptIdMapping.get(firstDeptId);
                    }
                    
                    // 根据职位查找或创建岗位
                    Set<Long> postIds = getOrCreatePostByTitle(dingtalkUser.getTitle());
                    
                    if (mapping != null) {
                        // 更新已有用户
                        AdminUserDO existingUser = adminUserMapper.selectById(mapping.getLocalId());
                        if (existingUser != null) {
                            existingUser.setNickname(dingtalkUser.getName());
                            existingUser.setMobile(normalizeMobile(dingtalkUser.getMobile()));
                            existingUser.setEmail(dingtalkUser.getEmail());
                            existingUser.setAvatar(dingtalkUser.getAvatar());
                            existingUser.setPosition(dingtalkUser.getTitle());
                            existingUser.setPostIds(postIds);
                            existingUser.setEmployeeStatus(EMPLOYEE_STATUS_ON_JOB);
                            existingUser.setStatus(CommonStatusEnum.ENABLE.getStatus());
                            if (localDeptId != null) {
                                existingUser.setDeptId(localDeptId);
                            }
                            adminUserMapper.updateById(existingUser);
                            // 同步岗位关联表 system_user_post
                            syncUserPosts(existingUser.getId(), postIds);
                            updateCount++;
                        } else {
                            // 映射存在但用户被删除了，重新创建用户
                            String baseName = cn.shuhe.system.module.system.util.PinyinUtils.toPinyin(dingtalkUser.getName());
                            if (baseName.isEmpty()) {
                                baseName = dingtalkUser.getUserid();
                            }
                            String username = baseName;
                            int suffix = 1;
                            while (adminUserMapper.selectByUsername(username) != null) {
                                username = baseName + suffix;
                                suffix++;
                            }
                            
                            AdminUserDO newUser = AdminUserDO.builder()
                                    .username(username)
                                    .password(passwordEncoder.encode("123456"))
                                    .nickname(dingtalkUser.getName())
                                    .mobile(normalizeMobile(dingtalkUser.getMobile()))
                                    .email(dingtalkUser.getEmail())
                                    .avatar(dingtalkUser.getAvatar())
                                    .position(dingtalkUser.getTitle())
                                    .postIds(postIds)
                                    .deptId(localDeptId)
                                    .status(CommonStatusEnum.ENABLE.getStatus())
                                    .employeeStatus(EMPLOYEE_STATUS_ON_JOB)
                                    .build();
                            adminUserMapper.insert(newUser);
                            // 同步岗位关联表 system_user_post
                            syncUserPosts(newUser.getId(), postIds);
                            
                            // 更新映射关系
                            mapping.setLocalId(newUser.getId());
                            dingtalkMappingMapper.updateById(mapping);
                            createCount++;
                        }
                    } else {
                        // 生成用户名：优先使用姓名拼音，如果重复则加数字后缀
                        String baseName = cn.shuhe.system.module.system.util.PinyinUtils.toPinyin(dingtalkUser.getName());
                        if (baseName.isEmpty()) {
                            baseName = dingtalkUser.getUserid();
                        }
                        String username = baseName;
                        int suffix = 1;
                        while (adminUserMapper.selectByUsername(username) != null) {
                            username = baseName + suffix;
                            suffix++;
                        }
                        
                        // 创建新用户
                        AdminUserDO newUser = AdminUserDO.builder()
                                .username(username)
                                .password(passwordEncoder.encode("123456")) // 默认密码
                                .nickname(dingtalkUser.getName())
                                .mobile(normalizeMobile(dingtalkUser.getMobile()))
                                .email(dingtalkUser.getEmail())
                                .avatar(dingtalkUser.getAvatar())
                                .position(dingtalkUser.getTitle())
                                .postIds(postIds)
                                .deptId(localDeptId)
                                .status(CommonStatusEnum.ENABLE.getStatus())
                                .employeeStatus(EMPLOYEE_STATUS_ON_JOB)
                                .build();
                        adminUserMapper.insert(newUser);
                        // 同步岗位关联表 system_user_post
                        syncUserPosts(newUser.getId(), postIds);
                        
                        // 创建映射关系
                        DingtalkMappingDO newMapping = DingtalkMappingDO.builder()
                                .configId(configId)
                                .type(MAPPING_TYPE_USER)
                                .localId(newUser.getId())
                                .dingtalkId(dingtalkUser.getUserid())
                                .build();
                        dingtalkMappingMapper.insert(newMapping);
                        createCount++;
                    }
                }
            }
            log.info("在职用户同步完成：新增 {} 个，更新 {} 个", createCount, updateCount);
            
            // 7. 获取并同步离职员工
            try {
                List<String> dimissionUserIds = dingtalkApiService.getDimissionEmployeeUserIds(accessToken);
                log.info("获取钉钉离职员工列表成功，共 {} 人", dimissionUserIds.size());
                
                // 获取离职员工的花名册信息（包含入职日期、离职日期、姓名）
                Map<String, DingtalkApiService.HrmEmployeeInfo> hrmInfoMap = new HashMap<>();
                Map<String, DingtalkApiService.HrmEmployeeInfo> hrmInfoByMobileMap = new HashMap<>();
                if (!dimissionUserIds.isEmpty()) {
                    List<DingtalkApiService.HrmEmployeeInfo> hrmInfoList = dingtalkApiService.getEmployeeRosterInfo(accessToken, config.getAgentId(), dimissionUserIds);
                    log.info("获取离职员工花名册成功，返回 {} 条记录（发送 {} 个userid）", hrmInfoList.size(), dimissionUserIds.size());
                    hrmInfoMap = hrmInfoList.stream()
                            .collect(Collectors.toMap(DingtalkApiService.HrmEmployeeInfo::getUserid, info -> info, (a, b) -> a));
                    // 额外建立手机号索引，用于回退匹配
                    for (DingtalkApiService.HrmEmployeeInfo info : hrmInfoList) {
                        if (StrUtil.isNotEmpty(info.getMobile())) {
                            hrmInfoByMobileMap.put(info.getMobile(), info);
                        }
                    }
                }
                
                for (String dimissionUserId : dimissionUserIds) {
                    DingtalkMappingDO mapping = mappingByDingtalkId.get(dimissionUserId);
                    DingtalkApiService.HrmEmployeeInfo hrmInfo = hrmInfoMap.get(dimissionUserId);
                    DingtalkApiService.DingtalkUser userInfo = null;
                    
                    if (mapping != null) {
                        // 更新已有用户为离职状态
                        AdminUserDO existingUser = adminUserMapper.selectById(mapping.getLocalId());
                        if (existingUser != null) {
                            // 对于已存在的用户，先尝试用数据库中的手机号匹配花名册
                            if ((hrmInfo == null || StrUtil.isEmpty(hrmInfo.getName())) 
                                    && StrUtil.isNotEmpty(existingUser.getMobile())) {
                                DingtalkApiService.HrmEmployeeInfo matchedHrm = hrmInfoByMobileMap.get(existingUser.getMobile());
                                if (matchedHrm != null) {
                                    hrmInfo = matchedHrm;
                                    log.info("通过已有手机号 {} 匹配到花名册信息，姓名: {}", existingUser.getMobile(), hrmInfo.getName());
                                }
                            }
                            
                            // 如果还是找不到，尝试调用用户详情API
                            if (hrmInfo == null || StrUtil.isEmpty(hrmInfo.getName())) {
                                userInfo = dingtalkApiService.getUserByUserId(accessToken, dimissionUserId);
                                // 如果有用户详情且有手机号，尝试用手机号匹配花名册
                                if (userInfo != null && StrUtil.isNotEmpty(userInfo.getMobile())) {
                                    DingtalkApiService.HrmEmployeeInfo matchedHrm = hrmInfoByMobileMap.get(userInfo.getMobile());
                                    if (matchedHrm != null) {
                                        hrmInfo = matchedHrm;
                                        log.info("通过API获取的手机号 {} 匹配到花名册信息，姓名: {}", userInfo.getMobile(), hrmInfo.getName());
                                    }
                                }
                            }
                            
                            existingUser.setEmployeeStatus(EMPLOYEE_STATUS_DIMISSION);
                            existingUser.setStatus(CommonStatusEnum.DISABLE.getStatus()); // 禁用离职员工账号
                            
                            // 优先使用花名册姓名，其次使用用户详情姓名
                            // 如果都获取不到，保留现有昵称（不覆盖为数字）
                            String realName = null;
                            if (hrmInfo != null && StrUtil.isNotEmpty(hrmInfo.getName())) {
                                realName = hrmInfo.getName();
                            } else if (userInfo != null && StrUtil.isNotEmpty(userInfo.getName())) {
                                realName = userInfo.getName();
                            }
                            
                            if (StrUtil.isNotEmpty(realName)) {
                                existingUser.setNickname(realName);
                                // 如果当前用户名是数字（之前用userid创建的），也更新用户名
                                if (existingUser.getUsername().matches("\\d+")) {
                                    String baseName = cn.shuhe.system.module.system.util.PinyinUtils.toPinyin(realName);
                                    if (StrUtil.isNotEmpty(baseName)) {
                                        String newUsername = baseName;
                                        int suffix = 1;
                                        // 确保用户名唯一（排除自己）
                                        AdminUserDO existByName;
                                        while ((existByName = adminUserMapper.selectByUsername(newUsername)) != null 
                                                && !existByName.getId().equals(existingUser.getId())) {
                                            newUsername = baseName + suffix;
                                            suffix++;
                                        }
                                        existingUser.setUsername(newUsername);
                                        log.info("更新离职员工用户名: {} -> {}", dimissionUserId, newUsername);
                                    }
                                }
                            }
                            // 如果两个API都获取不到姓名，不更新nickname和username，保留现有值
                            
                            // 更新入职/离职日期（仅从花名册获取）
                            if (hrmInfo != null) {
                                existingUser.setHireDate(parseDate(hrmInfo.getConfirmJoinTime()));
                                existingUser.setResignDate(parseDate(hrmInfo.getLastWorkDay()));
                            }
                            
                            adminUserMapper.updateById(existingUser);
                            dimissionUpdateCount++;
                        } else {
                            // 用户被删除了，删除旧映射
                            // 注意：由于唯一索引包含deleted字段，可能存在重复的已删除记录导致冲突
                            // 这种情况下直接跳过，不影响业务
                            try {
                                dingtalkMappingMapper.deleteById(mapping.getId());
                            } catch (Exception e) {
                                log.warn("删除旧映射记录失败（可能已存在重复的已删除记录）: mappingId={}, dingtalkId={}", 
                                        mapping.getId(), dimissionUserId);
                            }
                            mapping = null;
                        }
                    }
                    if (mapping == null) {
                        // 创建离职员工 - 优先使用花名册姓名，其次使用用户详情姓名
                        try {
                            String name = null;
                            if (hrmInfo != null && StrUtil.isNotEmpty(hrmInfo.getName())) {
                                name = hrmInfo.getName();
                            } else if (userInfo != null && StrUtil.isNotEmpty(userInfo.getName())) {
                                name = userInfo.getName();
                            } else {
                                name = dimissionUserId; // 最后回退到userid
                                log.warn("离职员工 {} 无法获取姓名，使用userid作为姓名", dimissionUserId);
                            }
                            
                            String baseName = cn.shuhe.system.module.system.util.PinyinUtils.toPinyin(name);
                            if (baseName.isEmpty()) {
                                baseName = dimissionUserId;
                            }
                            String username = baseName;
                            int suffix = 1;
                            while (adminUserMapper.selectByUsername(username) != null) {
                                username = baseName + suffix;
                                suffix++;
                            }
                            
                            // 合并用户信息来源（手机号需要标准化格式）
                            String mobile = normalizeMobile(hrmInfo != null && StrUtil.isNotEmpty(hrmInfo.getMobile()) ? hrmInfo.getMobile() 
                                    : (userInfo != null ? userInfo.getMobile() : null));
                            String email = hrmInfo != null && StrUtil.isNotEmpty(hrmInfo.getEmail()) ? hrmInfo.getEmail()
                                    : (userInfo != null ? userInfo.getEmail() : null);
                            String avatar = hrmInfo != null && StrUtil.isNotEmpty(hrmInfo.getAvatar()) ? hrmInfo.getAvatar()
                                    : (userInfo != null ? userInfo.getAvatar() : null);
                            
                            AdminUserDO newUser = AdminUserDO.builder()
                                    .username(username)
                                    .password(passwordEncoder.encode("123456"))
                                    .nickname(name)
                                    .mobile(mobile)
                                    .email(email)
                                    .avatar(avatar)
                                    .status(CommonStatusEnum.DISABLE.getStatus()) // 离职员工账号禁用
                                    .employeeStatus(EMPLOYEE_STATUS_DIMISSION)
                                    .hireDate(hrmInfo != null ? parseDate(hrmInfo.getConfirmJoinTime()) : null)
                                    .resignDate(hrmInfo != null ? parseDate(hrmInfo.getLastWorkDay()) : null)
                                    .build();
                            adminUserMapper.insert(newUser);
                            
                            // 创建映射关系
                            DingtalkMappingDO newMapping = DingtalkMappingDO.builder()
                                    .configId(configId)
                                    .type(MAPPING_TYPE_USER)
                                    .localId(newUser.getId())
                                    .dingtalkId(dimissionUserId)
                                    .build();
                            dingtalkMappingMapper.insert(newMapping);
                            dimissionCreateCount++;
                        } catch (Exception e) {
                            log.warn("创建离职员工失败，跳过该用户: dingtalkId={}, error={}", dimissionUserId, e.getMessage());
                        }
                    }
                }
                log.info("离职员工同步完成：新增 {} 个，更新 {} 个，花名册匹配成功 {} 个", 
                        dimissionCreateCount, dimissionUpdateCount, 
                        hrmInfoMap.size() + hrmInfoByMobileMap.size());
            } catch (Exception e) {
                log.warn("获取离职员工信息失败（可能没有智能人事权限）: {}", e.getMessage());
            }
            
            // 8. 尝试获取在职员工的花名册信息（入职日期等）
            try {
                List<String> onJobUserIds = dingtalkApiService.getOnJobEmployeeUserIds(accessToken);
                log.info("获取钉钉在职员工列表成功，共 {} 人", onJobUserIds.size());
                
                if (!onJobUserIds.isEmpty()) {
                    List<DingtalkApiService.HrmEmployeeInfo> hrmInfoList = dingtalkApiService.getEmployeeRosterInfo(accessToken, config.getAgentId(), onJobUserIds);
                    Map<String, DingtalkApiService.HrmEmployeeInfo> hrmInfoMap = hrmInfoList.stream()
                            .collect(Collectors.toMap(DingtalkApiService.HrmEmployeeInfo::getUserid, info -> info, (a, b) -> a));
                    
                    // 更新在职员工的入职日期
                    for (DingtalkApiService.HrmEmployeeInfo hrmInfo : hrmInfoList) {
                        DingtalkMappingDO mapping = mappingByDingtalkId.get(hrmInfo.getUserid());
                        if (mapping == null) {
                            // 可能是新同步的用户，重新查询
                            List<DingtalkMappingDO> newMappings = dingtalkMappingMapper.selectList(
                                    DingtalkMappingDO::getDingtalkId, hrmInfo.getUserid());
                            if (!newMappings.isEmpty()) {
                                mapping = newMappings.get(0);
                            }
                        }
                        
                        if (mapping != null) {
                            AdminUserDO user = adminUserMapper.selectById(mapping.getLocalId());
                            if (user != null && StrUtil.isNotEmpty(hrmInfo.getConfirmJoinTime())) {
                                user.setHireDate(parseDate(hrmInfo.getConfirmJoinTime()));
                                adminUserMapper.updateById(user);
                            }
                        }
                    }
                    log.info("已更新在职员工花名册信息");
                }
            } catch (Exception e) {
                log.warn("获取在职员工花名册信息失败（可能没有智能人事权限）: {}", e.getMessage());
            }
            
            // 9. 更新配置的最后同步时间和结果
            config.setLastSyncTime(LocalDateTime.now());
            config.setLastSyncResult(String.format("用户同步成功：在职新增 %d，在职更新 %d，离职新增 %d，离职更新 %d", 
                    createCount, updateCount, dimissionCreateCount, dimissionUpdateCount));
            dingtalkConfigMapper.updateById(config);
            
            log.info("钉钉用户同步全部完成");
            
        } catch (Exception e) {
            log.error("钉钉用户同步失败", e);
            config.setLastSyncTime(LocalDateTime.now());
            config.setLastSyncResult("用户同步失败：" + e.getMessage());
            dingtalkConfigMapper.updateById(config);
            throw new RuntimeException("钉钉用户同步失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 同步用户岗位关联表
     * @param userId 用户ID
     * @param postIds 岗位ID集合
     */
    private void syncUserPosts(Long userId, Set<Long> postIds) {
        if (userId == null) {
            return;
        }
        // 获取当前用户已有的岗位ID
        Set<Long> dbPostIds = new HashSet<>();
        List<UserPostDO> existingPosts = userPostMapper.selectListByUserId(userId);
        if (existingPosts != null) {
            for (UserPostDO up : existingPosts) {
                dbPostIds.add(up.getPostId());
            }
        }
        
        Set<Long> newPostIds = CollUtil.emptyIfNull(postIds);
        
        // 计算需要新增的岗位
        Set<Long> createPostIds = new HashSet<>(newPostIds);
        createPostIds.removeAll(dbPostIds);
        
        // 计算需要删除的岗位
        Set<Long> deletePostIds = new HashSet<>(dbPostIds);
        deletePostIds.removeAll(newPostIds);
        
        // 执行新增
        if (!createPostIds.isEmpty()) {
            List<UserPostDO> insertList = new ArrayList<>();
            for (Long postId : createPostIds) {
                UserPostDO userPost = new UserPostDO();
                userPost.setUserId(userId);
                userPost.setPostId(postId);
                insertList.add(userPost);
            }
            userPostMapper.insertBatch(insertList);
        }
        
        // 执行删除
        if (!deletePostIds.isEmpty()) {
            userPostMapper.deleteByUserIdAndPostId(userId, deletePostIds);
        }
    }
    
    /**
     * 根据职位名称查找或创建岗位
     * @param title 职位名称
     * @return 岗位ID集合
     */
    private Set<Long> getOrCreatePostByTitle(String title) {
        if (StrUtil.isEmpty(title)) {
            return null;
        }
        
        // 查找是否存在同名岗位
        PostDO existingPost = postMapper.selectByName(title);
        if (existingPost != null) {
            Set<Long> postIds = new HashSet<>();
            postIds.add(existingPost.getId());
            return postIds;
        }
        
        // 不存在则创建新岗位
        PostDO newPost = new PostDO();
        newPost.setName(title);
        // 生成岗位编码：使用拼音
        String code = cn.shuhe.system.module.system.util.PinyinUtils.toPinyin(title);
        if (code.isEmpty()) {
            code = "post" + System.currentTimeMillis();
        }
        // 确保编码唯一
        String baseCode = code;
        int suffix = 1;
        while (postMapper.selectByCode(code) != null) {
            code = baseCode + suffix;
            suffix++;
        }
        newPost.setCode(code);
        newPost.setSort(100);
        newPost.setStatus(CommonStatusEnum.ENABLE.getStatus());
        newPost.setRemark("从钉钉同步自动创建");
        postMapper.insert(newPost);
        
        log.info("自动创建岗位: name={}, code={}, id={}", title, code, newPost.getId());
        
        Set<Long> postIds = new HashSet<>();
        postIds.add(newPost.getId());
        return postIds;
    }

    /**
     * 解析日期字符串为LocalDateTime
     */
    private LocalDateTime parseDate(String dateStr) {
        if (StrUtil.isEmpty(dateStr)) {
            return null;
        }
        try {
            // 尝试多种格式
            if (dateStr.length() == 10) {
                // yyyy-MM-dd 格式
                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                return date.atStartOfDay();
            } else if (dateStr.length() == 13) {
                // 时间戳（毫秒）
                return LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(Long.parseLong(dateStr)),
                        java.time.ZoneId.systemDefault());
            }
            return null;
        } catch (Exception e) {
            log.warn("解析日期失败: {}", dateStr);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncDingtalkPost(Long configId) {
        // 1. 校验配置存在
        DingtalkConfigDO config = validateDingtalkConfigExists(configId);
        log.info("开始同步钉钉岗位，配置ID: {}, 配置名称: {}", configId, config.getName());
        
        try {
            // 2. 获取钉钉access_token
            String accessToken = dingtalkApiService.getAccessToken(config);
            log.info("获取钉钉access_token成功");
            
            // 3. 获取钉钉部门列表
            List<DingtalkApiService.DingtalkDept> depts = dingtalkApiService.getDeptList(accessToken, 1L);
            
            // 4. 收集所有用户的职位
            Set<String> allTitles = new HashSet<>();
            Set<String> processedUserIds = new HashSet<>();
            
            for (DingtalkApiService.DingtalkDept dept : depts) {
                List<DingtalkApiService.DingtalkUser> users = dingtalkApiService.getUserList(accessToken, dept.getDeptId());
                
                for (DingtalkApiService.DingtalkUser user : users) {
                    if (processedUserIds.contains(user.getUserid())) {
                        continue;
                    }
                    processedUserIds.add(user.getUserid());
                    
                    if (StrUtil.isNotEmpty(user.getTitle())) {
                        allTitles.add(user.getTitle());
                    }
                }
            }
            
            log.info("从钉钉收集到 {} 个不同的职位", allTitles.size());
            
            // 5. 对比现有岗位，创建新岗位
            int createCount = 0;
            int existCount = 0;
            int maxSort = 100; // 新岗位的排序从100开始
            
            for (String title : allTitles) {
                PostDO existingPost = postMapper.selectByName(title);
                if (existingPost != null) {
                    existCount++;
                    continue;
                }
                
                // 创建新岗位
                PostDO newPost = new PostDO();
                newPost.setName(title);
                // 生成岗位编码：使用拼音
                String code = cn.shuhe.system.module.system.util.PinyinUtils.toPinyin(title);
                if (code.isEmpty()) {
                    code = "post" + System.currentTimeMillis();
                }
                // 确保编码唯一
                String baseCode = code;
                int suffix = 1;
                while (postMapper.selectByCode(code) != null) {
                    code = baseCode + suffix;
                    suffix++;
                }
                newPost.setCode(code);
                newPost.setSort(maxSort++);
                newPost.setStatus(CommonStatusEnum.ENABLE.getStatus());
                newPost.setRemark("从钉钉同步");
                postMapper.insert(newPost);
                createCount++;
                
                log.debug("创建岗位: name={}, code={}", title, code);
            }
            
            log.info("钉钉岗位同步完成：新增 {} 个，已存在 {} 个", createCount, existCount);
            
        } catch (Exception e) {
            log.error("同步钉钉岗位失败", e);
            throw new RuntimeException("同步钉钉岗位失败: " + e.getMessage());
        }
    }

}