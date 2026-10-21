package com.buldreinfo.model;

import java.util.List;

public record Profile(ProfileIdentity identity, ProfileKpis kpis, List<ProfileDiscipline> disciplines) {
	public record ProfileIdentity(int id, String firstname, String lastname, boolean emailVisibleToAll, String themePreference, String languagePreference, MediaIdentity mediaIdentity, List<String> emails, List<UserRegion> userRegions, List<String> activityRegions, String lastActivity) {}
	public record ProfileKpis(int numImagesCreated, int numVideosCreated, int numImageTags, int numVideoTags) {}
	/**
	 * One overview chart: problem type × grade system × pitch kind, plus the bolting style for single-pitch ascents. A
	 * multi-pitch chart spans both bolting styles (a multi-pitch effort does not change with it), so {@code isBolted}
	 * is {@code null} there; {@code isBouldering} is always {@code false} on those charts (boulders have no pitches).
	 * {@code type} is the localized type name and the client's chart heading; the grade system is its secondary line.
	 */
	public record ProfileDiscipline(String type, Boolean isBolted, boolean isBouldering, int gradeSystemId, String gradeSystem, boolean multiPitch, List<ProfileDisciplineGradeDistribution> gradeDistribution) {}
	public record ProfileDisciplineGradeDistribution(String grade, String color, int fa, int tick) {}
}
