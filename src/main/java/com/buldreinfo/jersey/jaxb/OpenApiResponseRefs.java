package com.buldreinfo.jersey.jaxb;

public final class OpenApiResponseRefs {
	public static final String BAD_REQUEST_CODE = "400";
	public static final String BAD_REQUEST_DESCRIPTION = "Invalid request parameters.";

	public static final String UNAUTHORIZED_CODE = "401";
	public static final String UNAUTHORIZED_DESCRIPTION = "Authentication required.";

	public static final String FORBIDDEN_CODE = "403";
	public static final String FORBIDDEN_DESCRIPTION = "Insufficient permissions.";

	public static final String INTERNAL_SERVER_ERROR_CODE = "500";
	public static final String INTERNAL_SERVER_ERROR_DESCRIPTION = "An unexpected error occurred";

	private OpenApiResponseRefs() {
	}
}
