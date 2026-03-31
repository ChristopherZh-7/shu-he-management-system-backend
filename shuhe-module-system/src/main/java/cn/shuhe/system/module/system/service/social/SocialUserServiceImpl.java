package cn.shuhe.system.module.system.service.social;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.lang.Assert;
import cn.shuhe.system.framework.common.exception.ServiceException;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.system.api.social.dto.SocialUserBindReqDTO;
import cn.shuhe.system.module.system.api.social.dto.SocialUserRespDTO;
import cn.shuhe.system.module.system.controller.admin.socail.vo.user.SocialUserPageReqVO;
import cn.shuhe.system.module.system.dal.dataobject.social.SocialUserBindDO;
import cn.shuhe.system.module.system.dal.dataobject.social.SocialUserDO;
import cn.shuhe.system.module.system.dal.mysql.social.SocialUserBindMapper;
import cn.shuhe.system.module.system.dal.mysql.social.SocialUserMapper;
import cn.shuhe.system.framework.common.enums.UserTypeEnum;
import cn.shuhe.system.module.system.enums.social.SocialTypeEnum;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.shuhe.system.framework.common.util.json.JsonUtils.toJsonString;
import static cn.shuhe.system.module.system.enums.ErrorCodeConstants.SOCIAL_USER_NOT_FOUND;

/**
 * 社交用户 Service 实现类
 *
 * @author ShuHe
 */
@Service
@Validated
@Slf4j
public class SocialUserServiceImpl implements SocialUserService {

    @Resource
    private SocialUserBindMapper socialUserBindMapper;
    @Resource
    private SocialUserMapper socialUserMapper;

    @Resource
    private SocialClientService socialClientService;

    @Override
    public List<SocialUserDO> getSocialUserList(Long userId, Integer userType) {
        // 获得绑定
        List<SocialUserBindDO> socialUserBinds = socialUserBindMapper.selectListByUserIdAndUserType(userId, userType);
        if (CollUtil.isEmpty(socialUserBinds)) {
            return Collections.emptyList();
        }
        // 获得社交用户
        return socialUserMapper.selectByIds(convertSet(socialUserBinds, SocialUserBindDO::getSocialUserId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String bindSocialUser(SocialUserBindReqDTO reqDTO) {
        // 获得社交用户
        SocialUserDO socialUser = authSocialUser(reqDTO.getSocialType(), reqDTO.getUserType(),
                reqDTO.getCode(), reqDTO.getState());
        Assert.notNull(socialUser, "社交用户不能为空");

        // 社交用户可能之前绑定过别的用户，需要进行解绑
        socialUserBindMapper.deleteByUserTypeAndSocialUserId(reqDTO.getUserType(), socialUser.getId());

        // 用户可能之前已经绑定过该社交类型，需要进行解绑
        socialUserBindMapper.deleteByUserTypeAndUserIdAndSocialType(reqDTO.getUserType(), reqDTO.getUserId(),
                socialUser.getType());

        // 绑定当前登录的社交用户
        SocialUserBindDO socialUserBind = SocialUserBindDO.builder()
                .userId(reqDTO.getUserId()).userType(reqDTO.getUserType())
                .socialUserId(socialUser.getId()).socialType(socialUser.getType()).build();
        socialUserBindMapper.insert(socialUserBind);
        return socialUser.getOpenid();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void bindDingtalkUserByUnionid(Long userId, String unionid, String nickname) {
        if (userId == null || StrUtil.isEmpty(unionid)) {
            return;
        }
        Integer userType = UserTypeEnum.ADMIN.getValue();
        Integer socialType = SocialTypeEnum.DINGTALK.getType();

        SocialUserDO socialUser = socialUserMapper.selectByTypeAndOpenid(socialType, unionid);
        if (socialUser == null) {
            socialUser = new SocialUserDO();
            socialUser.setType(socialType);
            socialUser.setOpenid(unionid);
            // 表字段 raw_token_info / raw_user_info / code / nickname 为 NOT NULL；非 OAuth 场景用占位，首次扫码后会由 authSocialUser 覆盖
            socialUser.setRawTokenInfo("{}");
            socialUser.setRawUserInfo("{}");
            socialUser.setCode("sync");
            socialUser.setNickname(StrUtil.emptyToDefault(nickname, ""));
            socialUserMapper.insert(socialUser);
        } else {
            if (StrUtil.isNotEmpty(nickname)) {
                socialUser.setNickname(nickname);
                socialUserMapper.updateById(socialUser);
            }
        }

        socialUserBindMapper.deleteByUserTypeAndSocialUserId(userType, socialUser.getId());
        socialUserBindMapper.deleteByUserTypeAndUserIdAndSocialType(userType, userId, socialType);

        SocialUserBindDO socialUserBind = SocialUserBindDO.builder()
                .userId(userId).userType(userType)
                .socialUserId(socialUser.getId()).socialType(socialType).build();
        socialUserBindMapper.insert(socialUserBind);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void unbindDingtalkForAdminUser(Long userId) {
        if (userId == null) {
            return;
        }
        socialUserBindMapper.deleteByUserTypeAndUserIdAndSocialType(
                UserTypeEnum.ADMIN.getValue(), userId, SocialTypeEnum.DINGTALK.getType());
    }

    @Override
    public void unbindSocialUser(Long userId, Integer userType, Integer socialType, String openid) {
        // 获得 openid 对应的 SocialUserDO 社交用户
        SocialUserDO socialUser = socialUserMapper.selectByTypeAndOpenid(socialType, openid);
        if (socialUser == null) {
            throw exception(SOCIAL_USER_NOT_FOUND);
        }

        // 获得对应的社交绑定关系
        socialUserBindMapper.deleteByUserTypeAndUserIdAndSocialType(userType, userId, socialUser.getType());
    }

    @Override
    public SocialUserRespDTO getSocialUserByUserId(Integer userType, Long userId, Integer socialType) {
        // 获得绑定用户
        SocialUserBindDO socialUserBind = socialUserBindMapper.selectByUserIdAndUserTypeAndSocialType(userId, userType, socialType);
        if (socialUserBind == null) {
            return null;
        }
        // 获得社交用户
        SocialUserDO socialUser = socialUserMapper.selectById(socialUserBind.getSocialUserId());
        Assert.notNull(socialUser, "社交用户不能为空");
        return new SocialUserRespDTO(socialUser.getOpenid(), socialUser.getNickname(), socialUser.getAvatar(),
                socialUserBind.getUserId());
    }

    @Override
    public SocialUserRespDTO getSocialUserByCode(Integer userType, Integer socialType, String code, String state) {
        // 获得社交用户
        SocialUserDO socialUser = authSocialUser(socialType, userType, code, state);
        Assert.notNull(socialUser, "社交用户不能为空");

        // 获得绑定用户
        SocialUserBindDO socialUserBind = socialUserBindMapper.selectByUserTypeAndSocialUserId(userType,
                socialUser.getId());
        return new SocialUserRespDTO(socialUser.getOpenid(), socialUser.getNickname(), socialUser.getAvatar(),
                socialUserBind != null ? socialUserBind.getUserId() : null);
    }

    @Override
    public SocialUserRespDTO getSocialUserByOpenid(Integer userType, Integer socialType, String openid) {
        SocialUserDO socialUser = socialUserMapper.selectByTypeAndOpenid(socialType, openid);
        if (socialUser == null) {
            return new SocialUserRespDTO(openid, null, null, null);
        }
        SocialUserBindDO socialUserBind = socialUserBindMapper.selectByUserTypeAndSocialUserId(userType,
                socialUser.getId());
        return new SocialUserRespDTO(socialUser.getOpenid(), socialUser.getNickname(), socialUser.getAvatar(),
                socialUserBind != null ? socialUserBind.getUserId() : null);
    }

    /**
     * 授权获得对应的社交用户
     * 如果授权失败，则会抛出 {@link ServiceException} 异常
     *
     * @param socialType 社交平台的类型 {@link SocialTypeEnum}
     * @param userType 用户类型
     * @param code     授权码
     * @param state    state
     * @return 授权用户
     */
    @NotNull
    public SocialUserDO authSocialUser(Integer socialType, Integer userType, String code, String state) {
        // 优先从 DB 中获取，因为 code 有且可以使用一次。
        // 在社交登录时，当未绑定 User 时，需要绑定登录，此时需要 code 使用两次
        SocialUserDO socialUser = socialUserMapper.selectByTypeAndCodeAnState(socialType, code, state);
        if (socialUser != null) {
            return socialUser;
        }

        // 请求获取
        AuthUser authUser = socialClientService.getAuthUser(socialType, userType, code, state);
        Assert.notNull(authUser, "三方用户不能为空");

        // 保存到 DB 中
        socialUser = socialUserMapper.selectByTypeAndOpenid(socialType, authUser.getUuid());
        if (socialUser == null) {
            socialUser = new SocialUserDO();
        }
        socialUser.setType(socialType).setCode(code).setState(state) // 需要保存 code + state 字段，保证后续可查询
                .setOpenid(authUser.getUuid()).setToken(authUser.getToken().getAccessToken()).setRawTokenInfo((toJsonString(authUser.getToken())))
                .setNickname(authUser.getNickname()).setAvatar(authUser.getAvatar()).setRawUserInfo(toJsonString(authUser.getRawUserInfo()));
        if (socialUser.getId() == null) {
            socialUserMapper.insert(socialUser);
        } else {
            socialUser.clean(); // 避免 updateTime 不更新：https://gitee.com/shuhecode/shuhe-boot-mini/issues/ID7FUL
            socialUserMapper.updateById(socialUser);
        }
        return socialUser;
    }

    // ==================== 社交用户 CRUD ====================

    @Override
    public SocialUserDO getSocialUser(Long id) {
        return socialUserMapper.selectById(id);
    }

    @Override
    public PageResult<SocialUserDO> getSocialUserPage(SocialUserPageReqVO pageReqVO) {
        return socialUserMapper.selectPage(pageReqVO);
    }

}
