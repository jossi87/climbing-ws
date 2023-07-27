package com.buldreinfo.jersey.jaxb.helpers;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

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
		List<Setup> res = null;
		long validMilliseconds = holder == null? 0 : holder.getValidMilliseconds();
		if (validMilliseconds > 0) {
			logger.debug("getSetups() - Still valid, tokenValidMs={}", validMilliseconds);
			res = holder.getSetups();
		}
		else {
			Stopwatch stopwatch = Stopwatch.createStarted();
			try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
				res = c.getBuldreinfoRepo().getSetups();
				holder = new SetupHolder(res, System.currentTimeMillis());
				c.setSuccess();
			} catch (Exception e) {
				throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
			}
			logger.debug("getSetups() - Fetched new setups in {}", stopwatch);
		}
		return res;
	}
}