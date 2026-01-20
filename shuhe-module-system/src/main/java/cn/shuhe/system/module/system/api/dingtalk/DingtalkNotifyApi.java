package cn.shuhe.system.module.system.api.dingtalk;

import cn.shuhe.system.module.system.api.dingtalk.dto.DingtalkNotifySendReqDTO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 钉钉通知 API 接口
 */
public interface DingtalkNotifyApi {

    /**
     * 发送钉钉工作通知
     *
     * @param reqDTO 发送请求
     * @return 是否成功
     */
    boolean sendWorkNotice(@Valid DingtalkNotifySendReqDTO reqDTO);

    /**
     * 发送钉钉工作通知给指定部门类型的负责人
     *
     * @param deptType 部门类型 (1-安全服务, 2-安全运营, 3-数据安全)
     * @param title 消息标题
     * @param content 消息内容（markdown格式）
     * @return 是否成功
     */
    boolean sendWorkNoticeToDeptTypeLeaders(Integer deptType, String title, String content);

    /**
     * 根据系统用户ID获取钉钉用户ID
     *
     * @param userId 系统用户ID
     * @return 钉钉用户ID，如果找不到返回null
     */
    String getDingtalkUserIdByLocalUserId(Long userId);

    /**
     * 根据部门类型获取部门负责人用户ID列表
     *
     * @param deptType 部门类型
     * @return 负责人用户ID列表
     */
    List<Long> getLeaderUserIdsByDeptType(Integer deptType);

}
