package com.br.capoeira.orcamento.budgetapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI budgetApiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Orcamento Microservice API")
                        .description("API para criacao e consulta de solicitacoes de orcamento.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Orcamento Microservice"))
                        .license(new License()
                                .name("Private")));
    }
}
