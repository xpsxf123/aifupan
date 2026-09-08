package com.jiuyu.replay.api.config;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("标题")
                        .contact(new Contact())
                        .description("我的API文档")
                        .version("v1")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("外部文档")
                        .url("https://springshop.wiki.github.org/docs"));
    }

    @Bean
    public OperationCustomizer globalHeaderOperationCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            Parameter parameter = new Parameter()
                    .name("token")
                    .description("Bearer Token")
                    .required(false)
                    .in(ParameterIn.HEADER.toString())
                    .schema(new Schema().type("string"));

            operation.addParametersItem(parameter);
            return operation;
        };
    }
}
