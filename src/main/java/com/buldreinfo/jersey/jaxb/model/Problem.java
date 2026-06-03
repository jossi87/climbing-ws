package com.buldreinfo.jersey.jaxb.model;

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

	public ProblemComment addComment(int id, String date, int idUser, MediaIdentity mediaIdentity, String name, String message, boolean danger, boolean resolved, List<Media> media) {
		ProblemComment comment = new ProblemComment(id, date, idUser, mediaIdentity, name, message, danger, resolved, media);
		this.comments.add(comment);
		return comment;
	}

	public void addSection(int id, int nr, String description, String grade, List<Media> media) {
		this.sections.add(new ProblemSection(id, nr, description, grade, media));
	}

	public ProblemTick addTick(int id, int idUser, MediaIdentity mediaIdentity, String date, String name, String suggestedGrade, boolean noPersonalGrade, String comment, double stars, boolean writable) {
		ProblemTick t = new ProblemTick(id, idUser, mediaIdentity, date, name, suggestedGrade, noPersonalGrade, comment, stars, writable);
		this.ticks.add(t);
		return t;
	}

	public void addTodo(int idUser, MediaIdentity mediaIdentity, String name) {
		this.todos.add(new ProblemTodo(idUser, mediaIdentity, name));
	}

	public Problem withCoordinates(Coordinates newCoordinates) {
		return new Problem(redirectUrl, areaId, areaLockedAdmin, areaLockedSuperadmin, areaName, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour, sectorId, sectorLockedAdmin, sectorLockedSuperadmin, sectorName, sectorAccessInfo, sectorAccessClosed, sectorSunFromHour, sectorSunToHour, sectorParking, sectorOutline, sectorWallDirectionCalculated, sectorWallDirectionManual, trails, neighbours, id, broken, trash, lockedAdmin, lockedSuperadmin, nr, name, rock, comment, grade, originalGrade, faDate, faDateHr, fa, lengthMeter, newCoordinates, media, numTicks, stars, ticked, ticks, todos, comments, t, sections, todo, externalLinks, pageViews, faAid, trivia, triviaMedia, startingAltitude, aspect, descent);
	}

	public Problem withFaAid(FaAid newFaAid) {
		return new Problem(redirectUrl, areaId, areaLockedAdmin, areaLockedSuperadmin, areaName, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour, sectorId, sectorLockedAdmin, sectorLockedSuperadmin, sectorName, sectorAccessInfo, sectorAccessClosed, sectorSunFromHour, sectorSunToHour, sectorParking, sectorOutline, sectorWallDirectionCalculated, sectorWallDirectionManual, trails, neighbours, id, broken, trash, lockedAdmin, lockedSuperadmin, nr, name, rock, comment, grade, originalGrade, faDate, faDateHr, fa, lengthMeter, coordinates, media, numTicks, stars, ticked, ticks, todos, comments, t, sections, todo, externalLinks, pageViews, newFaAid, trivia, triviaMedia, startingAltitude, aspect, descent);
	}
}