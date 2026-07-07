package com.buldreinfo.service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.FrontpageRepository;
import com.buldreinfo.infrastructure.CacheConstants;
import com.buldreinfo.model.Frontpage;

@Service
public class FrontpageService {
	private final FrontpageRepository frontpageRepo;

	public FrontpageService(FrontpageRepository frontpageRepo) {
		this.frontpageRepo = frontpageRepo;
	}

	@Cacheable(value = CacheConstants.FRONTPAGE_CACHE_NAME, key = "#setup.idRegion()", sync = true)
	public Frontpage getAnonymousFrontpage(Setup setup) {
		return getFrontpage(Optional.empty(), setup);
	}

	public Frontpage getAuthenticatedFrontpage(int authUserId, Setup setup) {
		return getFrontpage(Optional.of(authUserId), setup);
	}

	private Frontpage getFrontpage(Optional<Integer> userId, Setup setup) {
		var statsFuture = CompletableFuture.supplyAsync(() -> frontpageRepo.getFrontpageStats(userId, setup));
		var randomMediaFuture = CompletableFuture.supplyAsync(() -> frontpageRepo.getFrontpageRandomMedia(setup));
		var firstAscentsFuture = CompletableFuture.supplyAsync(() -> frontpageRepo.getFrontpageFirstAscents(userId, setup));
		var newestCommentsFuture = CompletableFuture.supplyAsync(() -> frontpageRepo.getFrontpageNewestAscents(userId, setup));
		var newestMediaFuture = CompletableFuture.supplyAsync(() -> frontpageRepo.getFrontpageNewestMedia(userId, setup));
		var lastCommentsFuture = CompletableFuture.supplyAsync(() -> frontpageRepo.getFrontpageLastComments(userId, setup));
		CompletableFuture.allOf(statsFuture, randomMediaFuture, firstAscentsFuture, newestCommentsFuture, newestMediaFuture, lastCommentsFuture).join();
		return new Frontpage(
				statsFuture.join(),
				randomMediaFuture.join(),
				firstAscentsFuture.join(),
				newestCommentsFuture.join(),
				newestMediaFuture.join(),
				lastCommentsFuture.join()
				);
	}
}
