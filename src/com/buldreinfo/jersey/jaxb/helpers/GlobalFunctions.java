package com.buldreinfo.jersey.jaxb.helpers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Preconditions;

public class GlobalFunctions {
	private static final Logger logger = LogManager.getLogger();
	private static final String MEDIA_ROOT_PRODUCTION = "/mnt/buldreinfo/media";
	private static final String MEDIA_ROOT_TEST = "c:/users/jostein/desktop/buldreinfo_test";

	public static Path getPathLeafletPrint() throws IOException {
		Path res = Paths.get("/var/lib/jenkins/workspace/buldreinfo-web/leaflet-puppeteer-print");
		if (!Files.exists(res)) {
			res = Paths.get("C:/git/buldreinfo-web/leaflet-puppeteer-print/index.js");
			Preconditions.checkArgument(Files.exists(res), res.toString() + " does not exist");
		}
		return res;
	}
	
	public static Path getPathMediaOriginal() throws IOException {
		return getPathRoot().resolve("original");
	}
	
	public static Path getPathMediaWebJpg() throws IOException {
		return getPathRoot().resolve("web/jpg");
	}
	
	public static Path getPathMediaWebWebp() throws IOException {
		return getPathRoot().resolve("web/webp");
	}
	
	public static Path getPathOriginalUsers() throws IOException {
		return getPathRoot().resolve("original/users");
	}
	
	public static Path getPathTemp() throws IOException {
		return getPathRoot().resolve("temp");
	}
	
	public static Path getPathWebUsers() throws IOException {
		return getPathRoot().resolve("web/users");
	}
	
	public static String getUrlJpgToImage(int id) {
		return "https://brattelinjer.no/buldreinfo_media/jpg/" + String.valueOf(id / 100 * 100) + "/" + id + ".jpg";
	}

	public static WebApplicationException getWebApplicationExceptionBadRequest(Exception e) {
		logger.warn(e.getMessage(), e);
		return new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(e.getMessage()).build());
	}
	
	public static WebApplicationException getWebApplicationExceptionInternalError(Exception e) {
		logger.fatal(e.getMessage(), e);
		return new WebApplicationException(Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e.getMessage()).build());
	}
	
	private static Path getPathRoot() throws IOException {
		Path res = Paths.get(MEDIA_ROOT_PRODUCTION);
		if (Files.exists(res)) {
			return res;
		}
		// Test
		res = Paths.get(MEDIA_ROOT_TEST);
		return res;
	}
}