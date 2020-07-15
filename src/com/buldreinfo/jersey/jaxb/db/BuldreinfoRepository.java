package com.buldreinfo.jersey.jaxb.db;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.imgscalr.Scalr;

import com.buldreinfo.jersey.jaxb.helpers.Auth0Profile;
import com.buldreinfo.jersey.jaxb.helpers.GradeHelper;
import com.buldreinfo.jersey.jaxb.helpers.MarkerHelper;
import com.buldreinfo.jersey.jaxb.helpers.MarkerHelper.LatLng;
import com.buldreinfo.jersey.jaxb.helpers.TimeAgo;
import com.buldreinfo.jersey.jaxb.metadata.MetaHelper;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.buldreinfo.jersey.jaxb.model.Activity;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Comment;
import com.buldreinfo.jersey.jaxb.model.FaAid;
import com.buldreinfo.jersey.jaxb.model.FaUser;
import com.buldreinfo.jersey.jaxb.model.Filter;
import com.buldreinfo.jersey.jaxb.model.FilterRequest;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.GradeDistribution;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.MediaMetadata;
import com.buldreinfo.jersey.jaxb.model.NewMedia;
import com.buldreinfo.jersey.jaxb.model.PermissionUser;
import com.buldreinfo.jersey.jaxb.model.Permissions;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Problem.Section;
import com.buldreinfo.jersey.jaxb.model.ProblemHse;
import com.buldreinfo.jersey.jaxb.model.PublicAscent;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.SitesRegion;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Todo;
import com.buldreinfo.jersey.jaxb.model.TodoUser;
import com.buldreinfo.jersey.jaxb.model.Type;
import com.buldreinfo.jersey.jaxb.model.User;
import com.buldreinfo.jersey.jaxb.model.app.Region;
import com.buldreinfo.jersey.jaxb.util.excel.ExcelReport;
import com.buldreinfo.jersey.jaxb.util.excel.ExcelReport.SheetHyperlink;
import com.buldreinfo.jersey.jaxb.util.excel.ExcelReport.SheetWriter;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.verico.pictures.ExifOrientation;
import com.verico.pictures.ThumbnailCreation;

import jersey.repackaged.com.google.common.base.Joiner;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
public class BuldreinfoRepository {
	private static final String ACTIVITY_TYPE_FA = "FA";
	private static final String ACTIVITY_TYPE_MEDIA = "MEDIA";
	private static final String ACTIVITY_TYPE_GUESTBOOK = "GUESTBOOK";
	private static final String ACTIVITY_TYPE_TICK = "TICK";
	private static final String PATH = "/mnt/buldreinfo/media/";
	private static Logger logger = LogManager.getLogger();
	private final DbConnection c;

	private final Gson gson = new Gson();

	protected BuldreinfoRepository(DbConnection c) {
		this.c = c;
	}

	public void addProblemMedia(int authUserId, Problem p, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, InterruptedException {
		Preconditions.checkArgument(authUserId != -1, "Insufficient permissions");
		Timestamp now = new Timestamp(System.currentTimeMillis());
		for (NewMedia m : p.getNewMedia()) {
			final int idSector = 0;
			final int idArea = 0;
			addNewMedia(authUserId, p.getId(), m.getPitch(), idSector, idArea, m, multiPart, now);
		}
		fillActivity(p.getId());
	}

	public void deleteMedia(int authUserId, int id) throws SQLException {
		List<Integer> idProblems = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT problem_id FROM media_problem WHERE media_id=?")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					idProblems.add(rst.getInt("problem_id"));
				}
			}
		}

		boolean ok = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT auth.write FROM ((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN permission auth ON (a.region_id=auth.region_id AND auth.user_id=?)) LEFT JOIN media_sector ms ON (s.id=ms.sector_id AND ms.media_id=?)) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem mp ON (p.id=mp.problem_id AND mp.media_id=?) GROUP BY auth.write")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, id);
			ps.setInt(3, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getInt(1) != 0;
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient credentials");
		try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE media SET deleted_user_id=?, deleted_timestamp=NOW() WHERE id=?")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, id);
			ps.execute();
		}

		for (int idProblem : idProblems) {
			fillActivity(idProblem);
		}
	}

	public void fillActivity(int idProblem) throws SQLException {
		/**
		 * Delete existing activities on problem
		 */
		try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM activity WHERE problem_id=?")) {
			ps.setInt(1, idProblem);
			ps.execute();
		}

		/**
		 * FA
		 */
		LocalDateTime problemActivityTimestamp = null;
		boolean exists = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT fa_date, last_updated FROM problem WHERE id=?")) {
			ps.setInt(1, idProblem);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					exists = true;
					Timestamp faDate = rst.getTimestamp("fa_date");
					Timestamp lastUpdated = rst.getTimestamp("last_updated");
					if (faDate != null && lastUpdated != null) {
						Calendar faCal = Calendar.getInstance();
						faCal.setTimeInMillis(faDate.getTime());
						Calendar luCal = Calendar.getInstance();
						luCal.setTimeInMillis(lastUpdated.getTime());
						problemActivityTimestamp = LocalDateTime.of(faCal.get(Calendar.YEAR), faCal.get(Calendar.MONTH)+1, faCal.get(Calendar.DAY_OF_MONTH), luCal.get(Calendar.HOUR_OF_DAY), luCal.get(Calendar.MINUTE), luCal.get(Calendar.SECOND));
					}
					else if (faDate != null) {
						Calendar faCal = Calendar.getInstance();
						faCal.setTimeInMillis(faDate.getTime());
						problemActivityTimestamp = LocalDateTime.of(faCal.get(Calendar.YEAR), faCal.get(Calendar.MONTH)+1, faCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
					}
				}
			}
		}
		if (!exists) {
			return;
		}
		try (PreparedStatement psAddActivity = c.getConnection().prepareStatement("INSERT INTO activity (activity_timestamp, type, problem_id, media_id, user_id, guestbook_id) VALUES (?, ?, ?, ?, ?, ?)")) {
			psAddActivity.setTimestamp(1, problemActivityTimestamp == null? new Timestamp(0) : Timestamp.valueOf(problemActivityTimestamp));
			psAddActivity.setString(2, ACTIVITY_TYPE_FA);
			psAddActivity.setInt(3, idProblem);
			psAddActivity.setNull(4, Types.INTEGER);
			psAddActivity.setNull(5, Types.INTEGER);
			psAddActivity.setNull(6, Types.INTEGER);
			psAddActivity.addBatch();


			/**
			 * Media
			 */
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.date_created FROM media_problem mp, media m WHERE mp.problem_id=? AND mp.media_id=m.id AND m.deleted_timestamp IS NULL ORDER BY date_created DESC")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					LocalDateTime useMediaActivityTimestamp = null;
					while (rst.next()) {
						int id = rst.getInt("id");
						Timestamp ts = rst.getTimestamp("date_created");
						LocalDateTime mediaActivityTimestamp = ts == null? null : ts.toLocalDateTime();
						if (mediaActivityTimestamp == null || (problemActivityTimestamp != null && ChronoUnit.DAYS.between(problemActivityTimestamp, mediaActivityTimestamp) < 4)) {
							useMediaActivityTimestamp = problemActivityTimestamp;
						}
						else if (useMediaActivityTimestamp == null || ChronoUnit.DAYS.between(useMediaActivityTimestamp, mediaActivityTimestamp) > 4) {
							useMediaActivityTimestamp = mediaActivityTimestamp;
						}
						psAddActivity.setTimestamp(1, useMediaActivityTimestamp == null? new Timestamp(0) : Timestamp.valueOf(useMediaActivityTimestamp));
						psAddActivity.setString(2, ACTIVITY_TYPE_MEDIA);
						psAddActivity.setInt(3, idProblem);
						psAddActivity.setInt(4, id);
						psAddActivity.setNull(5, Types.INTEGER);
						psAddActivity.setNull(6, Types.INTEGER);
						psAddActivity.addBatch();
					}
				}
			}

			/**
			 * Tick
			 */
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT user_id, date, created FROM tick WHERE problem_id=? ORDER BY date, created")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int userId = rst.getInt("user_id");
						LocalDateTime tickActivityTimestamp = null;
						Timestamp tickDate = rst.getTimestamp("date");
						Timestamp tickCreated = rst.getTimestamp("created");
						if (tickDate != null && tickCreated != null) {
							Calendar tickCal = Calendar.getInstance();
							tickCal.setTimeInMillis(tickDate.getTime());
							Calendar createdCal = Calendar.getInstance();
							createdCal.setTimeInMillis(tickCreated.getTime());
							tickActivityTimestamp = LocalDateTime.of(tickCal.get(Calendar.YEAR), tickCal.get(Calendar.MONTH)+1, tickCal.get(Calendar.DAY_OF_MONTH), createdCal.get(Calendar.HOUR_OF_DAY), createdCal.get(Calendar.MINUTE), createdCal.get(Calendar.SECOND));
						}
						else if (tickDate != null) {
							Calendar tickCal = Calendar.getInstance();
							tickCal.setTimeInMillis(tickDate.getTime());
							tickActivityTimestamp = LocalDateTime.of(tickCal.get(Calendar.YEAR), tickCal.get(Calendar.MONTH)+1, tickCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
						}
						psAddActivity.setTimestamp(1, tickActivityTimestamp == null? new Timestamp(0) : Timestamp.valueOf(tickActivityTimestamp));
						psAddActivity.setString(2, ACTIVITY_TYPE_TICK);
						psAddActivity.setInt(3, idProblem);
						psAddActivity.setNull(4, Types.INTEGER);
						psAddActivity.setInt(5, userId);
						psAddActivity.setNull(6, Types.INTEGER);
						psAddActivity.addBatch();
					}
				}
			}

			/**
			 * Guestbook
			 */
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, post_time FROM guestbook WHERE problem_id=? ORDER BY post_time")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Timestamp postTime = rst.getTimestamp("post_time");
						psAddActivity.setTimestamp(1, postTime);
						psAddActivity.setString(2, ACTIVITY_TYPE_GUESTBOOK);
						psAddActivity.setInt(3, idProblem);
						psAddActivity.setNull(4, Types.INTEGER);
						psAddActivity.setNull(5, Types.INTEGER);
						psAddActivity.setInt(6, id);
						psAddActivity.addBatch();
					}
				}
			}

			/**
			 * Execute psAddActivity
			 */
			psAddActivity.executeBatch();
		}
	}

	public List<Activity> getActivity(int authUserId, Setup setup, int idArea, int idSector, int lowerGrade, boolean fa, boolean comments, boolean ticks, boolean media) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		final List<Activity> res = new ArrayList<>();
		/**
		 * Fetch activities to return
		 */
		final Set<Integer> faActivitityIds = new HashSet<>();
		final Set<Integer> tickActivitityIds = new HashSet<>();
		final Set<Integer> mediaActivitityIds = new HashSet<>();
		final Set<Integer> guestbookActivitityIds = new HashSet<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT x.activity_timestamp, x.problem_id, p.hidden problem_visibility, p.name problem_name, p.grade, GROUP_CONCAT(concat(x.id,'-',x.type) SEPARATOR ',') activities" + 
				" FROM (((((activity x INNER JOIN problem p ON x.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?)" + 
				" WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR auth.user_id IS NOT NULL)" + 
				"   AND (a.hidden=0 OR auth.write>=a.hidden) AND (s.hidden=0 OR auth.write>=s.hidden) AND (p.hidden=0 OR auth.write>=p.hidden)" + 
				(lowerGrade == 0? "" : " AND p.grade>=" + lowerGrade) +
				(fa? "" : " AND x.type!='FA'") +
				(comments? "" : " AND x.type!='GUESTBOOK'") +
				(ticks? "" : " AND x.type!='TICK'") +
				(media? "" : " AND x.type!='MEDIA'") +
				(idArea==0? "" : " AND a.id=" + idArea) +
				(idSector==0? "" : " AND s.id=" + idSector) +
				" GROUP BY x.activity_timestamp, x.problem_id, p.hidden, p.name, p.grade" +
				" ORDER BY -x.activity_timestamp, x.problem_id DESC LIMIT 100")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					Timestamp activityTimestamp = rst.getTimestamp("activity_timestamp");
					int problemId = rst.getInt("problem_id");
					int problemVisibility = rst.getInt("problem_visibility");
					String problemName = rst.getString("problem_name");
					String grade = GradeHelper.intToString(setup, rst.getInt("grade"));
					Set<Integer> activityIds = new HashSet<>();
					for (String activity : rst.getString("activities").split(",")) {
						String[] str = activity.split("-");
						int idActivity = Integer.parseInt(str[0]);
						String type = str[1];
						activityIds.add(idActivity);
						switch (type) {
						case ACTIVITY_TYPE_FA: faActivitityIds.add(idActivity); break;
						case ACTIVITY_TYPE_TICK: tickActivitityIds.add(idActivity); break;
						case ACTIVITY_TYPE_GUESTBOOK: guestbookActivitityIds.add(idActivity); break;
						case ACTIVITY_TYPE_MEDIA: mediaActivitityIds.add(idActivity); break;
						default: throw new RuntimeException("Invalid type: " + type);
						}
					}

					String timeAgo = TimeAgo.getTimeAgo(activityTimestamp.toLocalDateTime().toLocalDate());
					res.add(new Activity(activityIds, timeAgo, problemId, problemVisibility, problemName, grade));
				}
			}
		}

		if (!tickActivitityIds.isEmpty()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') END picture, t.comment description, t.stars, t.grade" + 
					" FROM activity a, tick t, user u" + 
					" WHERE a.id IN (" + Joiner.on(",").join(tickActivitityIds) + ")" + 
					"   AND a.user_id=u.id AND a.problem_id=t.problem_id AND u.id=t.user_id")) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						int userId = rst.getInt("user_id");
						String name = rst.getString("name");
						String picture = rst.getString("picture");
						String description = rst.getString("description");
						int stars = rst.getInt("stars");
						String personalGrade = GradeHelper.intToString(setup, rst.getInt("grade"));
						a.setTick(userId, name, picture, description, stars, personalGrade);
					}
				}
			}
		}

		if (!guestbookActivitityIds.isEmpty()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') END picture, g.message" + 
					" FROM activity a, guestbook g, user u" + 
					" WHERE a.id IN (" + Joiner.on(",").join(guestbookActivitityIds) + ")" + 
					"   AND a.guestbook_id=g.id AND g.user_id=u.id")) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						int userId = rst.getInt("user_id");
						String name = rst.getString("name");
						String picture = rst.getString("picture");
						String message = rst.getString("message");
						a.setGuestbook(userId, name, picture, message);
					}
				}
			}
		}

		if (!faActivitityIds.isEmpty()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, u.id user_id, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') END picture, p.description, MAX(m.id) random_media_id" + 
					" FROM ((((activity a INNER JOIN problem p ON a.problem_id=p.id) LEFT JOIN fa ON p.id=fa.problem_id) LEFT JOIN user u ON fa.user_id=u.id) LEFT JOIN media_problem mp ON p.id=mp.problem_id) LEFT JOIN media m ON (mp.media_id=m.id AND m.deleted_user_id IS NULL AND m.is_movie=0)" + 
					" WHERE a.id IN (" + Joiner.on(",").join(faActivitityIds) + ")" + 
					" GROUP BY a.id, u.firstname, u.lastname, u.id, u.picture, p.description" +
					" ORDER BY u.firstname, u.lastname")) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						String name = rst.getString("name");
						int userId = rst.getInt("user_id");
						String picture = rst.getString("picture");
						String description = rst.getString("description");
						int problemRandomMediaId = rst.getInt("random_media_id");
						a.addFa(name, userId, picture, description, problemRandomMediaId);
					}
				}
			}
		}

		if (!mediaActivitityIds.isEmpty()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, m.id media_id, m.is_movie" + 
					" FROM activity a, media m, media_problem mp" + 
					" WHERE a.id IN (" + Joiner.on(",").join(mediaActivitityIds) + ")" + 
					"   AND a.media_id=m.id AND m.id=mp.media_id AND a.problem_id=mp.problem_id" +
					" ORDER BY mp.sorting, m.id")) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						int mediaId = rst.getInt("media_id");
						boolean isMovie = rst.getBoolean("is_movie");
						a.addMedia(mediaId, isMovie);
					}
				}
			}
		}
		logger.debug("getActivity(authUserId={}, setup={}) - res.size()={}, duration={}", authUserId, setup, res.size(), stopwatch);
		return res;
	}

	public Area getArea(int authUserId, int reqId) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE area SET hits=hits+1 WHERE id=?")) {
			ps.setInt(1, reqId);
			ps.execute();
		}
		MarkerHelper markerHelper = new MarkerHelper();
		Area a = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.id region_id, CONCAT(r.url,'/area/',a.id) canonical, a.hidden, a.for_developers, a.name, a.description, a.latitude, a.longitude, a.hits FROM (area a INNER JOIN region r ON a.region_id=r.id) LEFT JOIN permission auth ON a.region_id=auth.region_id WHERE a.id=? AND (a.hidden=0 OR (auth.user_id=? AND (a.hidden<=1 OR auth.write>=a.hidden))) GROUP BY r.id, r.url, a.hidden, a.for_developers, a.name, a.description, a.latitude, a.longitude, a.hits")) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int regionId = rst.getInt("region_id");
					String canonical = rst.getString("canonical");
					int visibility = rst.getInt("hidden");
					boolean forDevelopers = rst.getBoolean("for_developers");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					LatLng l = markerHelper.getLatLng(rst.getDouble("latitude"), rst.getDouble("longitude"));
					int hits = rst.getInt("hits");
					List<Media> media = getMediaArea(reqId);
					if (media.isEmpty()) {
						media = null;
					}
					a = new Area(regionId, canonical, reqId, visibility, forDevelopers, name, comment, l.getLat(), l.getLng(), -1, -1, media, null, hits);
				}
			}
		}

		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT s.id, s.sorting, s.hidden, s.name, s.description, s.parking_latitude, s.parking_longitude, s.polygon_coords, s.polyline, COUNT(DISTINCT p.id) num_problems, MAX(m.id) media_id FROM ((((area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem mp ON p.id=mp.problem_id) LEFT JOIN media m ON mp.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL) LEFT JOIN permission auth ON a.region_id=auth.region_id WHERE a.id=? AND (p.id IS NULL OR (p.hidden=0 OR (auth.user_id=? AND (p.hidden<=1 OR auth.write>=p.hidden)))) AND (s.hidden=0 OR (auth.user_id=? AND (s.hidden<=1 OR auth.write>=s.hidden))) GROUP BY s.id, s.sorting, s.hidden, s.name, s.description, s.parking_latitude, s.parking_longitude, s.polygon_coords, s.polyline ORDER BY s.name")) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId);
			ps.setInt(3, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int sorting = rst.getInt("sorting");
					int visibility = rst.getInt("hidden");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					LatLng l = markerHelper.getLatLng(rst.getDouble("parking_latitude"), rst.getDouble("parking_longitude"));
					String polygonCoords = rst.getString("polygon_coords");
					String polyline = rst.getString("polyline");
					int numProblems = rst.getInt("num_problems");
					int randomMediaId = rst.getInt("media_id");
					if (randomMediaId == 0) {
						List<Media> x = getMediaSector(id, 0);
						if (!x.isEmpty()) {
							randomMediaId = x.get(0).getId();
						}
					}
					a.addSector(id, sorting, visibility, name, comment, l.getLat(), l.getLng(), polygonCoords, polyline, numProblems, randomMediaId);
				}
			}
		}
		a.orderSectors();
		logger.debug("getArea(authUserId={}, reqId={}) - duration={}", authUserId, reqId, stopwatch);
		return a;
	}

	public Collection<Area> getAreaList(int authUserId, int reqIdRegion) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		MarkerHelper markerHelper = new MarkerHelper();
		List<Area> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.id region_id, CONCAT(r.url,'/area/',a.id) canonical, a.id, a.hidden, a.for_developers, a.name, a.description, a.latitude, a.longitude, COUNT(DISTINCT s.id) num_sectors, COUNT(DISTINCT p.id) num_problems, a.hits FROM ((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND ((a.region_id=? AND a.hidden=0) OR (auth.user_id IS NOT NULL AND (a.hidden<=1 OR auth.write>=a.hidden))) GROUP BY r.id, r.url, a.id, a.hidden, a.for_developers, a.name, a.description, a.latitude, a.longitude, a.hits ORDER BY a.name")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, reqIdRegion);
			ps.setInt(3, reqIdRegion);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("region_id");
					String canonical = rst.getString("canonical");
					int id = rst.getInt("id");
					int visibility = rst.getInt("hidden");
					boolean forDevelopers = rst.getBoolean("for_developers");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					if (comment != null) {
						int ix = comment.indexOf("<strong>Forhold:</strong>");
						if (ix != -1) {
							comment = comment.substring(ix+25);
							ix = comment.indexOf("<strong>");
							comment = comment.substring(0, ix);
						}
					}
					LatLng l = markerHelper.getLatLng(rst.getDouble("latitude"), rst.getDouble("longitude"));
					int numSectors = rst.getInt("num_sectors");
					int numProblems = rst.getInt("num_problems");
					int hits = rst.getInt("hits");
					res.add(new Area(idRegion, canonical, id, visibility, forDevelopers, name, comment, l.getLat(), l.getLng(), numSectors, numProblems, null, null, hits));
				}
			}
		}
		logger.debug("getAreaList(authUserId={}, reqIdRegion={}) - res.size()={} - duration={}", authUserId, reqIdRegion, res.size(), stopwatch);
		return res;
	}

	public int getAuthUserId(Auth0Profile profile) throws SQLException, NoSuchAlgorithmException, IOException {
		int authUserId = -1;
		String picture = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT e.user_id, u.picture FROM user_email e, user u WHERE e.user_id=u.id AND lower(e.email)=?")) {
			ps.setString(1, profile.getEmail());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					authUserId = rst.getInt("user_id");
					picture = rst.getString("picture");
				}
			}
		}
		if (authUserId == -1 && profile.getName() != null) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, picture FROM user WHERE TRIM(CONCAT(firstname, ' ', COALESCE(lastname,'')))=?")) {
				ps.setString(1, profile.getName());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						authUserId = rst.getInt("id");
						picture = rst.getString("picture");
						// Add email to user
						try (PreparedStatement ps2 = c.getConnection().prepareStatement("INSERT INTO user_email (user_id, email) VALUES (?, ?)")) {
							ps2.setInt(1, authUserId);
							ps2.setString(2, profile.getEmail());
							ps2.execute();
						}
					}
				}
			}
		}
		if (authUserId == -1) {
			final boolean autoCommit = true;
			authUserId = addUser(profile.getEmail(), profile.getFirstname(), profile.getLastname(), profile.getPicture(), autoCommit);
		}
		else if (profile.getPicture() != null && (picture == null || !picture.equals(profile.getPicture()))) {
			if (picture != null && picture.contains("fbsbx.com") && !profile.getPicture().contains("fbsbx.com")) {
				logger.debug("Dont change from facebook-image, new image is most likely avatar with text...");
			} else {
				downloadUserImage(authUserId, profile.getPicture());
				try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE user SET picture=? WHERE id=?")) {
					ps.setString(1, profile.getPicture());
					ps.setInt(2, authUserId);
					ps.executeUpdate();
				}
			}
		}
		logger.debug("getAuthUserId(profile={}) - authUserId={}", profile, authUserId);
		return authUserId;
	}

	public List<Filter> getFilter(int authUserId, Setup setup, FilterRequest fr) throws SQLException {
		List<Filter> res = new ArrayList<>();
		String sqlStr = "SELECT a.name area_name, a.hidden area_visibility, s.name sector_name, s.hidden sector_visibility, p.id problem_id, p.hidden problem_visibility, p.name problem_name, p.latitude, p.longitude, ROUND(ROUND(AVG(t.stars)*2)/2,1) stars, p.grade grade, MAX(m.id) media_id, MAX(CASE WHEN t.user_id=? THEN 1 ELSE 0 END) ticked"
				+ " FROM (((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN permission auth ON r.id=auth.region_id) LEFT JOIN media_problem mp ON p.id=mp.problem_id) LEFT JOIN media m ON mp.media_id=m.id AND m.deleted_user_id IS NULL) LEFT JOIN tick t ON p.id=t.problem_id"
				+ " WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)"
				+ "   AND (r.id=? OR auth.user_id IS NOT NULL)"
				+ "   AND ((a.region_id=? AND a.hidden=0) OR (auth.user_id=? AND (a.hidden<=1 OR auth.write>=a.hidden)))"
				+ "   AND (s.hidden=0 OR (auth.user_id=? AND (s.hidden<=1 OR auth.write>=s.hidden)))"
				+ "   AND (p.hidden=0 OR (auth.user_id=? AND (p.hidden<=1 OR auth.write>=p.hidden)))"
				+ "   AND p.grade IN (" + Joiner.on(",").join(fr.getGrades()) + ")"
				+ "   AND p.type_id IN (" + Joiner.on(",").join(fr.getTypes()) + ")"
				+ "   GROUP BY a.id, a.name, a.hidden, s.id, s.name, s.hidden, p.id, p.hidden, p.name"
				+ "   ORDER BY p.name, p.latitude, p.longitude, p.grade";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			ps.setInt(4, setup.getIdRegion());
			ps.setInt(5, authUserId);
			ps.setInt(6, authUserId);
			ps.setInt(7, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String areaName = rst.getString("area_name");
					int areaVisibility = rst.getInt("area_visibility");
					String sectorName = rst.getString("sector_name");
					int sectorVisibility = rst.getInt("sector_visibility");
					int problemId = rst.getInt("problem_id");
					String problemName = rst.getString("problem_name");
					int problemVisibility = rst.getInt("problem_visibility");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					double stars = rst.getDouble("stars");
					int grade = rst.getInt("grade");
					int mediaId = rst.getInt("media_id");
					boolean ticked = rst.getBoolean("ticked");
					res.add(new Filter(areaVisibility, areaName, sectorVisibility, sectorName, problemId, problemVisibility, problemName, latitude, longitude, stars, GradeHelper.intToString(setup, grade), ticked, mediaId));
				}
			}
		}
		logger.debug("getFilter(authUserId={}, idRegion={}, fr={}) - res.size()={}", authUserId, setup.getIdRegion(), fr, res.size());
		return res;
	}

	public Frontpage getFrontpage(int authUserId, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Frontpage res = new Frontpage();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT p.id) num_problems, COUNT(DISTINCT CASE WHEN p.latitude IS NOT NULL AND p.longitude IS NOT NULL THEN p.id END) num_problems_with_coordinates, COUNT(DISTINCT svg.problem_id) num_problems_with_topo FROM (((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?)) LEFT JOIN svg ON p.id=svg.problem_id WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR auth.user_id IS NOT NULL)")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.setNumProblems(rst.getInt("num_problems"));
					res.setNumProblemsWithCoordinates(rst.getInt("num_problems_with_coordinates"));
					res.setNumProblemsWithTopo(rst.getInt("num_problems_with_topo"));
				}
			}
		}
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT CASE WHEN m.is_movie=0 THEN mp.id END) num_images, COUNT(DISTINCT CASE WHEN m.is_movie=1 THEN mp.id END) num_movies FROM ((((((media m INNER JOIN media_problem mp ON m.id=mp.media_id) INNER JOIN problem p ON mp.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND m.deleted_user_id IS NULL AND (a.region_id=? OR auth.user_id IS NOT NULL)")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.setNumImages(rst.getInt("num_images"));
					res.setNumMovies(rst.getInt("num_movies"));
				}
			}
		}
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT t.id) num_ticks FROM (((((tick t INNER JOIN problem p ON t.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR auth.user_id IS NOT NULL)")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.setNumTicks(rst.getInt("num_ticks"));
				}
			}
		}

		/**
		 * RandomMedia
		 */
		setRandomMedia(res, authUserId, setup, !setup.isBouldering()); // Show all images on climbing sites, not only routes with >2 stars
		if (res.getRandomMedia() == null) {
			setRandomMedia(res, authUserId, setup, true);
		}
		logger.debug("getFrontpage(authUserId={}, setup={}) - duration={}", authUserId, setup, stopwatch);
		return res;
	}

	public Collection<GradeDistribution> getGradeDistribution(int authUserId, Setup setup, int optionalAreaId, int optionalSectorId) throws SQLException {
		Map<String, GradeDistribution> res = GradeHelper.getGradeDistributionBase(setup);
		String sqlStr = "SELECT ROUND((IFNULL(AVG(NULLIF(t.grade,0)), p.grade) + p.grade)/2) grade, COUNT(DISTINCT p.id) num"
				+ " FROM ((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN permission auth ON a.region_id=auth.region_id) LEFT JOIN tick t ON p.id=t.problem_id"
				+ " WHERE p.grade!=0"
				+ (optionalAreaId!=0? " AND a.id=?" : " AND p.sector_id=?")
				+ "   AND (p.hidden=0 OR (auth.user_id=? AND (p.hidden<=1 OR auth.write>=p.hidden)))"
				+ " GROUP BY p.grade"
				+ " ORDER BY p.grade";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, optionalAreaId!=0? optionalAreaId : optionalSectorId);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int gradeNumber = rst.getInt("grade");
					final String grade = GradeHelper.intToStringBase(setup, gradeNumber);
					int num = rst.getInt("num");
					res.get(grade).incrementNum(num);
				}
			}
		}
		return res.values();
	}

	public Path getImage(boolean webP, int id) throws SQLException, IOException {
		Path p = null;
		if (webP) {
			p = Paths.get(PATH + "web/webp").resolve(String.valueOf(id / 100 * 100)).resolve(id + ".webp");
		} else {
			p = Paths.get(PATH + "web/jpg").resolve(String.valueOf(id / 100 * 100)).resolve(id + ".jpg");
		}
		Preconditions.checkArgument(Files.exists(p), p.toString() + " does not exist");
		return p;
	}

	public Point getMediaDimention(int id) throws SQLException {
		Point res = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT width, height FROM media WHERE id=?")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res = new Point(rst.getInt("width"), rst.getInt("height"));
				}
			}
		}
		return res;
	}

	public Permissions getPermissions(int authUserId, int idRegion) throws SQLException {
		// Ensure superadmin
		Preconditions.checkArgument(authUserId != -1, "Insufficient credentials");
		Preconditions.checkArgument(idRegion > 0, "Insufficient credentials");
		boolean writePermissions = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT auth.write FROM permission auth WHERE auth.region_id=? AND auth.user_id=?")) {
			ps.setInt(1, idRegion);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int write = rst.getInt("write");
					writePermissions = write == 2;
				}
			}
		}
		Preconditions.checkArgument(writePermissions, "Insufficient credentials");
		// Return users
		Permissions res = new Permissions();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT x.id, x.name, x.picture, DATE_FORMAT(MAX(x.last_login),'%Y.%m.%d') last_login, x.w FROM (SELECT u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, MAX(l.when) last_login, IFNULL(p.write,-1) w FROM (user u INNER JOIN user_login l ON u.id=l.user_id) LEFT JOIN permission p ON u.id=p.user_id AND l.region_id=p.region_id WHERE l.region_id=? GROUP BY u.id, u.firstname, u.lastname, u.picture UNION SELECT u.id, TRIM(CONCAT(u.firstname, ' ', u.lastname)) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, MAX(l.when) last_login, IFNULL(p.write,-1) w FROM user u, permission p, user_login l WHERE u.id=p.user_id AND p.region_id=? AND u.id=l.user_id GROUP BY u.id, u.firstname, u.lastname, u.picture) x GROUP BY x.id, x.name, x.picture, x.w ORDER BY x.w DESC, x.name")) {
			ps.setInt(1, idRegion);
			ps.setInt(2, idRegion);
			try (ResultSet rst = ps.executeQuery()) {
				final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
				while (rst.next()) {
					int userId = rst.getInt("id");
					String name = rst.getString("name");
					String picture = rst.getString("picture");
					String lastLogin = rst.getString("last_login");
					int write = rst.getInt("w");
					String timeAgo = TimeAgo.getTimeAgo(LocalDate.parse(lastLogin, formatter));
					res.getUsers().add(new PermissionUser(userId, name, picture, timeAgo, write, authUserId==userId));
				}
			}
		}
		return res;
	}

	public Problem getProblem(int authUserId, Setup s, int reqId) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE problem SET hits=hits+1 WHERE id=?")) {
			ps.setInt(1, reqId);
			ps.execute();
		}
		TodoUser todoUser = getTodo(authUserId, s, authUserId);
		List<Integer> todoIdPlants = todoUser == null? Lists.newArrayList() : todoUser.getTodo().stream().map(x -> x.getProblemId()).collect(Collectors.toList());
		MarkerHelper markerHelper = new MarkerHelper();
		Problem p = null;
		String sqlStr = "SELECT a.id area_id, a.hidden area_hidden, a.name area_name, s.id sector_id, s.hidden sector_hidden, s.name sector_name, s.parking_latitude sector_lat, s.parking_longitude sector_lng, s.polygon_coords sector_polygon_coords, s.polyline sector_polyline, CONCAT(r.url,'/problem/',p.id) canonical, p.id, p.hidden hidden, p.nr, p.name, p.description, p.hits, DATE_FORMAT(p.fa_date,'%Y-%m-%d') fa_date, DATE_FORMAT(p.fa_date,'%d/%m-%y') fa_date_hr,"
				+ " ROUND((IFNULL(AVG(NULLIF(t.grade,0)), p.grade) + p.grade)/2) grade, p.grade original_grade, p.latitude, p.longitude,"
				+ " group_concat(DISTINCT CONCAT('{\"id\":', u.id, ',\"name\":\"', TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))), '\",\"picture\":\"', CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END, '\"}') ORDER BY u.firstname, u.lastname SEPARATOR ',') fa,"
				+ " COUNT(DISTINCT t.id) num_ticks, ROUND(ROUND(AVG(t.stars)*2)/2,1) stars,"
				+ " MAX(CASE WHEN (t.user_id=? OR u.id=?) THEN 1 END) ticked, ty.id type_id, ty.type, ty.subtype"
				+ " FROM ((((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN tick t ON t.problem_id=p.id) LEFT JOIN permission auth ON r.id=auth.region_id"
				+ " WHERE (?=0 OR rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?))"
				+ "   AND p.id=?"
				+ "   AND (p.hidden=0 OR (auth.user_id=? AND (p.hidden<=1 OR auth.write>=p.hidden)))"
				+ "   AND (?=0 OR r.id=? OR auth.user_id IS NOT NULL)"
				+ " GROUP BY r.url, a.id, a.hidden, a.name, s.id, s.hidden, s.name, s.parking_latitude, s.parking_longitude, s.polygon_coords, s.polyline, p.id, p.hidden, p.nr, p.name, p.description, p.hits, p.grade, p.latitude, p.longitude, p.fa_date, ty.id, ty.type, ty.subtype"
				+ " ORDER BY p.name";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, authUserId);
			ps.setInt(3, s.getIdRegion());
			ps.setInt(4, s.getIdRegion());
			ps.setInt(5, reqId);
			ps.setInt(6, authUserId);
			ps.setInt(7, s.getIdRegion());
			ps.setInt(8, s.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					int areaVisibility = rst.getInt("area_hidden");
					String areaName = rst.getString("area_name");
					int sectorId = rst.getInt("sector_id");
					int sectorVisibility = rst.getInt("sector_hidden");
					String sectorName = rst.getString("sector_name");
					LatLng sectorL = markerHelper.getLatLng(rst.getDouble("sector_lat"), rst.getDouble("sector_lng"));
					String sectorPolygonCoords = rst.getString("sector_polygon_coords");
					String sectorPolyline = rst.getString("sector_polyline");
					String canonical = rst.getString("canonical");
					int id = rst.getInt("id");
					int visibility = rst.getInt("hidden");
					int nr = rst.getInt("nr");
					int grade = rst.getInt("grade");
					int originalGrade = rst.getInt("original_grade");
					String faDate = rst.getString("fa_date");
					String faDateHr = rst.getString("fa_date_hr");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					String faStr = rst.getString("fa");
					List<FaUser> fa = Strings.isNullOrEmpty(faStr) ? null : gson.fromJson("[" + faStr + "]", new TypeToken<ArrayList<FaUser>>(){}.getType());
					LatLng l = markerHelper.getLatLng(rst.getDouble("latitude"), rst.getDouble("longitude"));
					int numTicks = rst.getInt("num_ticks");
					double stars = rst.getDouble("stars");
					boolean ticked = rst.getBoolean("ticked");
					List<Media> media = getMediaProblem(s, sectorId, id);
					Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
					int hits = rst.getInt("hits");

					int sectorIdProblemPrev = 0;
					int sectorIdProblemNext = 0;
					List<Sector.Problem> problems = getSectorDontUpdateHits(authUserId, false, s, sectorId).getProblems();
					if (problems.size() > 1) {
						for (int i = 0; i < problems.size(); i++) {
							Sector.Problem prob = problems.get(i);
							if (prob.getId() == id) {
								sectorIdProblemPrev = problems.get((i == 0? problems.size()-1 : i-1)).getId();
								sectorIdProblemNext = problems.get((i == problems.size()-1? 0 : i+1)).getId();
							}
						}
					}

					p = new Problem(areaId, areaVisibility, areaName, sectorId, sectorVisibility, sectorName,
							sectorL.getLat(), sectorL.getLng(), sectorPolygonCoords, sectorPolyline,
							sectorIdProblemPrev, sectorIdProblemNext,
							canonical, id, visibility, nr, name, comment,
							GradeHelper.intToString(s, grade),
							GradeHelper.intToString(s, originalGrade), faDate, faDateHr, fa, l.getLat(),
							l.getLng(), media, numTicks, stars, ticked, null, t, todoIdPlants.contains(id), hits);
				}
			}
		}
		Preconditions.checkNotNull(p);
		// Ascents
		sqlStr = "SELECT t.id id_tick, u.id id_user, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, CAST(t.date AS char) date, CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) name, t.comment, t.stars, t.grade FROM tick t, user u WHERE t.problem_id=? AND t.user_id=u.id ORDER BY t.date, t.id";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id_tick");
					int idUser = rst.getInt("id_user");
					String picture = rst.getString("picture");
					String date = rst.getString("date");
					String name = rst.getString("name");
					String comment = rst.getString("comment");
					double stars = rst.getDouble("stars");
					int grade = rst.getInt("grade");
					boolean writable = idUser == authUserId;
					p.addTick(id, idUser, picture, date, name, GradeHelper.intToString(s, grade), comment, stars, writable);
				}
			}
		}
		// Comments
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT g.id, CAST(g.post_time AS char) date, u.id user_id, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) name, g.message, g.danger, g.resolved FROM guestbook g, user u WHERE g.problem_id=? AND g.user_id=u.id ORDER BY g.post_time")) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String date = rst.getString("date");
					int idUser = rst.getInt("user_id");
					String picture = rst.getString("picture");
					String name = rst.getString("name");
					String message = rst.getString("message");
					boolean danger = rst.getBoolean("danger");
					boolean resolved = rst.getBoolean("resolved");
					p.addComment(id, date, idUser, picture, name, message, danger, resolved);
				}
			}
		}
		// Sections
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, nr, description, grade FROM problem_section WHERE problem_id=? ORDER BY nr")) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int nr = rst.getInt("nr");
					String description = rst.getString("description");
					int grade = rst.getInt("grade");
					List<Media> sectionMedia = new ArrayList<>();
					if (p.getMedia() != null) {
						sectionMedia = p.getMedia().stream().filter(x -> x.getPitch() == nr).collect(Collectors.toList());
						p.getMedia().removeAll(sectionMedia);
					}
					p.addSection(id, nr, description, GradeHelper.intToString(s, grade), sectionMedia);
				}
			}
		}
		// First aid ascent
		if (!s.isBouldering()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT DATE_FORMAT(a.aid_date,'%Y-%m-%d') aid_date, DATE_FORMAT(a.aid_date,'%d/%m-%y') aid_date_hr, a.aid_description, u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture FROM (fa_aid a LEFT JOIN fa_aid_user au ON a.problem_id=au.problem_id) LEFT JOIN user u ON au.user_id=u.id WHERE a.problem_id=?")) {
				ps.setInt(1, p.getId());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						String aidDate = rst.getString("aid_date");
						String aidDateHr = rst.getString("aid_date_hr");
						String aidDescription = rst.getString("aid_description");
						FaAid faAid = p.getFaAid();
						if (faAid == null) {
							faAid = new FaAid(p.getId(), aidDate, aidDateHr, aidDescription);
							p.setFaAid(faAid);
						}
						int userId = rst.getInt("id");
						if (userId != 0) {
							String userName = rst.getString("name");
							String picture = rst.getString("picture");
							FaUser user = new FaUser(userId, userName, picture);
							faAid.getUsers().add(user);
						}
					}
				}
			}
		}
		logger.debug("getProblem(authUserId={}, reqRegionId={}, reqId={}) - duration={} - p={}", authUserId, s.getIdRegion(), reqId, stopwatch, p);
		return p;
	}

	public List<ProblemHse> getProblemsHse(int authUserId, Setup setup) throws SQLException {
		List<ProblemHse> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id area_id, a.name area_name, a.hidden area_hidden, s.id sector_id, s.name sector_name, s.hidden sector_hidden, p.id problem_id, p.name problem_name, p.hidden problem_hidden, g.message FROM (((((area a INNER JOIN region r ON r.id=a.region_id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN guestbook g ON p.id=g.problem_id AND g.danger=1 AND g.id IN (SELECT MAX(id) id FROM guestbook WHERE danger=1 OR resolved=1 GROUP BY problem_id)) LEFT JOIN permission auth ON a.region_id=auth.region_id WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (p.hidden=0 OR (auth.user_id=? AND (p.hidden<=1 OR auth.write>=p.hidden))) GROUP BY a.id, a.name, a.hidden, s.id, s.name, s.hidden, p.id, p.name, p.hidden, g.message ORDER BY a.name, s.name, p.name")) {
			ps.setInt(1, setup.getIdRegion());
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					int areaVisibility = rst.getInt("area_hidden");
					String areaName = rst.getString("area_name");
					int sectorId = rst.getInt("sector_id");
					int sectorVisibility = rst.getInt("sector_hidden");
					String sectorName = rst.getString("sector_name");
					int problemId = rst.getInt("problem_id");
					int problemVisibility = rst.getInt("problem_hidden");
					String problemName = rst.getString("problem_name");
					String message = rst.getString("message");
					res.add(new ProblemHse(areaId, areaVisibility, areaName, sectorId, sectorVisibility, sectorName, problemId,
							problemVisibility, problemName, message));
				}
			}
		}
		return res;
	}

	public Collection<Region> getRegions(String uniqueId, boolean climbingNotBouldering) throws SQLException {
		final String regionTypeFilter = climbingNotBouldering? "rt.type_id!=1" : "rt.type_id=1";
		final int idUser = upsertUserReturnId(uniqueId);
		MarkerHelper markerHelper = new MarkerHelper();
		Map<Integer, Region> regionMap = new HashMap<>();
		Map<Integer, com.buldreinfo.jersey.jaxb.model.app.Area> areaMap = new HashMap<>();
		Map<Integer, com.buldreinfo.jersey.jaxb.model.app.Sector> sectorMap = new HashMap<>();
		Map<Integer, com.buldreinfo.jersey.jaxb.model.app.Problem> problemMap = new HashMap<>();
		// Regions
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.id, r.name FROM region r INNER JOIN region_type rt ON r.id=rt.region_id WHERE " + regionTypeFilter)) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String name = rst.getString("name");
					Region r = new Region(id, name);
					regionMap.put(r.getId(), r);
				}
			}
		}
		// Areas
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.region_id, a.id, a.name, a.description, a.latitude, a.longitude FROM ((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON a.region_id=auth.region_id WHERE " + regionTypeFilter + " AND (a.hidden=0 OR (auth.user_id=? AND (a.hidden<=1 OR auth.write>=a.hidden))) GROUP BY a.region_id, a.id, a.name, a.description, a.latitude, a.longitude")) {
			ps.setInt(1, idUser);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int regionId = rst.getInt("region_id");
					Region r = regionMap.get(regionId);
					if (r != null) {
						int id = rst.getInt("id");
						String name = rst.getString("name");
						String comment = rst.getString("description");
						LatLng l = markerHelper.getLatLng(rst.getDouble("latitude"), rst.getDouble("longitude"));
						com.buldreinfo.jersey.jaxb.model.app.Area a = new com.buldreinfo.jersey.jaxb.model.app.Area(regionId,
								id, name, comment, l.getLat(), l.getLng());
						r.getAreas().add(a);
						areaMap.put(a.getId(), a);
					}
				}
			}
		}
		// Sectors
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT s.area_id, s.id, s.name, s.description, s.parking_latitude, s.parking_longitude FROM (((sector s INNER JOIN area a ON a.id=s.area_id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON r.id=auth.region_id WHERE " + regionTypeFilter + " AND (s.hidden=0 OR (auth.user_id=? AND (s.hidden<=1 OR auth.write>=s.hidden))) GROUP BY s.area_id, s.id, s.name, s.description, s.parking_latitude, s.parking_longitude")) {
			ps.setInt(1, idUser);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					com.buldreinfo.jersey.jaxb.model.app.Area a = areaMap.get(areaId);
					if (a != null) {
						int id = rst.getInt("id");
						String name = rst.getString("name");
						String comment = rst.getString("description");
						LatLng l = markerHelper.getLatLng(rst.getDouble("parking_latitude"),
								rst.getDouble("parking_longitude"));
						com.buldreinfo.jersey.jaxb.model.app.Sector s = new com.buldreinfo.jersey.jaxb.model.app.Sector(areaId,
								id, name, comment, l.getLat(), l.getLng());
						a.getSectors().add(s);
						sectorMap.put(s.getId(), s);
					}
				}
			}
		}
		// Problems
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT p.sector_id, p.id, p.nr, p.name, p.description, p.grade, TRIM(CONCAT(IFNULL(p.fa_date,''), ' ', GROUP_CONCAT(DISTINCT CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) ORDER BY u.firstname SEPARATOR ', '))) fa, p.latitude, p.longitude FROM ((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN permission auth ON r.id=auth.region_id WHERE " + regionTypeFilter + " AND (p.hidden=0 OR (auth.user_id=? AND (p.hidden<=1 OR auth.write>=p.hidden))) GROUP BY p.sector_id, p.id, p.nr, p.name, p.description, p.grade, p.fa_date, p.latitude, p.longitude")) {
			ps.setInt(1, idUser);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int sectorId = rst.getInt("sector_id");
					com.buldreinfo.jersey.jaxb.model.app.Sector s = sectorMap.get(sectorId);
					if (s != null) {
						int id = rst.getInt("id");
						int nr = rst.getInt("nr");
						String name = rst.getString("name");
						String comment = rst.getString("description");
						int grade = rst.getInt("grade");
						String fa = rst.getString("fa");
						LatLng l = markerHelper.getLatLng(rst.getDouble("latitude"), rst.getDouble("longitude"));
						com.buldreinfo.jersey.jaxb.model.app.Problem p = new com.buldreinfo.jersey.jaxb.model.app.Problem(
								sectorId, id, nr, name, comment, grade, fa, l.getLat(), l.getLng());
						s.getProblems().add(p);
						problemMap.put(p.getId(), p);
					}
				}
			}
		}
		// Media (sectors)
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ms.sector_id, m.id, m.is_movie FROM (((((media m INNER JOIN media_sector ms ON m.id=ms.media_id AND m.deleted_user_id IS NULL) INNER JOIN sector s ON ms.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON a.region_id=auth.region_id WHERE " + regionTypeFilter + " AND (a.hidden=0 OR (auth.user_id=? AND (a.hidden<=1 OR auth.write>=a.hidden))) GROUP BY ms.sector_id, m.id, m.is_movie ORDER BY m.is_movie, m.id")) {
			ps.setInt(1, idUser);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int sectorId = rst.getInt("sector_id");
					com.buldreinfo.jersey.jaxb.model.app.Sector s = sectorMap.get(sectorId);
					if (s != null) {
						int id = rst.getInt("id");
						boolean isMovie = rst.getBoolean("is_movie");
						s.getMedia().add(new com.buldreinfo.jersey.jaxb.model.app.Media(id, isMovie, 0));
					}
				}
			}
		}
		// Media (problems)
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT mp.problem_id, m.id, m.is_movie, mp.milliseconds t FROM ((((((media m INNER JOIN media_problem mp ON m.id=mp.media_id AND m.deleted_user_id IS NULL) INNER JOIN problem p ON mp.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON r.id=auth.region_id WHERE " + regionTypeFilter + " AND (a.hidden=0 OR (auth.user_id=? AND (a.hidden<=1 OR auth.write>=a.hidden))) GROUP BY mp.problem_id, m.id, m.is_movie, mp.milliseconds ORDER BY m.is_movie, m.id")) {
			ps.setInt(1, idUser);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int problemId = rst.getInt("problem_id");
					com.buldreinfo.jersey.jaxb.model.app.Problem p = problemMap.get(problemId);
					if (p != null) {
						int id = rst.getInt("id");
						boolean isMovie = rst.getBoolean("is_movie");
						int t = rst.getInt("t");
						p.getMedia().add(new com.buldreinfo.jersey.jaxb.model.app.Media(id, isMovie, t));
					}
				}
			}
		}
		// Return
		return regionMap.values();
	}

	public List<Search> getSearch(int authUserId, Setup setup, SearchRequest sr) throws SQLException {
		List<Search> res = new ArrayList<>();
		// Areas
		List<Search> areas = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, a.name, a.hidden, MAX(m.id) media_id FROM ((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON r.id=auth.region_id) LEFT JOIN media_area ma ON a.id=ma.area_id) LEFT JOIN media m ON ma.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR auth.user_id IS NOT NULL) AND (a.name LIKE ? OR a.name LIKE ?) AND (a.hidden=0 OR (auth.user_id=? AND (a.hidden<=1 OR auth.write>=a.hidden))) GROUP BY a.id, a.name, a.hidden ORDER BY a.name LIMIT 8")) {
			ps.setInt(1, setup.getIdRegion());
			ps.setInt(2, setup.getIdRegion());
			ps.setString(3, sr.getValue() + "%");
			ps.setString(4, "% " + sr.getValue() + "%");
			ps.setInt(5, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String name = rst.getString("name");
					int visibility = rst.getInt("hidden");
					int mediaId = rst.getInt("media_id");
					areas.add(new Search(name, null, "/area/" + id, null, mediaId, visibility));
				}
			}
		}
		// Sectors
		List<Search> sectors = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT s.id, a.name area_name, s.name sector_name, s.hidden, MAX(m.id) media_id FROM (((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN permission auth ON r.id=auth.region_id) LEFT JOIN media_sector ms ON s.id=ms.sector_id) LEFT JOIN media m ON ms.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR auth.user_id IS NOT NULL) AND (s.name LIKE ? OR s.name LIKE ?) AND (s.hidden=0 OR (auth.user_id=? AND (s.hidden<=1 OR auth.write>=s.hidden))) GROUP BY s.id, a.name, s.name, s.hidden ORDER BY a.name, s.name LIMIT 8")) {
			ps.setInt(1, setup.getIdRegion());
			ps.setInt(2, setup.getIdRegion());
			ps.setString(3, sr.getValue() + "%");
			ps.setString(4, "% " + sr.getValue() + "%");
			ps.setInt(5, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String areaName = rst.getString("area_name");
					String sectorName = rst.getString("sector_name");
					int visibility = rst.getInt("hidden");
					int mediaId = rst.getInt("media_id");
					sectors.add(new Search(sectorName, areaName, "/sector/" + id, null, mediaId, visibility));
				}
			}
		}
		// Problems
		List<Search> problems = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.name area_name, s.name sector_name, p.id, p.name, p.grade, p.hidden, MAX(m.id) media_id FROM ((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN permission auth ON r.id=auth.region_id) LEFT JOIN media_problem mp ON p.id=mp.problem_id) LEFT JOIN media m ON mp.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR auth.user_id IS NOT NULL) AND (p.name LIKE ? OR p.name LIKE ?) AND (p.hidden=0 OR (auth.user_id=? AND (p.hidden<=1 OR auth.write>=p.hidden))) GROUP BY a.name, s.name, p.id, p.name, p.grade, p.hidden ORDER BY p.name, p.grade LIMIT 8")) {
			ps.setInt(1, setup.getIdRegion());
			ps.setInt(2, setup.getIdRegion());
			ps.setString(3, sr.getValue() + "%");
			ps.setString(4, "% " + sr.getValue() + "%");
			ps.setInt(5, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String areaName = rst.getString("area_name");
					String sectorName = rst.getString("sector_name");
					int id = rst.getInt("id");
					String name = rst.getString("name");
					int grade = rst.getInt("grade");
					int visibility = rst.getInt("hidden");
					int mediaId = rst.getInt("media_id");
					problems.add(new Search(name + " [" + GradeHelper.intToString(setup, grade) + "]",
							areaName + " / " + sectorName, "/problem/" + id, null, mediaId, visibility));
				}
			}
		}
		// Users
		List<Search> users = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT CASE WHEN picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', id, '.jpg') END picture, id, TRIM(CONCAT(firstname, ' ', COALESCE(lastname,''))) name FROM user WHERE (firstname LIKE ? OR lastname LIKE ? OR CONCAT(firstname, ' ', COALESCE(lastname,'')) LIKE ?) ORDER BY TRIM(CONCAT(firstname, ' ', COALESCE(lastname,''))) LIMIT 8")) {
			ps.setString(1, sr.getValue() + "%");
			ps.setString(2, sr.getValue() + "%");
			ps.setString(3, sr.getValue() + "%");
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String picture = rst.getString("picture");
					int id = rst.getInt("id");
					String name = rst.getString("name");
					users.add(new Search(name, null, "/user/" + id, picture, 0, 0));
				}
			}
		}
		// Truncate result to max 8
		while (areas.size() + sectors.size() + problems.size() + users.size() > 8) {
			if (problems.size() > 5) {
				problems.remove(problems.size() - 1);
			} else if (areas.size() > 1) {
				areas.remove(areas.size() - 1);
			} else if (sectors.size() > 1) {
				sectors.remove(sectors.size() - 1);
			} else if (users.size() > 1) {
				users.remove(users.size() - 1);
			}
		}
		if (!areas.isEmpty()) {
			res.addAll(areas);
		}
		if (!sectors.isEmpty()) {
			res.addAll(sectors);
		}
		if (!problems.isEmpty()) {
			res.addAll(problems);
		}
		if (!users.isEmpty()) {
			res.addAll(users);
		}
		return res;
	}
	
	public Sector getSector(int authUserId, boolean orderByGrade, Setup setup, int reqId) throws IOException, SQLException {
		final boolean updateHits = true;
		return getSector(authUserId, orderByGrade, setup, reqId, updateHits);
	}
	
	public Sector getSectorDontUpdateHits(int authUserId, boolean orderByGrade, Setup setup, int reqId) throws IOException, SQLException {
		final boolean updateHits = false;
		return getSector(authUserId, orderByGrade, setup, reqId, updateHits);
	}

	public String getSitemapTxt(Setup setup) throws SQLException {
		List<String> urls = new ArrayList<>();
		// Fixed urls
		urls.add(setup.getUrl(null));
		urls.add(setup.getUrl("/ethics"));
		urls.add(setup.getUrl("/gpl-3.0.txt"));
		urls.add(setup.getUrl("/browse"));
		urls.add(setup.getUrl("/filter"));
		urls.add(setup.getUrl("/sites/bouldering"));
		urls.add(setup.getUrl("/sites/climbing"));
		// Users
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT f.user_id FROM area a, sector s, problem p, fa f WHERE a.region_id=? AND a.hidden=0 AND a.id=s.area_id AND s.hidden=0 AND s.id=p.sector_id AND p.hidden=0 AND p.id=f.problem_id GROUP BY f.user_id UNION SELECT t.user_id FROM area a, sector s, problem p, tick t WHERE a.region_id=? AND a.hidden=0 AND a.id=s.area_id AND s.hidden=0 AND s.id=p.sector_id AND p.hidden=0 AND p.id=t.problem_id GROUP BY t.user_id")) {
			ps.setInt(1, setup.getIdRegion());
			ps.setInt(2, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int userId = rst.getInt("user_id");
					urls.add(setup.getUrl("/user/" + userId));
				}
			}
		}
		// Areas, sectors, problems
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT CONCAT('/area/', a.id) url FROM region r, area a WHERE r.id=? AND r.id=a.region_id AND a.hidden=0 UNION SELECT CONCAT('/sector/', s.id) url FROM region r, area a, sector s WHERE r.id=? AND r.id=a.region_id AND a.hidden=0 AND a.id=s.area_id AND s.hidden=0 UNION SELECT CONCAT('/problem/', p.id) url FROM region r, area a, sector s, problem p WHERE r.id=? AND r.id=a.region_id AND a.hidden=0 AND a.id=s.area_id AND s.hidden=0 AND s.id=p.sector_id AND p.hidden=0")) {
			ps.setInt(1, setup.getIdRegion());
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					urls.add(setup.getUrl(rst.getString("url")));
				}
			}
		}
		return Joiner.on("\r\n").join(urls);
	}

	public List<SitesRegion> getSites(boolean isBouldering) throws SQLException {
		List<SitesRegion> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.name, r.url, r.polygon_coords, COUNT(p.id) num_problems FROM (((region_type rt INNER JOIN region r ON rt.type_id=? AND rt.region_id=r.id) LEFT JOIN area a ON r.id=a.region_id) LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id GROUP BY r.name, r.url, r.polygon_coords")) {
			ps.setInt(1, isBouldering? 1 : 2);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String name = rst.getString("name");
					String url = rst.getString("url");
					String polygonCoords = rst.getString("polygon_coords");
					int numProblems = rst.getInt("num_problems");
					res.add(new SitesRegion(name, url, polygonCoords, numProblems));
				}
			}
		}
		return res;
	}

	public Ticks getTicks(int authUserId, Setup setup, int page) throws SQLException {
		final int take = 200;
		int numTicks = 0;
		int skip = (page-1)*take;
		String sqlStr = "SELECT a.name area_name, a.hidden area_visibility, s.name sector_name, s.hidden sector_visibility, p.id problem_id, t.grade problem_grade, p.name problem_name, p.hidden problem_visibility, DATE_FORMAT(t.date,'%Y.%m.%d') ts, TRIM(CONCAT(u.firstname, ' ', IFNULL(u.lastname,''))) name"
				+ " FROM ((((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN tick t ON p.id=t.problem_id) INNER JOIN user u ON t.user_id=u.id) LEFT JOIN permission auth ON r.id=auth.region_id"
				+ "  WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)"
				+ "    AND (r.id=? OR auth.user_id IS NOT NULL)"
				+ "    AND ((a.region_id=? AND a.hidden=0) OR (auth.user_id=? AND (a.hidden<=1 OR auth.write>=a.hidden)))"
				+ "    AND (s.hidden=0 OR (auth.user_id=? AND (s.hidden<=1 OR auth.write>=s.hidden)))"
				+ "    AND (p.hidden=0 OR (auth.user_id=? AND (p.hidden<=1 OR auth.write>=p.hidden)))"
				+ " GROUP BY a.name, a.hidden, s.name, s.hidden, p.id, t.grade, p.name, p.hidden, t.date, u.firstname, u.lastname"
				+ " ORDER BY t.date DESC, problem_name, name";
		List<PublicAscent> ticks = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, setup.getIdRegion());
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			ps.setInt(4, authUserId);
			ps.setInt(5, authUserId);
			ps.setInt(6, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					numTicks++;
					if ((numTicks-1) < skip || ticks.size() == take) {
						continue;
					}
					String areaName = rst.getString("area_name");
					int areaVisibility = rst.getInt("area_visibility");
					String sectorName = rst.getString("sector_name");
					int sectorVisibility = rst.getInt("sector_visibility");
					int problemId = rst.getInt("problem_id");
					int problemGrade = rst.getInt("problem_grade");
					String problemName = rst.getString("problem_name");
					int problemVisibility = rst.getInt("problem_visibility");
					String date = rst.getString("ts");
					String name = rst.getString("name");
					ticks.add(new PublicAscent(areaName, areaVisibility, sectorName, sectorVisibility, problemId, GradeHelper.intToString(setup, problemGrade), problemName, problemVisibility, date, name));
				}
			}
		}
		int numPages = (int)(Math.ceil(numTicks / 200f));
		Ticks res = new Ticks(ticks, page, numPages);
		logger.debug("getTicks(authUserId={}, idRegion={}, page={}) - res={}", authUserId, setup.getIdRegion(), page, res);
		return res;
	}

	public TodoUser getTodo(int authUserId, Setup setup, int reqId) throws SQLException {
		MarkerHelper markerHelper = new MarkerHelper();
		final int userId = reqId > 0? reqId : authUserId;
		List<Todo> todo = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT t.id, t.priority, a.name area_name, s.name sector_name, p.id problem_id, p.name problem_name, p.grade problem_grade, p.hidden problem_visibility, p.latitude problem_latitude, p.longitude problem_longitude, s.polygon_coords, s.parking_latitude sector_latitude, s.parking_longitude sector_longitude, a.latitude area_latitude, a.longitude area_longitude, MAX(CASE WHEN m.is_movie=0 THEN m.id END) problem_random_media_id FROM (((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN todo t ON p.id=t.problem_id) LEFT JOIN media_problem mp ON p.id=mp.problem_id) LEFT JOIN media m ON (mp.media_id=m.id AND m.deleted_user_id IS NULL)) LEFT JOIN permission auth ON r.id=auth.region_id WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR auth.user_id IS NOT NULL) AND t.user_id=? AND (p.hidden=0 OR (auth.user_id=? AND (p.hidden<=1 OR auth.write>=p.hidden))) GROUP BY t.id, t.priority, a.name, s.name, p.id, p.name, p.grade, p.hidden, p.latitude, p.longitude, s.polygon_coords, s.parking_latitude, s.parking_longitude, a.latitude, a.longitude ORDER BY t.priority")) {
			ps.setInt(1, setup.getIdRegion());
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, userId);
			ps.setInt(4, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int priority = rst.getInt("priority");
					String areaName = rst.getString("area_name");
					String sectorName = rst.getString("sector_name");
					int problemId = rst.getInt("problem_id");
					String problemName = rst.getString("problem_name");
					int problemGrade = rst.getInt("problem_grade");
					int problemVisibility = rst.getInt("problem_visibility");
					double problemLatitude = rst.getDouble("problem_latitude");
					double problemLongitude = rst.getDouble("problem_longitude");
					String polygonCoords = rst.getString("polygon_coords");
					double sectorLatitude = rst.getDouble("sector_latitude");
					double sectorLongitude = rst.getDouble("sector_longitude");
					double areaLatitude = rst.getDouble("area_latitude");
					double areaLongitude = rst.getDouble("area_longitude");
					LatLng l = markerHelper.getLatLng(problemLatitude, problemLongitude);
					if (problemLatitude == 0 || problemLongitude == 0) {
						if (!Strings.isNullOrEmpty(polygonCoords)) {
							String[] latLng = polygonCoords.split(";")[0].split(",");
							l = markerHelper.getLatLng(Double.parseDouble(latLng[0]), Double.parseDouble(latLng[1]));
						}
						else if (sectorLatitude > 0 && sectorLongitude > 0) {
							l = markerHelper.getLatLng(sectorLatitude, sectorLongitude);
						}
						else if (areaLatitude > 0 && areaLongitude > 0) {
							l = markerHelper.getLatLng(areaLatitude, areaLongitude);
						}
					}
					int randomMediaId = rst.getInt("problem_random_media_id");
					todo.add(new Todo(id, priority, areaName, sectorName, problemId, problemName, GradeHelper.intToString(setup, problemGrade), problemVisibility, l.getLat(), l.getLng(), randomMediaId));
				}
			}
		}
		TodoUser res = null;
		String sqlStr = "SELECT u.id, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name FROM user u WHERE u.id=?";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, userId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String picture = rst.getString("picture");
					boolean readOnly = authUserId != userId;
					String name = rst.getString("name");
					res = new TodoUser(id, name, picture, readOnly, todo);
				}
			}
		}
		logger.debug("getTodo(authUserId={}, idRegion={}, reqId={}) - res={}", authUserId, setup.getIdRegion(), reqId, res);
		return res;
	}

	public List<Type> getTypes(int regionId) throws SQLException {
		List<Type> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT t.id, t.type, t.subtype FROM type t, region_type rt WHERE t.id=rt.type_id AND rt.region_id=? GROUP BY t.id, t.type, t.subtype ORDER BY t.id, t.type, t.subtype")) {
			ps.setInt(1, regionId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String type = rst.getString("type");
					String subtype = rst.getString("subtype");
					res.add(new Type(id, type, subtype));
				}
			}
		}
		return res;
	}

	public User getUser(int authUserId, Setup setup, int reqId) throws SQLException {
		Preconditions.checkArgument(reqId > 0 || authUserId != -1, "Invalid parameters - reqId=" + reqId + ", authUserId=" + authUserId);
		Stopwatch stopwatch = Stopwatch.createStarted();
		boolean readOnly = true;
		if (authUserId != -1) {
			if (reqId == authUserId || reqId <= 0) {
				readOnly = false;
				reqId = authUserId;
			}
		}
		if (reqId <= 0) {
			throw new SQLException("reqId=" + reqId + ", authUserId=" + authUserId);
		}
		User res = null;
		String sqlStr = "SELECT CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, COUNT(DISTINCT CASE WHEN mC.is_movie=0 THEN mC.id END) num_images_created, COUNT(DISTINCT CASE WHEN mC.is_movie=1 THEN mC.id END) num_videos_created, COUNT(DISTINCT CASE WHEN mT.is_movie=0 THEN mT.id END) num_image_tags, COUNT(DISTINCT CASE WHEN mT.is_movie=1 THEN mT.id END) num_video_tags" + " FROM ((user u LEFT JOIN media mC ON u.id=mC.photographer_user_id AND mC.deleted_user_id IS NULL) LEFT JOIN media_user mu ON u.id=mu.user_id) LEFT JOIN media mT ON mu.media_id=mT.id AND mT.deleted_user_id IS NULL WHERE u.id=? GROUP BY u.firstname, u.lastname, u.picture";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, reqId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String picture = rst.getString("picture");
					String name = rst.getString("name");
					int numImagesCreated = rst.getInt("num_images_created");
					int numVideosCreated = rst.getInt("num_videos_created");
					int numImageTags = rst.getInt("num_image_tags");
					int numVideoTags = rst.getInt("num_video_tags");
					res = new User(readOnly, reqId, picture, name, numImagesCreated, numVideosCreated, numImageTags, numVideoTags);
				}
			}
		}
		if (res == null) {
			return res;
		}

		sqlStr = "SELECT a.name area_name, a.hidden area_visibility, s.name sector_name, s.hidden sector_visibility, t.id id_tick, p.id id_problem, p.hidden, p.name, CASE WHEN (t.id IS NOT NULL) THEN t.comment ELSE p.description END comment, DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%Y-%m-%d') date, DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%d/%m-%y') date_hr, t.stars, CASE WHEN (f.user_id IS NOT NULL) THEN f.user_id ELSE 0 END fa, (CASE WHEN t.id IS NOT NULL THEN t.grade ELSE p.grade END) grade"
				+ " FROM ((((((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?)) LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?) LEFT JOIN fa f ON (p.id=f.problem_id AND f.user_id=?)"
				+ " WHERE (t.user_id IS NOT NULL OR f.user_id IS NOT NULL) AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR auth.user_id IS NOT NULL) AND (p.hidden=0 OR (auth.user_id IS NOT NULL AND (p.hidden<=1 OR auth.write>=p.hidden)))"
				+ " GROUP BY a.name, a.hidden, s.name, s.hidden, t.id, p.id, p.hidden, p.name, p.description, p.fa_date, t.date, t.stars, t.grade, p.grade";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, reqId);
			ps.setInt(3, reqId);
			ps.setInt(4, setup.getIdRegion());
			ps.setInt(5, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String areaName = rst.getString("area_name");
					int areaVisibility = rst.getInt("area_visibility");
					String sectorName = rst.getString("sector_name");
					int sectorVisibility = rst.getInt("sector_visibility");
					int id = rst.getInt("id_tick");
					int idProblem = rst.getInt("id_problem");
					int visibility = rst.getInt("hidden");
					String name = rst.getString("name");
					String comment = rst.getString("comment");
					String date = rst.getString("date");
					String dateHr = rst.getString("date_hr");
					double stars = rst.getDouble("stars");
					boolean fa = rst.getBoolean("fa");
					int grade = rst.getInt("grade");
					res.addTick(areaName, areaVisibility, sectorName, sectorVisibility, id, idProblem, visibility, name, comment, date, dateHr, stars, fa, GradeHelper.intToString(setup, grade), grade);
				}
			}
		}
		// First aid ascent
		if (!setup.isBouldering()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.name area_name, a.hidden area_visibility, s.name sector_name, s.hidden sector_visibility, p.id id_problem, p.hidden, p.name, aid.aid_description description, DATE_FORMAT(aid.aid_date,'%Y-%m-%d') date, DATE_FORMAT(aid.aid_date,'%d/%m-%y') date_hr" + 
					" FROM ((((((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN fa_aid aid ON p.id=aid.problem_id) INNER JOIN fa_aid_user aid_u ON (p.id=aid_u.problem_id AND aid_u.user_id=?) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?))" + 
					" WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR auth.user_id IS NOT NULL) AND (p.hidden=0 OR (auth.user_id IS NOT NULL AND (p.hidden<=1 OR auth.write>=p.hidden)))" + 
					" GROUP BY a.name, a.hidden, s.name, s.hidden, p.id, p.hidden, p.name, aid.aid_description, aid.aid_date")) {
				ps.setInt(1, reqId);
				ps.setInt(2, authUserId);
				ps.setInt(3, setup.getIdRegion());
				ps.setInt(4, setup.getIdRegion());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						String areaName = rst.getString("area_name");
						int areaVisibility = rst.getInt("area_visibility");
						String sectorName = rst.getString("sector_name");
						int sectorVisibility = rst.getInt("sector_visibility");
						int idProblem = rst.getInt("id_problem");
						int visibility = rst.getInt("hidden");
						String name = rst.getString("name");
						String comment = rst.getString("description");
						if (!Strings.isNullOrEmpty(comment)) {
							comment = "First ascent (AID): " + comment;
						}
						else {
							comment = "First ascent (AID)";
						}
						String date = rst.getString("date");
						String dateHr = rst.getString("date_hr");
						int grade = 0;
						res.addTick(areaName, areaVisibility, sectorName, sectorVisibility, 0, idProblem, visibility, name, comment, date, dateHr, 0, true, GradeHelper.intToString(setup, grade), grade);
					}
				}
			}
		}
		// Order ticks
		res.getTicks().sort((t1, t2) -> -ComparisonChain
				.start()
				.compare(Strings.nullToEmpty(t1.getDate()), Strings.nullToEmpty(t2.getDate()))
				.compare(t1.getIdProblem(), t2.getIdProblem())
				.result());
		for (int i = 0; i < res.getTicks().size(); i++) {
			res.getTicks().get(i).setNum(i);
		}
		logger.debug("getUser(authUserId={}, regionId={}, reqId={}) - duration={}", authUserId, setup.getIdRegion(), reqId, stopwatch);
		return res;
	}

	public List<User> getUserSearch(int authUserId, String value) throws SQLException {
		if (authUserId == -1) {
			throw new SQLException("User not logged in...");
		}
		List<User> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, CONCAT(firstname, ' ', COALESCE(lastname,'')) name FROM user WHERE (firstname LIKE ? OR lastname LIKE ? OR CONCAT(firstname, ' ', COALESCE(lastname,'')) LIKE ?) ORDER BY firstname, lastname")) {
			ps.setString(1, value + "%");
			ps.setString(2, value + "%");
			ps.setString(3, value + "%");
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String name = rst.getString("name");
					res.add(new User(true, id, null, name, -1, -1, -1, -1));
				}
			}
		}
		return res;
	}

	public byte[] getUserTicks(int authUserId) throws SQLException, IOException {
		byte[] bytes;
		try (ExcelReport report = new ExcelReport()) {
			String sqlStr = "SELECT r.id region_id, ty.type, CONCAT(r.url,'/problem/',p.id) url, a.name area_name, s.name sector_name, p.name, CASE WHEN (t.id IS NOT NULL) THEN t.comment ELSE p.description END comment, DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%Y-%m-%d') date, t.stars, CASE WHEN (f.user_id IS NOT NULL) THEN f.user_id ELSE 0 END fa, (CASE WHEN t.id IS NOT NULL THEN t.grade ELSE p.grade END) grade" + 
					" FROM (((((((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN type ty ON rt.type_id=ty.id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?)) LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?) LEFT JOIN fa f ON (p.id=f.problem_id AND f.user_id=?)" + 
					" WHERE (t.user_id IS NOT NULL OR f.user_id IS NOT NULL) AND (p.hidden=0 OR (auth.user_id IS NOT NULL AND (p.hidden<=1 OR auth.write>=p.hidden)))" + 
					" GROUP BY r.id, ty.type, r.url, a.name, a.hidden, s.name, s.hidden, t.id, p.id, p.hidden, p.name, p.description, p.fa_date, t.date, t.stars, t.grade, p.grade" + 
					" ORDER BY ty.type, a.name, s.name, p.name";
			try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
				ps.setInt(1, authUserId);
				ps.setInt(2, authUserId);
				ps.setInt(3, authUserId);
				try (ResultSet rst = ps.executeQuery()) {
					Map<String, SheetWriter> writers = new HashMap<>();
					while (rst.next()) {
						int regionId = rst.getInt("region_id");
						String type = rst.getString("type");
						String url = rst.getString("url");
						String areaName = rst.getString("area_name");
						String sectorName = rst.getString("sector_name");
						String name = rst.getString("name");
						String comment = rst.getString("comment");
						Date date = rst.getDate("date");
						int stars = rst.getInt("stars");
						boolean fa = rst.getBoolean("fa");
						String grade = GradeHelper.intToString(new MetaHelper().getSetup(regionId), rst.getInt("grade"));
						SheetWriter writer = writers.get(type);
						if (writer == null) {
							writer = report.addSheet(type);
							writers.put(type, writer);
						}
						writer.incrementRow();
						writer.write("AREA", areaName);
						writer.write("SECTOR", sectorName);
						writer.write("NAME", name);
						writer.write("FIRST ASCENT", fa? "Yes" : null);
						writer.write("DATE", date);
						writer.write("GRADE", grade);
						writer.write("STARS", stars);
						writer.write("DESCRIPTION", comment);
						writer.write("URL", SheetHyperlink.of(url));
					}
					for (SheetWriter writer : writers.values()) {
						writer.close();
					}
				}
				try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
					report.writeExcel(os);
					bytes = os.toByteArray();
				}
			}
			sqlStr = "SELECT r.id region_id, CONCAT(r.url,'/problem/',p.id) url, a.name area_name, s.name sector_name, p.name, aid.aid_description comment, DATE_FORMAT(aid.aid_date,'%Y-%m-%d') date" + 
					" FROM (((((((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN type ty ON rt.type_id=ty.id) INNER JOIN fa_aid aid ON p.id=aid.problem_id) INNER JOIN fa_aid_user aid_u ON (p.id=aid_u.problem_id AND aid_u.user_id=?) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?))" + 
					" WHERE p.hidden=0 OR (auth.user_id IS NOT NULL AND (p.hidden<=1 OR auth.write>=p.hidden))" + 
					" GROUP BY r.id, ty.type, r.url, a.name, a.hidden, s.name, s.hidden, p.id, p.hidden, p.name, aid.aid_description, aid.aid_date" + 
					" ORDER BY ty.type, a.name, s.name, p.name";
			try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr);
					SheetWriter writer = report.addSheet("First_AID_Ascent")) {
				ps.setInt(1, authUserId);
				ps.setInt(2, authUserId);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						String url = rst.getString("url");
						String areaName = rst.getString("area_name");
						String sectorName = rst.getString("sector_name");
						String name = rst.getString("name");
						String comment = rst.getString("comment");
						Date date = rst.getDate("date");
						writer.incrementRow();
						writer.write("AREA", areaName);
						writer.write("SECTOR", sectorName);
						writer.write("NAME", name);
						writer.write("DATE", date);
						writer.write("DESCRIPTION", comment);
						writer.write("URL", SheetHyperlink.of(url));
					}
					writer.close();
				}
				try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
					report.writeExcel(os);
					bytes = os.toByteArray();
				}
			}
		}
		return bytes;
	}

	public Area setArea(int authUserId, int idRegion, Area a, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, InterruptedException {
		Preconditions.checkArgument(authUserId != -1, "Insufficient credentials");
		Preconditions.checkArgument(idRegion > 0, "Insufficient credentials");
		boolean writePermissions = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT auth.write FROM permission auth WHERE auth.region_id=? AND auth.user_id=?")) {
			ps.setInt(1, idRegion);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int write = rst.getInt("write");
					writePermissions = write >= 1;
				}
			}
		}
		Preconditions.checkArgument(writePermissions, "Insufficient credentials");
		int idArea = -1;
		if (a.getId() > 0) {
			// Check if this actual area is writable for the user (remember that all regions can be visible to user, origin does not matter). Area region can be different to html-page region. Also check for admin/superadmin.
			int writable = 0;
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT 1 FROM area a, permission auth WHERE a.id=? AND a.region_id=auth.region_id AND auth.user_id=? AND auth.write>0 AND auth.write>=a.hidden")) {
				ps.setInt(1, a.getId());
				ps.setInt(2, authUserId);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						writable = 1;
					}
				}
			}
			if (writable != 1) {
				throw new SQLException("Insufficient credentials");
			}
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE area SET name=?, description=?, latitude=?, longitude=?, hidden=?, for_developers=? WHERE id=?")) {
				ps.setString(1, a.getName());
				ps.setString(2, Strings.emptyToNull(a.getComment()));
				if (a.getLat() > 0) {
					ps.setDouble(3, a.getLat());
				} else {
					ps.setNull(3, Types.DOUBLE);
				}
				if (a.getLng() > 0) {
					ps.setDouble(4, a.getLng());
				} else {
					ps.setNull(4, Types.DOUBLE);
				}
				ps.setInt(5, a.getVisibility());
				ps.setBoolean(6, a.isForDevelopers());
				ps.setInt(7, a.getId());
				ps.execute();
			}
			idArea = a.getId();

			// Also update sectors and problems (last_updated and visibility [if>0])
			String sqlStr = null;
			if (a.getVisibility() > 0) {
				sqlStr = "UPDATE (area a LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), a.hidden=?, s.last_updated=now(), s.hidden=?, p.last_updated=now(), p.hidden=? WHERE a.id=?";
				try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
					ps.setInt(1, a.getVisibility());
					ps.setInt(2, a.getVisibility());
					ps.setInt(3, a.getVisibility());
					ps.setInt(4, idArea);
					ps.execute();
				}
			} else {
				sqlStr = "UPDATE (area a LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), p.last_updated=now() WHERE a.id=?";
				try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
					ps.setInt(1, idArea);
					ps.execute();
				}
			}
		} else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO area (android_id, region_id, name, description, latitude, longitude, hidden, for_developers, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())", PreparedStatement.RETURN_GENERATED_KEYS)) {
				ps.setLong(1, System.currentTimeMillis());
				ps.setInt(2, idRegion);
				ps.setString(3, a.getName());
				ps.setString(4, Strings.emptyToNull(a.getComment()));
				if (a.getLat() > 0) {
					ps.setDouble(5, a.getLat());
				} else {
					ps.setNull(5, Types.DOUBLE);
				}
				if (a.getLng() > 0) {
					ps.setDouble(6, a.getLng());
				} else {
					ps.setNull(6, Types.DOUBLE);
				}
				ps.setInt(7, a.getVisibility());
				ps.setBoolean(8, a.isForDevelopers());
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idArea = rst.getInt(1);
					}
				}
			}
		}
		if (idArea == -1) {
			throw new SQLException("idArea == -1");
		}
		// New media
		if (a.getNewMedia() != null) {
			Timestamp now = new Timestamp(System.currentTimeMillis());
			for (NewMedia m : a.getNewMedia()) {
				final int idProblem = 0;
				final int pitch = 0;
				final int idSector = 0;
				addNewMedia(authUserId, idProblem, pitch, idSector, idArea, m, multiPart, now);
			}
		}
		return getArea(authUserId, idArea);
	}

	public Problem setProblem(int authUserId, Setup s, Problem p, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, ParseException, InterruptedException {
		final boolean orderByGrade = s.isBouldering();
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		final Date dt = Strings.isNullOrEmpty(p.getFaDate()) ? null : new Date(sdf.parse(p.getFaDate()).getTime());
		int idProblem = -1;
		if (p.getId() > 0) {
			fillProblemCoordinationsHistory(authUserId, p);
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE ((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN permission auth ON (a.region_id=auth.region_id AND auth.user_id=? AND auth.write>0 AND auth.write>=p.hidden) SET p.name=?, p.description=?, p.grade=?, p.fa_date=?, p.latitude=?, p.longitude=?, p.hidden=?, p.nr=?, p.type_id=?, p.last_updated=now() WHERE p.id=?")) {
				ps.setInt(1, authUserId);
				ps.setString(2, p.getName());
				ps.setString(3, Strings.emptyToNull(p.getComment()));
				ps.setInt(4, GradeHelper.stringToInt(s, p.getOriginalGrade()));
				ps.setDate(5, dt);
				if (p.getLat() > 0) {
					ps.setDouble(6, p.getLat());
				} else {
					ps.setNull(6, Types.DOUBLE);
				}
				if (p.getLng() > 0) {
					ps.setDouble(7, p.getLng());
				} else {
					ps.setNull(7, Types.DOUBLE);
				}
				ps.setInt(8, p.getVisibility());
				ps.setInt(9, p.getNr());
				ps.setInt(10, p.getT().getId());
				ps.setInt(11, p.getId());
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Insufficient credentials");
				}
			}
			idProblem = p.getId();
		} else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO problem (android_id, sector_id, name, description, grade, fa_date, latitude, longitude, hidden, nr, type_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
				ps.setLong(1, System.currentTimeMillis());
				ps.setInt(2, p.getSectorId());
				ps.setString(3, p.getName());
				ps.setString(4, Strings.emptyToNull(p.getComment()));
				ps.setInt(5, GradeHelper.stringToInt(s, p.getOriginalGrade()));
				ps.setDate(6, dt);
				if (p.getLat() > 0) {
					ps.setDouble(7, p.getLat());
				} else {
					ps.setNull(7, Types.DOUBLE);
				}
				if (p.getLng() > 0) {
					ps.setDouble(8, p.getLng());
				} else {
					ps.setNull(8, Types.DOUBLE);
				}
				ps.setInt(9, p.getVisibility());
				ps.setInt(10, p.getNr() == 0 ? getSector(authUserId, orderByGrade, s, p.getSectorId()).getProblems().stream().map(x -> x.getNr()).mapToInt(Integer::intValue).max().orElse(0) + 1 : p.getNr());
				ps.setInt(11, p.getT().getId());
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idProblem = rst.getInt(1);
					}
				}
			}
		}
		if (idProblem == -1) {
			throw new SQLException("idProblem == -1");
		}
		// Also update last_updated on problem, sector and area (ALSO CHECKS PERMISSION [SINCE INSERT DOES NOT])
		String sqlStr = "UPDATE problem p, sector s, area a, permission auth SET p.last_updated=now(), s.last_updated=now(), a.last_updated=now() WHERE p.id=? AND p.sector_id=s.id AND s.area_id=a.id AND a.region_id=auth.region_id AND auth.user_id=? AND auth.write>0 AND auth.write>=p.hidden";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, idProblem);
			ps.setInt(2, authUserId);
			int res = ps.executeUpdate();
			if (res == 0) {
				throw new SQLException("Insufficient credentials");
			}
		}
		// New media
		if (p.getNewMedia() != null) {
			Timestamp now = new Timestamp(System.currentTimeMillis());
			for (NewMedia m : p.getNewMedia()) {
				final int idSector = 0;
				final int idArea = 0;
				addNewMedia(authUserId, idProblem, m.getPitch(), idSector, idArea, m, multiPart, now);
			}
		}
		// FA
		if (p.getFa() != null) {
			Set<Integer> fas = new HashSet<>();
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT user_id FROM fa WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						fas.add(rst.getInt("user_id"));
					}
				}
			}
			for (FaUser x : p.getFa()) {
				Preconditions.checkArgument(x.getId() != 0);
				if (x.getId() > 0) { // Existing user
					boolean exists = fas.remove(x.getId());
					if (!exists) {
						try (PreparedStatement ps2 = c.getConnection().prepareStatement("INSERT INTO fa (problem_id, user_id) VALUES (?, ?)")) {
							ps2.setInt(1, idProblem);
							ps2.setInt(2, x.getId());
							ps2.execute();
						}
					}
				} else { // New user
					final boolean autoCommit = false;
					int idUser = addUser(null, x.getName(), null, null, autoCommit);
					Preconditions.checkArgument(idUser > 0);
					try (PreparedStatement ps2 = c.getConnection().prepareStatement("INSERT INTO fa (problem_id, user_id) VALUES (?, ?)")) {
						ps2.setInt(1, idProblem);
						ps2.setInt(2, idUser);
						ps2.execute();
					}
				}
			}
			if (!fas.isEmpty()) {
				try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM fa WHERE problem_id=? AND user_id=?")) {
					for (int x : fas) {
						ps.setInt(1, idProblem);
						ps.setInt(2, x);
						ps.addBatch();
					}
					ps.executeBatch();
				}
			}
		} else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM fa WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
			}
		}
		// Sections
		try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM problem_section WHERE problem_id=?")) {
			ps.setInt(1, idProblem);
			ps.execute();
		}
		if (p.getSections() != null && p.getSections().size() > 1) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO problem_section (problem_id, nr, description, grade) VALUES (?, ?, ?, ?)")) {
				for (Section section : p.getSections()) {
					ps.setInt(1, idProblem);
					ps.setInt(2, section.getNr());
					ps.setString(3, section.getDescription());
					ps.setInt(4, GradeHelper.stringToInt(s, section.getGrade()));
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		// First aid ascent
		if (!s.isBouldering()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM fa_aid WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
				ps.execute();
			}
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM fa_aid_user WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
				ps.execute();
			}
			if (p.getFaAid() != null) {
				FaAid faAid = p.getFaAid();
				final Date aidDt = Strings.isNullOrEmpty(faAid.getDate()) ? null : new Date(sdf.parse(faAid.getDate()).getTime());
				try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO fa_aid (problem_id, aid_date, aid_description) VALUES (?, ?, ?)")) {
					ps.setInt(1, faAid.getProblemId());
					ps.setDate(2, aidDt);
					ps.setString(3, faAid.getDescription());
					ps.execute();
				}
				if (!faAid.getUsers().isEmpty()) {
					try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO fa_aid_user (problem_id, user_id) VALUES (?, ?)")) {
						for (FaUser u : faAid.getUsers()) {
							int idUser = u.getId();
							if (idUser <= 0) {
								final boolean autoCommit = false;
								idUser = addUser(null, u.getName(), null, null, autoCommit);
							}
							Preconditions.checkArgument(idUser > 0);
							ps.setInt(1, faAid.getProblemId());
							ps.setInt(2, idUser);
							ps.addBatch();
						}
						ps.executeBatch();
					}	
				}
			}
		}
		fillActivity(idProblem);
		return getProblem(authUserId, s, idProblem);
	}

	public Sector setSector(int authUserId, boolean orderByGrade, Setup setup, Sector s, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, InterruptedException {
		int idSector = -1;
		if (s.getId() > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE sector s, area a, permission auth SET s.name=?, s.description=?, s.parking_latitude=?, s.parking_longitude=?, s.hidden=?, s.polygon_coords=?, s.polyline=? WHERE s.id=? AND s.area_id=a.id AND a.region_id=auth.region_id AND auth.user_id=? AND auth.write>0 AND auth.write>=s.hidden")) {
				ps.setString(1, s.getName());
				ps.setString(2, Strings.emptyToNull(s.getComment()));
				if (s.getLat() > 0) {
					ps.setDouble(3, s.getLat());
				} else {
					ps.setNull(3, Types.DOUBLE);
				}
				if (s.getLng() > 0) {
					ps.setDouble(4, s.getLng());
				} else {
					ps.setNull(4, Types.DOUBLE);
				}
				ps.setInt(5, s.getVisibility());
				ps.setString(6, Strings.emptyToNull(s.getPolygonCoords()));
				ps.setString(7, Strings.emptyToNull(s.getPolyline()));
				ps.setInt(8, s.getId());
				ps.setInt(9, authUserId);
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Insufficient credentials");
				}
			}
			idSector = s.getId();

			// Also update problems (last_updated and visibility [if >1]) + last_updated on area
			String sqlStr = null;
			if (s.getVisibility() > 0) {
				sqlStr = "UPDATE (area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), s.hidden=?, p.last_updated=now(), p.hidden=? WHERE s.id=?";
				try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
					ps.setInt(1, s.getVisibility());
					ps.setInt(2, s.getVisibility());
					ps.setInt(3, idSector);
					ps.execute();
				}
			} else {
				sqlStr = "UPDATE (area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), p.last_updated=now() WHERE s.id=?";
				try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
					ps.setInt(1, idSector);
					ps.execute();
				}
			}
		} else {
			int writable = 0;
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT 1 FROM area a, permission auth WHERE a.id=? AND a.region_id=auth.region_id AND auth.user_id=? AND auth.write>0 AND auth.write>=a.hidden")) {
				ps.setInt(1, s.getAreaId());
				ps.setInt(2, authUserId);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						writable = 1;
					}
				}
			}
			if (writable != 1) {
				throw new SQLException("Insufficient credentials");
			}
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO sector (android_id, area_id, name, description, parking_latitude, parking_longitude, hidden, polygon_coords, polyline, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())", PreparedStatement.RETURN_GENERATED_KEYS)) {
				ps.setLong(1, System.currentTimeMillis());
				ps.setInt(2, s.getAreaId());
				ps.setString(3, s.getName());
				ps.setString(4, Strings.emptyToNull(s.getComment()));
				if (s.getLat() > 0) {
					ps.setDouble(5, s.getLat());
				} else {
					ps.setNull(5, Types.DOUBLE);
				}
				if (s.getLng() > 0) {
					ps.setDouble(6, s.getLng());
				} else {
					ps.setNull(6, Types.DOUBLE);
				}
				ps.setInt(7, s.getVisibility());
				ps.setString(8, Strings.emptyToNull(s.getPolygonCoords()));
				ps.setString(9, Strings.emptyToNull(s.getPolyline()));
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idSector = rst.getInt(1);
					}
				}
			}
		}
		if (idSector == -1) {
			throw new SQLException("idSector == -1");
		}
		// New media
		if (s.getNewMedia() != null) {
			Timestamp now = new Timestamp(System.currentTimeMillis());
			for (NewMedia m : s.getNewMedia()) {
				final int pitch = 0;
				final int idProblem = 0;
				final int idArea = 0;
				addNewMedia(authUserId, idProblem, pitch, idSector, idArea, m, multiPart, now);
			}
		}
		return getSector(authUserId, orderByGrade, setup, idSector);
	}

	public void setTick(int authUserId, Setup setup, Tick t) throws SQLException, ParseException {
		Preconditions.checkArgument(authUserId != -1);
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		// Remove from project list (if existing)
		try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM todo WHERE user_id=? AND problem_id=?")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, t.getIdProblem());
			ps.execute();
		}
		final Date dt = Strings.isNullOrEmpty(t.getDate()) ? null : new Date(sdf.parse(t.getDate()).getTime());
		logger.debug("setTick(authUserId={}, dt={}, t={}", authUserId, dt, t);
		if (t.isDelete()) {
			Preconditions.checkArgument(t.getId() > 0, "Cannot delete a tick without id");
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM tick WHERE id=? AND user_id=? AND problem_id=?")) {
				ps.setInt(1, t.getId());
				ps.setInt(2, authUserId);
				ps.setInt(3, t.getIdProblem());
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
				}
			}
		} else if (t.getId() == -1) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO tick (problem_id, user_id, date, grade, comment, stars) VALUES (?, ?, ?, ?, ?, ?)")) {
				ps.setInt(1, t.getIdProblem());
				ps.setInt(2, authUserId);
				ps.setDate(3, dt);
				ps.setInt(4, GradeHelper.stringToInt(setup, t.getGrade()));
				ps.setString(5, Strings.emptyToNull(t.getComment()));
				ps.setDouble(6, t.getStars());
				ps.execute();
			}

		} else if (t.getId() > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE tick SET date=?, grade=?, comment=?, stars=? WHERE id=? AND problem_id=? AND user_id=?")) {
				ps.setDate(1, dt);
				ps.setInt(2, GradeHelper.stringToInt(setup, t.getGrade()));
				ps.setString(3, Strings.emptyToNull(t.getComment()));
				ps.setDouble(4, t.getStars());
				ps.setInt(5, t.getId());
				ps.setInt(6, t.getIdProblem());
				ps.setInt(7, authUserId);
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
				}
			}
		} else {
			throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
		}
		fillActivity(t.getIdProblem());
	}

	public void setUser(int authUserId, boolean useBlueNotRed) throws SQLException {
		try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE user SET use_blue_not_red=? WHERE id=?")) {
			ps.setBoolean(1, useBlueNotRed);
			ps.setInt(2, authUserId);
			ps.execute();
		}
	}

	public void upsertComment(int authUserId, Comment co) throws SQLException {
		Preconditions.checkArgument(authUserId > 0);
		if (co.getId() > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE guestbook SET danger=?, resolved=? WHERE id=?")) {
				ps.setBoolean(1, co.isDanger());
				ps.setBoolean(2, co.isResolved());
				ps.setInt(3, co.getId());
				ps.execute();
			}
		} else {
			Preconditions.checkNotNull(Strings.emptyToNull(co.getComment()));
			int parentId = 0;
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT MIN(id) FROM guestbook WHERE problem_id=?")) {
				ps.setInt(1, co.getIdProblem());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						parentId = rst.getInt(1);
					}
				}
			}

			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO guestbook (post_time, message, problem_id, user_id, parent_id, danger, resolved) VALUES (now(), ?, ?, ?, ?, ?, ?)")) {
				ps.setString(1, co.getComment());
				ps.setInt(2, co.getIdProblem());
				ps.setInt(3, authUserId);
				if (parentId == 0) {
					ps.setNull(4, Types.INTEGER);
				} else {
					ps.setInt(4, parentId);
				}
				ps.setBoolean(5, co.isDanger());
				ps.setBoolean(6, co.isResolved());
				ps.execute();
			}
		}
		fillActivity(co.getIdProblem());
	}

	public void upsertPermissionUser(int regionId, int authUserId, PermissionUser u) throws SQLException {
		// Ensure superadmin
		Preconditions.checkArgument(authUserId != -1, "Insufficient credentials");
		Preconditions.checkArgument(regionId > 0, "Insufficient credentials");
		boolean writePermissions = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT auth.write FROM permission auth WHERE auth.region_id=? AND auth.user_id=?")) {
			ps.setInt(1, regionId);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int write = rst.getInt("write");
					writePermissions = write == 2;
				}
			}
		}
		Preconditions.checkArgument(writePermissions, "Insufficient credentials");
		// Upsert
		if (u.getWrite() == -1) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM permission WHERE region_id=? AND user_id=?")) {
				ps.setInt(1, regionId);
				ps.setInt(2, u.getUserId());
				ps.execute();
			}
		}
		else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO permission (region_id, user_id, `write`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `write`=?")) {
				ps.setInt(1, regionId);
				ps.setInt(2, u.getUserId());
				ps.setInt(3, u.getWrite());
				ps.setInt(4, u.getWrite());
				ps.execute();
			}
		}
	}

	public void upsertSvg(int authUserId, int problemId, int mediaId, Svg svg) throws SQLException {
		// Check for write permissions
		boolean ok = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT 1 FROM ((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN permission auth ON (a.region_id=auth.region_id AND auth.user_id=? AND auth.write>0 AND auth.write>=p.hidden) WHERE p.id=?")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, problemId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = true;
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient credentials");
		// Delete/Insert/Update
		if (svg.isDelete() || Strings.emptyToNull(svg.getPath()) == null) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM svg WHERE media_id=? AND problem_id=?")) {
				ps.setInt(1, mediaId);
				ps.setInt(2, problemId);
				ps.execute();
			}
		} else if (svg.getId() <= 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO svg (media_id, problem_id, path, has_anchor, anchors, texts) VALUES (?, ?, ?, ?, ?, ?)")) {
				ps.setInt(1, mediaId);
				ps.setInt(2, problemId);
				ps.setString(3, svg.getPath());
				ps.setBoolean(4, svg.isHasAnchor());
				ps.setString(5, svg.getAnchors());
				ps.setString(6, svg.getTexts());
				ps.execute();
			}
		} else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE svg SET media_id=?, problem_id=?, path=?, has_anchor=?, anchors=?, texts=? WHERE id=?")) {
				ps.setInt(1, mediaId);
				ps.setInt(2, problemId);
				ps.setString(3, svg.getPath());
				ps.setBoolean(4, svg.isHasAnchor());
				ps.setString(5, svg.getAnchors());
				ps.setString(6, svg.getTexts());
				ps.setInt(7, svg.getId());
				ps.execute();
			}
		}
	}

	public void upsertTodo(int authUserId, Todo todo) throws SQLException {
		// Delete/Insert/Update
		if (todo.isDelete()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM todo WHERE id=?")) {
				ps.setInt(1, todo.getId());
				ps.execute();
			}
		} else if (todo.getId() <= 0) {
			int priority = 1;
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT MAX(priority) FROM todo WHERE user_id=?")) {
				ps.setInt(1, authUserId);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						priority = rst.getInt(1);
					}
				}
			}
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO todo (user_id, problem_id, priority) VALUES (?, ?, ?)")) {
				ps.setInt(1, authUserId);
				ps.setInt(2, todo.getProblemId());
				ps.setInt(3, ++priority);
				ps.execute();
			}
		} else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE todo SET priority=? WHERE id=?")) {
				ps.setInt(1, todo.getPriority());
				ps.setInt(2, todo.getId());
				ps.execute();
			}
		}
	}

	private int addNewMedia(int idUser, int idProblem, int pitch, int idSector, int idArea, NewMedia m, FormDataMultiPart multiPart, Timestamp now) throws SQLException, IOException, NoSuchAlgorithmException, InterruptedException {
		logger.debug("addNewMedia(idUser={}, idProblem={}, pitch, idSector={}, idArea={}, m={}) initialized", idUser, idProblem, pitch, idSector, m);
		Preconditions.checkArgument((idProblem > 0 && idSector == 0 && idArea == 0) || (idProblem == 0 && idSector > 0 && idArea == 0) || (idProblem == 0 && idSector == 0 && idArea > 0));
		try (InputStream is = multiPart.getField(m.getName()).getValueAs(InputStream.class)) {
			/**
			 * DB
			 */
			int idMedia = -1;
			final String suffix = "jpg";
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media (is_movie, suffix, photographer_user_id, uploader_user_id, date_created) VALUES (?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
				ps.setBoolean(1, false);
				ps.setString(2, suffix);
				ps.setInt(3, getExistingOrInsertUser(m.getPhotographer()));
				ps.setInt(4, idUser);
				ps.setTimestamp(5, now);
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idMedia = rst.getInt(1);
					}
				}
			}
			Preconditions.checkArgument(idMedia > 0);
			if (idProblem > 0) {
				try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_problem (media_id, problem_id, pitch) VALUES (?, ?, ?)")) {
					ps.setInt(1, idMedia);
					ps.setInt(2, idProblem);
					if (pitch > 0) {
						ps.setInt(3, pitch);
					}
					else { 
						ps.setNull(3, Types.NUMERIC);
					}
					ps.execute();
				}
			} else if (idSector > 0) {
				try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_sector (media_id, sector_id) VALUES (?, ?)")) {
					ps.setInt(1, idMedia);
					ps.setInt(2, idSector);
					ps.execute();
				}
			} else if (idArea > 0) {
				try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_area (media_id, area_id) VALUES (?, ?)")) {
					ps.setInt(1, idMedia);
					ps.setInt(2, idArea);
					ps.execute();
				}
			}
			if (!Strings.isNullOrEmpty(m.getInPhoto())) {
				try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_user (media_id, user_id) VALUES (?, ?)")) {
					ps.setInt(1, idMedia);
					ps.setInt(2, getExistingOrInsertUser(m.getInPhoto()));
					ps.execute();
				}
			}

			/**
			 * IO
			 */
			long ms = System.currentTimeMillis();

			/**
			 * To fix:
			 * 2020.05.15 13:59:48,610 [http-nio-8080-exec-258] FATAL com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions:20 - /mnt/buldreinfo/media/temp/1589543988604_a.jpg: Read-only file system
			 * 
			 * Add the following to /lib/systemd/system/tomcat9.service
			 * ReadWritePaths=/mnt/buldreinfo/media/
			 * 
			 * systemctl daemon-reload
			 * service tomcat9 restart
			 */
			// Save received file
			Path original = Paths.get(PATH + "temp");
			Files.createDirectories(original);
			original = original.resolve(ms + "_" + m.getName());
			Preconditions.checkArgument(Files.exists(original.getParent()), original.getParent().toString() + " does not exist");
			Preconditions.checkArgument(!Files.exists(original), original.toString() + " does already exist");
			Files.copy(is, original);
			Preconditions.checkArgument(Files.exists(original), original.toString() + " does not exist");

			final Path p = Paths.get(PATH + "original").resolve(String.valueOf(idMedia / 100 * 100)).resolve(idMedia + "." + suffix);
			Files.createDirectories(p.getParent());
			Preconditions.checkArgument(!Files.exists(p), p.toString() + " does already exist");

			// If not JPG/JPEG --> convert to JPG, else --> copy to destination
			// (ALWAYS JPG)
			final String inputExtension = com.google.common.io.Files.getFileExtension(original.getFileName().toString());
			if (!inputExtension.equalsIgnoreCase("jpg") && !inputExtension.equalsIgnoreCase("jpeg")) {
				BufferedImage src = ImageIO.read(original.toFile());
				BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
				dst.createGraphics().drawImage(src, 0, 0, Color.WHITE, null);
				ImageIO.write(dst, "jpg", p.toFile());
				src.flush();
				dst.flush();
			} else {
				Files.copy(original, p);
			}
			Preconditions.checkArgument(Files.exists(p), p.toString() + " does not exist");

			// Rotate (if EXIF-rotated)
			try (ThumbnailCreation creation = ThumbnailCreation.image(p.toFile())) {
				ExifOrientation orientation = creation.getExifRotation();

				if (orientation != null && orientation != ExifOrientation.HORIZONTAL_NORMAL) {
					logger.info("Rotating " + p.toString() + " using " + orientation);
					creation.rotate(orientation).preserveExif().saveTo(com.google.common.io.Files.asByteSink(p.toFile()));
				}
			}
			Preconditions.checkArgument(Files.exists(p), p.toString() + " does not exist");
			// Create scaled jpg and webp + update crc32 and dimentions in db
			createScaledImages(c, getDateTaken(p), idMedia, suffix);

			return idMedia;
		}
	}

	private int addUser(String email, String firstname, String lastname, String picture, boolean autoCommit) throws SQLException {
		int id = -1;
		try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO user (firstname, lastname, picture) VALUES (?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, firstname);
			ps.setString(2, lastname);
			ps.setString(3, picture);
			ps.executeUpdate();
			try (ResultSet rst = ps.getGeneratedKeys()) {
				if (rst != null && rst.next()) {
					id = rst.getInt(1);
					logger.debug("addUser(email={}, firstname={}, lastname={}, picture={}, autoCommit={}) - getInt(1)={}", email, firstname, lastname, picture, autoCommit, id);
				}
			}
		}
		if (autoCommit) {
			c.getConnection().commit();
		}
		Preconditions.checkArgument(id > 0, "id=" + id + ", firstname=" + firstname + ", lastname=" + lastname);
		if (!Strings.isNullOrEmpty(email)) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO user_email (user_id, email) VALUES (?, ?)")) {
				ps.setInt(1, id);
				ps.setString(2, email.toLowerCase());
				ps.execute();
			}
		}
		if (picture != null) {
			downloadUserImage(id, picture);
		}
		return id;
	}

	private void createScaledImages(DbConnection c, String dateTaken, int id, String suffix) throws IOException, InterruptedException, SQLException {
		final Path original = Paths.get(PATH + "original").resolve(String.valueOf(id / 100 * 100)).resolve(id + "." + suffix);
		final Path webp = Paths.get(PATH + "web/webp").resolve(String.valueOf(id / 100 * 100)).resolve(id + ".webp");
		final Path jpg = Paths.get(PATH + "web/jpg").resolve(String.valueOf(id / 100 * 100)).resolve(id + ".jpg");
		Files.createDirectories(webp.getParent());
		Files.createDirectories(jpg.getParent());
		Preconditions.checkArgument(Files.exists(original), original.toString() + " does not exist");
		Preconditions.checkArgument(!Files.exists(webp), webp.toString() + " does already exist");
		Preconditions.checkArgument(!Files.exists(jpg), jpg.toString() + " does already exist");
		// Scaled JPG
		BufferedImage bOriginal = ImageIO.read(original.toFile());
		final int width = bOriginal.getWidth();
		final int height = bOriginal.getHeight();
		BufferedImage bScaled = Scalr.resize(bOriginal, 1920, Scalr.OP_ANTIALIAS);
		logger.debug("createScaledImages(original={}) - jpg={}, bOriginal={}, bScaled={}", original, jpg, bOriginal, bScaled);
		ImageIO.write(bScaled, "jpg", jpg.toFile());
		bOriginal.flush();
		bOriginal = null;
		bScaled.flush();
		bScaled = null;
		Preconditions.checkArgument(Files.exists(jpg));
		// Scaled WebP
		String[] cmd = new String[] { "/bin/bash", "-c", "cwebp \"" + jpg.toString() + "\" -af -m 6 -o \"" + webp.toString() + "\"" };
		Process process = Runtime.getRuntime().exec(cmd);
		process.waitFor();
		Preconditions.checkArgument(Files.exists(webp), "WebP does not exist. Command=" + Lists.newArrayList(cmd));
		final int crc32 = com.google.common.io.Files.asByteSource(webp.toFile()).hash(Hashing.crc32()).asInt();

		/**
		 * Final DB
		 */
		try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE media SET date_taken=?, checksum=?, width=?, height=? WHERE id=?")) {
			ps.setString(1, dateTaken);
			ps.setInt(2, crc32);
			ps.setInt(3, width);
			ps.setInt(4, height);
			ps.setInt(5, id);
			ps.execute();
		}
	}

	private void downloadUserImage(int userId, String url) {
		try {
			final Path original = Paths.get(PATH + "original/users").resolve(userId + ".jpg");
			final Path resized = Paths.get(PATH + "web/users").resolve(userId + ".jpg");
			Files.createDirectories(original.getParent());
			try (InputStream in = new URL(url).openStream()) {
				Files.copy(in, original, StandardCopyOption.REPLACE_EXISTING);
				in.close();
				// Resize avatar
				Files.createDirectories(resized.getParent());
				Files.deleteIfExists(resized);
				BufferedImage bOriginal = ImageIO.read(original.toFile());
				BufferedImage bScaled = Scalr.resize(bOriginal, 35, Scalr.OP_ANTIALIAS);
				ImageIO.write(bScaled, "jpg", resized.toFile());
				bOriginal.flush();
				bOriginal = null;
				bScaled.flush();
				bScaled = null;
				Preconditions.checkArgument(Files.exists(resized));
			}
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
	}

	private void fillProblemCoordinationsHistory(int authUserId, Problem p) throws SQLException {
		double latitude = 0;
		double longitude = 0;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT latitude, longitude FROM problem WHERE id=?")) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					latitude = rst.getDouble("latitude");
					longitude = rst.getDouble("longitude");
				}
			}
		}
		if (latitude != 0 && longitude != 0 && (latitude != p.getLat() || longitude != p.getLng())) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO problem_coordinations_history (problem_id, user_id, latitude, longitude) VALUES (?, ?, ?, ?)")) {
				ps.setInt(1, p.getId());
				ps.setInt(2, authUserId);
				ps.setDouble(3, latitude);
				ps.setDouble(4, longitude);
				ps.execute();
			}
		}
	}

	private String getDateTaken(Path p) {
		if (Files.exists(p) && p.getFileName().toString().toLowerCase().endsWith(".jpg")) {
			try {
				Metadata metadata = ImageMetadataReader.readMetadata(p.toFile());
				ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
				java.util.Date date = directory.getDateOriginal(TimeZone.getDefault());
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
				return sdf.format(date.getTime());
			} catch (Exception e) {
			}
		}
		return null;
	}

	private int getExistingOrInsertUser(String name) throws SQLException, NoSuchAlgorithmException {
		if (Strings.isNullOrEmpty(name)) {
			return 1049; // Unknown
		}
		int usId = -1;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id FROM user WHERE CONCAT(firstname, ' ', COALESCE(lastname,''))=?")) {
			ps.setString(1, name);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					usId = rst.getInt("id");
				}
			}
		}
		if (usId == -1) {
			final boolean autoCommit = false;
			usId = addUser(null, name, null, null, autoCommit);
		}
		Preconditions.checkArgument(usId > 0);
		return usId;
	}

	private List<Media> getMediaArea(int id) throws SQLException {
		List<Media> media = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.width, m.height, m.is_movie, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM (((media m INNER JOIN media_area ma ON m.id=ma.media_id AND m.deleted_user_id IS NULL AND ma.area_id=?) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id GROUP BY m.id, m.width, m.height, m.is_movie, ma.sorting, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.is_movie, -ma.sorting DESC, m.id")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int itId = rst.getInt("id");
					int pitch = 0;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged);
					media.add(new Media(itId, pitch, width, height, tyId, null, 0, null, mediaMetadata));
				}
			}
		}
		return media;
	}

	private List<Media> getMediaProblem(Setup s, int sectorId, int problemId) throws SQLException {
		List<Media> media = getMediaSector(sectorId, problemId);
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.width, m.height, m.is_movie, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, mp.pitch, ROUND(mp.milliseconds/1000) t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM (((media m INNER JOIN media_problem mp ON m.id=mp.media_id AND m.deleted_user_id IS NULL AND mp.problem_id=?) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id GROUP BY m.id, m.width, m.height, m.is_movie, mp.sorting, m.date_created, m.date_taken, mp.pitch, mp.milliseconds, c.firstname, c.lastname ORDER BY m.is_movie, -mp.sorting DESC, m.id")) {
			ps.setInt(1, problemId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int itId = rst.getInt("id");
					int pitch = rst.getInt("pitch");
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String t = rst.getString("t");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged);
					media.add(new Media(itId, pitch, width, height, tyId, t, 0, null, mediaMetadata));
				}
			}
		}
		if (media.isEmpty()) {
			media = null;
		}
		return media;
	}

	private List<Media> getMediaSector(int idSector, int optionalIdProblem) throws SQLException {
		List<Media> media = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.width, m.height, m.is_movie, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM (((media m INNER JOIN media_sector ms ON m.id=ms.media_id AND m.deleted_user_id IS NULL AND ms.sector_id=?) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id GROUP BY m.id, m.width, m.height, m.is_movie, ms.sorting, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.is_movie, -ms.sorting DESC, m.id")) {
			ps.setInt(1, idSector);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int itId = rst.getInt("id");
					int pitch = 0;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					List<Svg> svgs = getSvgs(itId);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged);
					Media m = new Media(itId, pitch, width, height, tyId, null, optionalIdProblem, svgs, mediaMetadata);
					if (optionalIdProblem != 0 && svgs != null
							&& svgs.stream().filter(svg -> svg.getProblemId() == optionalIdProblem).findAny().isPresent()) {
						media.clear();
						media.add(m);
						break;
					} else {
						media.add(m);
					}
				}
			}
		}
		return media;
	}

	private Sector getSector(int authUserId, boolean orderByGrade, Setup setup, int reqId, boolean updateHits) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		if (updateHits) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE sector SET hits=hits+1 WHERE id=?")) {
				ps.setInt(1, reqId);
				ps.execute();
			}
		}
		MarkerHelper markerHelper = new MarkerHelper();
		Sector s = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id area_id, a.hidden area_hidden, a.name area_name, CONCAT(r.url,'/sector/',s.id) canonical, s.hidden, s.name, s.description, s.parking_latitude, s.parking_longitude, s.polygon_coords, s.polyline, s.hits FROM ((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN permission auth ON a.region_id=auth.region_id WHERE s.id=? AND (s.hidden=0 OR (auth.user_id=? AND (s.hidden<=1 OR auth.write>=s.hidden))) GROUP BY r.url, a.id, a.hidden, a.name, s.hidden, s.name, s.description, s.parking_latitude, s.parking_longitude, s.polygon_coords, s.polyline, s.hits")) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					int areaVisibility = rst.getInt("area_hidden");
					String areaName = rst.getString("area_name");
					String canonical = rst.getString("canonical");
					int visibility = rst.getInt("hidden");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					LatLng l = markerHelper.getLatLng(rst.getDouble("parking_latitude"), rst.getDouble("parking_longitude"));
					String polygonCoords = rst.getString("polygon_coords");
					String polyline = rst.getString("polyline");
					int hits = rst.getInt("hits");
					List<Media> media = getMediaSector(reqId, 0);
					media.addAll(getMediaArea(areaId));
					if (media.isEmpty()) {
						media = null;
					}
					s = new Sector(orderByGrade, areaId, areaVisibility, areaName, canonical, reqId, visibility, name, comment, l.getLat(), l.getLng(), polygonCoords, polyline, media, null, hits);
				}
			}
		}
		String sqlStr = "SELECT p.id, p.hidden, p.nr, p.name, p.description, ROUND((IFNULL(AVG(NULLIF(t.grade,0)), p.grade) + p.grade)/2) grade, p.latitude, p.longitude,"
				+ " COUNT(DISTINCT ps.id) num_pitches,"
				+ " COUNT(DISTINCT CASE WHEN m.is_movie=0 THEN m.id END) num_images,"
				+ " COUNT(DISTINCT CASE WHEN m.is_movie=1 THEN m.id END) num_movies,"
				+ " group_concat(DISTINCT CONCAT(TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') fa,"
				+ " COUNT(DISTINCT t.id) num_ticks, ROUND(ROUND(AVG(t.stars)*2)/2,1) stars,"
				+ " MAX(CASE WHEN (t.user_id=? OR u.id=?) THEN 1 END) ticked, ty.id type_id, ty.type, ty.subtype,"
				+ " danger.danger"
				+ " FROM (((((((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN permission auth ON a.region_id=auth.region_id) LEFT JOIN (media_problem mp LEFT JOIN media m ON mp.media_id=m.id AND m.deleted_user_id IS NULL) ON p.id=mp.problem_id) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN tick t ON p.id=t.problem_id) LEFT JOIN (SELECT problem_id, danger FROM guestbook WHERE (danger=1 OR resolved=1) AND id IN (SELECT max(id) id FROM guestbook WHERE (danger=1 OR resolved=1) GROUP BY problem_id)) danger ON p.id=danger.problem_id) LEFT JOIN problem_section ps ON p.id=ps.problem_id"
				+ " WHERE p.sector_id=?"
				+ "   AND (p.hidden=0 OR (auth.user_id=? AND (p.hidden<=1 OR auth.write>=p.hidden)))"
				+ " GROUP BY p.id, p.hidden, p.nr, p.name, p.description, p.grade, p.latitude, p.longitude, ty.id, ty.type, ty.subtype, danger.danger"
				+ (orderByGrade? " ORDER BY ROUND((IFNULL(AVG(NULLIF(t.grade,0)), p.grade) + p.grade)/2) DESC, p.name" : " ORDER BY p.nr");
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, authUserId);
			ps.setInt(3, reqId);
			ps.setInt(4, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int visibility = rst.getInt("hidden");
					int nr = rst.getInt("nr");
					int grade = rst.getInt("grade");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					String fa = rst.getString("fa");
					LatLng l = markerHelper.getLatLng(rst.getDouble("latitude"), rst.getDouble("longitude"));
					int numPitches = rst.getInt("num_pitches");
					boolean hasImages = rst.getInt("num_images")>0;
					boolean hasMovies = rst.getInt("num_movies")>0;
					int numTicks = rst.getInt("num_ticks");
					double stars = rst.getDouble("stars");
					boolean ticked = rst.getBoolean("ticked");
					Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
					boolean danger = rst.getBoolean("danger");
					s.addProblem(id, visibility, nr, name, comment, grade, GradeHelper.intToString(setup, grade), fa, numPitches, hasImages, hasMovies, l.getLat(), l.getLng(), numTicks, stars, ticked, t, danger);
				}
			}
		}
		logger.debug("getSector(authUserId={}, orderByGrade={}, reqId={}) - duration={}", authUserId, orderByGrade, reqId, stopwatch);
		return s;
	}

	private List<Svg> getSvgs(int idMedia) throws SQLException {
		List<Svg> res = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT p.id problem_id, p.nr, s.id, s.path, s.has_anchor, s.texts, s.anchors FROM svg s, problem p WHERE s.media_id=? AND s.problem_id=p.id")) {
			ps.setInt(1, idMedia);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					if (res == null) {
						res = new ArrayList<>();
					}
					int id = rst.getInt("id");
					int problemId = rst.getInt("problem_id");
					int nr = rst.getInt("nr");
					String path = rst.getString("path");
					boolean hasAnchor = rst.getBoolean("has_anchor");
					String texts = rst.getString("texts");
					String anchors = rst.getString("anchors");
					res.add(new Svg(false, id, problemId, nr, path, hasAnchor, texts, anchors));
				}
			}
		}
		return res;
	}

	private void setRandomMedia(Frontpage res, int authUserId, Setup setup, boolean fallbackSolution) throws SQLException {
		String sqlStr = "SELECT m.id id_media, m.width, m.height, a.id id_area, a.name area, s.id id_sector, s.name sector, p.id id_problem, p.name problem, p.grade,"
				+ " CONCAT('{\"id\":', u.id, ',\"name\":\"', TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))), '\"}') photographer," 
				+ " GROUP_CONCAT(DISTINCT CONCAT('{\"id\":', u2.id, ',\"name\":\"', TRIM(CONCAT(u2.firstname, ' ', COALESCE(u2.lastname,''))), '\"}') SEPARATOR ', ') tagged"
				+ " FROM ((((((((((media m INNER JOIN media_problem mp ON m.is_movie=0 AND m.id=mp.media_id) INNER JOIN problem p ON mp.problem_id=p.id AND p.hidden=0) INNER JOIN sector s ON p.sector_id=s.id AND s.hidden=0) INNER JOIN area a ON s.area_id=a.id AND a.hidden=0) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN user u ON m.photographer_user_id=u.id) INNER JOIN tick t ON p.id=t.problem_id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u2 ON mu.user_id=u2.id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?)"
				+ " WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR auth.user_id IS NOT NULL) AND m.deleted_user_id IS NULL"
				+ " GROUP BY m.id, p.id, p.name, m.photographer_user_id, u.firstname, u.lastname, p.grade"
				+ " HAVING AVG(t.stars)>=2 ORDER BY rand() LIMIT 1";
		if (fallbackSolution) {
			sqlStr = sqlStr.replace("INNER JOIN tick", "LEFT JOIN tick");
			sqlStr = sqlStr.replace("HAVING AVG(t.stars)>=2", "");
		}
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id_media");
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int idArea = rst.getInt("id_area");
					String area = rst.getString("area");
					int idSector = rst.getInt("id_sector");
					String sector = rst.getString("sector");
					int idProblem = rst.getInt("id_problem");
					String problem = rst.getString("problem");
					int grade = rst.getInt("grade");
					String photographerJson = rst.getString("photographer");
					String taggedJson = rst.getString("tagged");
					Frontpage.RandomMedia.User photographer = photographerJson == null? null : gson.fromJson(photographerJson, Frontpage.RandomMedia.User.class);
					List<Frontpage.RandomMedia.User> tagged = taggedJson == null? null : gson.fromJson("[" + taggedJson + "]", new TypeToken<ArrayList<Frontpage.RandomMedia.User>>(){}.getType());
					res.setRandomMedia(idMedia, width, height, idArea, area, idSector, sector, idProblem, problem, GradeHelper.intToString(setup, grade), photographer, tagged);
				}
			}
		}
	}

	private int upsertUserReturnId(String uniqueId) throws SQLException {
		int idUser = 0;
		if (Strings.isNullOrEmpty(uniqueId)) {
			return idUser;
		}
		String sqlStr = "INSERT INTO android_user (unique_id, last_sync) VALUES (?, now()) ON DUPLICATE KEY UPDATE last_sync=now()";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setString(1, uniqueId);
			ps.execute();
		}
		sqlStr = "SELECT user_id FROM android_user au WHERE unique_id=?";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setString(1, uniqueId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					idUser = rst.getInt(1);
				}
			}
		}
		return idUser;
	}
}