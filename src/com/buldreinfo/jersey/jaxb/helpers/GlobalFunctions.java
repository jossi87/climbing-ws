package com.buldreinfo.jersey.jaxb.helpers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class GlobalFunctions {
	private static final Logger logger = LogManager.getLogger();
	private static final String MEDIA_ROOT_PRODUCTION = "/mnt/buldreinfo/media";

	public static String getFilename(String purpose, String ext) {
		purpose = removeIllegalCharacters(purpose);
		final String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		return String.format("%s_Buldreinfo_BratteLinjer_%s.%s", dateTime, purpose, ext);
	}
	
	public static Path getPathLeafletPrint() throws IOException {
		Path res = Paths.get("/var/lib/jenkins/workspace/climbing-web/leaflet-puppeteer-print/index.js");
		if (!Files.exists(res)) {
			res = Paths.get("C:/git/buldreinfo-web/leaflet-puppeteer-print/index.js");
			Preconditions.checkArgument(Files.exists(res), res.toString() + " does not exist");
		}
		return res;
	}
	
	public static Path getPathMediaOriginalJpg() throws IOException {
		return getPathRoot().resolve("original/jpg");
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
	
	public static int getCrc32(Path p) throws IOException {
		return com.google.common.io.Files.asByteSource(p.toFile()).hash(Hashing.crc32()).asInt();
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
		if (!Files.exists(res)) {
			throw new RuntimeException(res.toString() + " does not exists");
		}
		return res;
	}
	
	private static String removeIllegalCharacters(String str) {
		return str.trim().replaceAll("[\\\\/:*?\"<>|] ", "_");
	}
}