package com.buldreinfo.model;

import java.time.LocalDateTime;
import java.util.List;

public record Profile(ProfileIdentity identity, ProfileKpis kpis, List<ProfileDiscipline> disciplines) {
	public record ProfileIdentity(int id, String firstname, String lastname, boolean emailVisibleToAll, String themePreference, String languagePreference, MediaIdentity mediaIdentity, List<String> emails, List<UserRegion> userRegions, List<String> activityRegions, LocalDateTime lastActivity) {}
	public record ProfileKpis(int numImagesCreated, int numVideosCreated, int numImageTags, int numVideoTags) {}
	public record ProfileDiscipline(String type, Boolean isBolted, boolean isBouldering, boolean isIceClimbing, int gradeSystemId, String gradeSystem, boolean multiPitch, List<ProfileDisciplineGradeDistribution> gradeDistribution) {}
	public record ProfileDisciplineGradeDistribution(String grade, String color, int fa, int tick) {}
}
