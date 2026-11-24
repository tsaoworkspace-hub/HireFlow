package com.portfolio.hireflow.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI hireFlowOpenApi() {
        return new OpenAPI().info(new Info()
                .title("HireFlow API")
                .version("v1")
                .description("招募申請、面試、錄用決策與求職者入口 API"));
    }
}
