package com.buldreinfo.jersey.jaxb.config;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class BuldreinfoConfig {
	public final static String PROPERTY_KEY_DB_HOSTNAME = "db.hostname";
	public final static String PROPERTY_KEY_DB_DATABASE = "db.database";
	public final static String PROPERTY_KEY_DB_USERNAME = "db.username";
	public final static String PROPERTY_KEY_DB_PASSWORD = "db.password";
	public final static String PROPERTY_KEY_GOOGLE_APIKEY = "google.apikey";
	public final static String PROPERTY_KEY_VEGVESEN_AUTH = "vegvesen.auth";
	public final static String PROPERTY_KEY_AKAMAI_ACCESS_KEY = "akamai.access_key";
	public final static String PROPERTY_KEY_AKAMAI_SECRET_KEY = "akamai.secret_key";
	private static BuldreinfoConfig config = null;

	public static synchronized BuldreinfoConfig getConfig() {
		BuldreinfoConfig result = config;
		if (result == null) {
			config = result = new BuldreinfoConfig();
		}
		return result;
	}

	private final Properties prop;

	public BuldreinfoConfig() {
		this.prop = new Properties();
		Path externalPath = Path.of("/usr/local/tomcat/conf/buldreinfo.properties");
		try {
			if (Files.exists(externalPath)) {
				try (InputStream input = Files.newInputStream(externalPath)) {
					prop.load(input);
				}
			}
			else {
				try (InputStream input = BuldreinfoConfig.class.getResourceAsStream("buldreinfo.properties")) {
					if (input == null) {
						throw new IllegalArgumentException("Properties not found at " + externalPath + " or in classpath");
					}
					prop.load(input);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Configuration initialization failed: " + e.getMessage(), e);
		}
	}

	public String getProperty(String key) {
		if (prop == null) {
			throw new IllegalArgumentException("properties is null");
		}
		String value = prop.getProperty(key);
		if (value == null || value.trim().isEmpty()) {
			throw new NullPointerException("Could not find property with key=" + key);
		}
		return value;
	}
}