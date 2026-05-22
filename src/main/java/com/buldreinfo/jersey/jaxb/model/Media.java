package com.buldreinfo.jersey.jaxb.model;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

public record Media(MediaIdentity identity, boolean uploadedByMe, int width, int height, boolean isMovie,
		String dateCreated, String dateTaken, User photographer, List<User> tagged, String description,
		List<MediaSvgElement> mediaSvgs, int svgProblemId, List<Svg> svgs,
		String embedUrl, int thumbnailSeconds,
		boolean inherited,
		int enableMoveToIdArea, int enableMoveToIdSector, int enableMoveToIdProblem,
		String url,
		List<MediaArea> areas, List<MediaSector> sectors, List<MediaProblem> problems, int guestbookId) {
	
	public record MediaArea(int areaId, String areaName, boolean trivia) {}
	public record MediaSector(int sectorId, String areaName, String sectorName, boolean trivia) {}
	public record MediaProblem(int problemId, String problemName, String problemGrade, int problemPitch, int problemNumPitches, long milliseconds, String areaName, String sectorName, boolean trivia) {}

	private static final TypeAdapter<Boolean> booleanCoercionAdapter = new TypeAdapter<>() {
		@Override
		public void write(JsonWriter out, Boolean value) throws IOException {
			if (value == null) {
				out.nullValue();
			} else {
				out.value(value);
			}
		}

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
	};

	private static final TypeAdapter<Integer> integerCoercionAdapter = new TypeAdapter<>() {
		@Override
		public void write(JsonWriter out, Integer value) throws IOException {
			if (value == null) {
				out.nullValue();
			} else {
				out.value(value);
			}
		}

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
	};

	public static Media fromResultSet(ResultSet rst, int currentAuthUserId, Gson baseGson) throws SQLException {
		Gson localGson = baseGson.newBuilder()
				.registerTypeAdapter(boolean.class, booleanCoercionAdapter)
				.registerTypeAdapter(Boolean.class, booleanCoercionAdapter)
				.registerTypeAdapter(int.class, integerCoercionAdapter)
				.registerTypeAdapter(Integer.class, integerCoercionAdapter)
				.create();

		MediaIdentity identity = new MediaIdentity(
				rst.getInt("id"), rst.getLong("version_stamp"), rst.getInt("focus_x"), 
				rst.getInt("focus_y"), rst.getString("media_primary_color_hex")
		);
		
		User photographer = null;
		int photographerId = rst.getInt("photographer_id");
		if (!rst.wasNull()) {
			photographer = User.from(photographerId, rst.getString("photographer_name"));
		}
		
		String taggedJson = rst.getString("tagged_json");
		List<User> taggedUsers = Strings.isNullOrEmpty(taggedJson) ? List.of() : localGson.fromJson(taggedJson, new TypeToken<List<User>>(){}.getType());
		
		String areasJson = rst.getString("areas_json");
		List<MediaArea> areas = Strings.isNullOrEmpty(areasJson) ? List.of() : localGson.fromJson(areasJson, new TypeToken<List<MediaArea>>(){}.getType());
		
		String sectorsJson = rst.getString("sectors_json");
		List<MediaSector> sectors = Strings.isNullOrEmpty(sectorsJson) ? List.of() : localGson.fromJson(sectorsJson, new TypeToken<List<MediaSector>>(){}.getType());
		
		String problemsJson = rst.getString("problems_json");
		List<MediaProblem> problems = Strings.isNullOrEmpty(problemsJson) ? List.of() : localGson.fromJson(problemsJson, new TypeToken<List<MediaProblem>>(){}.getType());
		
		List<MediaSvgElement> svgElements = parseSvgElements(rst.getString("svgs_json"), localGson);
		
		return new Media(
				identity, rst.getInt("uploader_user_id") == currentAuthUserId, 
				rst.getInt("width"), rst.getInt("height"), rst.getBoolean("is_movie"), 
				rst.getString("date_created"), rst.getString("date_taken"), 
				photographer, taggedUsers, rst.getString("description"),
				svgElements, 0, List.of(), rst.getString("embed_url"), rst.getInt("thumbnail_seconds"), 
				false, 0, 0, 0, null, 
				areas, sectors, problems, rst.getInt("guestbook_id")
		);
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