package cn.shuhe.system.module.system.convert.user;

import cn.hutool.core.bean.BeanUtil;
import cn.shuhe.system.framework.common.util.collection.CollectionUtils;
import cn.shuhe.system.framework.common.util.collection.MapUtils;
import cn.shuhe.system.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.shuhe.system.module.system.controller.admin.dept.vo.post.PostSimpleRespVO;
import cn.shuhe.system.module.system.controller.admin.permission.vo.role.RoleSimpleRespVO;
import cn.shuhe.system.module.system.controller.admin.user.vo.profile.UserProfileRespVO;
import cn.shuhe.system.module.system.controller.admin.user.vo.user.UserRespVO;
import cn.shuhe.system.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.dept.PostDO;
import cn.shuhe.system.module.system.dal.dataobject.permission.RoleDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserConvert {

    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    default List<UserRespVO> convertList(List<AdminUserDO> list, Map<Long, DeptDO> deptMap) {
        return CollectionUtils.convertList(list, user -> convert(user, deptMap.get(user.getDeptId())));
    }

    default UserRespVO convert(AdminUserDO user, DeptDO dept) {
        UserRespVO userVO = BeanUtil.toBean(user, UserRespVO.class);
        if (dept != null) {
            userVO.setDeptName(dept.getName());
        }
        return userVO;
    }

    default List<UserSimpleRespVO> convertSimpleList(List<AdminUserDO> list, Map<Long, DeptDO> deptMap) {
        return CollectionUtils.convertList(list, user -> {
            UserSimpleRespVO userVO = BeanUtil.toBean(user, UserSimpleRespVO.class);
            MapUtils.findAndThen(deptMap, user.getDeptId(), dept -> userVO.setDeptName(dept.getName()));
            return userVO;
        });
    }

    default UserProfileRespVO convert(AdminUserDO user, List<RoleDO> userRoles,
                                      DeptDO dept, List<PostDO> posts) {
        UserProfileRespVO userVO = BeanUtil.toBean(user, UserProfileRespVO.class);
        userVO.setRoles(userRoles == null ? null
                : CollectionUtils.convertList(userRoles, r -> BeanUtil.toBean(r, RoleSimpleRespVO.class)));
        userVO.setDept(dept == null ? null : BeanUtil.toBean(dept, DeptSimpleRespVO.class));
        userVO.setPosts(posts == null ? null
                : CollectionUtils.convertList(posts, p -> BeanUtil.toBean(p, PostSimpleRespVO.class)));
        if (user != null) {
            userVO.setPasswordMustChange(Integer.valueOf(1).equals(user.getPasswordMustChange()));
        }
        return userVO;
    }

}
