package com.buldreinfo.infrastructure;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.filters.HitTrackingFilter;
import com.buldreinfo.model.Language;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class RequestContext {
	private final RegionRepository regionRepo;

	public RequestContext(RegionRepository regionRepo) {
		this.regionRepo = regionRepo;
	}

	public boolean acceptsWebp(HttpServletRequest request) {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("image/webp")) return true;

		String ua = request.getHeader("User-Agent");
		if (ua != null && ua.contains("Safari") && !ua.contains("Chrome")) {
			int idx = ua.indexOf("Version/");
			if (idx >= 0) {
				String versionPart = ua.substring(idx + 8).split(" ")[0].split("\\.")[0];
				try {
					return Integer.parseInt(versionPart) >= 14;
				} catch (NumberFormatException _) {
					return false;
				}
			}
		}
		return false;
	}

	public Optional<Integer> getAuthenticatedUserId() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		return (auth != null && auth.getPrincipal() instanceof Integer userId) ? Optional.of(userId) : Optional.empty();
	}

	/**
	 * The language the client asked for ({@code ?lang=}), or {@code null} when it asked for none.
	 * The generated PDFs resolve their labels from this code, while the DAO layer only needs the
	 * database id ({@link #getLanguageId(HttpServletRequest)}). Read it on the request thread.
	 */
	public String getLanguageCode(HttpServletRequest request) {
		String code = request.getParameter("lang");
		return (code == null || code.isBlank()) ? null : code.strip();
	}

	public int getLanguageId(HttpServletRequest request) {
		String code = getLanguageCode(request);
		if (code == null) {
			return 0;
		}
		return regionRepo.getLanguages().stream()
				.filter(l -> l.code().equalsIgnoreCase(code))
				.map(Language::id)
				.findFirst()
				.orElse(0);
	}

	public Setup getSetup(HttpServletRequest request) {
		String serverName = request.getServerName();
		return regionRepo.getSetups(getLanguageId(request)).stream()
				.filter(s -> s.domain().equalsIgnoreCase(serverName))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("Invalid serverName=" + serverName));
	}

	public boolean isHitTrackingEnabled(HttpServletRequest request) {
		Object attr = request.getAttribute(HitTrackingFilter.SHOULD_UPDATE_HITS_KEY);
		return (attr instanceof Boolean) && (Boolean) attr;
	}
}