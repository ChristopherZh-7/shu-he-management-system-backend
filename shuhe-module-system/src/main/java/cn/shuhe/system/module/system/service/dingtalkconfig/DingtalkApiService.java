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
    private static final String DINGTALK_DEPT_GET_URL = "https://oapi.dingtalk.com/topapi/v2/department/get";
    private static final String DINGTALK_USER_LIST_URL = "https://oapi.dingtalk.com/topapi/v2/user/list";
    private static final String DINGTALK_USER_GET_URL = "https://oapi.dingtalk.com/topapi/v2/user/get";
    // 智能人事API
    private static final String DINGTALK_HRM_EMPLOYEE_LIST_URL = "https://oapi.dingtalk.com/topapi/smartwork/hrm/employee/queryonjob";
    private static final String DINGTALK_HRM_DIMISSION_LIST_URL = "https://oapi.dingtalk.com/topapi/smartwork/hrm/employee/querydimission";
    private static final String DINGTALK_HRM_ROSTER_META_URL = "https://oapi.dingtalk.com/topapi/smartwork/hrm/roster/meta/get";
    private static final String DINGTALK_HRM_EMPLOYEE_ROSTER_URL = "https://oapi.dingtalk.com/topapi/smartwork/hrm/employee/v2/list";
    // 新版离职信息API（可获取离职时间）
    private static final String DINGTALK_HRM_DIMISSION_INFO_URL = "https://api.dingtalk.com/v1.0/hrm/employees/dimissionInfos";

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
        /** 部门主管用户ID列表 */
        private List<String> deptManagerUseridList;
    }

    /**
     * 获取部门详情（包含部门主管信息）
     *
     * @param accessToken access_token
     * @param deptId 部门ID
     * @return 部门详情
     */
    public DingtalkDept getDeptDetail(String accessToken, Long deptId) {
        String url = DINGTALK_DEPT_GET_URL + "?access_token=" + accessToken;
        
        Map<String, Object> params = new HashMap<>();
        params.put("dept_id", deptId);
        
        try {
            String result = HttpUtil.post(url, JSONUtil.toJsonStr(params));
            log.debug("钉钉部门详情API返回: deptId={}, result={}", deptId, result);
            JSONObject json = JSONUtil.parseObj(result);
            
            int errcode = json.getInt("errcode", -1);
            if (errcode != 0) {
                log.warn("获取钉钉部门详情失败: deptId={}, errcode={}, errmsg={}", 
                        deptId, errcode, json.getStr("errmsg", "未知错误"));
                return null;
            }
            
            JSONObject resultObj = json.getJSONObject("result");
            if (resultObj == null) {
                return null;
            }
            
            DingtalkDept dept = new DingtalkDept();
            dept.setDeptId(resultObj.getLong("dept_id"));
            dept.setName(resultObj.getStr("name"));
            dept.setParentId(resultObj.getLong("parent_id"));
            dept.setOrder(resultObj.getInt("order", 0));
            
            // 获取部门主管列表
            JSONArray managerList = resultObj.getJSONArray("dept_manager_userid_list");
            if (managerList != null && !managerList.isEmpty()) {
                List<String> managers = new ArrayList<>();
                for (int i = 0; i < managerList.size(); i++) {
                    managers.add(managerList.getStr(i));
                }
                dept.setDeptManagerUseridList(managers);
            }
            
            return dept;
        } catch (Exception e) {
            log.warn("获取钉钉部门详情异常: deptId={}, error={}", deptId, e.getMessage());
            return null;
        }
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
                user.setTitle(userJson.getStr("title"));
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
            user.setTitle(resultObj.getStr("title"));
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
        /** 职位 */
        private String title;
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
     * 批量获取员工离职信息（包含离职时间等详细信息）
     * 使用新版API: GET /v1.0/hrm/employees/dimissionInfos
     *
     * @param accessToken access_token
     * @param userIds 员工userId列表
     * @return 离职信息Map，key为userId，value为离职时间
     */
    public Map<String, String> getDimissionInfos(String accessToken, List<String> userIds) {
        Map<String, String> resultMap = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return resultMap;
        }
        
        // 每次最多查询50人
        int batchSize = 50;
        for (int i = 0; i < userIds.size(); i += batchSize) {
            List<String> batchUserIds = userIds.subList(i, Math.min(i + batchSize, userIds.size()));
            Map<String, String> batchResult = getDimissionInfosBatch(accessToken, batchUserIds);
            resultMap.putAll(batchResult);
        }
        return resultMap;
    }
    
    /**
     * 批量获取离职信息（单批次，最多50人）
     */
    private Map<String, String> getDimissionInfosBatch(String accessToken, List<String> userIds) {
        Map<String, String> resultMap = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return resultMap;
        }
        
        // 新版API使用GET请求，userIdList作为Query参数（JSON数组格式，需URL编码）
        // 根据钉钉API文档，userIdList需要是JSON数组格式，如 ["userId1","userId2"]
        String userIdListJson = JSONUtil.toJsonStr(userIds);
        String userIdListParamEncoded = cn.hutool.core.util.URLUtil.encodeAll(userIdListJson);
        String url = DINGTALK_HRM_DIMISSION_INFO_URL + "?userIdList=" + userIdListParamEncoded;
        
        log.info("调用钉钉离职信息API, userIds数量={}", userIds.size());
        
        try {
            // 新版API需要在Header中传递access_token
            cn.hutool.http.HttpRequest request = cn.hutool.http.HttpRequest.get(url)
                    .header("x-acs-dingtalk-access-token", accessToken)
                    .header("Content-Type", "application/json");
            
            String result = request.execute().body();
            log.info("钉钉离职信息API返回: {}", result);
            
            JSONObject json = JSONUtil.parseObj(result);
            
            // 检查是否有错误
            if (json.containsKey("code")) {
                String code = json.getStr("code");
                String message = json.getStr("message", "未知错误");
                log.warn("获取钉钉离职信息失败: code={}, message={}", code, message);
                return resultMap;
            }
            
            // 解析返回的离职信息列表
            JSONArray resultList = json.getJSONArray("result");
            if (resultList != null && !resultList.isEmpty()) {
                for (int i = 0; i < resultList.size(); i++) {
                    JSONObject info = resultList.getJSONObject(i);
                    String userId = info.getStr("userId");
                    // 离职时间字段可能是 lastWorkDay 或 gmtQuit
                    String dimissionTime = info.getStr("lastWorkDay");
                    if (StrUtil.isEmpty(dimissionTime)) {
                        dimissionTime = info.getStr("gmtQuit");
                    }
                    if (StrUtil.isEmpty(dimissionTime)) {
                        dimissionTime = info.getStr("gmtDimission");
                    }
                    if (StrUtil.isNotEmpty(userId) && StrUtil.isNotEmpty(dimissionTime)) {
                        resultMap.put(userId, dimissionTime);
                        log.info("获取到离职时间: userId={}, dimissionTime={}", userId, dimissionTime);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("调用钉钉离职信息API异常: {}", e.getMessage());
        }
        
        return resultMap;
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
        // sys00-name: 姓名, sys00-confirmJoinTime: 入职时间（注意是sys00分组）, sys01-regularTime: 转正日期
        // 离职相关字段（钉钉API可能不返回）: sys01-terminateTime, sys02-lastWorkDay, sys02-quitDate, sys02-terminationDate
        // 需要的花名册字段，包含职级 sys01-positionLevel（注意职级在sys01分组下）
        params.put("field_filter_list", "sys00-name,sys00-mobile,sys00-email,sys00-confirmJoinTime,sys01-regularTime,sys01-terminateTime,sys02-lastWorkDay,sys02-quitDate,sys02-terminationDate,sys00-avatar,sys01-positionLevel");

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
                                String fieldValue;
                                // 对于职级字段，使用 label（可读名称）而不是 value（数字代码）
                                if ("sys01-positionLevel".equals(fieldCode)) {
                                    fieldValue = firstValue.getStr("label");
                                    if (StrUtil.isEmpty(fieldValue)) {
                                        fieldValue = firstValue.getStr("value");
                                    }
                                } else {
                                    fieldValue = firstValue.getStr("value");
                                    if (StrUtil.isEmpty(fieldValue)) {
                                        // 有些字段用 label 而不是 value
                                        fieldValue = firstValue.getStr("label");
                                    }
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
            
            log.debug("解析花名册用户: userid={}, name={}, mobile={}, positionLevel={}", 
                    info.getUserid(), info.getName(), info.getMobile(), info.getPositionLevel());
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
            case "sys00-confirmJoinTime":
                // 入职日期（注意是sys00分组，不是sys01）
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
            case "sys01-positionLevel":
                // 职级（岗位职级，属于sys01分组）
                info.setPositionLevel(fieldValue);
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
        /** 职级 */
        private String positionLevel;
    }

    // ==================== 消息通知 ====================

    private static final String DINGTALK_MESSAGE_SEND_URL = "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2";

    /**
     * 发送钉钉工作通知
     *
     * @param accessToken access_token
     * @param agentId 应用agentId
     * @param userIdList 接收人钉钉用户ID列表
     * @param title 消息标题
     * @param content 消息内容（markdown格式）
     * @return 是否成功
     */
    public boolean sendWorkNotice(String accessToken, String agentId, List<String> userIdList, String title, String content) {
        if (userIdList == null || userIdList.isEmpty()) {
            log.warn("发送钉钉通知失败：接收人列表为空");
            return false;
        }
        
        String url = DINGTALK_MESSAGE_SEND_URL + "?access_token=" + accessToken;
        
        // 构建消息体 - 使用 markdown 格式
        Map<String, Object> msg = new HashMap<>();
        msg.put("msgtype", "markdown");
        
        Map<String, String> markdown = new HashMap<>();
        markdown.put("title", title);
        markdown.put("text", content);
        msg.put("markdown", markdown);
        
        Map<String, Object> params = new HashMap<>();
        params.put("agent_id", agentId);
        params.put("userid_list", String.join(",", userIdList));
        params.put("msg", msg);
        
        try {
            String result = HttpUtil.post(url, JSONUtil.toJsonStr(params));
            log.debug("钉钉消息发送API返回: {}", result);
            JSONObject json = JSONUtil.parseObj(result);
            
            int errcode = json.getInt("errcode", -1);
            if (errcode != 0) {
                String errmsg = json.getStr("errmsg", "未知错误");
                log.error("发送钉钉工作通知失败: errcode={}, errmsg={}", errcode, errmsg);
                return false;
            }
            
            log.info("发送钉钉工作通知成功，接收人: {}, 标题: {}", userIdList, title);
            return true;
        } catch (Exception e) {
            log.error("发送钉钉工作通知异常", e);
            return false;
        }
    }

    /**
     * 发送钉钉工作通知（简化版，发给单个用户）
     */
    public boolean sendWorkNotice(String accessToken, String agentId, String userId, String title, String content) {
        return sendWorkNotice(accessToken, agentId, Arrays.asList(userId), title, content);
    }

    // ==================== 互动卡片消息 ====================

    /**
     * 发送互动卡片消息（ActionCard，带按钮）
     *
     * @param accessToken access_token
     * @param agentId 应用agentId
     * @param userId 接收人钉钉用户ID
     * @param title 消息标题
     * @param content 消息内容（markdown格式）
     * @param buttonTitle 按钮文字
     * @param buttonUrl 按钮跳转URL
     * @return 是否成功
     */
    public boolean sendActionCardMessage(String accessToken, String agentId, String userId, 
                                          String title, String content, 
                                          String buttonTitle, String buttonUrl) {
        return sendActionCardMessage(accessToken, agentId, Arrays.asList(userId), title, content, buttonTitle, buttonUrl);
    }

    /**
     * 发送互动卡片消息给多人（ActionCard，带按钮）
     *
     * @param accessToken access_token
     * @param agentId 应用agentId
     * @param userIdList 接收人钉钉用户ID列表
     * @param title 消息标题
     * @param content 消息内容（markdown格式）
     * @param buttonTitle 按钮文字
     * @param buttonUrl 按钮跳转URL
     * @return 是否成功
     */
    public boolean sendActionCardMessage(String accessToken, String agentId, List<String> userIdList,
                                          String title, String content,
                                          String buttonTitle, String buttonUrl) {
        if (userIdList == null || userIdList.isEmpty()) {
            log.warn("发送钉钉ActionCard消息失败：接收人列表为空");
            return false;
        }

        String url = DINGTALK_MESSAGE_SEND_URL + "?access_token=" + accessToken;

        // 构建 ActionCard 消息体
        Map<String, Object> msg = new HashMap<>();
        msg.put("msgtype", "action_card");

        Map<String, Object> actionCard = new HashMap<>();
        actionCard.put("title", title);
        actionCard.put("markdown", content);
        actionCard.put("single_title", buttonTitle);
        actionCard.put("single_url", buttonUrl);
        msg.put("action_card", actionCard);

        Map<String, Object> params = new HashMap<>();
        params.put("agent_id", agentId);
        params.put("userid_list", String.join(",", userIdList));
        params.put("msg", msg);

        try {
            String result = HttpUtil.post(url, JSONUtil.toJsonStr(params));
            log.debug("钉钉ActionCard消息发送API返回: {}", result);
            JSONObject json = JSONUtil.parseObj(result);

            int errcode = json.getInt("errcode", -1);
            if (errcode != 0) {
                String errmsg = json.getStr("errmsg", "未知错误");
                log.error("发送钉钉ActionCard消息失败: errcode={}, errmsg={}", errcode, errmsg);
                return false;
            }

            log.info("发送钉钉ActionCard消息成功，接收人: {}, 标题: {}", userIdList, title);
            return true;
        } catch (Exception e) {
            log.error("发送钉钉ActionCard消息异常", e);
            return false;
        }
    }

    // ==================== OA审批流程 ====================

    private static final String DINGTALK_PROCESS_CREATE_URL = "https://oapi.dingtalk.com/topapi/processinstance/create";
    private static final String DINGTALK_PROCESS_FORM_SCHEMA_URL = "https://oapi.dingtalk.com/topapi/workbench/process/get_by_code";

    /**
     * OA审批表单字段
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OaFormField {
        /** 字段名称（组件名称） */
        private String name;
        /** 字段值 */
        private String value;
    }

    /**
     * 获取OA审批表单Schema（用于查看表单组件类型）
     * 
     * API文档：https://dingtalk.apifox.cn/api-140991760
     * 
     * @param accessToken access_token
     * @param processCode 流程模板唯一标识（process_code）
     * @return 表单Schema的JSON字符串，包含所有组件的类型信息
     */
    public String getFormSchema(String accessToken, String processCode) {
        // 注意：钉钉新版API使用 topapi/workbench/process/get_by_code
        // 或者使用 topapi/process/form/get 获取表单结构
        String url = "https://oapi.dingtalk.com/topapi/process/form/get?access_token=" + accessToken;
        
        Map<String, Object> params = new HashMap<>();
        params.put("process_code", processCode);
        
        try {
            String requestBody = JSONUtil.toJsonStr(params);
            log.info("获取表单Schema请求: processCode={}", processCode);
            String result = HttpUtil.post(url, requestBody);
            log.info("获取表单Schema返回: {}", result);
            
            JSONObject json = JSONUtil.parseObj(result);
            int errcode = json.getInt("errcode", -1);
            if (errcode != 0) {
                String errmsg = json.getStr("errmsg", "未知错误");
                log.error("获取表单Schema失败: errcode={}, errmsg={}", errcode, errmsg);
                return null;
            }
            
            // 返回完整的schema信息，包含所有组件的类型
            return result;
        } catch (Exception e) {
            log.error("获取表单Schema异常", e);
            return null;
        }
    }

    /**
     * 获取审批实例ID列表（新版API）
     * 
     * API文档：https://open.dingtalk.com/document/orgapp/obtain-an-approval-list-of-instance-ids
     * 
     * 注意：
     * - 如果只传入startTime，时间距离当前时间不能超过120天
     * - 如果同时传入startTime和endTime，时间范围不能超过120天，startTime距当前时间不能超过365天
     * - 批量获取的实例ID个数最多不能超过10000个
     * 
     * @param accessToken access_token（需要调用getAccessToken获取）
     * @param processCode 审批流的唯一码
     * @param startTime 审批实例开始时间，Unix时间戳，单位毫秒
     * @param endTime 审批实例结束时间，Unix时间戳，单位毫秒（可选，不传则默认取当前时间）
     * @param userIds 发起人userId列表（可选，最大10个）
     * @param statuses 流程实例状态列表（可选）：RUNNING-审批中, TERMINATED-已撤销, COMPLETED-审批完成
     * @return 审批实例ID列表
     */
    public List<String> getProcessInstanceIdList(String accessToken, String processCode, 
                                                  Long startTime, Long endTime,
                                                  List<String> userIds, List<String> statuses) {
        List<String> allInstanceIds = new ArrayList<>();
        getProcessInstanceIdListPage(accessToken, processCode, startTime, endTime, userIds, statuses, 0L, allInstanceIds);
        return allInstanceIds;
    }

    /**
     * 分页获取审批实例ID列表
     */
    private void getProcessInstanceIdListPage(String accessToken, String processCode,
                                               Long startTime, Long endTime,
                                               List<String> userIds, List<String> statuses,
                                               Long nextToken, List<String> allInstanceIds) {
        // 新版API使用 api.dingtalk.com 域名
        String url = "https://api.dingtalk.com/v1.0/workflow/processes/instanceIds/query";

        Map<String, Object> params = new HashMap<>();
        params.put("processCode", processCode);
        params.put("startTime", startTime);
        if (endTime != null) {
            params.put("endTime", endTime);
        }
        params.put("nextToken", nextToken);
        params.put("maxResults", 20); // 每页最多20条
        
        if (userIds != null && !userIds.isEmpty()) {
            params.put("userIds", userIds);
        }
        if (statuses != null && !statuses.isEmpty()) {
            params.put("statuses", statuses);
        }

        try {
            String requestBody = JSONUtil.toJsonStr(params);
            log.info("获取审批实例ID列表请求: processCode={}, startTime={}, nextToken={}", processCode, startTime, nextToken);
            
            // 新版API需要使用 x-acs-dingtalk-access-token header
            String result = cn.hutool.http.HttpRequest.post(url)
                    .header("x-acs-dingtalk-access-token", accessToken)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .execute()
                    .body();
            
            log.info("获取审批实例ID列表返回: {}", result);
            JSONObject json = JSONUtil.parseObj(result);
            
            // 新版API的错误处理
            if (json.containsKey("code")) {
                String code = json.getStr("code");
                String message = json.getStr("message", "未知错误");
                log.error("获取审批实例ID列表失败: code={}, message={}", code, message);
                return;
            }
            
            // 解析返回结果
            JSONObject resultObj = json.getJSONObject("result");
            if (resultObj == null) {
                // 有些情况下直接返回 list 和 nextToken
                JSONArray list = json.getJSONArray("list");
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        allInstanceIds.add(list.getStr(i));
                    }
                    // 检查是否有更多数据
                    String nextTokenStr = json.getStr("nextToken");
                    if (StrUtil.isNotEmpty(nextTokenStr) && !"null".equals(nextTokenStr)) {
                        Long nextPage = Long.parseLong(nextTokenStr);
                        if (nextPage > 0 && allInstanceIds.size() < 10000) {
                            getProcessInstanceIdListPage(accessToken, processCode, startTime, endTime, 
                                    userIds, statuses, nextPage, allInstanceIds);
                        }
                    }
                }
                return;
            }
            
            JSONArray list = resultObj.getJSONArray("list");
            if (list != null && !list.isEmpty()) {
                for (int i = 0; i < list.size(); i++) {
                    allInstanceIds.add(list.getStr(i));
                }
            }
            
            // 如果还有更多数据，继续分页获取（但不超过10000条）
            String nextTokenStr = resultObj.getStr("nextToken");
            if (StrUtil.isNotEmpty(nextTokenStr) && !"null".equals(nextTokenStr)) {
                Long nextPage = Long.parseLong(nextTokenStr);
                if (nextPage > 0 && allInstanceIds.size() < 10000) {
                    getProcessInstanceIdListPage(accessToken, processCode, startTime, endTime, 
                            userIds, statuses, nextPage, allInstanceIds);
                }
            }
        } catch (Exception e) {
            log.error("获取审批实例ID列表异常", e);
        }
    }

    /**
     * 获取审批实例ID列表（简化版 - 只需processCode和startTime）
     * 
     * @param accessToken access_token
     * @param processCode 审批流的唯一码
     * @param startTime 审批实例开始时间，Unix时间戳，单位毫秒
     * @return 审批实例ID列表
     */
    public List<String> getProcessInstanceIdList(String accessToken, String processCode, Long startTime) {
        return getProcessInstanceIdList(accessToken, processCode, startTime, null, null, null);
    }

    /**
     * 获取OA审批实例详情（用于查看成功提交的表单数据格式）
     * 
     * API文档：https://open.dingtalk.com/document/orgapp/obtains-the-details-of-a-single-approval-instance
     * 
     * @param accessToken access_token
     * @param processInstanceId 审批实例ID
     * @return 审批实例详情的JSON字符串
     */
    public String getProcessInstanceDetail(String accessToken, String processInstanceId) {
        String url = "https://oapi.dingtalk.com/topapi/processinstance/get?access_token=" + accessToken;
        
        Map<String, Object> params = new HashMap<>();
        params.put("process_instance_id", processInstanceId);
        
        try {
            String requestBody = JSONUtil.toJsonStr(params);
            log.info("获取审批实例详情请求: processInstanceId={}", processInstanceId);
            String result = HttpUtil.post(url, requestBody);
            log.info("获取审批实例详情返回: {}", result);
            
            JSONObject json = JSONUtil.parseObj(result);
            int errcode = json.getInt("errcode", -1);
            if (errcode != 0) {
                String errmsg = json.getStr("errmsg", "未知错误");
                log.error("获取审批实例详情失败: errcode={}, errmsg={}", errcode, errmsg);
                return null;
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取审批实例详情异常", e);
            return null;
        }
    }

    /**
     * 发起钉钉OA审批流程
     *
     * @param accessToken access_token
     * @param processCode 流程模板唯一标识（在钉钉管理后台获取）
     * @param originatorUserId 发起人钉钉用户ID
     * @param deptId 发起人部门ID（钉钉部门ID）
     * @param formFields 表单字段列表
     * @return OA审批实例ID，失败返回null
     */
    public String startOaApprovalProcess(String accessToken, String processCode, 
                                          String originatorUserId, Long deptId,
                                          List<OaFormField> formFields) {
        String url = DINGTALK_PROCESS_CREATE_URL + "?access_token=" + accessToken;

        // 构建表单组件值 - 新模板"外出API"使用独立组件，不含DDBizSuite套件
        JSONArray formComponentValues = new JSONArray();
        
        for (OaFormField field : formFields) {
            String fieldName = field.getName();
            String fieldValue = field.getValue();
            
            // 所有字段都作为独立组件直接添加
            JSONObject fieldJson = new JSONObject();
            fieldJson.put("name", fieldName);
            fieldJson.put("value", fieldValue);
            log.debug("添加表单字段: name={}, value={}", fieldName, fieldValue);
            formComponentValues.add(fieldJson);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("process_code", processCode);
        params.put("originator_user_id", originatorUserId);
        params.put("dept_id", deptId);
        params.put("form_component_values", formComponentValues);

        try {
            String requestBody = JSONUtil.toJsonStr(params);
            log.info("钉钉OA审批发起API请求: {}", requestBody);
            String result = HttpUtil.post(url, requestBody);
            log.info("钉钉OA审批发起API返回: {}", result);
            JSONObject json = JSONUtil.parseObj(result);

            int errcode = json.getInt("errcode", -1);
            if (errcode != 0) {
                String errmsg = json.getStr("errmsg", "未知错误");
                log.error("发起钉钉OA审批失败: errcode={}, errmsg={}", errcode, errmsg);
                return null;
            }

            JSONObject resultObj = json.getJSONObject("result");
            if (resultObj != null) {
                String processInstanceId = resultObj.getStr("process_instance_id");
                log.info("发起钉钉OA审批成功，processInstanceId={}", processInstanceId);
                return processInstanceId;
            }

            // 兼容直接返回 process_instance_id 的情况
            String processInstanceId = json.getStr("process_instance_id");
            if (StrUtil.isNotEmpty(processInstanceId)) {
                log.info("发起钉钉OA审批成功，processInstanceId={}", processInstanceId);
                return processInstanceId;
            }

            log.warn("发起钉钉OA审批返回结果异常: {}", result);
            return null;
        } catch (Exception e) {
            log.error("发起钉钉OA审批异常", e);
            return null;
        }
    }

    /**
     * 发起外出申请OA审批（便捷方法 - 独立组件模式）
     *
     * @param accessToken access_token
     * @param processCode 外出申请流程的process_code
     * @param originatorUserId 发起人钉钉用户ID
     * @param deptId 发起人部门ID
     * @param outsideType 外出类型
     * @param startTime 开始时间（格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime 结束时间（格式：yyyy-MM-dd HH:mm:ss）
     * @param duration 时长（小时）
     * @param projectName 关联项目
     * @param reason 外出事由
     * @param destination 外出地点
     * @return OA审批实例ID
     */
    public String startOutsideApproval(String accessToken, String processCode,
                                        String originatorUserId, Long deptId,
                                        String outsideType, String startTime, String endTime,
                                        String duration, String projectName,
                                        String reason, String destination) {
        List<OaFormField> formFields = new ArrayList<>();
        
        // 注意：字段名称需要与钉钉OA表单中的组件名称完全一致
        // 以下是常见的字段名，用户需要根据实际钉钉配置调整
        if (StrUtil.isNotEmpty(outsideType)) {
            formFields.add(OaFormField.builder().name("外出类型").value(outsideType).build());
        }
        if (StrUtil.isNotEmpty(startTime)) {
            formFields.add(OaFormField.builder().name("开始时间").value(startTime).build());
        }
        if (StrUtil.isNotEmpty(endTime)) {
            formFields.add(OaFormField.builder().name("结束时间").value(endTime).build());
        }
        if (StrUtil.isNotEmpty(duration)) {
            formFields.add(OaFormField.builder().name("时长").value(duration).build());
        }
        if (StrUtil.isNotEmpty(projectName)) {
            formFields.add(OaFormField.builder().name("关联项目").value(projectName).build());
        }
        if (StrUtil.isNotEmpty(reason)) {
            formFields.add(OaFormField.builder().name("外出事由").value(reason).build());
        }
        if (StrUtil.isNotEmpty(destination)) {
            formFields.add(OaFormField.builder().name("外出地点").value(destination).build());
        }

        return startOaApprovalProcess(accessToken, processCode, originatorUserId, deptId, formFields);
    }

    /**
     * 发起外出申请OA审批（DDBizSuite套件模式）
     * 
     * 适用于使用钉钉内置"外出"套件（DDBizSuite, biz_type: attendance.goout）的表单
     * 套件内字段通过biz_alias传递：type(外出类型)、startTime(开始时间)、finishTime(结束时间)、duration(时长)
     *
     * @param accessToken access_token
     * @param processCode 外出申请流程的process_code
     * @param originatorUserId 发起人钉钉用户ID
     * @param deptId 发起人部门ID
     * @param outsideType 外出类型（"1天内短期外出" 或 "超过1天连续外出"）
     * @param startTime 开始时间（格式根据外出类型：短期用 yyyy-MM-dd HH:mm，长期用 yyyy-MM-dd）
     * @param endTime 结束时间（格式同上）
     * @param durationValue 时长数值（短期为小时数，长期为天数，支持小数如4.12）
     * @param projectName 关联项目
     * @param reason 外出事由
     * @param destination 外出地点
     * @return OA审批实例ID
     */
    // DDBizSuite 外出类型选项配置（根据钉钉表单配置）
    // "1天内短期外出" -> key: option_1K5LWVBW9D4W0, unit: hour
    // "超过1天连续外出" -> key: option_LGZF5Y7PTSW0, unit: day
    private static final String OUTSIDE_TYPE_SHORT_KEY = "option_1K5LWVBW9D4W0";
    private static final String OUTSIDE_TYPE_LONG_KEY = "option_LGZF5Y7PTSW0";

    public String startOutsideSuiteApproval(String accessToken, String processCode,
                                             String originatorUserId, Long deptId,
                                             String outsideType, String startTime, String endTime,
                                             double durationValue, String projectName,
                                             String reason, String destination) {
        // 使用新版API（经Python测试验证成功）
        String url = "https://api.dingtalk.com/v1.0/workflow/processInstances";

        // 构建表单组件值 - 使用格式7：每个字段独立传递（经测试验证成功）
        log.info("外出审批参数: type={}, startTime={}, endTime={}, duration={}", 
                outsideType, startTime, endTime, durationValue);

        JSONArray formComponentValues = new JSONArray();
        
        // 1. 外出类型
        JSONObject typeField = new JSONObject();
        typeField.put("name", "外出类型");
        typeField.put("value", outsideType);  // 如"超过1天连续外出"或"1天内短期外出"
        formComponentValues.add(typeField);
        
        // 2. 开始时间
        JSONObject startField = new JSONObject();
        startField.put("name", "开始时间");
        startField.put("value", startTime);
        formComponentValues.add(startField);
        
        // 3. 结束时间
        JSONObject endField = new JSONObject();
        endField.put("name", "结束时间");
        endField.put("value", endTime);
        formComponentValues.add(endField);
        
        // 4. 时长
        JSONObject durationField = new JSONObject();
        durationField.put("name", "时长");
        durationField.put("value", String.valueOf(durationValue));
        formComponentValues.add(durationField);
        
        // 5. 关联项目
        if (StrUtil.isNotEmpty(projectName)) {
            JSONObject projectField = new JSONObject();
            projectField.put("name", "关联项目");
            projectField.put("value", projectName);
            formComponentValues.add(projectField);
        }
        
        // 6. 外出事由
        if (StrUtil.isNotEmpty(reason)) {
            JSONObject reasonField = new JSONObject();
            reasonField.put("name", "外出事由");
            reasonField.put("value", reason);
            formComponentValues.add(reasonField);
        }
        
        // 7. 外出地点
        if (StrUtil.isNotEmpty(destination)) {
            JSONObject destField = new JSONObject();
            destField.put("name", "外出地点");
            destField.put("value", destination);
            formComponentValues.add(destField);
        }

        // 新版API使用驼峰命名
        Map<String, Object> params = new HashMap<>();
        params.put("processCode", processCode);
        params.put("originatorUserId", originatorUserId);
        params.put("deptId", deptId);
        params.put("formComponentValues", formComponentValues);

        try {
            String requestBody = JSONUtil.toJsonStr(params);
            log.info("钉钉OA审批发起API请求(新版): {}", requestBody);
            
            // 新版API使用Header传递accessToken
            String result = HttpUtil.createPost(url)
                    .header("x-acs-dingtalk-access-token", accessToken)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .execute()
                    .body();
            
            log.info("钉钉OA审批发起API返回(新版): {}", result);
            JSONObject json = JSONUtil.parseObj(result);

            // 新版API成功时直接返回 instanceId
            String instanceId = json.getStr("instanceId");
            if (StrUtil.isNotEmpty(instanceId)) {
                log.info("发起钉钉OA审批成功，instanceId={}", instanceId);
                return instanceId;
            }

            // 新版API错误时返回 code 和 message
            String code = json.getStr("code");
            String message = json.getStr("message");
            if (StrUtil.isNotEmpty(code)) {
                log.error("发起钉钉OA审批失败: code={}, message={}", code, message);
                return null;
            }

            log.warn("发起钉钉OA审批返回结果异常: {}", result);
            return null;
        } catch (Exception e) {
            log.error("发起钉钉OA审批异常", e);
            return null;
        }
    }
}
