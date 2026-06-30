package com.buldreinfo.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.RegionRepository;

@Service
public class ServerUrlService {
	public record ServerUrl(String url, String description) {}
	private static final String LOCAL_DEV_ORIGIN = "http://localhost:3001";
	private static final String LOCAL_SWAGGER_ORIGIN = "http://localhost:8080";
	private static final Logger logger = LogManager.getLogger();

	private final RegionRepository regionRepo;

	public ServerUrlService(RegionRepository regionRepo) {
		this.regionRepo = regionRepo;
	}

	public Set<String> getAllowedOrigins() {
		Set<String> origins = new HashSet<>();
		origins.add(LOCAL_DEV_ORIGIN);
		origins.add(LOCAL_SWAGGER_ORIGIN);
		try {
			regionRepo.getSetups().stream()
			.map(Setup::domain)
			.map(domain -> "https://" + domain)
			.forEach(origins::add);
		} catch (Exception e) {
			logger.error("Failed to fetch CORS origins from database.", e);
		}
		return origins;
	}

	public List<ServerUrl> getServerUrls() {
		Set<String> seen = new HashSet<>();
		var urls = new ArrayList<ServerUrl>();

		urls.add(new ServerUrl(LOCAL_SWAGGER_ORIGIN, "Local development"));

		try {
			for (Setup setup : regionRepo.getSetups()) {
				String domain = setup.domain();
				if (seen.add(domain)) {
					urls.add(new ServerUrl("https://" + domain + "/api", domain));
				}
			}
		} catch (Exception e) {
			logger.error("Failed to fetch server URLs from database", e);
		}

		return urls;
	}
}
