package com.sedroad.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SedRoad API")
                        .version("1.0.0")
                        .description("세대로드(SedRoad) - 세대를 연결하는 여행길 API 문서")
                );
    }
}

