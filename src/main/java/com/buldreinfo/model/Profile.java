package com.buldreinfo.model;

import java.util.List;

public record Profile(ProfileIdentity identity, ProfileKpis kpis, List<ProfileDiscipline> disciplines) {
	public record ProfileIdentity(int id, String firstname, String lastname, boolean emailVisibleToAll, String themePreference, MediaIdentity mediaIdentity, List<String> emails, List<UserRegion> userRegions, String lastActivity) {}
	public record ProfileKpis(int numImagesCreated, int numVideosCreated, int numImageTags, int numVideoTags) {}
	public record ProfileDiscipline(String discipline, String url, List<ProfileDisciplineGradeDistribution> gradeDistribution) {}
	public record ProfileDisciplineGradeDistribution(String grade, String color, int fa, int tick) {}
}
