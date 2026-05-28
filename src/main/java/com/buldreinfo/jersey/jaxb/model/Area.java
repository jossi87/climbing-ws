package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.model.Sector.SectorProblem;

public record Area(String redirectUrl, String regionName, int id, boolean trash, boolean lockedAdmin, boolean lockedSuperadmin, boolean forDevelopers,
		String accessInfo, String accessClosed, boolean noDogsAllowed, int sunFromHour, int sunToHour, String name, String comment,
		Coordinates coordinates, int numSectors, int numProblems, List<AreaSector> sectors, List<AreaSectorOrder> sectorOrder,
		List<Media> media, List<Media> triviaMedia, List<ExternalLink> externalLinks, String pageViews
		) {
	public record AreaSector(String areaName, int id, int sorting, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment,
			String accessInfo, String accessClosed, int sunFromHour, int sunToHour,
			Coordinates parking, List<Coordinates> outline, CompassDirection wallDirectionCalculated, CompassDirection wallDirectionManual,
			Slope approach, Slope descent, MediaIdentity randomMedia, List<SectorProblem> problems, int progress, List<GradeCount> gradeCounts) {
		public AreaSector withProgress(int newProgress) {
			return new AreaSector(areaName, id, sorting, lockedAdmin, lockedSuperadmin, name, comment, accessInfo, accessClosed, sunFromHour, sunToHour, parking, outline, wallDirectionCalculated, wallDirectionManual, approach, descent, randomMedia, problems, newProgress, gradeCounts);
		}
		public AreaSector withSlopes(Slope newApproach, Slope newDescent) {
			return new AreaSector(areaName, id, sorting, lockedAdmin, lockedSuperadmin, name, comment, accessInfo, accessClosed, sunFromHour, sunToHour, parking, outline, wallDirectionCalculated, wallDirectionManual, newApproach, newDescent, randomMedia, problems, progress, gradeCounts);
		}
	}
	public record GradeCount(String grade, String color, int num) {}
	public record AreaSectorOrder(int id, String name, int sorting) {}

	public Area withCoordinates(Coordinates newCoordinates) {
		return new Area(redirectUrl, regionName, id, trash, lockedAdmin, lockedSuperadmin, forDevelopers, accessInfo, accessClosed, noDogsAllowed, sunFromHour, sunToHour, name, comment, newCoordinates, numSectors, numProblems, sectors, sectorOrder, media, triviaMedia, externalLinks, pageViews);
	}
}