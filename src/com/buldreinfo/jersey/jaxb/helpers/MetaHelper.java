package com.buldreinfo.jersey.jaxb.helpers;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.server.Server;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

import jakarta.servlet.http.HttpServletRequest;

public class MetaHelper {
	private static final Logger logger = LogManager.getLogger();
	private static MetaHelper meta = null;
	public static synchronized MetaHelper getMeta() {
		MetaHelper result = meta;
		if (result == null) {
			meta = result = new MetaHelper();
		}
		return result;
	}
	private SetupHolder holder;

	private MetaHelper() {
	}
	
	public Setup getSetup(HttpServletRequest request) {
		Preconditions.checkNotNull(request);
		Preconditions.checkNotNull(request.getServerName(), "Invalid request=" + request);
		final String serverName = request.getServerName().toLowerCase().replace("www.", "");
		return getSetups()
				.stream()
				.filter(x -> serverName.equalsIgnoreCase(x.getDomain()))
				.findAny()
				.orElseThrow(() -> new RuntimeException("Invalid serverName=" + serverName));
	}

	public Setup getSetup(int regionId) {
		return getSetups()
				.stream()
				.filter(x -> x.getIdRegion() == regionId)
				.findAny()
				.orElseThrow(() -> new RuntimeException("Invalid regionId=" + regionId));
	}

	public List<Setup> getSetups() {
		long validMilliseconds = holder == null? 0 : holder.getValidMilliseconds();
		if (validMilliseconds > 0) {
			logger.debug("getSetups() - From cache, validMilliseconds={}", validMilliseconds);
		}
		else {
			Stopwatch stopwatch = Stopwatch.createStarted();
			Server.runSql(c -> holder = new SetupHolder(Server.getDao().getSetups(c), System.currentTimeMillis()));
			logger.debug("getSetups() - Fetched new setups in {}", stopwatch);
		}
		return holder.getSetups();
	}
}