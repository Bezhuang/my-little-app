package com.bezhuang.my_little_app_backend;

import com.bezhuang.my_little_app_backend.config.ai.SiliconFlowConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.bezhuang.my_little_app_backend.mapper")
@EnableTransactionManagement
@EnableConfigurationProperties({SiliconFlowConfig.class})
public class MyLittleAppBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyLittleAppBackendApplication.class, args);
    }

}
