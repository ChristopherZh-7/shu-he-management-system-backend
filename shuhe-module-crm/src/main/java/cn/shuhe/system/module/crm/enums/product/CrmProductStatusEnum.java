package cn.shuhe.system.module.crm.enums.product;

import cn.hutool.core.util.ObjUtil;
import cn.shuhe.system.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CRM 商品状态
 *
 * @author ZanGe丶
 * @since 2023-11-30 21:53
 */
@Getter
@AllArgsConstructor
public enum CrmProductStatusEnum implements ArrayValuable<Integer> {

    DISABLE(0, "关闭"),
    ENABLE(1, "开启");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmProductStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static boolean isEnable(Integer status) {
        return ObjUtil.equal(ENABLE.status, status);
    }

    public static boolean isDisable(Integer status) {
        return ObjUtil.equal(DISABLE.status, status);
    }

}
