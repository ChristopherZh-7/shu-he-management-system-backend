package cn.shuhe.system.module.crm.dal.typehandler;

import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.framework.common.util.json.JsonUtils;
import cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO.Personnel;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 提前投入人员列表 TypeHandler
 */
@Slf4j
public class PersonnelListTypeHandler extends AbstractJsonTypeHandler<List<Personnel>> {

    private static final TypeReference<List<Personnel>> TYPE_REF = new TypeReference<>() {};

    public PersonnelListTypeHandler(Class<?> type) {
        super(type);
    }

    public PersonnelListTypeHandler(Class<?> type, java.lang.reflect.Field field) {
        super(type, field);
    }

    @Override
    public List<Personnel> parse(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return JsonUtils.getObjectMapper().readValue(json, TYPE_REF);
        } catch (Exception e) {
            log.warn("[PersonnelListTypeHandler] 解析失败, json={}", json, e);
            return null;
        }
    }

    @Override
    public String toJson(List<Personnel> obj) {
        return obj == null ? null : JsonUtils.toJsonString(obj);
    }

}
