package com.sedroad.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SedRoad API")
                        .version("1.0.0")
                        .description("세대로드(SedRoad) - 세대를 연결하는 여행길 API 문서")
                        .contact(new Contact()
                                .name("SedRoad Team")
                                .email("support@sedroad.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:3000")
                                .description("로컬 개발 서버"),
                        new Server()
                                .url("https://api.sedroad.com")
                                .description("프로덕션 서버")
                ));
    }
}

