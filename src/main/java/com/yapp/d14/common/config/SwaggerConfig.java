package com.yapp.d14.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String PROD_SERVER_URL = "https://hilit.my";
    private static final String LOCAL_SERVER_URL = "http://localhost:8080";

    @Bean
    public OpenAPI openAPI(Environment environment) {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("D14 API")
                        .version("v1")
                        .description("YAPP-APP-TEAM-1 API 명세서입니다."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));

        if (!environment.acceptsProfiles(Profiles.of("local"))) {
            openAPI.addServersItem(new Server().url(PROD_SERVER_URL));
            openAPI.addServersItem(new Server().url(LOCAL_SERVER_URL));
        }

        return openAPI;
    }
}
