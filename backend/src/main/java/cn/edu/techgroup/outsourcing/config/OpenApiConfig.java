package cn.edu.techgroup.outsourcing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI projectOpenApi() {
        return new OpenAPI().info(new Info()
                .title("计算机技术组外包需求管理系统 API")
                .version("v1")
                .description("需求提交、评估、分配、进度与验收接口"));
    }
}
