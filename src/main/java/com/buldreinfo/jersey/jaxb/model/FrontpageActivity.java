package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record FrontpageActivity(List<FrontpageFirstAscent> firstAscents,
		List<FrontpageActivityAscent> recentAscents,
		List<FrontpageActivityMedia> newestMedia,
		List<FrontpageActivityComment> FrontpageActivityComment) {
	public record FrontpageFirstAscent(String timeAgo,
			int areaId, String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin,
			int sectorId, String sectorName, boolean sectorLockedAdmin, boolean sectorLockedSuperadmin,
			int problemId, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String problemName, String problemSubtype, String grade,
			List<User> users) {}
	public record FrontpageActivityAscent(String timeAgo,
			int areaId, String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin,
			int sectorId, String sectorName, boolean sectorLockedAdmin, boolean sectorLockedSuperadmin,
			int problemId, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String problemName, String problemSubtype, String grade,
			User u, boolean repeat) {}
	public record FrontpageActivityMedia(MediaIdentity identity,
			int areaId, String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin,
			int sectorId, String sectorName, boolean sectorLockedAdmin, boolean sectorLockedSuperadmin,
			int problemId, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String problemName, String grade) {}
	public record FrontpageActivityComment(String timeAgo,
			int areaId, String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin,
			int sectorId, String sectorName, boolean sectorLockedAdmin, boolean sectorLockedSuperadmin,
			int problemId, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String problemName,
			User u, String comment) {}
}