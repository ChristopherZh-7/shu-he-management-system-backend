package cn.shuhe.system.module.system.service.social;

import cn.shuhe.system.framework.common.exception.ServiceException;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.system.api.social.dto.SocialUserBindReqDTO;
import cn.shuhe.system.module.system.api.social.dto.SocialUserRespDTO;
import cn.shuhe.system.module.system.controller.admin.socail.vo.user.SocialUserPageReqVO;
import cn.shuhe.system.module.system.dal.dataobject.social.SocialUserDO;
import cn.shuhe.system.module.system.enums.social.SocialTypeEnum;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 社交用户 Service 接口，例如说社交平台的授权登录
 *
 * @author ShuHe
 */
public interface SocialUserService {

    /**
     * 获得指定用户的社交用户列表
     *
     * @param userId   用户编号
     * @param userType 用户类型
     * @return 社交用户列表
     */
    List<SocialUserDO> getSocialUserList(Long userId, Integer userType);

    /**
     * 绑定社交用户
     *
     * @param reqDTO 绑定信息
     * @return 社交用户 openid
     */
    String bindSocialUser(@Valid SocialUserBindReqDTO reqDTO);

    /**
     * 钉钉通讯录同步后，用 unionid（与 OAuth 扫码登录写入的 openid 一致）预绑定管理端用户与钉钉身份
     *
     * @param userId  后台用户编号 {@link cn.shuhe.system.framework.common.enums.UserTypeEnum#ADMIN}
     * @param unionid 钉钉 unionid
     * @param nickname 展示昵称，可空
     */
    void bindDingtalkUserByUnionid(Long userId, String unionid, String nickname);

    /**
     * 解除管理端用户与钉钉（type=钉钉）的绑定，用于离职同步等场景
     *
     * @param userId 后台用户编号 {@link cn.shuhe.system.framework.common.enums.UserTypeEnum#ADMIN}
     */
    void unbindDingtalkForAdminUser(Long userId);

    /**
     * 取消绑定社交用户
     *
     * @param userId 用户编号
     * @param userType 全局用户类型
     * @param socialType 社交平台的类型 {@link SocialTypeEnum}
     * @param openid 社交平台的 openid
     */
    void unbindSocialUser(Long userId, Integer userType, Integer socialType, String openid);

    /**
     * 获得社交用户，基于 userId
     *
     * @param userType 用户类型
     * @param userId 用户编号
     * @param socialType 社交平台的类型
     * @return 社交用户
     */
    SocialUserRespDTO getSocialUserByUserId(Integer userType, Long userId, Integer socialType);

    /**
     * 获得社交用户
     *
     * 在认证信息不正确的情况下，也会抛出 {@link ServiceException} 业务异常
     *
     * @param userType 用户类型
     * @param socialType 社交平台的类型
     * @param code 授权码
     * @param state state
     * @return 社交用户
     */
    SocialUserRespDTO getSocialUserByCode(Integer userType, Integer socialType, String code, String state);

    /**
     * 根据 openid（钉钉为 unionId）解析已绑定的后台用户，用于端内免登等非 OAuth code 场景
     *
     * @param userType   用户类型
     * @param socialType 社交平台类型
     * @param openid     社交平台唯一标识
     * @return 未找到社交用户或未完成绑定时，{@link SocialUserRespDTO#getUserId()} 为 null
     */
    SocialUserRespDTO getSocialUserByOpenid(Integer userType, Integer socialType, String openid);

    // ==================== 社交用户 CRUD ====================

    /**
     * 获得社交用户
     *
     * @param id 编号
     * @return 社交用户
     */
    SocialUserDO getSocialUser(Long id);

    /**
     * 获得社交用户分页
     *
     * @param pageReqVO 分页查询
     * @return 社交用户分页
     */
    PageResult<SocialUserDO> getSocialUserPage(SocialUserPageReqVO pageReqVO);

}
