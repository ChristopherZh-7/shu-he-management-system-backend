package cn.shuhe.system.module.system.api.dept;

import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.service.dept.DeptService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * 部门 API 实现类
 *
 * @author 芋道源码
 */
@Service
public class DeptApiImpl implements DeptApi {

    @Resource
    private DeptService deptService;

    @Override
    public DeptRespDTO getDept(Long id) {
        DeptDO dept = deptService.getDept(id);
        return BeanUtils.toBean(dept, DeptRespDTO.class);
    }

    @Override
    public List<DeptRespDTO> getDeptList(Collection<Long> ids) {
        List<DeptDO> depts = deptService.getDeptList(ids);
        return BeanUtils.toBean(depts, DeptRespDTO.class);
    }

    @Override
    public void validateDeptList(Collection<Long> ids) {
        deptService.validateDeptList(ids);
    }

    @Override
    public List<DeptRespDTO> getChildDeptList(Long id) {
        List<DeptDO> childDeptList = deptService.getChildDeptList(id);
        return BeanUtils.toBean(childDeptList, DeptRespDTO.class);
    }

    @Override
    public List<DeptRespDTO> getDeptListByLeaderUserId(Long userId) {
        List<DeptDO> deptList = deptService.getDeptListByLeaderUserId(userId);
        return BeanUtils.toBean(deptList, DeptRespDTO.class);
    }

    @Override
    public List<DeptRespDTO> getDeptListByDeptType(Integer deptType) {
        List<DeptDO> deptList = deptService.getDeptListByDeptType(deptType);
        return BeanUtils.toBean(deptList, DeptRespDTO.class);
    }

    @Override
    public boolean isLeafDept(Long id) {
        if (id == null) {
            return false;
        }
        List<DeptDO> childList = deptService.getChildDeptList(id);
        return childList == null || childList.isEmpty();
    }

    @Override
    public List<DeptRespDTO> getLeafDeptListByDeptType(Integer deptType) {
        // 获取指定类型的所有部门
        List<DeptDO> allDepts = deptService.getDeptListByDeptType(deptType);
        if (allDepts == null || allDepts.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // 收集所有部门ID
        java.util.Set<Long> allDeptIds = allDepts.stream()
                .map(DeptDO::getId)
                .collect(java.util.stream.Collectors.toSet());

        // 收集所有父部门ID
        java.util.Set<Long> parentIds = allDepts.stream()
                .map(DeptDO::getParentId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        // 叶子部门 = 所有部门 - 有子部门的部门（即不在任何部门的parentId中出现的）
        java.util.List<DeptDO> leafDepts = allDepts.stream()
                .filter(dept -> !parentIds.contains(dept.getId()))
                .collect(java.util.stream.Collectors.toList());

        return BeanUtils.toBean(leafDepts, DeptRespDTO.class);
    }

    @Override
    public Long findLeaderUserIdRecursively(Long deptId) {
        if (deptId == null) {
            return null;
        }

        java.util.Set<Long> visited = new java.util.HashSet<>();
        Long currentDeptId = deptId;

        while (currentDeptId != null && !visited.contains(currentDeptId)) {
            visited.add(currentDeptId);

            DeptDO dept = deptService.getDept(currentDeptId);
            if (dept == null) {
                break;
            }

            // 如果当前部门有负责人，返回负责人ID
            if (dept.getLeaderUserId() != null) {
                return dept.getLeaderUserId();
            }

            // 向上查找父部门
            currentDeptId = dept.getParentId();
        }

        // 如果找到根部门还没有负责人，尝试查找总经办（假设总经办deptType = 0 或特定名称）
        // 这里暂时返回null，让调用方处理
        return null;
    }

    @Override
    public Long findLeaderDeptIdRecursively(Long deptId) {
        if (deptId == null) {
            return null;
        }

        java.util.Set<Long> visited = new java.util.HashSet<>();
        Long currentDeptId = deptId;

        while (currentDeptId != null && !visited.contains(currentDeptId)) {
            visited.add(currentDeptId);

            DeptDO dept = deptService.getDept(currentDeptId);
            if (dept == null) {
                break;
            }

            // 如果当前部门有负责人，返回当前部门ID
            if (dept.getLeaderUserId() != null) {
                return currentDeptId;
            }

            // 向上查找父部门
            currentDeptId = dept.getParentId();
        }

        return null;
    }

}
