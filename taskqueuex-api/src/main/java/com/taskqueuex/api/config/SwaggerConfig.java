package com.taskqueuex.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI taskQueueXOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("TaskQueueX API")
                .description("Distributed background job processing platform API")
                .version("1.0.0")
                .contact(new Contact()
                    .name("TaskQueueX")
                    .email("support@taskqueuex.com")));
    }
}
