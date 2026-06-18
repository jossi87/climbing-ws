package com.buldreinfo.jersey.jaxb.model;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

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
	public enum Association{ AREAS, SECTORS, PROBLEMS, TRAILS, GUESTBOOK, USER_AVATAR }

	private static final TypeAdapter<Boolean> booleanCoercionAdapter = new TypeAdapter<>() {
		@Override
		public Boolean read(JsonReader in) throws IOException {
			JsonToken peek = in.peek();
			if (peek == JsonToken.NUMBER) {
				return in.nextInt() != 0;
			} else if (peek == JsonToken.BOOLEAN) {
				return in.nextBoolean();
			} else if (peek == JsonToken.NULL) {
				in.nextNull();
				return false;
			}
			throw new IllegalStateException("Expected BOOLEAN or NUMBER but found: " + peek);
		}

		@Override
		public void write(JsonWriter out, Boolean value) throws IOException {
			if (value == null) {
				out.nullValue();
			} else {
				out.value(value);
			}
		}
	};

	private static final TypeAdapter<Integer> integerCoercionAdapter = new TypeAdapter<>() {
		@Override
		public Integer read(JsonReader in) throws IOException {
			JsonToken peek = in.peek();
			if (peek == JsonToken.NULL) {
				in.nextNull();
				return 0;
			} else if (peek == JsonToken.NUMBER) {
				return in.nextInt();
			} else if (peek == JsonToken.STRING) {
				String str = in.nextString();
				return Strings.isNullOrEmpty(str) ? 0 : Integer.parseInt(str);
			}
			throw new IllegalStateException("Expected NUMBER or NULL but found: " + peek);
		}

		@Override
		public void write(JsonWriter out, Integer value) throws IOException {
			if (value == null) {
				out.nullValue();
			} else {
				out.value(value);
			}
		}
	};

	private static final Gson MEDIA_GSON = new Gson()
			.newBuilder()
			.registerTypeAdapter(boolean.class, booleanCoercionAdapter)
			.registerTypeAdapter(Boolean.class, booleanCoercionAdapter)
			.registerTypeAdapter(int.class, integerCoercionAdapter)
			.registerTypeAdapter(Integer.class, integerCoercionAdapter)
			.create();

	public static Media fromResultSet(ResultSet rst, Optional<Integer> authUserId) throws SQLException {
		Gson localGson = MEDIA_GSON;

		MediaIdentity identity = new MediaIdentity(
				rst.getInt("id"), rst.getLong("version_stamp"), rst.getInt("focus_x"), 
				rst.getInt("focus_y"), rst.getString("media_primary_color_hex")
				);

		User photographer = null;
		int photographerId = rst.getInt("photographer_id");
		if (!rst.wasNull()) {
			photographer = User.from(photographerId, rst.getString("photographer_name"));
		}

		List<User> taggedUsers = parseAndSortJsonArray(rst, "tagged_json", localGson, new TypeToken<List<User>>(){}.getType(),
				Comparator.comparing(User::name, Comparator.nullsLast(Comparator.naturalOrder())));

		List<MediaArea> areas = parseAndSortJsonArray(rst, "areas_json", localGson, new TypeToken<List<MediaArea>>(){}.getType(),
				Comparator.comparing(MediaArea::areaName, Comparator.nullsLast(Comparator.naturalOrder())));

		List<MediaSector> sectors = parseAndSortJsonArray(rst, "sectors_json", localGson, new TypeToken<List<MediaSector>>(){}.getType(),
				Comparator.comparing(MediaSector::sectorName, Comparator.nullsLast(Comparator.naturalOrder())));

		List<MediaProblem> problems = parseAndSortJsonArray(rst, "problems_json", localGson, new TypeToken<List<MediaProblem>>(){}.getType(),
				Comparator.comparingLong(MediaProblem::milliseconds)
						.thenComparing(MediaProblem::problemName, Comparator.nullsLast(Comparator.naturalOrder())));

		List<MediaTrail> trails = parseAndSortJsonArray(rst, "trails_json", localGson, new TypeToken<List<MediaTrail>>(){}.getType(),
				Comparator.comparingInt(MediaTrail::trailId));

		List<MediaSvgElement> svgElements = parseSvgElements(rst.getString("svgs_json"), localGson);

		List<Svg> svgsList = parseAndSortJsonArray(rst, "svgs_table_json", localGson, new TypeToken<List<Svg>>(){}.getType(), null);

		return new Media(
				identity, rst.getInt("uploader_user_id") == authUserId.orElse(0), 
				rst.getInt("width"), rst.getInt("height"), rst.getBoolean("is_movie"), rst.getBoolean("is_360"),
				rst.getString("date_created"), rst.getString("date_taken"), 
				photographer, taggedUsers, rst.getString("description"),
				svgElements, 0, svgsList, rst.getString("embed_url"), rst.getInt("thumbnail_seconds"), 
				false, areas, sectors, problems, trails, rst.getInt("guestbook_id"), 0
				);
	}

	private static <T> List<T> parseAndSortJsonArray(ResultSet rst, String column, Gson gson, java.lang.reflect.Type type, Comparator<? super T> comparator) throws SQLException {
		String json = rst.getString(column);
		if (Strings.isNullOrEmpty(json)) {
			return List.of();
		}
		List<T> result = gson.fromJson(json, type);
		if (result != null && !result.isEmpty() && comparator != null) {
			result = result.stream()
					.sorted(comparator)
					.toList();
		}
		return result;
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
	        Preconditions.checkArgument(userAvatarId() == authUserId.orElseThrow(), "Cannot associate media to another user's avatar");
	        activeAssociation = Association.USER_AVATAR;
	        associationCount++;
	    }
	    Preconditions.checkArgument(associationCount == 1, "Media must be associated with exactly one entity type. Found: " + associationCount);
	    return activeAssociation;
	}

	private static List<MediaSvgElement> parseSvgElements(String svgsJson, Gson gson) {
		List<MediaSvgElement> svgElements = new ArrayList<>();
		if (!Strings.isNullOrEmpty(svgsJson)) {
			JsonArray svgArray = gson.fromJson(svgsJson, JsonArray.class);
			for (JsonElement el : svgArray) {
				JsonObject obj = el.getAsJsonObject();
				int svgId = obj.get("id").getAsInt();
				if (obj.has("path") && !obj.get("path").isJsonNull()) {
					svgElements.add(MediaSvgElement.fromPath(svgId, obj.get("path").getAsString()));
				} else {
					int rappelX = obj.has("rappelX") && !obj.get("rappelX").isJsonNull() ? obj.get("rappelX").getAsInt() : 0;
					int rappelY = obj.has("rappelY") && !obj.get("rappelY").isJsonNull() ? obj.get("rappelY").getAsInt() : 0;

					boolean bolted = false;
					if (obj.has("rappelBolted") && !obj.get("rappelBolted").isJsonNull()) {
						JsonElement rbEl = obj.get("rappelBolted");
						if (rbEl.isJsonPrimitive() && rbEl.getAsJsonPrimitive().isNumber()) {
							bolted = rbEl.getAsInt() != 0;
						} else {
							bolted = rbEl.getAsBoolean();
						}
					}

					svgElements.add(MediaSvgElement.fromRappel(svgId, rappelX, rappelY, bolted));
				}
			}
		}
		return svgElements;
	}
}