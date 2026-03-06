package cn.shuhe.system.module.crm.dal.typehandler;

import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.framework.common.util.json.JsonUtils;
import cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO.DeptAllocation;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门金额分配列表 TypeHandler
 * <p>
 * 兼容两种数据库存储格式：
 * 1. 旧格式（原 dept_ids）：[119, 117] - 仅部门ID数组
 * 2. 新格式：[{"deptId":119,"deptName":"安全服务部","amount":500000}] - 完整对象数组
 *
 * @author shuhe
 */
@Slf4j
public class DeptAllocationListTypeHandler extends AbstractJsonTypeHandler<List<DeptAllocation>> {

    private static final TypeReference<List<DeptAllocation>> TYPE_REF = new TypeReference<>() {};

    public DeptAllocationListTypeHandler(Class<?> type) {
        super(type);
    }

    public DeptAllocationListTypeHandler(Class<?> type, java.lang.reflect.Field field) {
        super(type, field);
    }

    @Override
    public List<DeptAllocation> parse(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("[")) {
            return null;
        }
        try {
            ObjectMapper mapper = JsonUtils.getObjectMapper();
            JsonNode root = mapper.readTree(json);
            if (!root.isArray() || root.isEmpty()) {
                return new ArrayList<>();
            }
            JsonNode first = root.get(0);
            // 旧格式：数组元素为数字
            if (first.isNumber()) {
                List<DeptAllocation> result = new ArrayList<>(root.size());
                for (JsonNode node : root) {
                    if (node.isNumber()) {
                        result.add(new DeptAllocation(node.asLong(), null, null));
                    }
                }
                return result;
            }
            // 新格式：数组元素为对象
            return mapper.readValue(json, TYPE_REF);
        } catch (Exception e) {
            log.warn("[DeptAllocationListTypeHandler] 解析 dept_allocations 失败，json={}", json, e);
            return null;
        }
    }

    @Override
    public String toJson(List<DeptAllocation> obj) {
        return obj == null ? null : JsonUtils.toJsonString(obj);
    }

}
