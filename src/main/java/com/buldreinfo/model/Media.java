package com.buldreinfo.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record Media(MediaIdentity identity, boolean uploadedByMe, int width, int height, boolean isMovie, boolean is360,
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

	public static Media fromResultSet(ObjectMapper objectMapper, ResultSet rst, Optional<Integer> authUserId) throws SQLException, JsonProcessingException {
		MediaIdentity identity = new MediaIdentity(
				rst.getInt("id"), rst.getLong("version_stamp"), rst.getInt("focus_x"), 
				rst.getInt("focus_y"), rst.getString("media_primary_color_hex")
				);

		int photographerId = rst.getInt("photographer_id");
		User photographer = (photographerId > 0) ? User.from(photographerId, rst.getString("photographer_name")) : null;

		List<User> taggedUsers = parseAndSort(rst, "tagged_json", objectMapper, new TypeReference<>() {}, Comparator.comparing(User::name, Comparator.nullsLast(Comparator.naturalOrder())));
		List<MediaArea> areas = parseAndSort(rst, "areas_json", objectMapper, new TypeReference<>() {}, Comparator.comparing(MediaArea::areaName, Comparator.nullsLast(Comparator.naturalOrder())));
		List<MediaSector> sectors = parseAndSort(rst, "sectors_json", objectMapper, new TypeReference<>() {}, Comparator.comparing(MediaSector::sectorName, Comparator.nullsLast(Comparator.naturalOrder())));
		List<MediaProblem> problems = parseAndSort(rst, "problems_json", objectMapper, new TypeReference<>() {}, 
				Comparator.comparingLong(MediaProblem::milliseconds).thenComparing(MediaProblem::problemName, Comparator.nullsLast(Comparator.naturalOrder())));
		List<MediaTrail> trails = parseAndSort(rst, "trails_json", objectMapper, new TypeReference<>() {}, Comparator.comparingInt(MediaTrail::trailId));
		List<Svg> svgsList = parseAndSort(rst, "svgs_table_json", objectMapper, new TypeReference<>() {}, null);

		return new Media(identity, rst.getInt("uploader_user_id") == authUserId.orElse(0), 
				rst.getInt("width"), rst.getInt("height"), rst.getBoolean("is_movie"), rst.getBoolean("is_360"),
				rst.getString("date_created"), rst.getString("date_taken"), 
				photographer, taggedUsers, rst.getString("description"),
				parseSvgElements(rst.getString("svgs_json"), objectMapper), 0, svgsList, rst.getString("embed_url"), 
				rst.getInt("thumbnail_seconds"), false, areas, sectors, problems, trails, rst.getInt("guestbook_id"), 0
				);
	}

	private static <T> List<T> parseAndSort(ResultSet rst, String column, ObjectMapper mapper, TypeReference<List<T>> type, Comparator<? super T> comparator) throws SQLException, JsonProcessingException {
	    String json = rst.getString(column);
	    if (json == null || json.isBlank()) return List.of();
	    List<T> result = new ArrayList<>(mapper.readValue(json, type));
	    if (comparator != null) result.sort(comparator);
	    return result;
	}

	private static List<MediaSvgElement> parseSvgElements(String json, ObjectMapper mapper) throws JsonProcessingException {
		if (json == null || json.isBlank()) return List.of();
		JsonNode arr = mapper.readTree(json);
		List<MediaSvgElement> list = new ArrayList<>();
		for (JsonNode obj : arr) {
			int id = obj.get("id").asInt();
			if (obj.hasNonNull("path")) list.add(MediaSvgElement.fromPath(id, obj.get("path").asText()));
			else list.add(MediaSvgElement.fromRappel(id, obj.path("rappelX").asInt(), obj.path("rappelY").asInt(), obj.path("rappelBolted").asBoolean()));
		}
		return list;
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
}
