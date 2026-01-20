package cn.shuhe.system.server;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SuppressWarnings("SpringComponentScan") //
@SpringBootApplication(scanBasePackages = {"${shuhe.info.base-package}.server", "${shuhe.info.base-package}.module"})
public class ShuheServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShuheServerApplication.class, args);
    }





}
