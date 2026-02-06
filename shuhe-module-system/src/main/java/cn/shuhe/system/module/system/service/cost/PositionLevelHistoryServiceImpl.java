package cn.shuhe.system.module.system.service.cost;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.system.controller.admin.cost.vo.PositionLevelHistoryPageReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.PositionLevelHistoryRespVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.PositionLevelHistorySaveReqVO;
import cn.shuhe.system.module.system.dal.dataobject.cost.PositionLevelHistoryDO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.dal.mysql.cost.PositionLevelHistoryMapper;
import cn.shuhe.system.module.system.dal.mysql.user.AdminUserMapper;
import cn.shuhe.system.module.system.dal.redis.RedisKeyConstants;
import cn.shuhe.system.module.system.service.dept.DeptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;

/**
 * 职级变更记录 Service 实现
 */
@Slf4j
@Service
public class PositionLevelHistoryServiceImpl implements PositionLevelHistoryService {

    @Resource
    private PositionLevelHistoryMapper positionLevelHistoryMapper;

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private DeptService deptService;

    private static final Map<Integer, String> CHANGE_TYPE_NAMES = Map.of(
            PositionLevelHistoryDO.CHANGE_TYPE_AUTO, "自动同步",
            PositionLevelHistoryDO.CHANGE_TYPE_MANUAL, "手动录入"
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHistory(PositionLevelHistorySaveReqVO reqVO) {
        // 校验用户是否存在
        AdminUserDO user = adminUserMapper.selectById(reqVO.getUserId());
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }

        // 如果没有填写变更前职级，使用用户当前职级
        String oldLevel = reqVO.getOldPositionLevel();
        if (StrUtil.isEmpty(oldLevel)) {
            oldLevel = user.getPositionLevel();
        } else {
            // 自动添加前缀，保持与钉钉同步数据格式一致
            oldLevel = addPositionLevelPrefix(oldLevel);
        }
        
        // 变更后职级也自动添加前缀
        String newLevel = addPositionLevelPrefix(reqVO.getNewPositionLevel());

        PositionLevelHistoryDO history = new PositionLevelHistoryDO();
        history.setUserId(reqVO.getUserId());
        history.setOldPositionLevel(oldLevel);
        history.setNewPositionLevel(newLevel);
        history.setEffectiveDate(reqVO.getEffectiveDate());
        history.setChangeType(PositionLevelHistoryDO.CHANGE_TYPE_MANUAL);
        history.setRemark(reqVO.getRemark());

        positionLevelHistoryMapper.insert(history);
        
        // 同步更新用户当前职级（根据所有变更记录重新计算）
        syncUserPositionLevel(reqVO.getUserId());
        
        return history.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHistory(PositionLevelHistorySaveReqVO reqVO) {
        // 校验记录是否存在
        PositionLevelHistoryDO existing = positionLevelHistoryMapper.selectById(reqVO.getId());
        if (existing == null) {
            throw exception(USER_NOT_EXISTS); // TODO: 添加专用错误码
        }

        // 自动添加前缀，保持与钉钉同步数据格式一致
        String oldLevel = addPositionLevelPrefix(reqVO.getOldPositionLevel());
        String newLevel = addPositionLevelPrefix(reqVO.getNewPositionLevel());

        PositionLevelHistoryDO update = new PositionLevelHistoryDO();
        update.setId(reqVO.getId());
        update.setUserId(reqVO.getUserId());
        update.setOldPositionLevel(oldLevel);
        update.setNewPositionLevel(newLevel);
        update.setEffectiveDate(reqVO.getEffectiveDate());
        update.setRemark(reqVO.getRemark());

        positionLevelHistoryMapper.updateById(update);
        
        // 同步更新用户当前职级
        syncUserPositionLevel(reqVO.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHistory(Long id) {
        // 先获取记录，拿到用户ID和变更前职级
        PositionLevelHistoryDO history = positionLevelHistoryMapper.selectById(id);
        if (history == null) {
            return;
        }
        
        Long userId = history.getUserId();
        String oldPositionLevel = history.getOldPositionLevel(); // 保存变更前职级，用于可能的还原
        
        positionLevelHistoryMapper.deleteById(id);
        
        // 同步更新用户当前职级（删除后重新计算，如果没有记录则还原）
        syncUserPositionLevelAfterDelete(userId, oldPositionLevel);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPositionChange(Long userId, String oldPositionLevel, String newPositionLevel) {
        // 如果职级相同或新职级为空，不记录
        if (StrUtil.isEmpty(newPositionLevel) || StrUtil.equals(oldPositionLevel, newPositionLevel)) {
            return;
        }

        log.info("[recordPositionChange] 用户 {} 职级变更: {} -> {}", userId, oldPositionLevel, newPositionLevel);

        PositionLevelHistoryDO history = new PositionLevelHistoryDO();
        history.setUserId(userId);
        history.setOldPositionLevel(oldPositionLevel);
        history.setNewPositionLevel(newPositionLevel);
        history.setEffectiveDate(LocalDate.now()); // 使用当天作为生效日期
        history.setChangeType(PositionLevelHistoryDO.CHANGE_TYPE_AUTO);
        history.setRemark("钉钉同步自动记录");

        positionLevelHistoryMapper.insert(history);
    }

    @Override
    public List<PositionLevelHistoryDO> getHistoryByUserId(Long userId) {
        return positionLevelHistoryMapper.selectByUserId(userId);
    }

    @Override
    @Cacheable(value = RedisKeyConstants.POSITION_LEVEL_HISTORY,
               key = "#userId + ':' + #year",
               unless = "#result == null")
    public List<PositionLevelHistoryDO> getHistoryByUserIdAndYear(Long userId, int year) {
        log.debug("[缓存未命中] 查询职级变更历史 userId={}, year={}", userId, year);
        return positionLevelHistoryMapper.selectByUserIdAndYear(userId, year);
    }

    @Override
    public String getPositionLevelAtDate(Long userId, LocalDate date) {
        // 查找生效日期 <= 指定日期的最新职级记录
        List<PositionLevelHistoryDO> histories = positionLevelHistoryMapper.selectList(
                new LambdaQueryWrapperX<PositionLevelHistoryDO>()
                        .eq(PositionLevelHistoryDO::getUserId, userId)
                        .le(PositionLevelHistoryDO::getEffectiveDate, date)
                        .orderByDesc(PositionLevelHistoryDO::getEffectiveDate)
                        .last("LIMIT 1"));

        if (CollUtil.isNotEmpty(histories)) {
            return histories.get(0).getNewPositionLevel();
        }
        return null;
    }

    @Override
    public PageResult<PositionLevelHistoryRespVO> getHistoryPage(PositionLevelHistoryPageReqVO reqVO) {
        // 如果按昵称搜索，先查询用户ID
        Set<Long> userIds = null;
        if (StrUtil.isNotEmpty(reqVO.getNickname())) {
            List<AdminUserDO> users = adminUserMapper.selectList(
                    new LambdaQueryWrapperX<AdminUserDO>()
                            .likeIfPresent(AdminUserDO::getNickname, reqVO.getNickname()));
            if (CollUtil.isEmpty(users)) {
                return new PageResult<>(Collections.emptyList(), 0L);
            }
            userIds = users.stream().map(AdminUserDO::getId).collect(Collectors.toSet());
        }

        // 分页查询
        LambdaQueryWrapperX<PositionLevelHistoryDO> wrapper = new LambdaQueryWrapperX<PositionLevelHistoryDO>()
                .eqIfPresent(PositionLevelHistoryDO::getUserId, reqVO.getUserId())
                .inIfPresent(PositionLevelHistoryDO::getUserId, userIds)
                .eqIfPresent(PositionLevelHistoryDO::getChangeType, reqVO.getChangeType())
                .geIfPresent(PositionLevelHistoryDO::getEffectiveDate, reqVO.getStartDate())
                .leIfPresent(PositionLevelHistoryDO::getEffectiveDate, reqVO.getEndDate())
                .orderByDesc(PositionLevelHistoryDO::getEffectiveDate);

        PageResult<PositionLevelHistoryDO> pageResult = positionLevelHistoryMapper.selectPage(reqVO, wrapper);

        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        // 获取用户信息
        Set<Long> allUserIds = pageResult.getList().stream()
                .map(PositionLevelHistoryDO::getUserId)
                .collect(Collectors.toSet());
        Map<Long, AdminUserDO> userMap = adminUserMapper.selectBatchIds(allUserIds).stream()
                .collect(Collectors.toMap(AdminUserDO::getId, u -> u));

        // 获取部门信息
        Set<Long> deptIds = userMap.values().stream()
                .map(AdminUserDO::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(deptIds);

        // 转换为VO
        List<PositionLevelHistoryRespVO> voList = pageResult.getList().stream()
                .map(history -> convertToVO(history, userMap, deptMap))
                .collect(Collectors.toList());

        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    public PositionLevelHistoryRespVO getHistory(Long id) {
        PositionLevelHistoryDO history = positionLevelHistoryMapper.selectById(id);
        if (history == null) {
            return null;
        }

        AdminUserDO user = adminUserMapper.selectById(history.getUserId());
        Map<Long, AdminUserDO> userMap = user != null
                ? Map.of(user.getId(), user)
                : Collections.emptyMap();

        Map<Long, DeptDO> deptMap = Collections.emptyMap();
        if (user != null && user.getDeptId() != null) {
            DeptDO dept = deptService.getDept(user.getDeptId());
            if (dept != null) {
                deptMap = Map.of(dept.getId(), dept);
            }
        }

        return convertToVO(history, userMap, deptMap);
    }

    /**
     * 同步更新用户当前职级
     * 根据所有职级变更记录，计算当前应该是什么职级
     */
    private void syncUserPositionLevel(Long userId) {
        LocalDate today = LocalDate.now();
        
        // 获取生效日期 <= 今天的最新职级
        String currentLevel = getPositionLevelAtDate(userId, today);
        
        if (currentLevel != null) {
            // 如果职级没有前缀，自动添加"初级/中级/高级"前缀
            String fullLevel = addPositionLevelPrefix(currentLevel);
            
            // 更新用户表的职级
            AdminUserDO update = new AdminUserDO();
            update.setId(userId);
            update.setPositionLevel(fullLevel);
            adminUserMapper.updateById(update);
            log.info("[syncUserPositionLevel] 用户 {} 职级更新为: {}", userId, fullLevel);
        }
    }

    /**
     * 删除记录后同步更新用户当前职级
     * 如果删除后没有有效记录，则还原到被删除记录的变更前职级
     */
    private void syncUserPositionLevelAfterDelete(Long userId, String deletedOldPositionLevel) {
        LocalDate today = LocalDate.now();
        
        // 获取生效日期 <= 今天的最新职级
        String currentLevel = getPositionLevelAtDate(userId, today);
        
        String targetLevel;
        if (currentLevel != null) {
            // 还有其他有效记录，使用最新记录的职级
            targetLevel = addPositionLevelPrefix(currentLevel);
            log.info("[syncUserPositionLevelAfterDelete] 用户 {} 根据剩余记录更新职级为: {}", userId, targetLevel);
        } else {
            // 没有有效记录了，还原到被删除记录的变更前职级
            if (StrUtil.isNotEmpty(deletedOldPositionLevel)) {
                targetLevel = addPositionLevelPrefix(deletedOldPositionLevel);
                log.info("[syncUserPositionLevelAfterDelete] 用户 {} 还原职级为变更前: {}", userId, targetLevel);
            } else {
                // 变更前职级也为空，不做处理
                log.warn("[syncUserPositionLevelAfterDelete] 用户 {} 无法还原职级，变更前职级为空", userId);
                return;
            }
        }
        
        // 更新用户表的职级
        AdminUserDO update = new AdminUserDO();
        update.setId(userId);
        update.setPositionLevel(targetLevel);
        adminUserMapper.updateById(update);
    }

    /**
     * 为职级添加前缀（如果没有的话）
     * P1-x → 初级P1-x
     * P2-x → 中级P2-x
     * P3-x → 高级P3-x
     */
    private String addPositionLevelPrefix(String positionLevel) {
        if (StrUtil.isEmpty(positionLevel)) {
            return positionLevel;
        }
        
        // 如果已经有前缀，直接返回
        if (positionLevel.startsWith("初级") || positionLevel.startsWith("中级") || positionLevel.startsWith("高级")) {
            return positionLevel;
        }
        
        // 根据职级添加前缀
        if (positionLevel.startsWith("P1")) {
            return "初级" + positionLevel;
        } else if (positionLevel.startsWith("P2")) {
            return "中级" + positionLevel;
        } else if (positionLevel.startsWith("P3")) {
            return "高级" + positionLevel;
        }
        
        return positionLevel;
    }

    /**
     * 转换为 VO
     */
    private PositionLevelHistoryRespVO convertToVO(PositionLevelHistoryDO history,
                                                    Map<Long, AdminUserDO> userMap,
                                                    Map<Long, DeptDO> deptMap) {
        PositionLevelHistoryRespVO vo = new PositionLevelHistoryRespVO();
        vo.setId(history.getId());
        vo.setUserId(history.getUserId());
        vo.setOldPositionLevel(history.getOldPositionLevel());
        vo.setNewPositionLevel(history.getNewPositionLevel());
        vo.setEffectiveDate(history.getEffectiveDate());
        vo.setChangeType(history.getChangeType());
        vo.setChangeTypeName(CHANGE_TYPE_NAMES.get(history.getChangeType()));
        vo.setRemark(history.getRemark());
        vo.setCreateTime(history.getCreateTime());

        // 填充用户信息
        AdminUserDO user = userMap.get(history.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setUsername(user.getUsername());

            // 填充部门信息
            if (user.getDeptId() != null) {
                DeptDO dept = deptMap.get(user.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getName());
                }
            }
        }

        return vo;
    }

}
