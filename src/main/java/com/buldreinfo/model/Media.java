package com.buldreinfo.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.buldreinfo.util.JsonHelper;

public record Media(MediaIdentity identity, boolean uploadedByMe, int width, int height, boolean isMovie, String suffix, boolean is360,
		String dateCreated, String dateTaken, User photographer, List<User> tagged, String description,
		List<MediaSvgElement> mediaSvgs, int svgProblemId, List<Svg> svgs,
		String embedUrl, int thumbnailSeconds, boolean inherited, List<MediaArea> areas, List<MediaSector> sectors, List<MediaProblem> problems, List<MediaTrail> trails, int guestbookId, int userAvatarId) {

	public record MediaArea(int areaId, String areaName, boolean trivia) {}
	public record MediaSector(int areaId, String areaName, int sectorId, String sectorName, boolean trivia) {}
	public record MediaProblem(int problemId, String problemName, String problemGrade, int problemPitch, int problemNumPitches, long milliseconds, int areaId, String areaName, int sectorId, String sectorName, boolean trivia) {}
	public record MediaTrail(int trailId, String trailTitle, List<MediaTrailSector> sectors) {
		public record MediaTrailSector(int areaId, String areaName, int sectorId, String sectorName) {}
	}
	public enum Association{ AREAS, GUESTBOOK, PROBLEMS, SECTORS, TRAILS, USER_AVATAR }

	public static Media fromResultSet(JsonHelper jsonHelper, ResultSet rs, Optional<Integer> authUserId) throws SQLException {
	    MediaIdentity identity = new MediaIdentity(
	            rs.getInt("id"), rs.getLong("version_stamp"), rs.getInt("focus_x"), 
	            rs.getInt("focus_y"), rs.getString("media_primary_color_hex")
	    );

	    int photographerId = rs.getInt("photographer_id");
	    User photographer = (photographerId > 0) ? User.from(photographerId, rs.getString("photographer_name")) : null;

	    return new Media(
	            identity, rs.getInt("uploader_user_id") == authUserId.orElse(0), 
	            rs.getInt("width"), rs.getInt("height"), rs.getBoolean("is_movie"), 
	            rs.getString("suffix"), rs.getBoolean("is_360"),
	            formatLocalDate(rs.getObject("date_created", LocalDateTime.class)),
	            formatLocalDate(rs.getObject("date_taken", LocalDateTime.class)), 
	            photographer, 
	            jsonHelper.parseArray(rs.getString("tagged_json"), User[].class, Comparator.comparing(User::name, Comparator.nullsLast(Comparator.naturalOrder()))),
	            rs.getString("description"),
	            jsonHelper.parseSvgElements(rs.getString("svgs_json")), 
	            0, 
	            jsonHelper.parseArray(rs.getString("svgs_table_json"), Svg[].class, null), 
	            rs.getString("embed_url"), 
	            rs.getInt("thumbnail_seconds"), false, 
	            jsonHelper.parseArray(rs.getString("areas_json"), MediaArea[].class, Comparator.comparing(MediaArea::areaName, Comparator.nullsLast(Comparator.naturalOrder()))),
	            jsonHelper.parseArray(rs.getString("sectors_json"), MediaSector[].class, Comparator.comparing(MediaSector::sectorName, Comparator.nullsLast(Comparator.naturalOrder()))),
	            jsonHelper.parseArray(rs.getString("problems_json"), MediaProblem[].class, Comparator.comparingLong(MediaProblem::milliseconds).thenComparing(MediaProblem::problemName, Comparator.nullsLast(Comparator.naturalOrder()))),
	            jsonHelper.parseArray(rs.getString("trails_json"), MediaTrail[].class, Comparator.comparingInt(MediaTrail::trailId)),
	            rs.getInt("guestbook_id"), 0
	    );
	}

	public Association ensureCorrectMediaAssociations(Optional<Integer> authUserId) {
		Association activeAssociation = null;
		int associationCount = 0;
		if (areas() != null && !areas().isEmpty()) {
			activeAssociation = Association.AREAS;
			associationCount++;
		}
		if (sectors() != null && !sectors().isEmpty()) {
			activeAssociation = Association.SECTORS;
			associationCount++;
		}
		if (problems() != null && !problems().isEmpty()) {
			activeAssociation = Association.PROBLEMS;
			associationCount++;
		}
		if (trails() != null && !trails().isEmpty()) {
			activeAssociation = Association.TRAILS;
			associationCount++;
		}
		if (guestbookId() > 0) {
			activeAssociation = Association.GUESTBOOK;
			associationCount++;
		}
		if (userAvatarId() > 0) {
			if (userAvatarId() != authUserId.orElseThrow()) {
				throw new IllegalArgumentException("Cannot associate media to another user's avatar");
			}
			activeAssociation = Association.USER_AVATAR;
			associationCount++;
		}
		if (associationCount != 1) {
			throw new IllegalArgumentException("Media must be associated with exactly one entity type. Found: " + associationCount);
		}
		return activeAssociation;
	}
	
	private static String formatLocalDate(LocalDateTime dt) {
	    return dt == null ? null : dt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
	}
}
