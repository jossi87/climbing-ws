package com.buldreinfo.jersey.jaxb;

public final class OpenApiResponseRefs {
	public static final String BAD_REQUEST_NAME = "BadRequest";
	public static final String UNAUTHORIZED_NAME = "Unauthorized";
	public static final String FORBIDDEN_NAME = "Forbidden";
	public static final String INTERNAL_SERVER_ERROR_NAME = "InternalServerError";

	public static final String BAD_REQUEST = "#/components/responses/" + BAD_REQUEST_NAME;
	public static final String UNAUTHORIZED = "#/components/responses/" + UNAUTHORIZED_NAME;
	public static final String FORBIDDEN = "#/components/responses/" + FORBIDDEN_NAME;
	public static final String INTERNAL_SERVER_ERROR = "#/components/responses/" + INTERNAL_SERVER_ERROR_NAME;

	private OpenApiResponseRefs() {
	}
}
