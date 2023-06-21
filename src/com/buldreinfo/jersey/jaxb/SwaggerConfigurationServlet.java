package com.buldreinfo.jersey.jaxb;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.swagger.jaxrs.config.BeanConfig;

/**
 * Creates https://brattelinjer.no/com.buldreinfo.jersey.jaxb/swagger.json
 */
public class SwaggerConfigurationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private BeanConfig beanConfig;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		beanConfig = new BeanConfig();
		beanConfig.setVersion("2.0.0");
		beanConfig.setSchemes(new String[]{"https"});
		beanConfig.setHost("brattelinjer.no");
		beanConfig.setBasePath("/com.buldreinfo.jersey.jaxb");
		beanConfig.setResourcePackage("com.buldreinfo.jersey.jaxb");
		beanConfig.setScan(true);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		beanConfig.setHost("test123");
		super.doGet(req, resp);
	}
}
