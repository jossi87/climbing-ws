package com.buldreinfo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.service.ServerUrlService;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class OpenApiConfig {
	private final ServerUrlService serverUrlService;

	public OpenApiConfig(ServerUrlService serverUrlService) {
		this.serverUrlService = serverUrlService;
	}

	@Bean
	public OpenAPI customOpenAPI() {
		HttpServletRequest request = null;
		var attrs = RequestContextHolder.getRequestAttributes();
		if (attrs instanceof ServletRequestAttributes sra) {
			request = sra.getRequest();
		}

		var api = new OpenAPI()
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

		for (var serverUrl : serverUrlService.getServerUrls(request)) {
			api.addServersItem(new Server()
					.url(serverUrl.url())
					.description(serverUrl.description()));
		}

		return api;
	}
}
