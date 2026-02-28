package cn.shuhe.system.module.system.api.dept;

import cn.shuhe.system.framework.common.util.collection.CollectionUtils;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 部门 API 接口
 *
 * @author ShuHe
 */
public interface DeptApi {

    /**
     * 获得部门信息
     *
     * @param id 部门编号
     * @return 部门信息
     */
    DeptRespDTO getDept(Long id);

    /**
     * 获得部门信息数组
     *
     * @param ids 部门编号数组
     * @return 部门信息数组
     */
    List<DeptRespDTO> getDeptList(Collection<Long> ids);

    /**
     * 校验部门们是否有效。如下情况，视为无效：
     * 1. 部门编号不存在
     * 2. 部门被禁用
     *
     * @param ids 角色编号数组
     */
    void validateDeptList(Collection<Long> ids);

    /**
     * 获得指定编号的部门 Map
     *
     * @param ids 部门编号数组
     * @return 部门 Map
     */
    default Map<Long, DeptRespDTO> getDeptMap(Collection<Long> ids) {
        List<DeptRespDTO> list = getDeptList(ids);
        return CollectionUtils.convertMap(list, DeptRespDTO::getId);
    }

    /**
     * 获得指定部门的所有子部门
     *
     * @param id 部门编号
     * @return 子部门列表
     */
    List<DeptRespDTO> getChildDeptList(Long id);

    /**
     * 获得指定用户作为负责人的部门列表
     *
     * @param userId 用户编号
     * @return 部门列表
     */
    List<DeptRespDTO> getDeptListByLeaderUserId(Long userId);

    /**
     * 根据部门类型获取部门列表
     *
     * @param deptType 部门类型
     * @return 部门列表
     */
    List<DeptRespDTO> getDeptListByDeptType(Integer deptType);

    /**
     * 检查部门是否是叶子部门（没有子部门）
     *
     * @param id 部门编号
     * @return true-是叶子部门, false-有子部门
     */
    boolean isLeafDept(Long id);

    /**
     * 获取指定部门类型下的所有叶子部门（没有子部门的部门）
     *
     * @param deptType 部门类型
     * @return 叶子部门列表
     */
    List<DeptRespDTO> getLeafDeptListByDeptType(Integer deptType);

    /**
     * 根据部门ID向上递归查找负责人ID
     * 查找顺序：当前部门 → 父部门 → ... → 根部门 → 总经办
     *
     * @param deptId 部门ID
     * @return 负责人ID，如果找不到返回null
     */
    Long findLeaderUserIdRecursively(Long deptId);

    /**
     * 查找负责人所在的部门ID（向上递归查找后返回的是哪个部门的负责人）
     *
     * @param deptId 起始部门ID
     * @return 最终找到负责人的部门ID
     */
    Long findLeaderDeptIdRecursively(Long deptId);

}
