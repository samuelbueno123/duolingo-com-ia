package com.duolinfo.ia.proj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;

@Configuration
public class SwaggerConfiguration{
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Duolinfo API")
                        .version("Alpha 0.0.2")
                        .description("API para gerenciamento do Duolinfo."));                
    }
        

}