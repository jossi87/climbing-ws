package com.buldreinfo.jersey.jaxb.config;

import java.io.InputStream;
import java.util.Properties;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class BuldreinfoConfig {
	public final static String PROPERTY_KEY_DB_HOSTNAME = "db.hostname";
	public final static String PROPERTY_KEY_DB_DATABASE = "db.database";
	public final static String PROPERTY_KEY_DB_USERNAME = "db.username";
	public final static String PROPERTY_KEY_DB_PASSWORD = "db.password";
	public final static String PROPERTY_KEY_AUTH0_CLIENT_SECRET = "auth0.secret";
	public final static String PROPERTY_KEY_GOOGLE_APIKEY = "google.apikey";
	public final static String PROPERTY_KEY_VEGVESEN_AUTH = "vegvesen.auth";
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
		try (InputStream input = BuldreinfoConfig.class.getResourceAsStream("buldreinfo.properties")) {
			Preconditions.checkArgument(input != null, "input is null");
            this.prop = new Properties();
            prop.load(input);
            Preconditions.checkArgument(prop != null, "properties is null");
            
        } catch (Exception e) {
        	throw new RuntimeException(e.getMessage(), e);
        }
	}
	
	public String getProperty(String key) {
		Preconditions.checkArgument(prop != null, "properties is null");
		String value = prop.getProperty(key);
		Preconditions.checkNotNull(Strings.emptyToNull(value), "Could not find property with key=" + key);
		return value;
	}
}