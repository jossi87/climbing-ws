package com.buldreinfo.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.ActivityRepository;
import com.buldreinfo.dao.ExternalLinksRepository;
import com.buldreinfo.dao.HierarchyRepository;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.ProblemRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.exception.UnauthorizedException;
import com.buldreinfo.model.Comment;
import com.buldreinfo.model.Problem;
import com.buldreinfo.model.ProblemSearchResult;
import com.buldreinfo.model.Redirect;

@Service
public class ProblemService {
	private final ActivityRepository activityRepo;
	private final ExternalLinksRepository externalLinksRepo;
	private final GeoService geoService;
	private final HierarchyRepository hierarchyRepo;
	private final MediaRepository mediaRepo;
	private final ProblemRepository problemRepo;
	private final SectorRepository sectorRepo;
	private final UserRepository userRepo;

	public ProblemService(
			ActivityRepository activityRepo,
			ExternalLinksRepository externalLinksRepo,
			GeoService geoService,
			HierarchyRepository hierarchyRepo,
			MediaRepository mediaRepo,
			ProblemRepository problemRepo,
			SectorRepository sectorRepo,
			UserRepository userRepo) {
		this.activityRepo = activityRepo;
		this.externalLinksRepo = externalLinksRepo;
		this.geoService = geoService;
		this.hierarchyRepo = hierarchyRepo;
		this.mediaRepo = mediaRepo;
		this.problemRepo = problemRepo;
		this.sectorRepo = sectorRepo;
		this.userRepo = userRepo;
	}

	@Transactional(readOnly = true)
	public Problem getProblem(Optional<Integer> authUserId, Setup setup, int reqId, boolean showHiddenMedia) {
		var linksFuture = CompletableFuture.supplyAsync(() -> externalLinksRepo.getExternalLinks(0, 0, reqId));

		Problem p = problemRepo.getProblemBase(authUserId, setup, reqId,
				linksFuture,
				sectorId -> sectorRepo.getSectorOutline(sectorId),
				sectorId -> sectorRepo.getSectorTrails(Collections.singleton(sectorId), trailIds -> mediaRepo.getMediaTrails(authUserId, trailIds)).get(sectorId),
				(areaId, sectorId, problemId) -> mediaRepo.getMediaProblem(setup, authUserId, areaId, sectorId, problemId, showHiddenMedia),
				guestbookId -> mediaRepo.getMediaGuestbook(authUserId, guestbookId)
				);

		if (p == null) {
			try {
				var res = hierarchyRepo.getCanonicalUrl(setup, 0, 0, reqId);
				if (res.redirectUrl() != null && !res.redirectUrl().isEmpty()) {
					return new Problem(res.redirectUrl(), 0, false, false, null, null, null, false, 0, 0, 0, false, false, null, null, null, 0, 0, null, null, null, null, null, null, 0, null, false, false, false, 0, null, null, null, null, null, null, null, null, 0, null, null, 0, 0.0, false, null, null, null, null, null, false, null, null, null, null, null, null, null, null);
				}
			} catch (NoSuchElementException _) {}
			throw new NoSuchElementException("Could not find problem with id=" + reqId);
		}
		return p;
	}

	@Transactional(readOnly = true)
	public List<ProblemSearchResult> getProblemsSearch(Optional<Integer> authUserId, Setup setup, String search) {
		return problemRepo.getProblemsSearch(authUserId, setup, search);
	}

	@Transactional
	public Redirect setProblem(Optional<Integer> authUserId, Setup s, Problem p) {
		if (authUserId.isEmpty()) throw new UnauthorizedException("User not logged in");

		var dt = (p.faDate() == null || p.faDate().isEmpty()) ? null : LocalDate.parse(p.faDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		var isLockedAdmin = p.lockedSuperadmin() ? false : p.lockedAdmin();

		if (p.coordinates() != null) {
			if (p.coordinates().latitude() == 0 || p.coordinates().longitude() == 0) {
				p = p.withCoordinates(null);
			} else {
				geoService.ensureConsistency(List.of(p.coordinates()));
			}
		}

		sectorRepo.tryFixSectorOrdering(p.sectorId(), p.id(), p.nr());
		var gradeId = s.gradeConverter().getIdGradeFromGrade(p.originalGrade());
		var coordinatesId = p.coordinates() == null || p.coordinates().id() == 0 ? null : p.coordinates().id();

		int nr = p.nr();
		if (p.id() <= 0) {
			sectorRepo.ensureAdminWriteSector(authUserId, p.sectorId());
			if (nr == 0) {
				nr = sectorRepo.getNextProblemNr(p.sectorId());
			}
		}

		int idProblem = problemRepo.setProblemDb(authUserId, s, p, isLockedAdmin, dt, gradeId, coordinatesId, nr,
				name -> userRepo.getExistingOrInsertUser(name)
				);

		externalLinksRepo.upsertExternalLinks(p.externalLinks(), 0, 0, idProblem);
		activityRepo.fillActivity(idProblem);

		return p.trash() ? Redirect.fromIdSector(p.sectorId()) : Redirect.fromIdProblem(idProblem);
	}

	@Transactional
	public int upsertComment(Optional<Integer> authUserId, Setup s, Comment co) {
		int userId = authUserId.orElseThrow(() -> new UnauthorizedException("Not logged in"));

		Problem p = null;
		if (co.id() > 0) {
			p = getProblem(authUserId, s, co.idProblem(), false);
		}

		int idGuestbook = problemRepo.upsertCommentDb(userId, co, p);
		activityRepo.fillActivity(co.idProblem());
		return idGuestbook;
	}
}
