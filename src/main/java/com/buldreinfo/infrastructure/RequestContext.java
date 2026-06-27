package com.buldreinfo.infrastructure;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.filters.HitTrackingFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.NoSuchElementException;
import java.util.Optional;

@Component
public class RequestContext {
    private final RegionRepository regionRepo;

    public RequestContext(RegionRepository regionRepo) {
        this.regionRepo = regionRepo;
    }

    public Setup getSetup(HttpServletRequest request) {
        String serverName = request.getServerName();
        return regionRepo.getSetups().stream()
                .filter(s -> s.domain().equalsIgnoreCase(serverName))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Invalid serverName=" + serverName));
    }

    public Optional<Integer> getAuthenticatedUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getPrincipal() instanceof Integer userId) ? Optional.of(userId) : Optional.empty();
    }

    public boolean isHitTrackingEnabled(HttpServletRequest request) {
        Object attr = request.getAttribute(HitTrackingFilter.SHOULD_UPDATE_HITS_KEY);
        return (attr instanceof Boolean) && (Boolean) attr;
    }
}