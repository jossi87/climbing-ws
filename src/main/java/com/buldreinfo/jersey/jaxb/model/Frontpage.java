package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record Frontpage(FrontpageStats stats,
		List<FrontpageRandomMedia> randomMedia,
		List<FrontpageFirstAscent> firstAscents,
		List<FrontpageRecentAscent> recentAscents,
		List<FrontpageNewestMedia> newestMedia,
		List<FrontpageLastComment> lastComments) {
	public record FrontpageStats(int areas, int problems, int ticks) {}
	public record FrontpageRandomMedia(MediaIdentity identity, int width, int height, int idArea, String area, int idSector, String sector, int idProblem, String problem, String grade, User photographer, List<User> tagged) {}
	public record FrontpageFirstAscent(String timeAgo, int areaId, String areaName,
			int problemId, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String problemName, String problemSubtype,
			String grade, List<User> users) {}
	public record FrontpageRecentAscent(String timeAgo, int areaId, String areaName,
			int problemId, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String problemName, String problemSubtype, String grade,
			User u, boolean repeat) {}
	public record FrontpageNewestMedia(MediaIdentity identity, boolean isMovie, int areaId, String areaName,
			int problemId, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String problemName, String grade) {}
	public record FrontpageLastComment(String timeAgo, int areaId, String areaName,
			int problemId, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String problemName,
			User u, String comment) {}
}