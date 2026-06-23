package com.buldreinfo.infrastructure;

public final class OpenApiConstants {
	public static final String BEARER_AUTH = "Bearer Authentication";
	
	public static final String APPLICATION_PDF = "application/pdf";
	public static final String APPLICATION_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	
	public static final String OK_CODE = "200";
	public static final String OK_DESCRIPTION = "OK";
	
	public static final String FOUND_CODE = "302";
    public static final String FOUND_DESCRIPTION = "The requested resource has resided temporarily under a different URI.";

    public static final String NOT_FOUND_CODE = "404";
    public static final String NOT_FOUND_DESCRIPTION = "The requested resource could not be found.";
	
	public static final String BAD_REQUEST_CODE = "400";
	public static final String BAD_REQUEST_DESCRIPTION = "Invalid request parameters.";

	public static final String UNAUTHORIZED_CODE = "401";
	public static final String UNAUTHORIZED_DESCRIPTION = "Authentication required.";

	public static final String FORBIDDEN_CODE = "403";
	public static final String FORBIDDEN_DESCRIPTION = "Insufficient permissions.";

	public static final String INTERNAL_SERVER_ERROR_CODE = "500";
	public static final String INTERNAL_SERVER_ERROR_DESCRIPTION = "An unexpected error occurred";

	private OpenApiConstants() {
	}
}
