package cn.shuhe.system.module.system.service.dingtalkconfig;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 钉钉API调用服务
 */
@Slf4j
@Service
public class DingtalkApiService {

    // 钉钉API地址
    private static final String DINGTALK_GET_TOKEN_URL = "https://oapi.dingtalk.com/gettoken";
    private static final String DINGTALK_DEPT_LIST_URL = "https://oapi.dingtalk.com/topapi/v2/department/listsub";
    private static final String DINGTALK_USER_LIST_URL = "https://oapi.dingtalk.com/topapi/v2/user/list";
    private static final String DINGTALK_USER_GET_URL = "https://oapi.dingtalk.com/topapi/v2/user/get";
    // 智能人事API
    private static final String DINGTALK_HRM_EMPLOYEE_LIST_URL = "https://oapi.dingtalk.com/topapi/smartwork/hrm/employee/queryonjob";
    private static final String DINGTALK_HRM_DIMISSION_LIST_URL = "https://oapi.dingtalk.com/topapi/smartwork/hrm/employee/querydimission";
    private static final String DINGTALK_HRM_ROSTER_META_URL = "https://oapi.dingtalk.com/topapi/smartwork/hrm/roster/meta/get";
    private static final String DINGTALK_HRM_EMPLOYEE_ROSTER_URL = "https://oapi.dingtalk.com/topapi/smartwork/hrm/employee/v2/list";

    /**
     * 获取钉钉access_token
     *
     * @param config 钉钉配置
     * @return access_token
     */
    public String getAccessToken(DingtalkConfigDO config) {
        String url = DINGTALK_GET_TOKEN_URL + "?appkey=" + config.getClientId() + "&appsecret=" + config.getClientSecret();
        String result = HttpUtil.get(url);
        JSONObject json = JSONUtil.parseObj(result);
        
        int errcode = json.getInt("errcode", -1);
        if (errcode != 0) {
            String errmsg = json.getStr("errmsg", "未知错误");
            log.error("获取钉钉access_token失败: errcode={}, errmsg={}", errcode, errmsg);
            throw new RuntimeException("获取钉钉access_token失败: " + errmsg);
        }
        
        return json.getStr("access_token");
    }

    /**
     * 获取钉钉部门列表（递归获取所有子部门）
     *
     * @param accessToken access_token
     * @param parentDeptId 父部门ID，根部门为1
     * @return 部门列表
     */
    public List<DingtalkDept> getDeptList(String accessToken, Long parentDeptId) {
        List<DingtalkDept> allDepts = new ArrayList<>();
        getDeptListRecursive(accessToken, parentDeptId, allDepts);
        return allDepts;
    }

    /**
     * 递归获取部门列表
     */
    private void getDeptListRecursive(String accessToken, Long parentDeptId, List<DingtalkDept> allDepts) {
        String url = DINGTALK_DEPT_LIST_URL + "?access_token=" + accessToken;
        
        Map<String, Object> params = new HashMap<>();
        params.put("dept_id", parentDeptId);
        
        String result = HttpUtil.post(url, JSONUtil.toJsonStr(params));
        log.debug("钉钉部门API返回: {}", result);
        JSONObject json = JSONUtil.parseObj(result);
        
        int errcode = json.getInt("errcode", -1);
        if (errcode != 0) {
            String errmsg = json.getStr("errmsg", "未知错误");
            log.error("获取钉钉部门列表失败: errcode={}, errmsg={}", errcode, errmsg);
            throw new RuntimeException("获取钉钉部门列表失败: " + errmsg);
        }
        
        // 兼容不同的返回格式
        Object resultData = json.get("result");
        JSONArray deptList = null;
        
        if (resultData instanceof JSONArray) {
            // result 直接是数组
            deptList = (JSONArray) resultData;
        } else if (resultData instanceof JSONObject) {
            // result 是对象，部门列表在 dept_list 字段
            JSONObject resultObj = (JSONObject) resultData;
            deptList = resultObj.getJSONArray("dept_list");
        }
        
        if (deptList == null || deptList.isEmpty()) {
            return;
        }
        
        for (int i = 0; i < deptList.size(); i++) {
            JSONObject deptJson = deptList.getJSONObject(i);
            DingtalkDept dept = new DingtalkDept();
            dept.setDeptId(deptJson.getLong("dept_id"));
            dept.setName(deptJson.getStr("name"));
            dept.setParentId(deptJson.getLong("parent_id"));
            dept.setOrder(deptJson.getInt("order", 0));
            allDepts.add(dept);
            
            // 递归获取子部门
            getDeptListRecursive(accessToken, dept.getDeptId(), allDepts);
        }
    }

    /**
     * 钉钉部门信息
     */
    @lombok.Data
    public static class DingtalkDept {
        /** 部门ID */
        private Long deptId;
        /** 部门名称 */
        private String name;
        /** 父部门ID */
        private Long parentId;
        /** 排序 */
        private Integer order;
    }

    /**
     * 获取钉钉部门下的用户列表
     *
     * @param accessToken access_token
     * @param deptId 部门ID
     * @return 用户列表
     */
    public List<DingtalkUser> getUserList(String accessToken, Long deptId) {
        List<DingtalkUser> allUsers = new ArrayList<>();
        getUserListPage(accessToken, deptId, 0, allUsers);
        return allUsers;
    }

    /**
     * 分页获取部门用户
     */
    private void getUserListPage(String accessToken, Long deptId, int cursor, List<DingtalkUser> allUsers) {
        String url = DINGTALK_USER_LIST_URL + "?access_token=" + accessToken;
        
        Map<String, Object> params = new HashMap<>();
        params.put("dept_id", deptId);
        params.put("cursor", cursor);
        params.put("size", 100);
        
        String result = HttpUtil.post(url, JSONUtil.toJsonStr(params));
        log.debug("钉钉用户API返回: {}", result);
        JSONObject json = JSONUtil.parseObj(result);
        
        int errcode = json.getInt("errcode", -1);
        if (errcode != 0) {
            String errmsg = json.getStr("errmsg", "未知错误");
            log.error("获取钉钉用户列表失败: errcode={}, errmsg={}", errcode, errmsg);
            throw new RuntimeException("获取钉钉用户列表失败: " + errmsg);
        }
        
        JSONObject resultObj = json.getJSONObject("result");
        if (resultObj == null) {
            return;
        }
        
        JSONArray userList = resultObj.getJSONArray("list");
        if (userList != null && !userList.isEmpty()) {
            for (int i = 0; i < userList.size(); i++) {
                JSONObject userJson = userList.getJSONObject(i);
                DingtalkUser user = new DingtalkUser();
                user.setUserid(userJson.getStr("userid"));
                user.setName(userJson.getStr("name"));
                user.setMobile(userJson.getStr("mobile"));
                user.setEmail(userJson.getStr("email"));
                user.setAvatar(userJson.getStr("avatar"));
                user.setDeptIdList(userJson.getJSONArray("dept_id_list"));
                allUsers.add(user);
            }
        }
        
        // 如果还有更多数据，继续分页获取
        Boolean hasMore = resultObj.getBool("has_more", false);
        if (hasMore) {
            int nextCursor = resultObj.getInt("next_cursor", 0);
            getUserListPage(accessToken, deptId, nextCursor, allUsers);
        }
    }

    /**
     * 根据userid获取单个用户信息
     * 
     * @param accessToken access_token
     * @param userid 用户ID
     * @return 用户信息，如果获取失败返回null
     */
    public DingtalkUser getUserByUserId(String accessToken, String userid) {
        String url = DINGTALK_USER_GET_URL + "?access_token=" + accessToken;
        
        Map<String, Object> params = new HashMap<>();
        params.put("userid", userid);
        
        try {
            String result = HttpUtil.post(url, JSONUtil.toJsonStr(params));
            log.debug("钉钉用户详情API返回: userid={}, result={}", userid, result);
            JSONObject json = JSONUtil.parseObj(result);
            
            int errcode = json.getInt("errcode", -1);
            if (errcode != 0) {
                log.warn("获取钉钉用户详情失败: userid={}, errcode={}, errmsg={}", 
                        userid, errcode, json.getStr("errmsg", "未知错误"));
                return null;
            }
            
            JSONObject resultObj = json.getJSONObject("result");
            if (resultObj == null) {
                return null;
            }
            
            DingtalkUser user = new DingtalkUser();
            user.setUserid(resultObj.getStr("userid"));
            user.setName(resultObj.getStr("name"));
            user.setMobile(resultObj.getStr("mobile"));
            user.setEmail(resultObj.getStr("email"));
            user.setAvatar(resultObj.getStr("avatar"));
            user.setDeptIdList(resultObj.getJSONArray("dept_id_list"));
            return user;
        } catch (Exception e) {
            log.warn("获取钉钉用户详情异常: userid={}, error={}", userid, e.getMessage());
            return null;
        }
    }

    /**
     * 钉钉用户信息
     */
    @lombok.Data
    public static class DingtalkUser {
        /** 用户ID */
        private String userid;
        /** 姓名 */
        private String name;
        /** 手机号 */
        private String mobile;
        /** 邮箱 */
        private String email;
        /** 头像 */
        private String avatar;
        /** 所属部门ID列表 */
        private JSONArray deptIdList;
        /** 入职日期 (从智能人事获取) */
        private String hireDate;
        /** 离职日期 (从智能人事获取) */
        private String resignDate;
        /** 在职状态: 1-在职, 2-离职 */
        private Integer employeeStatus;
    }

    // ==================== 智能人事API ====================

    /**
     * 获取在职员工userid列表
     *
     * @param accessToken access_token
     * @return 在职员工userid列表
     */
    public List<String> getOnJobEmployeeUserIds(String accessToken) {
        List<String> allUserIds = new ArrayList<>();
        getOnJobEmployeeUserIdsPage(accessToken, 0, allUserIds);
        return allUserIds;
    }

    /**
     * 分页获取在职员工userid列表
     */
    private void getOnJobEmployeeUserIdsPage(String accessToken, int offset, List<String> allUserIds) {
        String url = DINGTALK_HRM_EMPLOYEE_LIST_URL + "?access_token=" + accessToken;

        Map<String, Object> params = new HashMap<>();
        params.put("status_list", "2,3,5,-1"); // 2-试用期, 3-正式, 5-待离职, -1-无状态
        params.put("offset", offset);
        params.put("size", 50);

        String result = HttpUtil.post(url, JSONUtil.toJsonStr(params));
        log.debug("钉钉在职员工列表API返回: {}", result);
        JSONObject json = JSONUtil.parseObj(result);

        int errcode = json.getInt("errcode", -1);
        if (errcode != 0) {
            String errmsg = json.getStr("errmsg", "未知错误");
            log.error("获取钉钉在职员工列表失败: errcode={}, errmsg={}", errcode, errmsg);
            throw new RuntimeException("获取钉钉在职员工列表失败: " + errmsg);
        }

        JSONObject resultObj = json.getJSONObject("result");
        if (resultObj == null) {
            return;
        }

        JSONArray dataList = resultObj.getJSONArray("data_list");
        if (dataList != null && !dataList.isEmpty()) {
            for (int i = 0; i < dataList.size(); i++) {
                allUserIds.add(dataList.getStr(i));
            }
        }

        // 如果还有更多数据，继续分页获取
        Boolean hasMore = resultObj.getBool("next_cursor") != null;
        Integer nextCursor = resultObj.getInt("next_cursor");
        if (nextCursor != null && nextCursor > 0) {
            getOnJobEmployeeUserIdsPage(accessToken, nextCursor, allUserIds);
        }
    }

    /**
     * 获取离职员工userid列表
     *
     * @param accessToken access_token
     * @return 离职员工userid列表
     */
    public List<String> getDimissionEmployeeUserIds(String accessToken) {
        List<String> allUserIds = new ArrayList<>();
        getDimissionEmployeeUserIdsPage(accessToken, 0, allUserIds);
        return allUserIds;
    }

    /**
     * 分页获取离职员工userid列表
     */
    private void getDimissionEmployeeUserIdsPage(String accessToken, int offset, List<String> allUserIds) {
        String url = DINGTALK_HRM_DIMISSION_LIST_URL + "?access_token=" + accessToken;

        Map<String, Object> params = new HashMap<>();
        params.put("offset", offset);
        params.put("size", 50);
        // 离职状态列表: 5-主动离职, 6-被动离职 (根据钉钉文档)
        params.put("status_list", Arrays.asList("5", "6"));

        log.info("调用钉钉离职员工列表API, offset={}, params={}", offset, params);
        String result = HttpUtil.post(url, JSONUtil.toJsonStr(params));
        log.info("钉钉离职员工列表API返回: {}", result);
        JSONObject json = JSONUtil.parseObj(result);

        int errcode = json.getInt("errcode", -1);
        if (errcode != 0) {
            String errmsg = json.getStr("errmsg", "未知错误");
            log.error("获取钉钉离职员工列表失败: errcode={}, errmsg={}", errcode, errmsg);
            throw new RuntimeException("获取钉钉离职员工列表失败: " + errmsg);
        }

        JSONObject resultObj = json.getJSONObject("result");
        if (resultObj == null) {
            return;
        }

        JSONArray dataList = resultObj.getJSONArray("data_list");
        if (dataList != null && !dataList.isEmpty()) {
            for (int i = 0; i < dataList.size(); i++) {
                allUserIds.add(dataList.getStr(i));
            }
        }

        // 如果还有更多数据，继续分页获取
        Integer nextCursor = resultObj.getInt("next_cursor");
        if (nextCursor != null && nextCursor > 0) {
            getDimissionEmployeeUserIdsPage(accessToken, nextCursor, allUserIds);
        }
    }

    /**
     * 获取员工花名册信息（包含入职日期、离职日期等）
     *
     * @param accessToken access_token
     * @param agentId 应用的agentId
     * @param userIds 用户ID列表
     * @return 员工花名册信息
     */
    public List<HrmEmployeeInfo> getEmployeeRosterInfo(String accessToken, String agentId, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<HrmEmployeeInfo> result = new ArrayList<>();
        // 每次最多查询50人
        int batchSize = 50;
        for (int i = 0; i < userIds.size(); i += batchSize) {
            List<String> batchUserIds = userIds.subList(i, Math.min(i + batchSize, userIds.size()));
            List<HrmEmployeeInfo> batchResult = getEmployeeRosterInfoBatch(accessToken, agentId, batchUserIds);
            result.addAll(batchResult);
        }
        return result;
    }

    /**
     * 批量获取员工花名册信息
     */
    private List<HrmEmployeeInfo> getEmployeeRosterInfoBatch(String accessToken, String agentId, List<String> userIds) {
        String url = DINGTALK_HRM_EMPLOYEE_ROSTER_URL + "?access_token=" + accessToken;

        Map<String, Object> params = new HashMap<>();
        params.put("agentid", agentId);  // 必须参数
        params.put("userid_list", String.join(",", userIds));
        // 需要的花名册字段
        // sys00-name: 姓名, sys01-confirmJoinTime: 入职时间, sys01-regularTime: 转正日期
        // 离职相关字段（钉钉API可能不返回）: sys01-terminateTime, sys02-lastWorkDay, sys02-quitDate, sys02-terminationDate
        params.put("field_filter_list", "sys00-name,sys00-mobile,sys00-email,sys01-confirmJoinTime,sys01-regularTime,sys01-terminateTime,sys02-lastWorkDay,sys02-quitDate,sys02-terminationDate,sys00-avatar");

        log.info("调用钉钉花名册API, agentId={}, userIds={}", agentId, userIds);
        String result = HttpUtil.post(url, JSONUtil.toJsonStr(params));
        log.info("钉钉花名册API返回: {}", result);
        JSONObject json = JSONUtil.parseObj(result);

        int errcode = json.getInt("errcode", -1);
        if (errcode != 0) {
            String errmsg = json.getStr("errmsg", "未知错误");
            log.error("获取钉钉花名册信息失败: errcode={}, errmsg={}", errcode, errmsg);
            // 智能人事API可能没有权限，不抛出异常，返回空列表
            return new ArrayList<>();
        }

        JSONArray resultArray = json.getJSONArray("result");
        if (resultArray == null || resultArray.isEmpty()) {
            return new ArrayList<>();
        }

        List<HrmEmployeeInfo> infoList = new ArrayList<>();
        for (int i = 0; i < resultArray.size(); i++) {
            JSONObject empJson = resultArray.getJSONObject(i);
            HrmEmployeeInfo info = new HrmEmployeeInfo();
            info.setUserid(empJson.getStr("userid"));

            // 解析字段 - 支持三种格式:
            // 格式1（钉钉实际格式）: field_data_list -> [{field_code, field_value_list: [{value}]}]
            // 格式2（直接）: field_data_list -> [{field_code, value}, ...]
            // 格式3（嵌套）: field_data_list -> [{field_list: [{field_code, value}]}]
            JSONArray fieldDataList = empJson.getJSONArray("field_data_list");
            if (fieldDataList != null) {
                for (int j = 0; j < fieldDataList.size(); j++) {
                    JSONObject fieldItem = fieldDataList.getJSONObject(j);
                    
                    String fieldCode = fieldItem.getStr("field_code");
                    if (StrUtil.isNotEmpty(fieldCode)) {
                        // 格式1：钉钉实际返回格式 - field_value_list 数组包含 value
                        JSONArray fieldValueList = fieldItem.getJSONArray("field_value_list");
                        if (fieldValueList != null && !fieldValueList.isEmpty()) {
                            JSONObject firstValue = fieldValueList.getJSONObject(0);
                            if (firstValue != null) {
                                String fieldValue = firstValue.getStr("value");
                                if (StrUtil.isEmpty(fieldValue)) {
                                    // 有些字段用 label 而不是 value
                                    fieldValue = firstValue.getStr("label");
                                }
                                parseHrmField(info, fieldCode, fieldValue);
                            }
                        } else {
                            // 格式2：直接包含 value
                            String fieldValue = fieldItem.getStr("value");
                            parseHrmField(info, fieldCode, fieldValue);
                        }
                    } else {
                        // 格式3：包含 field_list 嵌套数组
                        JSONArray fieldList = fieldItem.getJSONArray("field_list");
                        if (fieldList != null) {
                            for (int k = 0; k < fieldList.size(); k++) {
                                JSONObject field = fieldList.getJSONObject(k);
                                String code = field.getStr("field_code");
                                String fieldValue = field.getStr("value");
                                parseHrmField(info, code, fieldValue);
                            }
                        }
                    }
                }
            }
            
            log.debug("解析花名册用户: userid={}, name={}, mobile={}", 
                    info.getUserid(), info.getName(), info.getMobile());
            infoList.add(info);
        }
        return infoList;
    }

    /**
     * 解析花名册字段
     */
    private void parseHrmField(HrmEmployeeInfo info, String fieldCode, String fieldValue) {
        if (StrUtil.isEmpty(fieldCode) || StrUtil.isEmpty(fieldValue)) {
            return;
        }
        switch (fieldCode) {
            case "sys00-name":
                info.setName(fieldValue);
                break;
            case "sys00-mobile":
                info.setMobile(fieldValue);
                break;
            case "sys00-email":
                info.setEmail(fieldValue);
                break;
            case "sys00-avatar":
                info.setAvatar(fieldValue);
                break;
            case "sys01-confirmJoinTime":
                // 入职日期
                info.setConfirmJoinTime(fieldValue);
                break;
            case "sys01-regularTime":
                // 转正日期 - 如果没有入职日期，用转正日期作为备选
                info.setRegularTime(fieldValue);
                if (StrUtil.isEmpty(info.getConfirmJoinTime())) {
                    info.setConfirmJoinTime(fieldValue);
                }
                break;
            case "sys01-terminateTime":
                // 离职时间
                info.setLastWorkDay(fieldValue);
                break;
            case "sys02-lastWorkDay":
            case "sys02-quitDate":
            case "sys02-terminationDate":
                // 最后工作日/离职日期（作为备选）
                if (StrUtil.isEmpty(info.getLastWorkDay())) {
                    info.setLastWorkDay(fieldValue);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 智能人事员工花名册信息
     */
    @lombok.Data
    public static class HrmEmployeeInfo {
        /** 用户ID */
        private String userid;
        /** 姓名 */
        private String name;
        /** 手机号 */
        private String mobile;
        /** 邮箱 */
        private String email;
        /** 头像 */
        private String avatar;
        /** 入职日期 (yyyy-MM-dd) */
        private String confirmJoinTime;
        /** 转正日期 (yyyy-MM-dd) */
        private String regularTime;
        /** 最后工作日/离职日期 (yyyy-MM-dd) */
        private String lastWorkDay;
    }
}
