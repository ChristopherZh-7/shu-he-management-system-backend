package cn.shuhe.system.module.project.enums;

import cn.shuhe.system.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 项目状态枚举
 */
@Getter
@AllArgsConstructor
public enum ProjectStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    IN_PROGRESS(1, "进行中"),
    PAUSED(2, "已暂停"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ProjectStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
