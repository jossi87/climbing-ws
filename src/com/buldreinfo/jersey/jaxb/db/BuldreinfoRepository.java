package com.buldreinfo.jersey.jaxb.db;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.imgscalr.Scalr;

import com.buldreinfo.jersey.jaxb.helpers.Auth0Profile;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.helpers.GradeHelper;
import com.buldreinfo.jersey.jaxb.helpers.MarkerHelper;
import com.buldreinfo.jersey.jaxb.helpers.MarkerHelper.LatLng;
import com.buldreinfo.jersey.jaxb.helpers.TimeAgo;
import com.buldreinfo.jersey.jaxb.metadata.MetaHelper;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup.GRADE_SYSTEM;
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
import com.buldreinfo.jersey.jaxb.model.MediaProblem;
import com.buldreinfo.jersey.jaxb.model.MediaSvg;
import com.buldreinfo.jersey.jaxb.model.MediaSvgElement;
import com.buldreinfo.jersey.jaxb.model.NewMedia;
import com.buldreinfo.jersey.jaxb.model.PermissionUser;
import com.buldreinfo.jersey.jaxb.model.Permissions;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Problem.Section;
import com.buldreinfo.jersey.jaxb.model.ProblemHse;
import com.buldreinfo.jersey.jaxb.model.PublicAscent;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.SitesRegion;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.TableOfContents;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Todo;
import com.buldreinfo.jersey.jaxb.model.Type;
import com.buldreinfo.jersey.jaxb.model.TypeNumTicked;
import com.buldreinfo.jersey.jaxb.model.User;
import com.buldreinfo.jersey.jaxb.model.UserMedia;
import com.buldreinfo.jersey.jaxb.model.UserRegion;
import com.buldreinfo.jersey.jaxb.model.app.Region;
import com.buldreinfo.jersey.jaxb.thumbnailcreator.ExifOrientation;
import com.buldreinfo.jersey.jaxb.thumbnailcreator.ThumbnailCreation;
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

import jersey.repackaged.com.google.common.base.Joiner;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
public class BuldreinfoRepository {
	private static final String ACTIVITY_TYPE_FA = "FA";
	private static final String ACTIVITY_TYPE_MEDIA = "MEDIA";
	private static final String ACTIVITY_TYPE_GUESTBOOK = "GUESTBOOK";
	private static final String ACTIVITY_TYPE_TICK = "TICK";
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
			final int idGuestbook = 0;
			addNewMedia(authUserId, p.getId(), m.getPitch(), idSector, idArea, idGuestbook, m, multiPart, now);
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
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM ((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN user_region ur ON (a.region_id=ur.region_id AND ur.user_id=?)) LEFT JOIN media_area ma ON (a.id=ma.area_id AND ma.media_id=?) LEFT JOIN media_sector ms ON (s.id=ms.sector_id AND ms.media_id=?)) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem mp ON (p.id=mp.problem_id AND mp.media_id=?) WHERE ma.media_id IS NOT NULL OR ms.media_id IS NOT NULL OR mp.media_id IS NOT NULL GROUP BY ur.admin_write, ur.superadmin_write")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, id);
			ps.setInt(3, id);
			ps.setInt(4, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
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
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT x.activity_timestamp, x.problem_id, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, p.name problem_name, p.grade, GROUP_CONCAT(concat(x.id,'-',x.type) SEPARATOR ',') activities" + 
				" FROM (((((activity x INNER JOIN problem p ON x.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)" + 
				" WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) " +
				"   AND (r.id=? OR ur.user_id IS NOT NULL)" + 
				"   AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin)=1 AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin)=1 AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1" + 
				(lowerGrade == 0? "" : " AND p.grade>=" + lowerGrade) +
				(fa? "" : " AND x.type!='FA'") +
				(comments? "" : " AND x.type!='GUESTBOOK'") +
				(ticks? "" : " AND x.type!='TICK'") +
				(media? "" : " AND x.type!='MEDIA'") +
				(idArea==0? "" : " AND a.id=" + idArea) +
				(idSector==0? "" : " AND s.id=" + idSector) +
				" GROUP BY x.activity_timestamp, x.problem_id, p.locked_admin, p.locked_superadmin, p.name, p.grade" +
				" ORDER BY -x.activity_timestamp, x.problem_id DESC LIMIT 100")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					Timestamp activityTimestamp = rst.getTimestamp("activity_timestamp");
					int problemId = rst.getInt("problem_id");
					boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
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
					res.add(new Activity(activityIds, timeAgo, problemId, problemLockedAdmin, problemLockedSuperadmin, problemName, grade));
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
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') END picture, g.message, mg.media_id" + 
					" FROM (((activity a INNER JOIN guestbook g ON a.guestbook_id=g.id) INNER JOIN user u ON g.user_id=u.id) LEFT JOIN media_guestbook mg ON g.id=mg.guestbook_id) LEFT JOIN media m ON (mg.media_id=m.id AND m.deleted_user_id IS NULL AND m.is_movie=0)" + 
					" WHERE a.id IN (" + Joiner.on(",").join(guestbookActivitityIds) + ")")) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						int userId = rst.getInt("user_id");
						String name = rst.getString("name");
						String picture = rst.getString("picture");
						String message = rst.getString("message");
						a.setGuestbook(userId, name, picture, message);
						
						int mediaId = rst.getInt("media_id");
						if (mediaId > 0) {
							boolean isMovie = false;
							String embedUrl = null;
							a.addMedia(mediaId, isMovie, embedUrl);
						}
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
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, m.id media_id, m.is_movie, m.embed_url" + 
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
						String embedUrl = rst.getString("embed_url");
						a.addMedia(mediaId, isMovie, embedUrl);
					}
				}
			}
		}
		logger.debug("getActivity(authUserId={}, setup={}) - res.size()={}, duration={}", authUserId, setup, res.size(), stopwatch);
		return res;
	}

	public Area getArea(Setup s, int authUserId, int reqId) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE area SET hits=hits+1 WHERE id=?")) {
			ps.setInt(1, reqId);
			ps.execute();
		}
		MarkerHelper markerHelper = new MarkerHelper();
		Area a = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.id region_id, CONCAT(r.url,'/area/',a.id) canonical, a.locked_admin, a.locked_superadmin, a.for_developers, a.name, a.description, a.latitude, a.longitude, a.hits FROM (area a INNER JOIN region r ON a.region_id=r.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE a.id=? AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin)=1 GROUP BY r.id, r.url, a.locked_admin, a.locked_superadmin, a.for_developers, a.name, a.description, a.latitude, a.longitude, a.hits")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, reqId);
			ps.setInt(3, s.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int regionId = rst.getInt("region_id");
					String canonical = rst.getString("canonical");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					boolean forDevelopers = rst.getBoolean("for_developers");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					LatLng l = markerHelper.getLatLng(rst.getDouble("latitude"), rst.getDouble("longitude"));
					int hits = rst.getInt("hits");
					boolean inherited = false;
					List<Media> media = getMediaArea(reqId, inherited);
					if (media.isEmpty()) {
						media = null;
					}
					a = new Area(regionId, canonical, reqId, lockedAdmin, lockedSuperadmin, forDevelopers, name, comment, l.getLat(), l.getLng(), -1, -1, media, null, hits);
				}
			}
		}
		Preconditions.checkNotNull(a, "Could not find area with id=" + reqId);
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT s.id, s.sorting, s.locked_admin, s.locked_superadmin, s.name, s.description, s.parking_latitude, s.parking_longitude, s.polygon_coords, s.polyline, MAX(m.id) media_id FROM ((((area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) LEFT JOIN problem p ON s.id=p.sector_id AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1) LEFT JOIN media_problem mp ON p.id=mp.problem_id) LEFT JOIN media m ON mp.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL WHERE a.id=? AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin)=1 GROUP BY s.id, s.sorting, s.locked_admin, s.locked_superadmin, s.name, s.description, s.parking_latitude, s.parking_longitude, s.polygon_coords, s.polyline ORDER BY s.name")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, reqId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int sorting = rst.getInt("sorting");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					LatLng l = markerHelper.getLatLng(rst.getDouble("parking_latitude"), rst.getDouble("parking_longitude"));
					String polygonCoords = rst.getString("polygon_coords");
					String polyline = rst.getString("polyline");
					int randomMediaId = rst.getInt("media_id");
					if (randomMediaId == 0) {
						boolean inherited = false;
						List<Media> x = getMediaSector(s, authUserId, id, 0, inherited, false);
						if (!x.isEmpty()) {
							randomMediaId = x.get(0).getId();
						}
					}
					a.addSector(id, sorting, lockedAdmin, lockedSuperadmin, name, comment, l.getLat(), l.getLng(), polygonCoords, polyline, randomMediaId);
				}
			}
		}
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT s.id, CASE WHEN p.grade IS NULL OR p.grade=0 THEN 'Projects' ELSE CONCAT(ty.type, 's', CASE WHEN ty.subtype IS NOT NULL THEN CONCAT(' (',ty.subtype,')') ELSE '' END) END type, COUNT(DISTINCT p.id) num, COUNT(DISTINCT CASE WHEN f.problem_id IS NOT NULL OR t.id IS NOT NULL THEN p.id END) num_ticked FROM (((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN fa f ON p.id=f.problem_id AND f.user_id=?) LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE a.id=? AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin)=1 AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1 GROUP BY s.id, CASE WHEN p.grade IS NULL OR p.grade=0 THEN 'Projects' ELSE CONCAT(ty.type, 's', CASE WHEN ty.subtype IS NOT NULL THEN CONCAT(' (',ty.subtype,')') ELSE '' END) END ORDER BY s.id, CASE WHEN p.grade IS NULL OR p.grade=0 THEN 'Projects' ELSE CONCAT(ty.type, 's', CASE WHEN ty.subtype IS NOT NULL THEN CONCAT(' (',ty.subtype,')') ELSE '' END) END")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, authUserId);
			ps.setInt(3, authUserId);
			ps.setInt(4, reqId);
			Map<String, TypeNumTicked> lookup = new LinkedHashMap<>();
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int sectorId = rst.getInt("id");
					String type = rst.getString("type");
					int num = rst.getInt("num");
					int numTicked = rst.getInt("num_ticked");
					TypeNumTicked typeNumTicked = new TypeNumTicked(type, num, numTicked);
					// Sector
					Optional<Area.Sector> optSector = a.getSectors().stream().filter(x -> x.getId() == sectorId).findAny();
					if (optSector.isPresent()) {
						optSector.get().getTypeNumTicked().add(typeNumTicked);
					}
					// Area
					TypeNumTicked areaTnt = lookup.get(type);
					if (areaTnt == null) {
						areaTnt = new TypeNumTicked(type, num, numTicked);
						a.getTypeNumTicked().add(areaTnt);
						lookup.put(type, areaTnt);
					}
					else {
						areaTnt.addNum(num);
						areaTnt.addTicked(numTicked);
					}
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
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.id region_id, CONCAT(r.url,'/area/',a.id) canonical, a.id, a.locked_admin, a.locked_superadmin, a.for_developers, a.name, a.description, a.latitude, a.longitude, COUNT(DISTINCT s.id) num_sectors, COUNT(DISTINCT p.id) num_problems, a.hits FROM ((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin)=1 GROUP BY r.id, r.url, a.id, a.locked_admin, a.locked_superadmin, a.for_developers, a.name, a.description, a.latitude, a.longitude, a.hits ORDER BY a.name")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, reqIdRegion);
			ps.setInt(3, reqIdRegion);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("region_id");
					String canonical = rst.getString("canonical");
					int id = rst.getInt("id");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
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
					res.add(new Area(idRegion, canonical, id, lockedAdmin, lockedSuperadmin, forDevelopers, name, comment, l.getLat(), l.getLng(), numSectors, numProblems, null, null, hits));
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
				if (downloadUserImage(authUserId, profile.getPicture())) {
					try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE user SET picture=? WHERE id=?")) {
						ps.setString(1, profile.getPicture());
						ps.setInt(2, authUserId);
						ps.executeUpdate();
					}
				}
			}
		}
		logger.debug("getAuthUserId(profile={}) - authUserId={}", profile, authUserId);
		return authUserId;
	}

	public Redirect getCanonicalUrl(int idArea, int idSector, int idProblem) throws SQLException {
		String sqlStr = null;
		int id = 0;
		if (idArea > 0) {
			sqlStr = "SELECT CONCAT(r.url,'/area/',a.id) url FROM region r, area a WHERE r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=?";
			id = idArea;
		}
		else if (idSector > 0) {
			sqlStr = "SELECT CONCAT(r.url,'/sector/',s.id) url FROM region r, area a, sector s WHERE r.id=a.region_id AND a.id=s.area_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=?";
			id = idSector;
		}
		else if (idProblem > 0) {
			sqlStr = "SELECT CONCAT(r.url,'/problem/',p.id) url FROM region r, area a, sector s, problem p WHERE r.id=a.region_id AND a.id=s.area_id AND s.id=p.sector_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND s.locked_admin=0 AND s.locked_superadmin=0 AND p.locked_admin=0 AND p.locked_superadmin=0 AND p.id=?";
			id = idProblem;
		}
		Preconditions.checkArgument(id > 0 && sqlStr != null, "Invalid parameters: idArea=" + idArea + ", idSector=" + idSector + ", idProblem=" + idProblem);
		Redirect res = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res = new Redirect(rst.getString("url"));
				}
			}
		}
		Preconditions.checkNotNull(res, "Could not find canonical url for idArea=" + idArea + ", idSector=" + idSector + ", idProblem=" + idProblem);
		return res;
	}

	public List<Filter> getFilter(int authUserId, Setup setup, FilterRequest fr) throws SQLException {
		List<Filter> res = new ArrayList<>();
		String sqlStr = "SELECT a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, p.id problem_id, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, p.name problem_name, coalesce(p.latitude,coalesce(s.parking_latitude,a.latitude)) latitude, coalesce(p.longitude,coalesce(s.parking_longitude,a.longitude)) longitude, ROUND(ROUND(AVG(t.stars)*2)/2,1) stars, p.grade, MAX(m.id) media_id, MAX(CASE WHEN t.user_id=? THEN 1 ELSE 0 END) ticked"
				+ " FROM (((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN media_problem mp ON p.id=mp.problem_id) LEFT JOIN media m ON mp.media_id=m.id AND m.deleted_user_id IS NULL) LEFT JOIN tick t ON p.id=t.problem_id"
				+ " WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)"
				+ "   AND (r.id=? OR ur.user_id IS NOT NULL)"
				+ "   AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin)=1"
				+ "   AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin)=1"
				+ "   AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1"
				+ "   AND p.grade IN (" + Joiner.on(",").join(fr.getGrades()) + ")"
				+ "   AND p.type_id IN (" + Joiner.on(",").join(fr.getTypes()) + ")"
				+ "   GROUP BY a.id, a.name, a.locked_admin, a.locked_superadmin, s.id, s.name, s.locked_admin, s.locked_superadmin, p.id, p.locked_admin, p.locked_superadmin, p.name, p.latitude, p.longitude, s.parking_latitude, s.parking_longitude, a.latitude, a.longitude"
				+ "   ORDER BY p.name, p.latitude, p.longitude, p.grade";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, authUserId);
			ps.setInt(3, setup.getIdRegion());
			ps.setInt(4, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String sectorName = rst.getString("sector_name");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					int problemId = rst.getInt("problem_id");
					String problemName = rst.getString("problem_name");
					boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					double stars = rst.getDouble("stars");
					int grade = rst.getInt("grade");
					int mediaId = rst.getInt("media_id");
					boolean ticked = rst.getBoolean("ticked");
					res.add(new Filter(areaLockedAdmin, areaLockedSuperadmin, areaName, sectorLockedAdmin, sectorLockedSuperadmin, sectorName, problemId, problemLockedAdmin, problemLockedSuperadmin, problemName, latitude, longitude, stars, GradeHelper.intToString(setup, grade), ticked, mediaId));
				}
			}
		}
		logger.debug("getFilter(authUserId={}, idRegion={}, fr={}) - res.size()={}", authUserId, setup.getIdRegion(), fr, res.size());
		return res;
	}

	public Frontpage getFrontpage(int authUserId, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Frontpage res = new Frontpage();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT p.id) num_problems, COUNT(DISTINCT CASE WHEN p.latitude IS NOT NULL AND p.longitude IS NOT NULL THEN p.id END) num_problems_with_coordinates, COUNT(DISTINCT svg.problem_id) num_problems_with_topo FROM (((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)) LEFT JOIN svg ON p.id=svg.problem_id WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL)")) {
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
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT CASE WHEN m.is_movie=0 THEN mp.id END) num_images, COUNT(DISTINCT CASE WHEN m.is_movie=1 THEN mp.id END) num_movies FROM ((((((media m INNER JOIN media_problem mp ON m.id=mp.media_id) INNER JOIN problem p ON mp.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND m.deleted_user_id IS NULL AND (a.region_id=? OR ur.user_id IS NOT NULL)")) {
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
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT t.id) num_ticks FROM (((((tick t INNER JOIN problem p ON t.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL)")) {
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
		Map<String, GradeDistribution> res = new LinkedHashMap<>();
		String sqlStr = "WITH x AS ("
				+ "  SELECT g.base_no grade_base_no, x.sorting, x.sector, x.t, COUNT(id_problem) num"
				+ "  FROM (SELECT s.name sector, s.sorting, ty.subtype t, ROUND((IFNULL(AVG(NULLIF(t.grade,0)), p.grade) + p.grade)/2) grade_id, p.id id_problem"
				+ "    FROM ((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) LEFT JOIN tick t ON p.id=t.problem_id AND t.grade>0"
				+ (optionalAreaId!=0? " WHERE a.id=?" : " WHERE p.sector_id=?")
				+ "      AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1"
				+ "    GROUP BY s.name, ty.subtype, p.id) x, grade g"
				+ "  WHERE x.grade_id=g.grade_id AND g.t=?"
				+ "  GROUP BY x.sorting, x.sector, g.base_no, x.t"
				+ ")"
				+ " SELECT g.base_no grade, x.sector, COALESCE(x.t,'Boulder') t, num"
				+ " FROM (SELECT g.base_no, MIN(g.grade_id) sort FROM grade g WHERE g.t=? GROUP BY g.base_no) g LEFT JOIN x ON g.base_no=x.grade_base_no"
				+ " ORDER BY g.sort, x.sorting, x.sector, x.t";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, optionalAreaId!=0? optionalAreaId : optionalSectorId);
			ps.setString(3, setup.getGradeSystem().toString());
			ps.setString(4, setup.getGradeSystem().toString());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String grade = rst.getString("grade");
					GradeDistribution g = res.get(grade);
					if (g == null) {
						g = new GradeDistribution(grade);
						res.put(grade, g);
					}
					String sector = rst.getString("sector");
					if (sector != null) {
						String t = rst.getString("t");
						int num = rst.getInt("num");
						g.addSector(sector, t, num);
					}
				}
			}
		}
		return res.values();
	}

	public Path getImage(boolean webP, int id) throws SQLException, IOException {
		Path p = null;
		if (webP) {
			p = GlobalFunctions.getPathMediaWebWebp().resolve(String.valueOf(id / 100 * 100)).resolve(id + ".webp");
		} else {
			p = GlobalFunctions.getPathMediaWebJpg().resolve(String.valueOf(id / 100 * 100)).resolve(id + ".jpg");
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

	public MediaSvg getMediaSvg(int id) throws SQLException {
		MediaSvg res = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.description, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM ((media m INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE m.id=?")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					String description = rst.getString("description");
					int pitch = 0;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(idMedia);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description);
					Media m = new Media(idMedia, pitch, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl, false);
					res = new MediaSvg(m);
				}
			}
		}
		return res;
	}

	public Permissions getPermissions(int authUserId, int idRegion) throws SQLException {
		ensureSuperadminWriteRegion(authUserId, idRegion);
		// Return users
		Permissions res = new Permissions();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT x.id, x.name, x.picture, DATE_FORMAT(MAX(x.last_login),'%Y.%m.%d') last_login, x.admin_read, x.admin_write, x.superadmin_read, x.superadmin_write FROM (SELECT u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, MAX(l.when) last_login, ur.admin_read, ur.admin_write, ur.superadmin_read, ur.superadmin_write FROM (user u INNER JOIN user_login l ON u.id=l.user_id) LEFT JOIN user_region ur ON u.id=ur.user_id AND l.region_id=ur.region_id WHERE l.region_id=? GROUP BY u.id, u.firstname, u.lastname, u.picture UNION SELECT u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, MAX(l.when) last_login, ur.admin_read, ur.admin_write, ur.superadmin_read, ur.superadmin_write FROM user u, user_region ur, user_login l WHERE u.id=ur.user_id AND ur.region_id=? AND u.id=l.user_id GROUP BY u.id, u.firstname, u.lastname, u.picture) x GROUP BY x.id, x.name, x.picture, x.admin_read, x.admin_write, x.superadmin_read, x.superadmin_write ORDER BY IFNULL(x.superadmin_write,0) DESC, IFNULL(x.superadmin_read,0) DESC, IFNULL(x.admin_write,0) DESC, IFNULL(x.admin_read,0) DESC, x.name")) {
			ps.setInt(1, idRegion);
			ps.setInt(2, idRegion);
			try (ResultSet rst = ps.executeQuery()) {
				final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
				while (rst.next()) {
					int userId = rst.getInt("id");
					String name = rst.getString("name");
					String picture = rst.getString("picture");
					String lastLogin = rst.getString("last_login");
					boolean adminRead = rst.getBoolean("admin_read");
					boolean adminWrite = rst.getBoolean("admin_write");
					boolean superadminRead = rst.getBoolean("superadmin_read");
					boolean superadminWrite = rst.getBoolean("superadmin_write");
					String timeAgo = TimeAgo.getTimeAgo(LocalDate.parse(lastLogin, formatter));
					res.getUsers().add(new PermissionUser(userId, name, picture, timeAgo, adminRead, adminWrite, superadminRead, superadminWrite, authUserId==userId));
				}
			}
		}
		return res;
	}

	public Problem getProblem(int authUserId, Setup s, int reqId, boolean showHiddenMedia) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE problem SET hits=hits+1 WHERE id=?")) {
			ps.setInt(1, reqId);
			ps.execute();
		}
		List<Integer> todoIdProblems = new ArrayList<>();
		Todo todo = getTodo(authUserId, s, authUserId);
		if (todo != null) {
			for (Todo.Area ta : todo.getAreas()) {
				for (Todo.Sector ts : ta.getSectors()) {
					for (Todo.Problem tp : ts.getProblems()) {
						todoIdProblems.add(tp.getId());
					}
				}
			}
		}
		MarkerHelper markerHelper = new MarkerHelper();
		Problem p = null;
		String sqlStr = "SELECT a.id area_id, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.name area_name, s.id sector_id, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, s.name sector_name, s.parking_latitude sector_lat, s.parking_longitude sector_lng, s.polygon_coords sector_polygon_coords, s.polyline sector_polyline, CONCAT(r.url,'/problem/',p.id) canonical, p.id, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, p.hits, DATE_FORMAT(p.fa_date,'%Y-%m-%d') fa_date, DATE_FORMAT(p.fa_date,'%d/%m-%y') fa_date_hr,"
				+ " ROUND((IFNULL(AVG(NULLIF(t.grade,0)), p.grade) + p.grade)/2) grade, p.grade original_grade, p.latitude, p.longitude,"
				+ " group_concat(DISTINCT CONCAT('{\"id\":', u.id, ',\"name\":\"', TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))), '\",\"picture\":\"', CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END, '\"}') ORDER BY u.firstname, u.lastname SEPARATOR ',') fa,"
				+ " COUNT(DISTINCT t.id) num_ticks, ROUND(ROUND(AVG(t.stars)*2)/2,1) stars,"
				+ " MAX(CASE WHEN (t.user_id=? OR u.id=?) THEN 1 END) ticked, ty.id type_id, ty.type, ty.subtype,"
				+ " p.trivia, p.starting_altitude, p.aspect, p.route_length, p.descent"
				+ " FROM ((((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN tick t ON t.problem_id=p.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?"
				+ " WHERE (?=0 OR rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?))"
				+ "   AND p.id=?"
				+ "   AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1"
				+ "   AND (?=0 OR r.id=? OR ur.user_id IS NOT NULL)"
				+ " GROUP BY r.url, a.id, a.locked_admin, a.locked_superadmin, a.name, s.id, s.locked_admin, s.locked_superadmin, s.name, s.parking_latitude, s.parking_longitude, s.polygon_coords, s.polyline, p.id, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, p.hits, p.grade, p.latitude, p.longitude, p.fa_date, ty.id, ty.type, ty.subtype, p.trivia, p.starting_altitude, p.aspect, p.route_length, p.descent"
				+ " ORDER BY p.name";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, authUserId);
			ps.setInt(3, authUserId);
			ps.setInt(4, s.getIdRegion());
			ps.setInt(5, s.getIdRegion());
			ps.setInt(6, reqId);
			ps.setInt(7, s.getIdRegion());
			ps.setInt(8, s.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String areaName = rst.getString("area_name");
					int sectorId = rst.getInt("sector_id");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					String sectorName = rst.getString("sector_name");
					LatLng sectorL = markerHelper.getLatLng(rst.getDouble("sector_lat"), rst.getDouble("sector_lng"));
					String sectorPolygonCoords = rst.getString("sector_polygon_coords");
					String sectorPolyline = rst.getString("sector_polyline");
					String canonical = rst.getString("canonical");
					int id = rst.getInt("id");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int nr = rst.getInt("nr");
					int grade = rst.getInt("grade");
					int originalGrade = rst.getInt("original_grade");
					String faDate = rst.getString("fa_date");
					String faDateHr = rst.getString("fa_date_hr");
					String name = rst.getString("name");
					String rock = rst.getString("rock");
					String comment = rst.getString("description");
					String faStr = rst.getString("fa");
					List<FaUser> fa = Strings.isNullOrEmpty(faStr) ? null : gson.fromJson("[" + faStr + "]", new TypeToken<ArrayList<FaUser>>(){}.getType());
					LatLng l = markerHelper.getLatLng(rst.getDouble("latitude"), rst.getDouble("longitude"));
					int numTicks = rst.getInt("num_ticks");
					double stars = rst.getDouble("stars");
					boolean ticked = rst.getBoolean("ticked");
					boolean inherited = false;
					List<Media> media = getMediaProblem(s, authUserId, sectorId, id, inherited, showHiddenMedia);
					Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
					int hits = rst.getInt("hits");
					String trivia = rst.getString("trivia");
					String startingAltitude = rst.getString("starting_altitude");
					String aspect = rst.getString("aspect");
					String routeLength = rst.getString("route_length");
					String descent = rst.getString("descent");

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

					p = new Problem(areaId, areaLockedAdmin, areaLockedSuperadmin, areaName, sectorId, sectorLockedAdmin, sectorLockedSuperadmin, sectorName,
							sectorL.getLat(), sectorL.getLng(), sectorPolygonCoords, sectorPolyline,
							sectorIdProblemPrev, sectorIdProblemNext,
							canonical, id, lockedAdmin, lockedSuperadmin, nr, name, rock, comment,
							GradeHelper.intToString(s, grade),
							GradeHelper.intToString(s, originalGrade), faDate, faDateHr, fa, l.getLat(),
							l.getLng(), media, numTicks, stars, ticked, null, t, todoIdProblems.contains(id), hits,
							trivia, startingAltitude, aspect, routeLength, descent);
				}
			}
		}
		Preconditions.checkNotNull(p, "Could not find problem with id=" + reqId);
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
				Problem.Comment lastComment = null;
				while (rst.next()) {
					int id = rst.getInt("id");
					String date = rst.getString("date");
					int idUser = rst.getInt("user_id");
					String picture = rst.getString("picture");
					String name = rst.getString("name");
					String message = rst.getString("message");
					boolean danger = rst.getBoolean("danger");
					boolean resolved = rst.getBoolean("resolved");
					List<Media> media = getMediaGuestbook(id);
					lastComment = p.addComment(id, date, idUser, picture, name, message, danger, resolved, media);
				}
				// Enable editing on last comment in thread if it is written by authenticated user
				if (lastComment != null && lastComment.getIdUser() == authUserId) {
					lastComment.setEditable(true);
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

	public ProblemHse getProblemsHse(int authUserId, Setup setup) throws SQLException {
		ProblemHse res = new ProblemHse();
		Map<Integer, ProblemHse.Area> areaLookup = new HashMap<>();
		Map<Integer, ProblemHse.Sector> sectorLookup = new HashMap<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id area_id, CONCAT(r.url,'/area/',a.id) area_url, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.id sector_id, CONCAT(r.url,'/sector/',s.id) sector_url, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, p.id problem_id, CONCAT(r.url,'/problem/',p.id) problem_url, p.nr problem_nr, p.grade problem_grade, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, DATE_FORMAT(g.post_time,'%Y.%m.%d') post_time, g.message FROM ((((((area a INNER JOIN region r ON r.id=a.region_id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN guestbook g ON p.id=g.problem_id AND g.danger=1 AND g.id IN (SELECT MAX(id) id FROM guestbook WHERE danger=1 OR resolved=1 GROUP BY problem_id)) INNER JOIN user u ON g.user_id=u.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1 GROUP BY a.id, a.name, a.locked_admin, a.locked_superadmin, s.id, s.name, s.locked_admin, s.locked_superadmin, p.id, p.nr, p.grade, p.name, p.locked_admin, p.locked_superadmin, u.firstname, u.lastname, g.post_time, g.message ORDER BY a.name, s.name, p.nr")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Area
					int areaId = rst.getInt("area_id");
					ProblemHse.Area a = areaLookup.get(areaId);
					if (a == null) {
						String areaUrl = rst.getString("area_url");
						String areaName = rst.getString("area_name");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						a = res.addArea(areaId, areaUrl, areaName, areaLockedAdmin, areaLockedSuperadmin);
						areaLookup.put(areaId, a);
					}
					// Sector
					int sectorId = rst.getInt("sector_id");
					ProblemHse.Sector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorUrl = rst.getString("sector_url");
						String sectorName = rst.getString("sector_name");
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						s = a.addSector(sectorId, sectorUrl, sectorName, sectorLockedAdmin, sectorLockedSuperadmin);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int id = rst.getInt("problem_id");
					String url = rst.getString("problem_url");
					int nr = rst.getInt("problem_nr");
					int grade = rst.getInt("problem_grade");
					boolean lockedAdmin = rst.getBoolean("problem_locked_admin"); 
					boolean lockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					String name = rst.getString("problem_name");
					String postBy = rst.getString("name");
					String postWhen = rst.getString("post_time");
					String postTxt = rst.getString("message");
					s.addProblem(id, url, lockedAdmin, lockedSuperadmin, nr, name, GradeHelper.intToString(setup, grade), postBy, postWhen, postTxt);
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
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.region_id, a.id, a.name, a.description, a.latitude, a.longitude FROM ((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE " + regionTypeFilter + " AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin)=1 GROUP BY a.region_id, a.id, a.name, a.description, a.latitude, a.longitude")) {
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
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT s.area_id, s.id, s.name, s.description, s.parking_latitude, s.parking_longitude FROM (((sector s INNER JOIN area a ON a.id=s.area_id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE " + regionTypeFilter + " AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin)=1 GROUP BY s.area_id, s.id, s.name, s.description, s.parking_latitude, s.parking_longitude")) {
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
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT p.sector_id, p.id, p.nr, p.name, p.description, p.grade, TRIM(CONCAT(IFNULL(p.fa_date,''), ' ', GROUP_CONCAT(DISTINCT CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) ORDER BY u.firstname SEPARATOR ', '))) fa, p.latitude, p.longitude FROM ((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE " + regionTypeFilter + " AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1 GROUP BY p.sector_id, p.id, p.nr, p.name, p.description, p.grade, p.fa_date, p.latitude, p.longitude")) {
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
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ms.sector_id, m.id, m.is_movie FROM (((((media m INNER JOIN media_sector ms ON m.id=ms.media_id AND m.deleted_user_id IS NULL AND m.embed_url IS NULL) INNER JOIN sector s ON ms.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE " + regionTypeFilter + " AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin)=1 GROUP BY ms.sector_id, m.id, m.is_movie ORDER BY m.is_movie, m.id")) {
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
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT mp.problem_id, m.id, m.is_movie, mp.milliseconds t FROM ((((((media m INNER JOIN media_problem mp ON m.id=mp.media_id AND m.deleted_user_id IS NULL AND m.embed_url IS NULL) INNER JOIN problem p ON mp.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE " + regionTypeFilter + " AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1 GROUP BY mp.problem_id, m.id, m.is_movie, mp.milliseconds ORDER BY m.is_movie, m.id")) {
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
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, a.name, a.locked_admin, a.locked_superadmin, MAX(m.id) media_id FROM ((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN media_area ma ON a.id=ma.area_id) LEFT JOIN media m ON ma.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND (a.name LIKE ? OR a.name LIKE ?) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin)=1 GROUP BY a.id, a.name, a.locked_admin, a.locked_superadmin ORDER BY a.name LIMIT 8")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			ps.setString(4, sr.getValue() + "%");
			ps.setString(5, "% " + sr.getValue() + "%");
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String name = rst.getString("name");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int mediaId = rst.getInt("media_id");
					areas.add(new Search(name, null, "/area/" + id, null, mediaId, lockedAdmin, lockedSuperadmin));
				}
			}
		}
		// Sectors
		List<Search> sectors = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT s.id, a.name area_name, s.name sector_name, s.locked_admin, s.locked_superadmin, MAX(m.id) media_id FROM (((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN media_sector ms ON s.id=ms.sector_id) LEFT JOIN media m ON ms.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND (s.name LIKE ? OR s.name LIKE ?) AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin)=1 GROUP BY s.id, a.name, s.name, s.locked_admin, s.locked_superadmin ORDER BY a.name, s.name LIMIT 8")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			ps.setString(4, sr.getValue() + "%");
			ps.setString(5, "% " + sr.getValue() + "%");
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String areaName = rst.getString("area_name");
					String sectorName = rst.getString("sector_name");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int mediaId = rst.getInt("media_id");
					sectors.add(new Search(sectorName, areaName, "/sector/" + id, null, mediaId, lockedAdmin, lockedSuperadmin));
				}
			}
		}
		// Problems
		List<Search> problems = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.name area_name, s.name sector_name, p.id, p.name, p.rock, p.grade, p.locked_admin, p.locked_superadmin, MAX(m.id) media_id FROM ((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN media_problem mp ON p.id=mp.problem_id) LEFT JOIN media m ON mp.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND (p.name LIKE ? OR p.name LIKE ? OR p.rock LIKE ? OR p.rock LIKE ?) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1 GROUP BY a.name, s.name, p.id, p.name, p.rock, p.grade, p.locked_admin, p.locked_superadmin ORDER BY p.name, p.grade LIMIT 8")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			ps.setString(4, sr.getValue() + "%");
			ps.setString(5, "% " + sr.getValue() + "%");
			ps.setString(6, sr.getValue() + "%");
			ps.setString(7, "% " + sr.getValue() + "%");
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String areaName = rst.getString("area_name");
					String sectorName = rst.getString("sector_name");
					int id = rst.getInt("id");
					String name = rst.getString("name");
					String rock = rst.getString("rock");
					int grade = rst.getInt("grade");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int mediaId = rst.getInt("media_id");
					problems.add(new Search(name + " [" + GradeHelper.intToString(setup, grade) + "]", areaName + " / " + sectorName + (rock == null? "" : " (rock: " + rock + ")"), "/problem/" + id, null, mediaId, lockedAdmin, lockedSuperadmin));
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
					users.add(new Search(name, null, "/user/" + id, picture, 0, false, false));
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
		urls.add(setup.getUrl("/toc"));
		// Users
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT f.user_id FROM area a, sector s, problem p, fa f WHERE a.region_id=? AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=p.sector_id AND p.locked_admin=0 AND p.locked_superadmin=0 AND p.id=f.problem_id GROUP BY f.user_id UNION SELECT t.user_id FROM area a, sector s, problem p, tick t WHERE a.region_id=? AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=p.sector_id AND p.locked_admin=0 AND p.locked_superadmin=0 AND p.id=t.problem_id GROUP BY t.user_id")) {
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
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT CONCAT('/area/', a.id) url FROM region r, area a WHERE r.id=? AND r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 UNION SELECT CONCAT('/sector/', s.id) url FROM region r, area a, sector s WHERE r.id=? AND r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 UNION SELECT CONCAT('/problem/', p.id) url FROM region r, area a, sector s, problem p WHERE r.id=? AND r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=p.sector_id AND p.locked_admin=0 AND p.locked_superadmin=0")) {
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

	public List<SitesRegion> getSites(GRADE_SYSTEM system) throws SQLException {
		List<SitesRegion> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.name, r.url, r.polygon_coords, COUNT(p.id) num_problems FROM (((region_type rt INNER JOIN region r ON rt.type_id=? AND rt.region_id=r.id) LEFT JOIN area a ON r.id=a.region_id) LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id GROUP BY r.name, r.url, r.polygon_coords")) {
			int type = 1;
			if (system.equals(GRADE_SYSTEM.BOULDER)) {
				type = 1;
			}
			else if (system.equals(GRADE_SYSTEM.CLIMBING)) {
				type = 2;
			}
			else if (system.equals(GRADE_SYSTEM.ICE)) {
				type = 10;
			}
			else {
				throw new RuntimeException("Invalid system: " + system);
			}
			ps.setInt(1, type);
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

	public TableOfContents getTableOfContents(int authUserId, Setup setup) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		TableOfContents toc = new TableOfContents();
		Map<Integer, TableOfContents.Area> areaLookup = new HashMap<>();
		Map<Integer, TableOfContents.Sector> sectorLookup = new HashMap<>();
		String sqlStr = "SELECT a.id area_id, CONCAT(r.url,'/area/',a.id) area_url, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.id sector_id, CONCAT(r.url,'/sector/',s.id) sector_url, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, p.id, CONCAT(r.url,'/problem/',p.id) url, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.description, ROUND((IFNULL(AVG(NULLIF(t.grade,0)), p.grade) + p.grade)/2) grade,"
				+ " group_concat(DISTINCT CONCAT(TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') fa,"
				+ " COUNT(DISTINCT t.id) num_ticks, ROUND(ROUND(AVG(t.stars)*2)/2,1) stars,"
				+ " MAX(CASE WHEN (t.user_id=? OR u.id=?) THEN 1 END) ticked, ty.id type_id, ty.type, ty.subtype"
				+ " FROM ((((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN tick t ON p.id=t.problem_id"
				+ " WHERE (a.region_id=? OR ur.user_id IS NOT NULL)"
				+ " AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin)=1"
				+ " AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin)=1"
				+ " AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1"
				+ " GROUP BY r.url, a.id, a.name, a.locked_admin, a.locked_superadmin, s.sorting, s.id, s.name, s.locked_admin, s.locked_superadmin, p.id, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.description, p.grade, ty.id, ty.type, ty.subtype"
				+ " ORDER BY a.name, s.sorting, s.name, p.nr";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, authUserId);
			ps.setInt(3, setup.getIdRegion());
			ps.setInt(4, authUserId);
			ps.setInt(5, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Area
					int areaId = rst.getInt("area_id");
					TableOfContents.Area a = areaLookup.get(areaId);
					if (a == null) {
						String areaUrl = rst.getString("area_url");
						String areaName = rst.getString("area_name");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						a = toc.addArea(areaId, areaUrl, areaName, areaLockedAdmin, areaLockedSuperadmin);
						areaLookup.put(areaId, a);
					}
					// Sector
					int sectorId = rst.getInt("sector_id");
					TableOfContents.Sector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorUrl = rst.getString("sector_url");
						String sectorName = rst.getString("sector_name");
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						s = a.addSector(sectorId, sectorUrl, sectorName, sectorLockedAdmin, sectorLockedSuperadmin);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int id = rst.getInt("id");
					String url = rst.getString("url");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int nr = rst.getInt("nr");
					String name = rst.getString("name");
					String description = rst.getString("description");
					int grade = rst.getInt("grade");
					String fa = rst.getString("fa");
					int numTicks = rst.getInt("num_ticks");
					double stars = rst.getDouble("stars");
					boolean ticked = rst.getBoolean("ticked");
					Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
					s.addProblem(id, url, lockedAdmin, lockedSuperadmin, nr, name, description, GradeHelper.intToString(setup, grade), fa, numTicks, stars, ticked, t);
				}
			}
		}
		// Sort areas (ae, oe, aa is sorted wrong by MySql):
		toc.getAreas().sort(Comparator.comparing(TableOfContents.Area::getName));
		logger.debug("getProblemList(authUserId={}, setup={}) - toc={} - duration={}", authUserId, setup, toc, stopwatch);
		return toc;
	}

	public Ticks getTicks(int authUserId, Setup setup, int page) throws SQLException {
		final int take = 200;
		int numTicks = 0;
		int skip = (page-1)*take;
		String sqlStr = "SELECT a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, p.id problem_id, t.grade problem_grade, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, DATE_FORMAT(t.date,'%Y.%m.%d') ts, TRIM(CONCAT(u.firstname, ' ', IFNULL(u.lastname,''))) name"
				+ " FROM ((((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN tick t ON p.id=t.problem_id) INNER JOIN user u ON t.user_id=u.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?"
				+ "  WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)"
				+ "    AND (r.id=? OR ur.user_id IS NOT NULL)"
				+ "    AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin)=1"
				+ "    AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin)=1"
				+ "    AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1"
				+ " GROUP BY a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, p.id, t.grade, p.name, p.locked_admin, p.locked_superadmin, t.date, u.firstname, u.lastname"
				+ " ORDER BY t.date DESC, problem_name, name";
		List<PublicAscent> ticks = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					numTicks++;
					if ((numTicks-1) < skip || ticks.size() == take) {
						continue;
					}
					String areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String sectorName = rst.getString("sector_name");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					int problemId = rst.getInt("problem_id");
					int problemGrade = rst.getInt("problem_grade");
					String problemName = rst.getString("problem_name");
					boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					String date = rst.getString("ts");
					String name = rst.getString("name");
					ticks.add(new PublicAscent(areaName, areaLockedAdmin, areaLockedSuperadmin, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, problemId, GradeHelper.intToString(setup, problemGrade), problemName, problemLockedAdmin, problemLockedSuperadmin, date, name));
				}
			}
		}
		int numPages = (int)(Math.ceil(numTicks / 200f));
		Ticks res = new Ticks(ticks, page, numPages);
		logger.debug("getTicks(authUserId={}, idRegion={}, page={}) - res={}", authUserId, setup.getIdRegion(), page, res);
		return res;
	}

	public Todo getTodo(int authUserId, Setup setup, int reqId) throws SQLException {
		MarkerHelper markerHelper = new MarkerHelper();
		final int userId = reqId > 0? reqId : authUserId;
		Todo res = null;
		String sqlStr = "SELECT u.id, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name FROM user u WHERE u.id=?";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, userId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String picture = rst.getString("picture");
					String name = rst.getString("name");
					res = new Todo(id, name, picture);
				}
			}
		}
		if (res == null) {
			return null;
		}

		// Build lists
		Map<Integer, Todo.Area> areaLookup = new HashMap<>();
		Map<Integer, Todo.Sector> sectorLookup = new HashMap<>();
		Map<Integer, Todo.Problem> problemLookup = new HashMap<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id area_id, CONCAT(r.url,'/area/',a.id) area_url, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.id sector_id, CONCAT(r.url,'/sector/',s.id) sector_url, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, t.id todo_id, p.id problem_id, CONCAT(r.url,'/problem/',p.id) problem_url, p.nr problem_nr, p.name problem_name, p.grade problem_grade, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, p.latitude problem_latitude, p.longitude problem_longitude, s.polygon_coords, s.parking_latitude sector_latitude, s.parking_longitude sector_longitude, a.latitude area_latitude, a.longitude area_longitude FROM (((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN todo t ON p.id=t.problem_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND t.user_id=? AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1 GROUP BY r.url, t.id, a.id, a.name, a.locked_admin, a.locked_superadmin, s.id, s.locked_admin, s.locked_superadmin, s.name, p.id, p.nr, p.name, p.grade, p.locked_admin, p.locked_superadmin, p.latitude, p.longitude, s.polygon_coords, s.parking_latitude, s.parking_longitude, a.latitude, a.longitude ORDER BY a.name, s.name, p.nr")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			ps.setInt(4, userId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Area
					int areaId = rst.getInt("area_id");
					Todo.Area a = areaLookup.get(areaId);
					if (a == null) {
						String areaUrl = rst.getString("area_url");
						String areaName = rst.getString("area_name");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						a = res.addArea(areaId, areaUrl, areaName, areaLockedAdmin, areaLockedSuperadmin);
						areaLookup.put(areaId, a);
					}
					// Sector
					int sectorId = rst.getInt("sector_id");
					Todo.Sector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorUrl = rst.getString("sector_url");
						String sectorName = rst.getString("sector_name");
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						s = a.addSector(sectorId, sectorUrl, sectorName, sectorLockedAdmin, sectorLockedSuperadmin);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int todoId = rst.getInt("todo_id");
					int problemId = rst.getInt("problem_id");
					String problemUrl = rst.getString("problem_url");
					int problemNr = rst.getInt("problem_nr");
					String problemName = rst.getString("problem_name");
					int problemGrade = rst.getInt("problem_grade");
					boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
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
					Todo.Problem p = s.addProblem(todoId, problemId, problemUrl, problemLockedAdmin, problemLockedSuperadmin, problemNr, problemName, GradeHelper.intToString(setup, problemGrade), l.getLat(), l.getLng());
					problemLookup.put(problemId, p);
				}
			}
		}
		if (!problemLookup.isEmpty()) {
			String problemIds = Joiner.on(",").join(problemLookup.keySet());
			sqlStr = String.format("SELECT t.problem_id, u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name FROM todo t, user u WHERE t.user_id=u.id AND t.user_id!=? AND problem_id IN (%s)", problemIds);
			try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
				ps.setInt(1, userId);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int problemId = rst.getInt("problem_id");
						int id = rst.getInt("id");
						String name = rst.getString("name");
						problemLookup.get(problemId).addPartner(id, name);
					}
				}
			}
		}
		// Sort areas (ae, oe, aa is sorted wrong by MySql):
		res.getAreas().sort(Comparator.comparing(Todo.Area::getName));
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
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name FROM user u WHERE u.id=?")) {
			ps.setInt(1, reqId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					List<UserRegion> userRegions = c.getBuldreinfoRepo().getUserRegion(reqId, setup);
					String picture = rst.getString("picture");
					String name = rst.getString("name");
					res = new User(readOnly, reqId, picture, name, userRegions);
				}
			}
		}
		if (res == null) {
			return res;
		}
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT CASE WHEN m_a2.is_movie=0 THEN m_a2.id END)+COUNT(DISTINCT CASE WHEN m_s2.is_movie=0 THEN m_s2.id END)+COUNT(DISTINCT CASE WHEN m_p2.is_movie=0 THEN m_p2.id END) num_images_created, COUNT(DISTINCT CASE WHEN m_a2.is_movie=1 THEN m_a2.id END)+COUNT(DISTINCT CASE WHEN m_s2.is_movie=1 THEN m_s2.id END)+COUNT(DISTINCT CASE WHEN m_p2.is_movie=1 THEN m_p2.id END) num_videos_created FROM (((((((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN user u ON u.id=?) LEFT JOIN media_area m_a ON a.id=m_a.area_id) LEFT JOIN media m_a2 ON m_a.media_id=m_a2.id AND m_a2.deleted_user_id IS NULL AND m_a2.photographer_user_id=u.id) LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN media_sector m_s ON s.id=m_s.sector_id) LEFT JOIN media m_s2 ON m_s.media_id=m_s2.id AND m_s2.deleted_user_id IS NULL AND m_s2.photographer_user_id=u.id) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem m_p ON p.id=m_p.problem_id) LEFT JOIN media m_p2 ON m_p.media_id=m_p2.id AND m_p2.deleted_user_id IS NULL AND m_p2.photographer_user_id=u.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL)")) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId);
			ps.setInt(3, setup.getIdRegion());
			ps.setInt(4, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.setNumImagesCreated(rst.getInt("num_images_created"));
					res.setNumVideosCreated(rst.getInt("num_videos_created"));
				}
			}
		}
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT CASE WHEN mu_a.user_id IS NOT NULL AND m_a2.is_movie=0 THEN m_a.id END)+COUNT(DISTINCT CASE WHEN mu_s.user_id IS NOT NULL AND m_s2.is_movie=0 THEN m_s.id END)+COUNT(DISTINCT CASE WHEN mu_p.user_id IS NOT NULL AND m_p2.is_movie=0 THEN m_p.id END) num_image_tags, COUNT(DISTINCT CASE WHEN mu_a.user_id IS NOT NULL AND m_a2.is_movie=1 THEN m_a.id END)+COUNT(DISTINCT CASE WHEN mu_s.user_id IS NOT NULL AND m_s2.is_movie=1 THEN m_s.id END)+COUNT(DISTINCT CASE WHEN mu_p.user_id IS NOT NULL AND m_p2.is_movie=1 THEN m_p.id END) num_video_tags FROM ((((((((((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN user u ON u.id=?) LEFT JOIN media_area m_a ON a.id=m_a.area_id) LEFT JOIN media m_a2 ON m_a.media_id=m_a2.id AND m_a2.deleted_user_id IS NULL) LEFT JOIN media_user mu_a ON m_a2.id=mu_a.media_id AND u.id=mu_a.user_id) LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN media_sector m_s ON s.id=m_s.sector_id) LEFT JOIN media m_s2 ON m_s.media_id=m_s2.id AND m_s2.deleted_user_id IS NULL) LEFT JOIN media_user mu_s ON m_s2.id=mu_s.media_id AND u.id=mu_s.user_id) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem m_p ON p.id=m_p.problem_id) LEFT JOIN media m_p2 ON m_p.media_id=m_p2.id AND m_p2.deleted_user_id IS NULL) LEFT JOIN media_user mu_p ON m_p2.id=mu_p.media_id AND u.id=mu_p.user_id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL)")) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId);
			ps.setInt(3, setup.getIdRegion());
			ps.setInt(4, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.setNumImageTags(rst.getInt("num_image_tags"));
					res.setNumVideoTags(rst.getInt("num_video_tags"));
				}
			}
		}

		String sqlStr = "SELECT a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, t.id id_tick, ty.subtype, p.id id_problem, p.locked_admin, p.locked_superadmin, p.name, CASE WHEN (t.id IS NOT NULL) THEN t.comment ELSE p.description END comment, DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%Y-%m-%d') date, DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%d/%m-%y') date_hr, t.stars, CASE WHEN (f.user_id IS NOT NULL) THEN f.user_id ELSE 0 END fa, (CASE WHEN t.id IS NOT NULL THEN t.grade ELSE p.grade END) grade"
				+ " FROM (((((((problem p INNER JOIN type ty ON p.type_id=ty.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)) LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?) LEFT JOIN fa f ON (p.id=f.problem_id AND f.user_id=?)"
				+ " WHERE (t.user_id IS NOT NULL OR f.user_id IS NOT NULL) AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1"
				+ " GROUP BY a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, t.id, ty.subtype, p.id, p.locked_admin, p.locked_superadmin, p.name, p.description, p.fa_date, t.date, t.stars, t.grade, p.grade";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, reqId);
			ps.setInt(3, reqId);
			ps.setInt(4, setup.getIdRegion());
			ps.setInt(5, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String sectorName = rst.getString("sector_name");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					int id = rst.getInt("id_tick");
					String subType = rst.getString("subtype");
					int idProblem = rst.getInt("id_problem");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					String name = rst.getString("name");
					String comment = rst.getString("comment");
					String date = rst.getString("date");
					String dateHr = rst.getString("date_hr");
					double stars = rst.getDouble("stars");
					boolean fa = rst.getBoolean("fa");
					int grade = rst.getInt("grade");
					res.addTick(areaName, areaLockedAdmin, areaLockedSuperadmin, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, id, subType, idProblem, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, stars, fa, GradeHelper.intToString(setup, grade), grade);
				}
			}
		}
		// First aid ascent
		if (!setup.isBouldering()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, p.id id_problem, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description description, DATE_FORMAT(aid.aid_date,'%Y-%m-%d') date, DATE_FORMAT(aid.aid_date,'%d/%m-%y') date_hr" + 
					" FROM ((((((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN fa_aid aid ON p.id=aid.problem_id) INNER JOIN fa_aid_user aid_u ON (p.id=aid_u.problem_id AND aid_u.user_id=?) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?))" + 
					" WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1" + 
					" GROUP BY a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, p.id, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description, aid.aid_date")) {
				ps.setInt(1, reqId);
				ps.setInt(2, authUserId);
				ps.setInt(3, setup.getIdRegion());
				ps.setInt(4, setup.getIdRegion());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						String areaName = rst.getString("area_name");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						String sectorName = rst.getString("sector_name");
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						int idProblem = rst.getInt("id_problem");
						boolean lockedAdmin = rst.getBoolean("locked_admin");
						boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
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
						res.addTick(areaName, areaLockedAdmin, areaLockedSuperadmin, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, 0, "Aid", idProblem, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, 0, true, GradeHelper.intToString(setup, grade), grade);
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

	public UserMedia getUserMedia(int authUserId, Setup setup, int reqId) throws SQLException {
		UserMedia res = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, m.id, m.description, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, mp.pitch, 0 t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, MAX(p.id) problem_id FROM ((((((((user u INNER JOIN media_user mu ON u.id=? AND u.id=mu.user_id) INNER JOIN media m ON mu.media_id=m.id AND m.deleted_user_id IS NULL) INNER JOIN user c ON m.photographer_user_id=c.id) INNER JOIN media_problem mp ON m.id=mp.media_id) INNER JOIN problem p ON mp.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1 GROUP BY u.firstname, u.lastname, u.picture, m.id, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, mp.pitch, c.firstname, c.lastname ORDER BY m.id DESC")) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String name = rst.getString("name");
					if (res == null) {
						String picture = rst.getString("picture");
						res = new UserMedia(reqId, name, picture);
					}
					int idMedia = rst.getInt("id");
					String description = rst.getString("description");
					int pitch = 0;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = name;
					int problemId = rst.getInt("problem_id");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(idMedia);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description);
					MediaProblem m = new MediaProblem(idMedia, pitch, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl, problemId);
					res.getMedia().add(m);
				}
			}
		}
		return res;
	}

	public List<UserRegion> getUserRegion(int authUserId, Setup setup) throws SQLException {
		List<UserRegion> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.id, r.name, CASE WHEN r.id=? OR ur.admin_read=1 OR ur.admin_write=1 OR ur.superadmin_read=1 OR ur.superadmin_write=1 THEN 1 ELSE 0 END read_only, ur.region_visible, CASE WHEN ur.superadmin_write=1 THEN 'Superadmin' WHEN ur.superadmin_read=1 THEN 'Superadmin (read)' WHEN ur.admin_read=1 THEN 'Admin (read)' WHEN ur.admin_write=1 THEN 'Admin' END role FROM (region r INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) GROUP BY r.id, r.name ORDER BY r.name")) {
			ps.setInt(1, setup.getIdRegion());
			ps.setInt(2, authUserId);
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String name = rst.getString("name");
					String role = rst.getString("role");
					boolean readOnly = rst.getBoolean("read_only");
					boolean enabled = readOnly || rst.getBoolean("region_visible");
					res.add(new UserRegion(id, name, role, enabled, readOnly));
				}
			}
		}
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
					res.add(new User(true, id, null, name, null));
				}
			}
		}
		return res;
	}

	public byte[] getUserTicks(int authUserId) throws SQLException, IOException {
		byte[] bytes;
		try (ExcelReport report = new ExcelReport()) {
			String sqlStr = "SELECT r.id region_id, ty.type, pt.subtype, CONCAT(r.url,'/problem/',p.id) url, a.name area_name, s.name sector_name, p.name, CASE WHEN (t.id IS NOT NULL) THEN t.comment ELSE p.description END comment, DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%Y-%m-%d') date, t.stars, CASE WHEN (f.user_id IS NOT NULL) THEN f.user_id ELSE 0 END fa, (CASE WHEN t.id IS NOT NULL THEN t.grade ELSE p.grade END) grade" + 
					" FROM ((((((((problem p INNER JOIN type pt ON p.type_id=pt.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN type ty ON rt.type_id=ty.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)) LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?) LEFT JOIN fa f ON (p.id=f.problem_id AND f.user_id=?)" + 
					" WHERE (t.user_id IS NOT NULL OR f.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1" + 
					" GROUP BY r.id, ty.type, pt.subtype, r.url, a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, t.id, p.id, p.locked_admin, p.locked_superadmin, p.name, p.description, p.fa_date, t.date, t.stars, t.grade, p.grade" + 
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
						String subType = rst.getString("subType");
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
						if (subType != null) {
							writer.write("TYPE", subType);							
						}
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
					" FROM (((((((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN type ty ON rt.type_id=ty.id) INNER JOIN fa_aid aid ON p.id=aid.problem_id) INNER JOIN fa_aid_user aid_u ON (p.id=aid_u.problem_id AND aid_u.user_id=?) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?))" + 
					" WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1" + 
					" GROUP BY r.id, ty.type, r.url, a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, p.id, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description, aid.aid_date" + 
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

	public void moveMedia(int authUserId, int id, boolean left) throws SQLException {
		boolean ok = false;
		int areaId = 0;
		int sectorId = 0;
		int problemId = 0;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ur.admin_write, ur.superadmin_write, ma.area_id, ms.sector_id, mp.problem_id FROM ((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN user_region ur ON (a.region_id=ur.region_id AND ur.user_id=?)) LEFT JOIN media_area ma ON (a.id=ma.area_id AND ma.media_id=?) LEFT JOIN media_sector ms ON (s.id=ms.sector_id AND ms.media_id=?)) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem mp ON (p.id=mp.problem_id AND mp.media_id=?) WHERE ma.media_id IS NOT NULL OR ms.media_id IS NOT NULL OR mp.media_id IS NOT NULL GROUP BY ur.admin_write, ur.superadmin_write, ma.area_id, ms.sector_id, mp.problem_id")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, id);
			ps.setInt(3, id);
			ps.setInt(4, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
					areaId = rst.getInt("area_id");
					sectorId = rst.getInt("sector_id");
					problemId = rst.getInt("problem_id");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
		
		String table = null;
		String column = null;
		int columnId = 0;
		if (areaId > 0) {
			table = "media_area";
			column = "area_id";
			columnId = areaId;
		} else if (sectorId > 0) {
			table = "media_sector";
			column = "sector_id";
			columnId = sectorId;
		} else {
			table = "media_problem";
			column = "problem_id";
			columnId = problemId;
		}
		List<Integer> idMediaList = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id FROM " + table + " x, media m WHERE x." + column + "=? AND x.media_id=m.id AND m.deleted_user_id IS NULL AND m.is_movie=0 ORDER BY -x.sorting DESC, m.id")) {
			ps.setInt(1, columnId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					idMediaList.add(idMedia);
				}
			}
		}
		final int ixToMove = idMediaList.indexOf(id);
		idMediaList.remove(ixToMove);
		Preconditions.checkArgument(ixToMove>=0, "Could not find " + id + " in " + idMediaList);
		if (left) {
			if (ixToMove == 0) {
				idMediaList.add(id); // Move from start to end
			} else {
				idMediaList.add(ixToMove-1, id);
			}
		} else {
			if (ixToMove == idMediaList.size()) {
				idMediaList.add(0, id); // Move from end to start
			} else {
				idMediaList.add(ixToMove+1, id);
			}
		}
		try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE " + table + " SET sorting=? WHERE " + column + "=? AND media_id=?")) {
			int sorting = 0;
			for (int idMedia : idMediaList) {
				ps.setInt(1, ++sorting);
				ps.setInt(2, columnId);
				ps.setInt(3, idMedia);
				ps.addBatch();
			}
			ps.executeBatch();
		}

		if (problemId > 0) {
			fillActivity(problemId);
		}
	}

	public Area setArea(Setup s, int authUserId, Area a, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, InterruptedException {
		Preconditions.checkArgument(authUserId != -1, "Insufficient credentials");
		Preconditions.checkArgument(s.getIdRegion() > 0, "Insufficient credentials");
		ensureAdminWriteRegion(authUserId, s.getIdRegion());
		int idArea = -1;
		final boolean isLockedAdmin = a.isLockedSuperadmin()? false : a.isLockedAdmin();
		if (a.getId() > 0) {
			ensureAdminWriteArea(authUserId, a.getId());
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE area SET name=?, description=?, latitude=?, longitude=?, locked_admin=?, locked_superadmin=?, for_developers=? WHERE id=?")) {
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
				ps.setBoolean(5, isLockedAdmin);
				ps.setBoolean(6, a.isLockedSuperadmin());
				ps.setBoolean(7, a.isForDevelopers());
				ps.setInt(8, a.getId());
				ps.execute();
			}
			idArea = a.getId();

			// Also update sectors and problems (last_updated and locked)
			String sqlStr = null;
			if (a.isLockedAdmin() || a.isLockedSuperadmin()) {
				sqlStr = "UPDATE (area a LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), a.locked_admin=?, a.locked_superadmin=?, s.last_updated=now(), s.locked_admin=?, s.locked_superadmin=?, p.last_updated=now(), p.locked_admin=?, p.locked_superadmin=? WHERE a.id=?";
				try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
					ps.setBoolean(1, isLockedAdmin);
					ps.setBoolean(2, a.isLockedSuperadmin());
					ps.setBoolean(3, isLockedAdmin);
					ps.setBoolean(4, a.isLockedSuperadmin());
					ps.setBoolean(5, isLockedAdmin);
					ps.setBoolean(6, a.isLockedSuperadmin());
					ps.setInt(7, a.getId());
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
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO area (android_id, region_id, name, description, latitude, longitude, locked_admin, locked_superadmin, for_developers, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())", PreparedStatement.RETURN_GENERATED_KEYS)) {
				ps.setLong(1, System.currentTimeMillis());
				ps.setInt(2, s.getIdRegion());
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
				ps.setBoolean(7, isLockedAdmin);
				ps.setBoolean(8, a.isLockedSuperadmin());
				ps.setBoolean(9, a.isForDevelopers());
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
				final int idGuestbook = 0;
				addNewMedia(authUserId, idProblem, pitch, idSector, idArea, idGuestbook, m, multiPart, now);
			}
		}
		return getArea(s, authUserId, idArea);
	}

	public Problem setProblem(int authUserId, Setup s, Problem p, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, ParseException, InterruptedException {
		final boolean orderByGrade = s.isBouldering();
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		final Date dt = Strings.isNullOrEmpty(p.getFaDate()) ? null : new Date(sdf.parse(p.getFaDate()).getTime());
		int idProblem = -1;
		final boolean isLockedAdmin = p.isLockedSuperadmin()? false : p.isLockedAdmin();
		if (p.getId() > 0) {
			fillProblemCoordinationsHistory(authUserId, p);
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE ((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN user_region ur ON (a.region_id=ur.region_id AND ur.user_id=? AND (ur.admin_write=1 OR ur.superadmin_write=1)) SET p.name=?, p.rock=?, p.description=?, p.grade=?, p.fa_date=?, p.latitude=?, p.longitude=?, p.locked_admin=?, p.locked_superadmin=?, p.nr=?, p.type_id=?, trivia=?, starting_altitude=?, aspect=?, route_length=?, descent=?, p.last_updated=now() WHERE p.id=?")) {
				ps.setInt(1, authUserId);
				ps.setString(2, p.getName());
				ps.setString(3, p.getRock());
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
				ps.setBoolean(9, isLockedAdmin);
				ps.setBoolean(10, p.isLockedSuperadmin());
				ps.setInt(11, p.getNr());
				ps.setInt(12, p.getT().getId());
				ps.setString(13, p.getTrivia());
				ps.setString(14, p.getStartingAltitude());
				ps.setString(15, p.getAspect());
				ps.setString(16, p.getRouteLength());
				ps.setString(17, p.getDescent());
				ps.setInt(18, p.getId());
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Insufficient credentials");
				}
			}
			idProblem = p.getId();
		} else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO problem (android_id, sector_id, name, description, grade, fa_date, latitude, longitude, locked_admin, locked_superadmin, nr, type_id, trivia, starting_altitude, aspect, route_length, descent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
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
				ps.setBoolean(9, isLockedAdmin);
				ps.setBoolean(10, p.isLockedSuperadmin());
				ps.setInt(11, p.getNr() == 0 ? getSector(authUserId, orderByGrade, s, p.getSectorId()).getProblems().stream().map(x -> x.getNr()).mapToInt(Integer::intValue).max().orElse(0) + 1 : p.getNr());
				ps.setInt(12, p.getT().getId());
				ps.setString(13, p.getTrivia());
				ps.setString(14, p.getStartingAltitude());
				ps.setString(15, p.getAspect());
				ps.setString(16, p.getRouteLength());
				ps.setString(17, p.getDescent());
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
		// Also update last_updated on problem, sector and area
		String sqlStr = "UPDATE problem p, sector s, area a SET p.last_updated=now(), s.last_updated=now(), a.last_updated=now() WHERE p.id=? AND p.sector_id=s.id AND s.area_id=a.id";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, idProblem);
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
				final int idGuestbook = 0;
				addNewMedia(authUserId, idProblem, m.getPitch(), idSector, idArea, idGuestbook, m, multiPart, now);
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
		return getProblem(authUserId, s, idProblem, false);
	}

	public Sector setSector(int authUserId, boolean orderByGrade, Setup setup, Sector s, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, InterruptedException {
		int idSector = -1;
		final boolean isLockedAdmin = s.isLockedSuperadmin()? false : s.isLockedAdmin();
		if (s.getId() > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE sector s, area a, user_region ur SET s.name=?, s.description=?, s.parking_latitude=?, s.parking_longitude=?, s.locked_admin=?, s.locked_superadmin=?, s.polygon_coords=?, s.polyline=? WHERE s.id=? AND s.area_id=a.id AND a.region_id=ur.region_id AND ur.user_id=? AND (ur.admin_write=1 OR ur.superadmin_write=1)")) {
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
				ps.setBoolean(5, isLockedAdmin);
				ps.setBoolean(6, s.isLockedSuperadmin());
				ps.setString(7, Strings.emptyToNull(s.getPolygonCoords()));
				ps.setString(8, Strings.emptyToNull(s.getPolyline()));
				ps.setInt(9, s.getId());
				ps.setInt(10, authUserId);
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Insufficient credentials");
				}
			}
			idSector = s.getId();

			// Also update problems (last_updated and locked) + last_updated on area
			String sqlStr = null;
			if (s.isLockedAdmin() || s.isLockedSuperadmin()) {
				sqlStr = "UPDATE (area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), s.locked_admin=?, s.locked_superadmin=?, p.last_updated=now(), p.locked_admin=?, p.locked_superadmin=? WHERE s.id=?";
				try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
					ps.setBoolean(1, isLockedAdmin);
					ps.setBoolean(2, s.isLockedSuperadmin());
					ps.setBoolean(3, isLockedAdmin);
					ps.setBoolean(4, s.isLockedSuperadmin());
					ps.setInt(5, idSector);
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
			ensureAdminWriteArea(authUserId, s.getAreaId());
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO sector (android_id, area_id, name, description, parking_latitude, parking_longitude, locked_admin, locked_superadmin, polygon_coords, polyline, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())", PreparedStatement.RETURN_GENERATED_KEYS)) {
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
				ps.setBoolean(7, isLockedAdmin);
				ps.setBoolean(8, s.isLockedSuperadmin());
				ps.setString(9, Strings.emptyToNull(s.getPolygonCoords()));
				ps.setString(10, Strings.emptyToNull(s.getPolyline()));
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
				final int idGuestbook = 0;
				addNewMedia(authUserId, idProblem, pitch, idSector, idArea, idGuestbook, m, multiPart, now);
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

	public void setUserRegion(int authUserId, int regionId, boolean delete) throws SQLException {
		if (delete) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM user_region WHERE user_id=? AND region_id=?")) {
				ps.setInt(1, authUserId);
				ps.setInt(2, regionId);
				ps.execute();
			}
		}
		else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO user_region (user_id, region_id, region_visible) VALUES (?, ?, 1)")) {
				ps.setInt(1, authUserId);
				ps.setInt(2, regionId);
				ps.execute();
			}
		}
	}

	public void toggleTodo(int authUserId, int problemId) throws SQLException {
		Preconditions.checkArgument(authUserId > 0, "User not logged in");
		Preconditions.checkArgument(problemId > 0, "Problem id not set");
		int todoId = -1;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id FROM todo WHERE user_id=? AND problem_id=?")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, problemId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					todoId = rst.getInt("id");
				}
			}
		}
		if (todoId > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM todo WHERE id=?")) {
				ps.setInt(1, todoId);
				ps.execute();
			}
		} else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO todo (user_id, problem_id) VALUES (?, ?)")) {
				ps.setInt(1, authUserId);
				ps.setInt(2, problemId);
				ps.execute();
			}
		}
	}

	public void upsertComment(int authUserId, Setup s, Comment co, FormDataMultiPart multiPart) throws SQLException, IOException, NoSuchAlgorithmException, InterruptedException {
		Preconditions.checkArgument(authUserId > 0);
		if (co.getId() > 0) {
			if (co.isDelete()) {
				List<Problem.Comment> comments = getProblem(authUserId, s, co.getIdProblem(), false).getComments();
				Preconditions.checkArgument(!comments.isEmpty(), "No comment on problem " + co.getIdProblem());
				Problem.Comment lastComment = comments.get(comments.size()-1);
				Preconditions.checkArgument(co.getId() == lastComment.getId(), "Comment not in end of thread");
				Preconditions.checkArgument(lastComment.isEditable(), "Comment not editable by " + authUserId);
				try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM guestbook WHERE id=?")) {
					ps.setInt(1, co.getId());
					ps.execute();
				}
			}
			else {
				try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE guestbook SET message=?, danger=?, resolved=? WHERE id=?")) {
					ps.setString(1, co.getComment());
					ps.setBoolean(2, co.isDanger());
					ps.setBoolean(3, co.isResolved());
					ps.setInt(4, co.getId());
					ps.execute();
					if (co.getNewMedia() != null) {
						// New media
						Timestamp now = new Timestamp(System.currentTimeMillis());
						for (NewMedia m : co.getNewMedia()) {
							final int idProblem = 0;
							final int idSector = 0;
							final int idArea = 0;
							addNewMedia(authUserId, idProblem, 0, idSector, idArea, co.getId(), m, multiPart, now);
						}
					}
				}
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

			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO guestbook (post_time, message, problem_id, user_id, parent_id, danger, resolved) VALUES (now(), ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
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
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						int idGuestbook = rst.getInt(1);
						if (co.getNewMedia() != null) {
							// New media
							Timestamp now = new Timestamp(System.currentTimeMillis());
							for (NewMedia m : co.getNewMedia()) {
								final int idProblem = 0;
								final int idSector = 0;
								final int idArea = 0;
								addNewMedia(authUserId, idProblem, 0, idSector, idArea, idGuestbook, m, multiPart, now);
							}
						}
					}
				}
			}
		}
		fillActivity(co.getIdProblem());
	}

	public void upsertMediaSvg(int authUserId, Setup setup, MediaSvg ms) throws SQLException {
		ensureAdminWriteRegion(authUserId, setup.getIdRegion());
		// Clear existing
		try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM media_svg WHERE media_id=?")) {
			ps.setInt(1, ms.getM().getId());
			ps.execute();
		}
		// Insert
		for (MediaSvgElement element : ms.getM().getMediaSvgs()) {
			if (element.getT().equals(MediaSvgElement.TYPE.PATH)) {
				try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_svg (media_id, path) VALUES (?, ?)")) {
					ps.setInt(1, ms.getM().getId());
					ps.setString(2, element.getPath());
					ps.execute();
				}
			}
			else if (element.getT().equals(MediaSvgElement.TYPE.RAPPEL_BOLTED) || element.getT().equals(MediaSvgElement.TYPE.RAPPEL_NOT_BOLTED)) {
				try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_svg (media_id, rappel_x, rappel_y, rappel_bolted) VALUES (?, ?, ?, ?)")) {
					ps.setInt(1, ms.getM().getId());
					ps.setInt(2, element.getRappelX());
					ps.setInt(3, element.getRappelY());
					ps.setBoolean(4, element.getT().equals(MediaSvgElement.TYPE.RAPPEL_BOLTED));
					ps.execute();
				}
			}
			else {
				throw new RuntimeException("Invalid type: " + element.getT());
			}
		}
	}

	public void upsertPermissionUser(int regionId, int authUserId, PermissionUser u) throws SQLException {
		ensureSuperadminWriteRegion(authUserId, regionId);
		// Upsert
		try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO user_region (user_id, region_id, admin_read, admin_write, superadmin_read, superadmin_write) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE admin_read=?, admin_write=?, superadmin_read=?, superadmin_write=?")) {
			ps.setInt(1, u.getUserId());
			ps.setInt(2, regionId);
			ps.setBoolean(3, u.isAdminRead());
			ps.setBoolean(4, u.isAdminWrite());
			ps.setBoolean(5, u.isSuperadminRead());
			ps.setBoolean(6, u.isSuperadminWrite());
			ps.setBoolean(7, u.isAdminRead());
			ps.setBoolean(8, u.isAdminWrite());
			ps.setBoolean(9, u.isSuperadminRead());
			ps.setBoolean(10, u.isSuperadminWrite());
			ps.execute();
		}
		// region_visible only set by user, if user have not asked to see a specific region --> remove row
		try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM user_region WHERE admin_read=0 AND admin_write=0 AND superadmin_read=0 AND superadmin_write=0 AND region_visible=0")) {
			ps.execute();
		}
	}

	public void upsertSvg(int authUserId, int problemId, int mediaId, Svg svg) throws SQLException {
		ensureAdminWriteProblem(authUserId, problemId);
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

	private int addNewMedia(int idUser, int idProblem, int pitch, int idSector, int idArea, int idGuestbook, NewMedia m, FormDataMultiPart multiPart, Timestamp now) throws SQLException, IOException, NoSuchAlgorithmException, InterruptedException {
		int idMedia = -1;
		logger.debug("addNewMedia(idUser={}, idProblem={}, pitch, idSector={}, idArea={}, m={}) initialized", idUser, idProblem, pitch, idSector, m);
		Preconditions.checkArgument((idProblem > 0 && idSector == 0 && idArea == 0 && idGuestbook == 0)
				|| (idProblem == 0 && idSector > 0 && idArea == 0 && idGuestbook == 0)
				|| (idProblem == 0 && idSector == 0 && idArea > 0 && idGuestbook == 0)
				|| (idProblem == 0 && idSector == 0 && idArea == 0 && idGuestbook > 0));

		boolean alreadyExistsInDb = false;
		boolean isMovie = false;
		String suffix = null;
		boolean setDateTakenWHAndChecksum = true;
		if (Strings.isNullOrEmpty(m.getName())) {
			// Embed video url
			Preconditions.checkNotNull(m.getEmbedThumbnailUrl(), "embedThumbnailUrl required");
			Preconditions.checkNotNull(m.getEmbedVideoUrl(), "embedVideoUrl required");
			// First check if video already exists in system, don't duplicate videos!
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id FROM media WHERE embed_url=?")) {
				ps.setString(1, m.getEmbedVideoUrl());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						alreadyExistsInDb = true;
						idMedia = rst.getInt(1);
					}
				}
			}
			suffix = "mp4";
			isMovie = true;
			setDateTakenWHAndChecksum = false;
		}
		else {
			suffix = "jpg";
			isMovie = false;
			setDateTakenWHAndChecksum = true;
		}

		/**
		 * DB
		 */
		if (!alreadyExistsInDb) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media (is_movie, suffix, photographer_user_id, uploader_user_id, date_created, description, embed_url) VALUES (?, ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
				ps.setBoolean(1, isMovie);
				ps.setString(2, suffix);
				ps.setInt(3, getExistingOrInsertUser(m.getPhotographer()));
				ps.setInt(4, idUser);
				ps.setTimestamp(5, now);
				ps.setString(6, m.getDescription());
				ps.setString(7, m.getEmbedVideoUrl());
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idMedia = rst.getInt(1);
					}
				}
			}
		}
		Preconditions.checkArgument(idMedia > 0);
		if (idProblem > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_problem (media_id, problem_id, pitch, milliseconds) VALUES (?, ?, ?, ?)")) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idProblem);
				if (pitch > 0) {
					ps.setInt(3, pitch);
				}
				else { 
					ps.setNull(3, Types.NUMERIC);
				}
				ps.setLong(4, m.getEmbedMilliseconds());
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
		} else if (idGuestbook > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_guestbook (media_id, guestbook_id) VALUES (?, ?)")) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idGuestbook);
				ps.execute();
			}
		} else {
			throw new RuntimeException("Server error");
		}
		if (!alreadyExistsInDb) {
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
			final Path p = GlobalFunctions.getPathMediaOriginalJpg().resolve(String.valueOf(idMedia / 100 * 100)).resolve(idMedia + ".jpg");
			Files.createDirectories(p.getParent());
			Preconditions.checkArgument(!Files.exists(p), p.toString() + " does already exist");

			Path original = GlobalFunctions.getPathTemp();
			Files.createDirectories(original);
			original = original.resolve(System.currentTimeMillis() + "_" + m.getName());
			Preconditions.checkArgument(Files.exists(original.getParent()), original.getParent().toString() + " does not exist");
			Preconditions.checkArgument(!Files.exists(original), original.toString() + " does already exist");
			if (isMovie) {
				try (InputStream in = new URL(m.getEmbedThumbnailUrl()).openStream()){
					Files.copy(in, original);
				}
				BufferedImage b = ImageIO.read(original.toFile());
				Graphics g = b.getGraphics();
				g.setFont(new Font("Arial", Font.BOLD, 40));
				final String str = "VIDEO";
				final int x = (b.getWidth()/2)-70;
				final int y = (b.getHeight()/2)-20;
				FontMetrics fm = g.getFontMetrics();
				Rectangle2D rect = fm.getStringBounds(str, g);
				g.setColor(Color.WHITE);
				g.fillRect(x,
						y - fm.getAscent(),
						(int) rect.getWidth(),
						(int) rect.getHeight());
				g.setColor(Color.BLUE);
				g.drawString(str, x, y);
				g.dispose();
				ImageIO.write(b, "jpg", p.toFile());
				b.flush();
			}
			else {
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
				try (InputStream is = multiPart.getField(m.getName()).getValueAs(InputStream.class)) {
					Files.copy(is, original);
				}
				Preconditions.checkArgument(Files.exists(original), original.toString() + " does not exist");

				// If not JPG/JPEG --> convert to JPG, else --> copy to destination
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
			}
			Preconditions.checkArgument(Files.exists(p) && Files.size(p)>0, p.toString() + " does not exist (or is 0 byte)");
			// Create scaled jpg and webp + update crc32 and dimentions in db
			createScaledImages(c, getDateTaken(p), idMedia, "jpg", setDateTakenWHAndChecksum);
		}
		return idMedia;
	}

	private int addUser(String email, String firstname, String lastname, String picture, boolean autoCommit) throws SQLException, IOException {
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

	private void createScaledImages(DbConnection c, String dateTaken, int id, String suffix, boolean setDateTakenWHAndChecksum) throws IOException, InterruptedException, SQLException {
		final Path original = GlobalFunctions.getPathMediaOriginalJpg().resolve(String.valueOf(id / 100 * 100)).resolve(id + "." + suffix);
		final Path webp = GlobalFunctions.getPathMediaWebWebp().resolve(String.valueOf(id / 100 * 100)).resolve(id + ".webp");
		final Path jpg = GlobalFunctions.getPathMediaWebJpg().resolve(String.valueOf(id / 100 * 100)).resolve(id + ".jpg");
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
		if (setDateTakenWHAndChecksum) {
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
	}

	private boolean downloadUserImage(int userId, String url) throws IOException {
		try {
			Path original = GlobalFunctions.getPathOriginalUsers().resolve(userId + ".jpg");
			Files.createDirectories(original.getParent());
			try (InputStream in = new URL(url).openStream()) {
				Files.copy(in, original, StandardCopyOption.REPLACE_EXISTING);
				in.close();
				// Resize avatar
				Path resized = GlobalFunctions.getPathWebUsers().resolve(userId + ".jpg");
				Files.createDirectories(resized.getParent());
				Files.deleteIfExists(resized);
				BufferedImage bOriginal = ImageIO.read(original.toFile());
				BufferedImage bScaled = Scalr.resize(bOriginal, Scalr.Mode.FIT_EXACT, 35, 35, Scalr.OP_ANTIALIAS);
				ImageIO.write(bScaled, "jpg", resized.toFile());
				bOriginal.flush();
				bOriginal = null;
				bScaled.flush();
				bScaled = null;
				Preconditions.checkArgument(Files.exists(resized));
				return true;
			}
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
			return false;
		}
	}

	private void ensureAdminWriteArea(int authUserId, int areaId) throws SQLException {
		boolean ok = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM area a, user_region ur WHERE a.id=? AND a.region_id=ur.region_id AND ur.user_id=? AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin)=1")) {
			ps.setInt(1, areaId);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}

	private void ensureAdminWriteProblem(int authUserId, int problemId) throws SQLException {
		boolean ok = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM area a, sector s, problem p, user_region ur WHERE p.id=? AND a.region_id=ur.region_id AND ur.user_id=? AND a.id=s.area_id AND s.id=p.sector_id AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin)=1 AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin)=1 AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1")) {
			ps.setInt(1, problemId);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}

	private void ensureAdminWriteRegion(int authUserId, int idRegion) throws SQLException {
		Preconditions.checkArgument(authUserId != -1, "Insufficient credentials");
		Preconditions.checkArgument(idRegion > 0, "Insufficient credentials");
		boolean ok = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM user_region ur WHERE ur.region_id=? AND ur.user_id=?")) {
			ps.setInt(1, idRegion);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}

	private void ensureSuperadminWriteRegion(int authUserId, int idRegion) throws SQLException {
		Preconditions.checkArgument(authUserId != -1, "Insufficient credentials");
		Preconditions.checkArgument(idRegion > 0, "Insufficient credentials");
		boolean ok = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ur.superadmin_write FROM user_region ur WHERE ur.region_id=? AND ur.user_id=?")) {
			ps.setInt(1, idRegion);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
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
	
	private int getExistingOrInsertUser(String name) throws SQLException, NoSuchAlgorithmException, IOException {
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
	
	private Map<Integer, String> getFaAidNamesOnSector(int sectorId) throws SQLException {
		Map<Integer, String> res = new HashMap<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT p.id, group_concat(DISTINCT CONCAT(TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') fa FROM problem p, fa_aid_user a, user u WHERE p.sector_id=? AND p.id=a.problem_id AND a.user_id=u.id GROUP BY p.id")) {
			ps.setInt(1, sectorId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idProblem = rst.getInt("id");
					String fa = rst.getString("fa");
					res.put(idProblem, fa);
				}
			}
		}
		return res;
	}
	
	private List<Media> getMediaArea(int id, boolean inherited) throws SQLException {
		List<Media> media = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.description, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM (((media m INNER JOIN media_area ma ON m.id=ma.media_id AND m.deleted_user_id IS NULL AND ma.area_id=?) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id GROUP BY m.id, m.description, m.width, m.height, m.is_movie, m.embed_url, ma.sorting, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.is_movie, m.embed_url, -ma.sorting DESC, m.id")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					String description = rst.getString("description");
					int pitch = 0;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(idMedia);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description);
					media.add(new Media(idMedia, pitch, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl, inherited));
				}
			}
		}
		return media;
	}

	private List<Media> getMediaGuestbook(int id) throws SQLException {
		List<Media> media = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.description, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM (((media m INNER JOIN media_guestbook mg ON m.id=mg.media_id AND m.deleted_user_id IS NULL AND mg.guestbook_id=?) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id GROUP BY m.id, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.is_movie, m.embed_url, m.id")) {
			ps.setInt(1, id);
			final boolean inherited = false;
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					String description = rst.getString("description");
					int pitch = 0;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(idMedia);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description);
					media.add(new Media(idMedia, pitch, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl, inherited));
				}
			}
		}
		return media;
	}

	private List<Media> getMediaProblem(Setup s, int authUserId, int sectorId, int problemId, boolean inherited, boolean showHiddenMedia) throws SQLException {
		List<Media> media = getMediaSector(s, authUserId, sectorId, problemId, true, showHiddenMedia);
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.description, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, mp.pitch, ROUND(mp.milliseconds/1000) t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM (((media m INNER JOIN media_problem mp ON m.id=mp.media_id AND m.deleted_user_id IS NULL AND mp.problem_id=?) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id GROUP BY m.id, m.description, m.width, m.height, m.is_movie, m.embed_url, mp.sorting, m.date_created, m.date_taken, mp.pitch, mp.milliseconds, c.firstname, c.lastname ORDER BY m.is_movie, m.embed_url, -mp.sorting DESC, m.id")) {
			ps.setInt(1, problemId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					String description = rst.getString("description");
					int pitch = rst.getInt("pitch");
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String t = rst.getString("t");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					if (embedUrl != null) {
						long seconds = Long.parseLong(t);
						if (seconds > 0) {
							if (embedUrl.contains("youtu")) {
								embedUrl += "?start=" + seconds;
							}
							else {
								embedUrl += "#t=" + seconds + "s";
							}
						}
					}
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(idMedia);
					List<Svg> svgs = getSvgs(s, authUserId, idMedia);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description);
					media.add(new Media(idMedia, pitch, width, height, tyId, t, mediaSvgs, problemId, svgs, mediaMetadata, embedUrl, inherited));
				}
			}
		}
		if (media.isEmpty()) {
			media = null;
		}
		return media;
	}

	private List<Media> getMediaSector(Setup s, int authUserId, int idSector, int optionalIdProblem, boolean inherited, boolean showHiddenMedia) throws SQLException {
		List<Media> allMedia = new ArrayList<>();
		List<Media> topoMedia = new ArrayList<>();
		String sqlStr = "SELECT m.id, m.description, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged"
				+ " FROM (((media m INNER JOIN media_sector ms ON m.id=ms.media_id AND m.deleted_user_id IS NULL AND ms.sector_id=?) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id"
				+ (optionalIdProblem>0? " WHERE m.id NOT IN (SELECT media_id FROM media_problem_exclude WHERE problem_id=" + optionalIdProblem + ")" : "")
				+ " GROUP BY m.id, m.description, m.width, m.height, m.is_movie, m.embed_url, ms.sorting, m.date_created, m.date_taken, c.firstname, c.lastname"
				+ " ORDER BY m.is_movie, m.embed_url, -ms.sorting DESC, m.id";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, idSector);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					String description = rst.getString("description");
					int pitch = 0;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(idMedia);
					List<Svg> svgs = getSvgs(s, authUserId, idMedia);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description);
					Media m = new Media(idMedia, pitch, width, height, tyId, null, mediaSvgs, optionalIdProblem, svgs, mediaMetadata, embedUrl, inherited);
					if (!showHiddenMedia && optionalIdProblem != 0 && svgs != null
							&& svgs.stream().filter(svg -> svg.getProblemId() == optionalIdProblem).findAny().isPresent()) {
						topoMedia.add(m);
					}
					allMedia.add(m);
				}
			}
		}
		if (!topoMedia.isEmpty()) {
			return topoMedia;
		}
		return allMedia;
	}

	private List<MediaSvgElement> getMediaSvgElements(int idMedia) throws SQLException {
		List<MediaSvgElement> res = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ms.id, ms.path, ms.rappel_x, ms.rappel_y, ms.rappel_bolted FROM media_svg ms WHERE ms.media_id=?")) {
			ps.setInt(1, idMedia);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					if (res == null) {
						res = new ArrayList<>();
					}
					int id = rst.getInt("id");
					String path = rst.getString("path");
					if (path != null) {
						res.add(new MediaSvgElement(id, path));
					}
					else {
						int rappelX = rst.getInt("rappel_x");
						int rappelY = rst.getInt("rappel_y");
						boolean rappelBolted = rst.getBoolean("rappel_bolted");
						res.add(new MediaSvgElement(id, rappelX, rappelY, rappelBolted));
					}
				}
			}
		}
		return res;
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
		Map<Integer, String> problemIdFirstAidAscentLookup = null;
		if (!setup.isBouldering()) {
			problemIdFirstAidAscentLookup = getFaAidNamesOnSector(reqId);
		}
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id area_id, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.name area_name, CONCAT(r.url,'/sector/',s.id) canonical, s.locked_admin, s.locked_superadmin, s.name, s.description, s.parking_latitude, s.parking_longitude, s.polygon_coords, s.polyline, s.hits FROM ((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE s.id=? AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin)=1 GROUP BY r.url, a.id, a.locked_admin, a.locked_superadmin, a.name, s.locked_admin, s.locked_superadmin, s.name, s.description, s.parking_latitude, s.parking_longitude, s.polygon_coords, s.polyline, s.hits")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, reqId);
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String areaName = rst.getString("area_name");
					String canonical = rst.getString("canonical");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					LatLng l = markerHelper.getLatLng(rst.getDouble("parking_latitude"), rst.getDouble("parking_longitude"));
					String polygonCoords = rst.getString("polygon_coords");
					String polyline = rst.getString("polyline");
					int hits = rst.getInt("hits");
					List<Media> media = getMediaSector(setup, authUserId, reqId, 0, false, false);
					media.addAll(getMediaArea(areaId, true));
					if (media.isEmpty()) {
						media = null;
					}
					s = new Sector(orderByGrade, areaId, areaLockedAdmin, areaLockedSuperadmin, areaName, canonical, reqId, lockedAdmin, lockedSuperadmin, name, comment, l.getLat(), l.getLng(), polygonCoords, polyline, media, null, hits);
				}
			}
		}
		Preconditions.checkNotNull(s, "Could not find sector with id=" + reqId);
		String sqlStr = "SELECT p.id, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, ROUND((IFNULL(AVG(NULLIF(t.grade,0)), p.grade) + p.grade)/2) grade, p.latitude, p.longitude,"
				+ " COUNT(DISTINCT ps.id) num_pitches,"
				+ " COUNT(DISTINCT CASE WHEN m.is_movie=0 THEN m.id END) num_images,"
				+ " COUNT(DISTINCT CASE WHEN m.is_movie=1 THEN m.id END) num_movies,"
				+ " CASE WHEN MAX(svg.id) IS NOT NULL THEN 1 ELSE 0 END has_topo,"
				+ " group_concat(DISTINCT CONCAT(TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') fa,"
				+ " COUNT(DISTINCT t.id) num_ticks, ROUND(ROUND(AVG(t.stars)*2)/2,1) stars,"
				+ " MAX(CASE WHEN (t.user_id=? OR u.id=?) THEN 1 END) ticked, ty.id type_id, ty.type, ty.subtype,"
				+ " danger.danger"
				+ " FROM ((((((((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) LEFT JOIN (media_problem mp LEFT JOIN media m ON mp.media_id=m.id AND m.deleted_user_id IS NULL) ON p.id=mp.problem_id) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN tick t ON p.id=t.problem_id) LEFT JOIN (SELECT problem_id, danger FROM guestbook WHERE (danger=1 OR resolved=1) AND id IN (SELECT max(id) id FROM guestbook WHERE (danger=1 OR resolved=1) GROUP BY problem_id)) danger ON p.id=danger.problem_id) LEFT JOIN problem_section ps ON p.id=ps.problem_id) LEFT JOIN svg ON p.id=svg.problem_id"
				+ " WHERE p.sector_id=?"
				+ "   AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin)=1"
				+ " GROUP BY p.id, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, p.grade, p.latitude, p.longitude, ty.id, ty.type, ty.subtype, danger.danger"
				+ " ORDER BY p.nr";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, authUserId);
			ps.setInt(3, authUserId);
			ps.setInt(4, reqId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int nr = rst.getInt("nr");
					int grade = rst.getInt("grade");
					String name = rst.getString("name");
					String rock = rst.getString("rock");
					String comment = rst.getString("description");
					String fa = rst.getString("fa");
					if (problemIdFirstAidAscentLookup != null && problemIdFirstAidAscentLookup.containsKey(id)) {
						fa = "FA: " + problemIdFirstAidAscentLookup.get(id) + ". FFA: " + fa;
					}
					LatLng l = markerHelper.getLatLngWithoutShifting(rst.getDouble("latitude"), rst.getDouble("longitude"));
					int numPitches = rst.getInt("num_pitches");
					boolean hasImages = rst.getInt("num_images")>0;
					boolean hasMovies = rst.getInt("num_movies")>0;
					boolean hasTopo = rst.getBoolean("has_topo");
					int numTicks = rst.getInt("num_ticks");
					double stars = rst.getDouble("stars");
					boolean ticked = rst.getBoolean("ticked");
					Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
					boolean danger = rst.getBoolean("danger");
					s.addProblem(id, lockedAdmin, lockedSuperadmin, nr, name, rock, comment, grade, GradeHelper.intToString(setup, grade), fa, numPitches, hasImages, hasMovies, hasTopo, l.getLat(), l.getLng(), numTicks, stars, ticked, t, danger);
				}
			}
		}
		if (!s.getProblems().isEmpty() && orderByGrade) {
			Collections.sort(s.getProblems(), Comparator.comparing(Sector.Problem::getGradeNumber).reversed());
		}
		logger.debug("getSector(authUserId={}, orderByGrade={}, reqId={}) - duration={}", authUserId, orderByGrade, reqId, stopwatch);
		return s;
	}

	private List<Svg> getSvgs(Setup s, int authUserId, int idMedia) throws SQLException {
		List<Svg> res = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT p.id problem_id, p.name problem_name, g.grade problem_grade, pt.subtype problem_subtype, g.group problem_grade_group, p.nr, s.id, s.path, s.has_anchor, s.texts, s.anchors, CASE WHEN p.type_id IN (1,2) THEN 1 ELSE 0 END prim, CASE WHEN t.id IS NOT NULL OR fa.user_id THEN 1 ELSE 0 END is_ticked, CASE WHEN t2.id IS NOT NULL THEN 1 ELSE 0 END is_todo, danger is_dangerous FROM ((((((svg s INNER JOIN problem p ON s.problem_id=p.id) INNER JOIN type pt ON p.type_id=pt.id) INNER JOIN grade g ON p.grade=g.grade_id AND g.t=?) LEFT JOIN fa ON (p.id=fa.problem_id AND fa.user_id=?)) LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?) LEFT JOIN todo t2 ON p.id=t2.problem_id AND t2.user_id=?) LEFT JOIN (SELECT problem_id, danger FROM guestbook WHERE (danger=1 OR resolved=1) AND id IN (SELECT max(id) id FROM guestbook WHERE (danger=1 OR resolved=1) GROUP BY problem_id)) danger ON p.id=danger.problem_id WHERE s.media_id=? ORDER BY p.nr DESC")) {
			ps.setString(1, s.getGradeSystem().toString());
			ps.setInt(2, authUserId);
			ps.setInt(3, authUserId);
			ps.setInt(4, authUserId);
			ps.setInt(5, idMedia);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					if (res == null) {
						res = new ArrayList<>();
					}
					int id = rst.getInt("id");
					int problemId = rst.getInt("problem_id");
					String problemName = rst.getString("problem_name");
					String problemGrade = rst.getString("problem_grade");
					int problemGradeGroup = rst.getInt("problem_grade_group");
					String problemSubtype = rst.getString("problem_subtype");
					int nr = rst.getInt("nr");
					String path = rst.getString("path");
					boolean hasAnchor = rst.getBoolean("has_anchor");
					String texts = rst.getString("texts");
					String anchors = rst.getString("anchors");
					boolean primary = rst.getBoolean("prim");
					boolean isTicked = rst.getBoolean("is_ticked");
					boolean isTodo = rst.getBoolean("is_todo");
					boolean isDangerous = rst.getBoolean("is_dangerous");
					res.add(new Svg(false, id, problemId, problemName, problemGrade, problemGradeGroup, problemSubtype, nr, path, hasAnchor, texts, anchors, primary, isTicked, isTodo, isDangerous));
				}
			}
		}
		return res;
	}

	private void setRandomMedia(Frontpage res, int authUserId, Setup setup, boolean fallbackSolution) throws SQLException {
		String sqlStr = "SELECT m.id id_media, m.width, m.height, a.id id_area, a.name area, s.id id_sector, s.name sector, p.id id_problem, p.name problem, p.grade,"
				+ " CONCAT('{\"id\":', u.id, ',\"name\":\"', TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))), '\"}') photographer," 
				+ " GROUP_CONCAT(DISTINCT CONCAT('{\"id\":', u2.id, ',\"name\":\"', TRIM(CONCAT(u2.firstname, ' ', COALESCE(u2.lastname,''))), '\"}') SEPARATOR ', ') tagged"
				+ " FROM ((((((((((media m INNER JOIN media_problem mp ON m.is_movie=0 AND m.id=mp.media_id) INNER JOIN problem p ON mp.problem_id=p.id AND p.locked_admin=0 AND p.locked_superadmin=0) INNER JOIN sector s ON p.sector_id=s.id AND s.locked_admin=0 AND s.locked_superadmin=0) INNER JOIN area a ON s.area_id=a.id AND a.locked_admin=0 AND a.locked_superadmin=0) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN user u ON m.photographer_user_id=u.id) INNER JOIN tick t ON p.id=t.problem_id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u2 ON mu.user_id=u2.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND m.deleted_user_id IS NULL"
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