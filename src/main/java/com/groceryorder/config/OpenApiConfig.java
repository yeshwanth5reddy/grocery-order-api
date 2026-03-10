package com.groceryorder.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI groceryOrderOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Grocery Order Management API")
                        .description("RESTful API for managing products, customers, and orders in a grocery delivery system. "
                                + "Features include inventory management, order lifecycle with state machine, "
                                + "stock validation, and price snapshot at purchase time. "
                                + "Register/login via /api/auth endpoints to get a JWT token, then use the Authorize button.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Yeshwanth Reddy")
                                .email("yeshwanth@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Token", new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token")));
    }
}
