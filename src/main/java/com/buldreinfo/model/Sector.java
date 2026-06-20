package com.buldreinfo.model;

import java.util.Collection;
import java.util.List;

public record Sector(String redirectUrl, boolean orderByGrade,
		int areaId, boolean areaLockedAdmin, boolean areaLockedSuperadmin, String areaAccessInfo, String areaAccessClosed, boolean areaNoDogsAllowed, int areaSunFromHour, int areaSunToHour, String areaName,
		int id, boolean trash, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment,
		String accessInfo, String accessClosed, int sunFromHour, int sunToHour, Coordinates parking, List<Coordinates> outline,
		CompassDirection wallDirectionCalculated, CompassDirection wallDirectionManual, Collection<Trail> trails,
		List<Media> media, List<Media> triviaMedia,
		List<SectorJump> sectors, List<SectorProblem> problems, List<SectorProblemOrder> problemOrder, List<ExternalLink> externalLinks, String pageViews) {
	public record SectorJump(int id, boolean lockedAdmin, boolean lockedSuperadmin, String name, int sorting) {}
	public record SectorProblemOrder(int id, String name, int nr) {}
	public record SectorProblem(int id, String broken, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String rock, String comment, int gradeWeight, String grade,
			String faUser, int faYear, String ffaUser, int ffaYear,
			int lengthMeter, int numPitches,
			boolean hasImages, boolean hasMovies, boolean hasTopo, Coordinates coordinates, int numTicks, double stars, boolean ticked, boolean todo, Type t,
			boolean danger) {}

	public Sector withParking(Coordinates newParking) {
		return new Sector(redirectUrl, orderByGrade, areaId, areaLockedAdmin, areaLockedSuperadmin, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour, areaName, id, trash, lockedAdmin, lockedSuperadmin, name, comment, accessInfo, accessClosed, sunFromHour, sunToHour, newParking, outline, wallDirectionCalculated, wallDirectionManual, trails, media, triviaMedia, sectors, problems, problemOrder, externalLinks, pageViews);
	}
}