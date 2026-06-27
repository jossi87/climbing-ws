package com.buldreinfo.model;

import java.util.Collection;
import java.util.List;

public record Problem(String redirectUrl, int areaId, boolean areaLockedAdmin, boolean areaLockedSuperadmin, String areaName,
		String areaAccessInfo, String areaAccessClosed, boolean areaNoDogsAllowed, int areaSunFromHour, int areaSunToHour,
		int sectorId, boolean sectorLockedAdmin, boolean sectorLockedSuperadmin, String sectorName,
		String sectorAccessInfo, String sectorAccessClosed, int sectorSunFromHour, int sectorSunToHour,
		Coordinates sectorParking, List<Coordinates> sectorOutline, CompassDirection sectorWallDirectionCalculated, CompassDirection sectorWallDirectionManual,
		Collection<Trail> trails, List<Neighbour> neighbours,
		int id, String broken, boolean trash, boolean lockedAdmin, boolean lockedSuperadmin,
		int nr, String name, String rock, String comment, String grade, String originalGrade, String faDate, String faDateHr, List<User> fa,
		int lengthMeter, Coordinates coordinates, List<Media> media, int numTicks, double stars, boolean ticked,
		List<ProblemTick> ticks, List<ProblemTodo> todos, List<ProblemComment> comments,
		Type t, List<ProblemSection> sections, boolean todo, List<ExternalLink> externalLinks,
		String pageViews, FaAid faAid, String trivia, List<Media> triviaMedia, String startingAltitude, String aspect, String descent
		) {
	public record ProblemTodo(int idUser, MediaIdentity mediaIdentity, String name) {}
	public record Neighbour(int id, int nr, String name, String grade, boolean ticked, boolean todo) {}

	public Problem withTicks(List<ProblemTick> newTicks) {
        return new Problem(redirectUrl, areaId, areaLockedAdmin, areaLockedSuperadmin, areaName, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour, sectorId, sectorLockedAdmin, sectorLockedSuperadmin, sectorName, sectorAccessInfo, sectorAccessClosed, sectorSunFromHour, sectorSunToHour, sectorParking, sectorOutline, sectorWallDirectionCalculated, sectorWallDirectionManual, trails, neighbours, id, broken, trash, lockedAdmin, lockedSuperadmin, nr, name, rock, comment, grade, originalGrade, faDate, faDateHr, fa, lengthMeter, coordinates, media, numTicks, stars, ticked, newTicks, todos, comments, t, sections, todo, externalLinks, pageViews, faAid, trivia, triviaMedia, startingAltitude, aspect, descent);
    }

    public Problem withComments(List<ProblemComment> newComments) {
        return new Problem(redirectUrl, areaId, areaLockedAdmin, areaLockedSuperadmin, areaName, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour, sectorId, sectorLockedAdmin, sectorLockedSuperadmin, sectorName, sectorAccessInfo, sectorAccessClosed, sectorSunFromHour, sectorSunToHour, sectorParking, sectorOutline, sectorWallDirectionCalculated, sectorWallDirectionManual, trails, neighbours, id, broken, trash, lockedAdmin, lockedSuperadmin, nr, name, rock, comment, grade, originalGrade, faDate, faDateHr, fa, lengthMeter, coordinates, media, numTicks, stars, ticked, ticks, todos, newComments, t, sections, todo, externalLinks, pageViews, faAid, trivia, triviaMedia, startingAltitude, aspect, descent);
    }

    public Problem withTodos(List<ProblemTodo> newTodos) {
        return new Problem(redirectUrl, areaId, areaLockedAdmin, areaLockedSuperadmin, areaName, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour, sectorId, sectorLockedAdmin, sectorLockedSuperadmin, sectorName, sectorAccessInfo, sectorAccessClosed, sectorSunFromHour, sectorSunToHour, sectorParking, sectorOutline, sectorWallDirectionCalculated, sectorWallDirectionManual, trails, neighbours, id, broken, trash, lockedAdmin, lockedSuperadmin, nr, name, rock, comment, grade, originalGrade, faDate, faDateHr, fa, lengthMeter, coordinates, media, numTicks, stars, ticked, ticks, newTodos, comments, t, sections, todo, externalLinks, pageViews, faAid, trivia, triviaMedia, startingAltitude, aspect, descent);
    }

	public Problem withCoordinates(Coordinates newCoordinates) {
		return new Problem(redirectUrl, areaId, areaLockedAdmin, areaLockedSuperadmin, areaName, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour, sectorId, sectorLockedAdmin, sectorLockedSuperadmin, sectorName, sectorAccessInfo, sectorAccessClosed, sectorSunFromHour, sectorSunToHour, sectorParking, sectorOutline, sectorWallDirectionCalculated, sectorWallDirectionManual, trails, neighbours, id, broken, trash, lockedAdmin, lockedSuperadmin, nr, name, rock, comment, grade, originalGrade, faDate, faDateHr, fa, lengthMeter, newCoordinates, media, numTicks, stars, ticked, ticks, todos, comments, t, sections, todo, externalLinks, pageViews, faAid, trivia, triviaMedia, startingAltitude, aspect, descent);
	}

	public Problem withFaAid(FaAid newFaAid) {
		return new Problem(redirectUrl, areaId, areaLockedAdmin, areaLockedSuperadmin, areaName, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour, sectorId, sectorLockedAdmin, sectorLockedSuperadmin, sectorName, sectorAccessInfo, sectorAccessClosed, sectorSunFromHour, sectorSunToHour, sectorParking, sectorOutline, sectorWallDirectionCalculated, sectorWallDirectionManual, trails, neighbours, id, broken, trash, lockedAdmin, lockedSuperadmin, nr, name, rock, comment, grade, originalGrade, faDate, faDateHr, fa, lengthMeter, coordinates, media, numTicks, stars, ticked, ticks, todos, comments, t, sections, todo, externalLinks, pageViews, newFaAid, trivia, triviaMedia, startingAltitude, aspect, descent);
	}
}