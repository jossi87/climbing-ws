package com.buldreinfo.jersey.jaxb;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;

/**
 * Creates:
 * - https://brattelinjer.no/com.buldreinfo.jersey.jaxb/openapi.json
 * - http://localhost:8080/com.buldreinfo.jersey.jaxb/openapi.json
 */
public class SwaggerConfigurationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		SwaggerConfiguration oasConfig = new SwaggerConfiguration().resourcePackages(Stream.of("com.buldreinfo.jersey.jaxb").collect(Collectors.toSet()));
		try {
			new JaxrsOpenApiContextBuilder<>()
	          .servletConfig(config)
	          .openApiConfiguration(oasConfig)
	          .buildContext(true);
		} catch (OpenApiConfigurationException e) {
			throw new ServletException(e.getMessage(), e);
		}
	}
}
