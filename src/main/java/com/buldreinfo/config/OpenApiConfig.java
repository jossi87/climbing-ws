package com.buldreinfo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.buldreinfo.infrastructure.OpenApiConstants;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Climbing API")
						.version("1.0")
						.description("API for buldreinfo.com and brattelinjer.no"))
				.addSecurityItem(new SecurityRequirement().addList(OpenApiConstants.BEARER_AUTH))
				.components(new Components()
						.addSecuritySchemes(OpenApiConstants.BEARER_AUTH, new SecurityScheme()
								.name(OpenApiConstants.BEARER_AUTH)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")));
	}
}
