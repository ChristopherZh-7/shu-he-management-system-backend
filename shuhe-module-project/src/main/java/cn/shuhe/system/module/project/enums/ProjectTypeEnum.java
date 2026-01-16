package cn.shuhe.system.module.project.enums;

import cn.shuhe.system.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 项目类型枚举 - 对应三个部门
 */
@Getter
@AllArgsConstructor
public enum ProjectTypeEnum implements ArrayValuable<Integer> {

    SECURITY_SERVICE(1, "安全服务"),
    SECURITY_OPERATION(2, "安全运营"),
    DATA_SECURITY(3, "数据安全");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ProjectTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 类型
     */
    private final Integer type;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
