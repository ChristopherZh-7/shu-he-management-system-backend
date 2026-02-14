package cn.shuhe.system.module.project.enums;

import cn.shuhe.system.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 工作模式枚举
 * 区分驻场和二线员工的不同工作记录方式
 */
@Getter
@AllArgsConstructor
public enum WorkModeEnum implements ArrayValuable<Integer> {

    ON_SITE(1, "驻场"),
    BACK_OFFICE(2, "二线");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(WorkModeEnum::getMode).toArray(Integer[]::new);

    private final Integer mode;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
