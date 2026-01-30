package cn.shuhe.system.framework.banner.core;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 项目启动后的扩展入口（预留）
 */
public class BannerApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // 启动提示与模块禁用信息已移除
    }

}
