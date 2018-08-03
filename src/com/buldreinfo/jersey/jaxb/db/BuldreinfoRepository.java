package com.buldreinfo.jersey.jaxb.db;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.imgscalr.Scalr;

import com.buldreinfo.jersey.jaxb.helpers.GradeHelper;
import com.buldreinfo.jersey.jaxb.helpers.MailSender;
import com.buldreinfo.jersey.jaxb.helpers.MarkerHelper;
import com.buldreinfo.jersey.jaxb.helpers.MarkerHelper.LatLng;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Comment;
import com.buldreinfo.jersey.jaxb.model.FaUser;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.NewMedia;
import com.buldreinfo.jersey.jaxb.model.OpenGraphImage;
import com.buldreinfo.jersey.jaxb.model.Permission;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Problem.Section;
import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.Register;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.Type;
import com.buldreinfo.jersey.jaxb.model.User;
import com.buldreinfo.jersey.jaxb.model.UserEdit;
import com.buldreinfo.jersey.jaxb.model.app.Region;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
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
	private static final String PATH = "/mnt/media/";
	private static Logger logger = LogManager.getLogger();
	private final DbConnection c;
	private final Gson gson = new Gson();

	protected BuldreinfoRepository(DbConnection c) {
		this.c = c;
	}

	public void addComment(int authUserId, Comment co) throws SQLException {
		Preconditions.checkArgument(authUserId>0);

		int parentId = 0;
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT MIN(id) FROM guestbook WHERE problem_id=?");
		ps.setInt(1, co.getIdProblem());
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			parentId = rst.getInt(1);
		}
		rst.close();
		ps.close();

		ps = c.getConnection().prepareStatement("INSERT INTO guestbook (post_time, message, problem_id, user_id, parent_id) VALUES (now(), ?, ?, ?, ?)");
		ps.setString(1, co.getComment());
		ps.setInt(2, co.getIdProblem());
		ps.setInt(3, authUserId);
		if (parentId == 0) {
			ps.setNull(4, Types.INTEGER);
		}
		else {
			ps.setInt(4, parentId);
		}

		ps.execute();
		ps.close();
	}

	public void addProblemMedia(int authUserId, Problem p, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, InterruptedException {
		Preconditions.checkArgument(authUserId != -1, "Insufficient permissions");
		for (NewMedia m : p.getNewMedia()) {
			final int idSector = 0;
			final int idArea = 0;
			addNewMedia(authUserId, p.getId(), idSector, idArea, m, multiPart);
		}
	}

	public void deleteMedia(int authUserId, int id) throws SQLException {
		boolean ok = false;
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT auth.write FROM ((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN permission auth ON (a.region_id=auth.region_id AND auth.user_id=?)) LEFT JOIN media_sector ms ON (s.id=ms.sector_id AND ms.media_id=?)) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem mp ON (p.id=mp.problem_id AND mp.media_id=?) GROUP BY auth.write");
		ps.setInt(1, authUserId);
		ps.setInt(2, id);
		ps.setInt(3, id);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			ok = rst.getInt(1)!=0;
		}
		rst.close();
		ps.close();
		Preconditions.checkArgument(ok, "Insufficient credentials");
		ps = c.getConnection().prepareStatement("UPDATE media SET deleted_user_id=?, deleted_timestamp=NOW() WHERE id=?");
		ps.setInt(1, authUserId);
		ps.setInt(2, id);
		ps.execute();
		ps.close();
	}

	public void deleteToken(String value) throws SQLException {
		Preconditions.checkNotNull(Strings.emptyToNull(value));
		PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM user_token WHERE token=?");
		ps.setString(1, value);
		ps.execute();
		ps.close();
	}

	public void forgotPassword(Setup setup, String username) throws SQLException, AddressException, UnsupportedEncodingException, MessagingException {
		final String token = UUID.randomUUID().toString();
		PreparedStatement ps = c.getConnection().prepareStatement("UPDATE user SET recover_token=? WHERE username=?");
		ps.setString(1, token);
		ps.setString(2, username);
		ps.execute();
		ps.close();
		StringBuilder builder = new StringBuilder();
		builder.append("Follow the instructions on " + setup.getUrl("/recover/" + token) + " to reset your password\n\n");
		builder.append("Please ignore this email if you did not request a new password from " + setup.getDomain());
		MailSender.sendMail(username, "Reset password (" + setup.getDomain() + ")", builder.toString());
	}

	public Area getArea(int authUserId, int reqId) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		MarkerHelper markerHelper = new MarkerHelper();
		Area a = null;
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.region_id, a.hidden, a.name, a.description, a.latitude, a.longitude FROM area a LEFT JOIN permission auth ON a.region_id=auth.region_id WHERE a.id=? AND (a.hidden=0 OR (auth.user_id=? AND (a.hidden<=1 OR auth.write>=a.hidden))) GROUP BY a.region_id, a.hidden, a.name, a.description, a.latitude, a.longitude");
		ps.setInt(1, reqId);
		ps.setInt(2, authUserId);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int regionId = rst.getInt("region_id");
			int visibility = rst.getInt("hidden");
			String name = rst.getString("name");
			String comment = rst.getString("description");
			LatLng l = markerHelper.getLatLng(rst.getDouble("latitude"), rst.getDouble("longitude"));
			List<Media> media = getMediaArea(reqId);
			if (media.isEmpty()) {
				media = null;
			}
			a = new Area(regionId, reqId, visibility, name, comment, l.getLat(), l.getLng(), -1, -1, media, null);
		}
		rst.close();
		ps.close();

		ps = c.getConnection().prepareStatement("SELECT s.id, s.hidden, s.name, s.description, s.parking_latitude, s.parking_longitude, s.polygon_coords, COUNT(DISTINCT p.id) num_problems FROM ((area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN permission auth ON a.region_id=auth.region_id WHERE a.id=? AND (s.hidden=0 OR (auth.user_id=? AND (s.hidden<=1 OR auth.write>=s.hidden))) GROUP BY s.id, s.hidden, s.name, s.description, s.parking_latitude, s.parking_longitude, s.polygon_coords ORDER BY s.name");
		ps.setInt(1, reqId);
		ps.setInt(2, authUserId);
		rst = ps.executeQuery();
		while (rst.next()) {
			int id = rst.getInt("id");
			int visibility = rst.getInt("hidden");
			String name = rst.getString("name");
			String comment = rst.getString("description");
			LatLng l = markerHelper.getLatLng(rst.getDouble("parking_latitude"), rst.getDouble("parking_longitude"));
			String polygonCoords = rst.getString("polygon_coords");
			int numProblems = rst.getInt("num_problems");
			a.addSector(id, visibility, name, comment, l.getLat(), l.getLng(), polygonCoords, numProblems);
		}
		rst.close();
		ps.close();
		a.orderSectors();
		logger.debug("getArea(authUserId={}, reqId={}) - duration={}", authUserId, reqId, stopwatch);
		return a;
	}

	public Collection<Area> getAreaList(int authUserId, int reqIdRegion) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		MarkerHelper markerHelper = new MarkerHelper();
		List<Area> res = new ArrayList<>();
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.region_id, a.id, a.hidden, a.name, a.description, a.latitude, a.longitude, COUNT(DISTINCT s.id) num_sectors, COUNT(DISTINCT p.id) num_problems FROM ((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND ((a.region_id=? AND a.hidden=0) OR (auth.user_id IS NOT NULL AND (a.hidden<=1 OR auth.write>=a.hidden))) GROUP BY a.id, a.hidden, a.name, a.description, a.latitude, a.longitude ORDER BY a.name");
		ps.setInt(1, authUserId);
		ps.setInt(2, reqIdRegion);
		ps.setInt(3, reqIdRegion);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int idRegion = rst.getInt("region_id");
			int id = rst.getInt("id");
			int visibility = rst.getInt("hidden");
			String name = rst.getString("name");
			String comment = rst.getString("description");
			LatLng l = markerHelper.getLatLng(rst.getDouble("latitude"), rst.getDouble("longitude"));
			int numSectors = rst.getInt("num_sectors");
			int numProblems = rst.getInt("num_problems");
			res.add(new Area(idRegion, id, visibility, name, comment, l.getLat(), l.getLng(), numSectors, numProblems, null, null));
		}
		rst.close();
		ps.close();
		logger.debug("getAreaList(authUserId={}, reqIdRegion={}) - res.size()={} - duration={}", authUserId, reqIdRegion, res.size(), stopwatch);
		return res;
	}

	public Frontpage getFrontpage(int authUserId, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Frontpage res = new Frontpage(setup.isShowLogoPlay(), setup.isShowLogoSis(), setup.isShowLogoBrv());
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT p.id) num_problems, COUNT(DISTINCT CASE WHEN p.latitude IS NOT NULL AND p.longitude IS NOT NULL THEN p.id END) num_problems_with_coordinates, COUNT(DISTINCT svg.problem_id) num_problems_with_topo FROM (((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?)) LEFT JOIN svg ON p.id=svg.problem_id WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR auth.user_id IS NOT NULL)");
		ps.setInt(1, authUserId);
		ps.setInt(2, setup.getIdRegion());
		ps.setInt(3, setup.getIdRegion());
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			res.setNumProblems(rst.getInt("num_problems"));
			res.setNumProblemsWithCoordinates(rst.getInt("num_problems_with_coordinates"));
			res.setNumProblemsWithTopo(rst.getInt("num_problems_with_topo"));
		}
		rst.close();
		ps.close();
		ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT CASE WHEN m.is_movie=0 THEN mp.id END) num_images, COUNT(DISTINCT CASE WHEN m.is_movie=1 THEN mp.id END) num_movies FROM ((((((media m INNER JOIN media_problem mp ON m.id=mp.media_id) INNER JOIN problem p ON mp.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND m.deleted_user_id IS NULL AND (a.region_id=? OR auth.user_id IS NOT NULL)");
		ps.setInt(1, authUserId);
		ps.setInt(2, setup.getIdRegion());
		ps.setInt(3, setup.getIdRegion());
		rst = ps.executeQuery();
		while (rst.next()) {
			res.setNumImages(rst.getInt("num_images"));
			res.setNumMovies(rst.getInt("num_movies"));
		}
		rst.close();
		ps.close();
		ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT t.id) num_ticks FROM (((((tick t INNER JOIN problem p ON t.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR auth.user_id IS NOT NULL)");
		ps.setInt(1, authUserId);
		ps.setInt(2, setup.getIdRegion());
		ps.setInt(3, setup.getIdRegion());
		rst = ps.executeQuery();
		while (rst.next()) {
			res.setNumTicks(rst.getInt("num_ticks"));
		}
		rst.close();
		ps.close();

		/**
		 * RandomMedia
		 */
		setRandomMedia(res, authUserId, setup.getIdRegion(), setup.getIdRegion() == 4); // Show all images on region 4 (brattelinjer.no), not only routes with >2 stars
		if (res.getRandomMedia() == null) {
			setRandomMedia(res, authUserId, setup.getIdRegion(), true);
		}

		/**
		 * Ascents
		 */
		String sqlStr = "SELECT p.id id_problem, p.name, DATE_FORMAT(t.date,'%d/%m-%y') date, u.id id_user, CONCAT(u.firstname, ' ', u.lastname) user, t.grade FROM (((((tick t INNER JOIN problem p ON (t.problem_id=p.id AND p.hidden=0)) INNER JOIN user u ON t.user_id=u.id INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR auth.user_id IS NOT NULL) GROUP BY p.id, p.name, t.date, u.id, u.firstname, u.lastname, t.grade, t.id ORDER BY t.date DESC, t.id DESC LIMIT 30";
		ps = c.getConnection().prepareStatement(sqlStr);
		ps.setInt(1, authUserId);
		ps.setInt(2, setup.getIdRegion());
		ps.setInt(3, setup.getIdRegion());
		rst = ps.executeQuery();
		while (rst.next()) {
			int idProblem = rst.getInt("id_problem");
			String problem = rst.getString("name");
			String date = rst.getString("date");
			int idUser = rst.getInt("id_user");
			String user = rst.getString("user");
			int grade = rst.getInt("grade");
			res.addAscent(idProblem, problem, GradeHelper.intToString(setup.getIdRegion(), grade), date, idUser, user);
		}
		rst.close();
		ps.close();

		/**
		 * FAs
		 */
		sqlStr = "SELECT a.id id_area, a.name area, s.id id_sector, s.name sector, p.id, p.name, DATE_FORMAT(p.fa_date,'%d/%m-%y') date, p.grade"
				+ " FROM ((((problem p INNER JOIN sector s ON p.sector_id=s.id AND p.hidden=0) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?)"
				+ " WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR auth.user_id IS NOT NULL)"
				+ " GROUP BY a.id, a.name, s.id, s.name, p.id, p.name, p.fa_date, p.grade"
				+ " ORDER BY p.fa_date DESC, p.id DESC LIMIT 30";
		ps = c.getConnection().prepareStatement(sqlStr);
		ps.setInt(1, authUserId);
		ps.setInt(2, setup.getIdRegion());
		ps.setInt(3, setup.getIdRegion());
		rst = ps.executeQuery();
		while (rst.next()) {
			int idArea = rst.getInt("id_area");
			String area = rst.getString("area");
			int idSector = rst.getInt("id_sector");
			String sector = rst.getString("sector");
			int idProblem = rst.getInt("id");
			String problem = rst.getString("name");
			String date = rst.getString("date");
			int grade = rst.getInt("grade");
			res.addFa(idArea, area, idSector, sector, idProblem, problem, GradeHelper.intToString(setup.getIdRegion(), grade), date);
		}
		rst.close();
		ps.close();

		/**
		 * Medias
		 */
		sqlStr = "SELECT p.id, p.name, m.is_movie, p.grade"
				+ " FROM ((((((media m INNER JOIN media_problem mp ON (m.deleted_user_id IS NULL AND m.id=mp.media_id)) INNER JOIN problem p ON (mp.problem_id=p.id AND p.hidden=0)) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?)"
				+ " WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR auth.user_id IS NOT NULL)"
				+ " GROUP BY m.id, p.id, p.name, m.is_movie, p.grade"
				+ " ORDER BY m.id DESC LIMIT 30";
		ps = c.getConnection().prepareStatement(sqlStr);
		ps.setInt(1, authUserId);
		ps.setInt(2, setup.getIdRegion());
		ps.setInt(3, setup.getIdRegion());
		rst = ps.executeQuery();
		while (rst.next()) {
			int idProblem = rst.getInt("id");
			String problem = rst.getString("name");
			int grade = rst.getInt("grade");
			String type = rst.getBoolean("is_movie")? "video" : "image";
			res.addMedia(idProblem, problem, GradeHelper.intToString(setup.getIdRegion(), grade), type);
		}
		rst.close();
		ps.close();

		/**
		 * Comments
		 */
		ps = c.getConnection().prepareStatement("SELECT DATE_FORMAT(MAX(g.post_time),'%d/%m-%y %H:%i') date, p.id, p.name FROM (((((guestbook g INNER JOIN problem p ON g.problem_id=p.id AND p.hidden=0) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR auth.user_id IS NOT NULL) GROUP BY p.id, p.name ORDER BY MAX(g.post_time) DESC LIMIT 30");
		ps.setInt(1, authUserId);
		ps.setInt(2, setup.getIdRegion());
		ps.setInt(3, setup.getIdRegion());
		rst = ps.executeQuery();
		while (rst.next()) {
			String date = rst.getString("date");
			int idProblem = rst.getInt("id");
			String problem = rst.getString("name");
			res.addComment(date, idProblem, problem);
		}
		rst.close();
		ps.close();
		logger.debug("getFrontpage(authUserId={}, setup={}) - duration={}", authUserId, setup, stopwatch);
		return res;
	}

	public Path getImage(boolean webP, int id) throws SQLException, IOException {
		Path p = null;
		if (webP) {
			p = Paths.get(PATH + "web/webp").resolve(String.valueOf(id/100*100)).resolve(id + ".webp");		
		}
		else {
			p = Paths.get(PATH + "web/jpg").resolve(String.valueOf(id/100*100)).resolve(id + ".jpg");
		}
		Preconditions.checkArgument(Files.exists(p), p.toString() + " does not exist");
		return p;
	}
	
	public OpenGraphImage getImage(Setup setup, int idMedia) {
		OpenGraphImage res = null;
		try {
			Path p = Paths.get(PATH + "web/jpg").resolve(String.valueOf(idMedia/100*100)).resolve(idMedia + ".jpg");
			if (Files.exists(p)) {
				BufferedImage b = ImageIO.read(p.toFile());
				String http = setup.getUrl("/buldreinfo_media/jpg/" + String.valueOf(idMedia/100*100) + "/" + idMedia + ".jpg");
				res = new OpenGraphImage(http, String.valueOf(b.getWidth()), String.valueOf(b.getHeight()));
				b.flush();
			}
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return res;
	}

	public Permission getPermission(String token, String username, String password) throws SQLException, NoSuchAlgorithmException {
		// Validate token
		if (!Strings.isNullOrEmpty(token)) {
			PreparedStatement ps = c.getConnection().prepareStatement("SELECT ut.token, auth.region_id, MAX(auth.write) permission FROM user_token ut LEFT JOIN permission auth ON ut.user_id=auth.user_id WHERE ut.token=? GROUP BY ut.token, auth.region_id");
			ps.setString(1, token);
			token = null;
			List<Integer> adminRegionIds = new ArrayList<>();
			List<Integer> superAdminRegionIds = new ArrayList<>();
			ResultSet rst = ps.executeQuery();
			while (rst.next()) {
				token = rst.getString("token");
				int idRegion = rst.getInt("region_id");
				int permission = rst.getInt("permission");
				if (idRegion > 0) {
					if (permission >= 1) {
						adminRegionIds.add(idRegion);
					}
					if (permission == 2) {
						superAdminRegionIds.add(idRegion);
					}
				}
			}
			rst.close();
			ps.close();
			if (!Strings.isNullOrEmpty(token)) {
				return new Permission(token, adminRegionIds, superAdminRegionIds);
			}
		}

		// Login with username and password
		if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password)) {
			logger.warn("getToken(token={}, username={}, password={}) - invalid parameters", token, username, password);
			return null;
		}
		int idUser = 0;
		token = null;
		List<Integer> adminRegionIds = new ArrayList<>();
		List<Integer> superAdminRegionIds = new ArrayList<>();
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT u.id, auth.region_id, MAX(auth.write) permission FROM user u LEFT JOIN permission auth ON u.id=auth.user_id WHERE u.username=? AND u.password=? GROUP BY u.id, auth.region_id");
		ps.setString(1, username);
		ps.setString(2, hashPassword(password));
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			idUser = rst.getInt("id");
			int idRegion = rst.getInt("region_id");
			int permission = rst.getInt("permission");
			if (idRegion > 0) {
				if (permission >= 1) {
					adminRegionIds.add(idRegion);
				}
				if (permission == 2) {
					superAdminRegionIds.add(idRegion);
				}
			}
		}
		rst.close();
		ps.close();
		token = UUID.randomUUID().toString();

		// Add token to db
		Preconditions.checkNotNull(token, "token cannot be null");
		Preconditions.checkArgument(idUser != 0, "idUser cannot be 0");
		ps = c.getConnection().prepareStatement("INSERT INTO user_token (user_id, token) VALUES (?, ?)");
		ps.setInt(1, idUser);
		ps.setString(2, token);
		ps.execute();
		ps.close();
		return new Permission(token, adminRegionIds, superAdminRegionIds);
	}

	public List<Problem> getProblem(int authUserId, int reqRegionId, int reqId, int reqGrade) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		MarkerHelper markerHelper = new MarkerHelper();
		List<Problem> res = new ArrayList<>();
		String condition = null;
		if (reqId == 0 && reqGrade>=0) {
			condition = "p.grade=?";
		}
		else if (reqId == 0 && reqGrade==-1) {
			condition = "p.hidden=2 AND p.grade!=?";
		}
		else {
			condition = "p.id=?";
		}
		String sqlStr = "SELECT a.id area_id, a.hidden area_hidden, a.name area_name, s.id sector_id, s.hidden sector_hidden, s.name sector_name, s.parking_latitude sector_lat, s.parking_longitude sector_lng, p.id, p.hidden hidden, p.nr, p.name, p.description, DATE_FORMAT(p.fa_date,'%Y-%m-%d') fa_date, DATE_FORMAT(p.fa_date,'%d/%m-%y') fa_date_hr,"
				+ " ROUND((IFNULL(AVG(NULLIF(t.grade,0)), p.grade) + p.grade)/2) grade, p.grade original_grade, p.latitude, p.longitude,"
				+ " group_concat(DISTINCT CONCAT('{\"id\":', u.id, ',\"firstname\":\"', u.firstname, '\",\"surname\":\"', u.lastname, '\",\"initials\":\"', LEFT(u.firstname,1), LEFT(u.lastname,1), '\"}') ORDER BY u.firstname, u.lastname SEPARATOR ',') fa,"
				+ " COUNT(DISTINCT t.id) num_ticks, ROUND(ROUND(AVG(t.stars)*2)/2,1) stars,"
				+ " MAX(CASE WHEN (t.user_id=? OR u.id=?) THEN 1 END) ticked,"
				+ " ty.id type_id, ty.type, ty.subtype"
				+ " FROM ((((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN tick t ON t.problem_id=p.id) LEFT JOIN permission auth ON r.id=auth.region_id"
				+ " WHERE (?=0 OR rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?))"
				+ "   AND " + condition
				+ "   AND (p.hidden=0 OR (auth.user_id=? AND (p.hidden<=1 OR auth.write>=p.hidden)))"
				+ "   AND (?=0 OR r.id=? OR auth.user_id IS NOT NULL)"
				+ " GROUP BY a.id, a.hidden, a.name, s.id, s.hidden, s.name, s.parking_latitude, s.parking_longitude, p.id, p.hidden, p.nr, p.name, p.description, p.grade, p.latitude, p.longitude, p.fa_date, ty.id, ty.type, ty.subtype"
				+ " ORDER BY p.name";
		PreparedStatement ps = c.getConnection().prepareStatement(sqlStr);
		ps.setInt(1, authUserId);
		ps.setInt(2, authUserId);
		ps.setInt(3, reqRegionId);
		ps.setInt(4, reqRegionId);
		if (reqId != 0) {
			ps.setInt(5, reqId);
		}
		else {
			ps.setInt(5, reqGrade);
		}
		ps.setInt(6, authUserId);
		ps.setInt(7, reqRegionId);
		ps.setInt(8, reqRegionId);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int areaId = rst.getInt("area_id");
			int areaVisibility = rst.getInt("area_hidden");
			String areaName = rst.getString("area_name");
			int sectorId = rst.getInt("sector_id");
			int sectorVisibility = rst.getInt("sector_hidden");
			String sectorName = rst.getString("sector_name");
			LatLng sectorL = markerHelper.getLatLng(rst.getDouble("sector_lat"), rst.getDouble("sector_lng"));
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
			List<FaUser> fa = Strings.isNullOrEmpty(faStr)? null: gson.fromJson("[" + faStr + "]", new TypeToken<ArrayList<FaUser>>(){}.getType());
			LatLng l = markerHelper.getLatLng(rst.getDouble("latitude"), rst.getDouble("longitude"));
			int numTicks = rst.getInt("num_ticks");
			double stars = rst.getDouble("stars");
			boolean ticked = rst.getBoolean("ticked");
			List<Media> media = getMediaProblem(reqRegionId, sectorId, id);
			Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
			res.add(new Problem(areaId, areaVisibility, areaName, sectorId, sectorVisibility, sectorName, sectorL.getLat(), sectorL.getLng(), id, visibility, nr, name, comment, GradeHelper.intToString(reqRegionId, grade), GradeHelper.intToString(reqRegionId, originalGrade), faDate, faDateHr, fa, l.getLat(), l.getLng(), media, numTicks, stars, ticked, null, t));
		}
		rst.close();
		ps.close();
		if (reqId!=0) {
			for (Problem p : res) {
				// Ascents
				sqlStr = "SELECT t.id id_tick, u.id id_user, CAST(t.date AS char) date, CONCAT(u.firstname, ' ', u.lastname) name, t.comment, t.stars, t.grade FROM tick t, user u WHERE t.problem_id=? AND t.user_id=u.id ORDER BY t.date";
				ps = c.getConnection().prepareStatement(sqlStr);
				ps.setInt(1, p.getId());
				rst = ps.executeQuery();
				while (rst.next()) {
					int id = rst.getInt("id_tick");
					int idUser = rst.getInt("id_user");
					String date = rst.getString("date");
					String name = rst.getString("name");
					String comment = rst.getString("comment");
					double stars = rst.getDouble("stars");
					int grade = rst.getInt("grade");
					boolean writable = idUser == authUserId;
					p.addTick(id, idUser, date, name, GradeHelper.intToString(reqRegionId, grade), comment, stars, writable);
				}
				rst.close();
				ps.close();
				// Comments
				ps = c.getConnection().prepareStatement("SELECT CAST(g.post_time AS char) date, u.id, CONCAT(u.firstname, ' ', u.lastname) name, g.message FROM guestbook g, user u WHERE g.problem_id=? AND g.user_id=u.id ORDER BY g.post_time");
				ps.setInt(1, p.getId());
				rst = ps.executeQuery();
				while (rst.next()) {
					String date = rst.getString("date");
					int idUser = rst.getInt("id");
					String name = rst.getString("name");
					String message = rst.getString("message");
					p.addComment(date, idUser, name, message);
				}
				rst.close();
				ps.close();
				// Sections
				ps = c.getConnection().prepareStatement("SELECT id, nr, description, grade FROM problem_section WHERE problem_id=? ORDER BY nr");
				ps.setInt(1, p.getId());
				rst = ps.executeQuery();
				while (rst.next()) {
					int id = rst.getInt("id");
					int nr = rst.getInt("nr");
					String description = rst.getString("description");
					int grade = rst.getInt("grade");
					p.addSection(id, nr, description, GradeHelper.intToString(reqRegionId, grade));
				}
				rst.close();
				ps.close();
			}
		}
		logger.debug("getProblem(authUserId={}, reqRegionId={}, reqId={}, reqGrade={}) - duration={} - res.size()={}", authUserId, reqRegionId, reqId, reqGrade, stopwatch, res.size());
		return res;
	}

	public Profile getProfile(String username, String password) throws NoSuchAlgorithmException, SQLException {
		Profile res = null;
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT u.id, u.username email, TRIM(CONCAT(u.firstname, ' ', u.lastname)) nickname FROM user u WHERE u.username=? AND u.password=?");
		ps.setString(1, username);
		ps.setString(2, password);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int id = rst.getInt("id");
			String email = rst.getString("email");
			String nickname = rst.getString("nickname");
			res = new Profile(id, email, nickname);
		}
		rst.close();
		ps.close();
		return res;
	}

	public Collection<Region> getRegions(String uniqueId) throws SQLException {
		final int idUser = upsertUserReturnId(uniqueId);
		MarkerHelper markerHelper = new MarkerHelper();
		Map<Integer, Region> regionMap = new HashMap<>();
		Map<Integer, com.buldreinfo.jersey.jaxb.model.app.Area> areaMap = new HashMap<>();
		Map<Integer, com.buldreinfo.jersey.jaxb.model.app.Sector> sectorMap = new HashMap<>();
		Map<Integer, com.buldreinfo.jersey.jaxb.model.app.Problem> problemMap = new HashMap<>();
		// Regions
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.id, r.name FROM region r INNER JOIN region_type rt ON r.id=rt.region_id WHERE rt.type_id=1");
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int id = rst.getInt("id");
			String name = rst.getString("name");
			Region r = new Region(id, name);
			regionMap.put(r.getId(), r);
		}
		rst.close();
		ps.close();
		// Areas
		ps = c.getConnection().prepareStatement("SELECT a.region_id, a.id, a.name, a.description, a.latitude, a.longitude FROM ((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON a.region_id=auth.region_id WHERE rt.type_id=1 AND (a.hidden=0 OR (auth.user_id=? AND (a.hidden<=1 OR auth.write>=a.hidden))) GROUP BY a.region_id, a.id, a.name, a.description, a.latitude, a.longitude");
		ps.setInt(1, idUser);
		rst = ps.executeQuery();
		while (rst.next()) {
			int regionId = rst.getInt("region_id");
			Region r = regionMap.get(regionId);
			if (r != null) {
				int id = rst.getInt("id");
				String name = rst.getString("name");
				String comment = rst.getString("description");
				LatLng l = markerHelper.getLatLng(rst.getDouble("latitude"), rst.getDouble("longitude"));
				com.buldreinfo.jersey.jaxb.model.app.Area a = new com.buldreinfo.jersey.jaxb.model.app.Area(regionId, id, name, comment, l.getLat(), l.getLng());
				r.getAreas().add(a);
				areaMap.put(a.getId(), a);
			}
		}
		rst.close();
		ps.close();
		// Sectors
		ps = c.getConnection().prepareStatement("SELECT s.area_id, s.id, s.name, s.description, s.parking_latitude, s.parking_longitude FROM (((sector s INNER JOIN area a ON a.id=s.area_id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON r.id=auth.region_id WHERE rt.type_id=1 AND (s.hidden=0 OR (auth.user_id=? AND (s.hidden<=1 OR auth.write>=s.hidden))) GROUP BY s.area_id, s.id, s.name, s.description, s.parking_latitude, s.parking_longitude");
		ps.setInt(1, idUser);
		rst = ps.executeQuery();
		while (rst.next()) {
			int areaId = rst.getInt("area_id");
			com.buldreinfo.jersey.jaxb.model.app.Area a = areaMap.get(areaId);
			if (a != null) {
				int id = rst.getInt("id");
				String name = rst.getString("name");
				String comment = rst.getString("description");
				LatLng l = markerHelper.getLatLng(rst.getDouble("parking_latitude"), rst.getDouble("parking_longitude"));
				com.buldreinfo.jersey.jaxb.model.app.Sector s = new com.buldreinfo.jersey.jaxb.model.app.Sector(areaId, id, name, comment, l.getLat(), l.getLng());
				a.getSectors().add(s);
				sectorMap.put(s.getId(), s);
			}
		}
		rst.close();
		ps.close();
		// Problems
		ps = c.getConnection().prepareStatement("SELECT p.sector_id, p.id, p.nr, p.name, p.description, p.grade, TRIM(CONCAT(IFNULL(p.fa_date,''), ' ', GROUP_CONCAT(DISTINCT CONCAT(u.firstname, ' ', u.lastname) ORDER BY u.firstname SEPARATOR ', '))) fa, p.latitude, p.longitude FROM ((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN permission auth ON r.id=auth.region_id WHERE rt.type_id=1 AND (p.hidden=0 OR (auth.user_id=? AND (p.hidden<=1 OR auth.write>=p.hidden))) GROUP BY p.sector_id, p.id, p.nr, p.name, p.description, p.grade, p.fa_date, p.latitude, p.longitude");
		ps.setInt(1, idUser);
		rst = ps.executeQuery();
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
				com.buldreinfo.jersey.jaxb.model.app.Problem p = new com.buldreinfo.jersey.jaxb.model.app.Problem(sectorId, id, nr, name, comment, grade, fa, l.getLat(), l.getLng());
				s.getProblems().add(p);
				problemMap.put(p.getId(), p);
			}
		}
		rst.close();
		ps.close();
		// Media (sectors)
		ps = c.getConnection().prepareStatement("SELECT ms.sector_id, m.id, m.is_movie FROM (((((media m INNER JOIN media_sector ms ON m.id=ms.media_id AND m.deleted_user_id IS NULL) INNER JOIN sector s ON ms.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON a.region_id=auth.region_id WHERE rt.type_id=1 AND (a.hidden=0 OR (auth.user_id=? AND (a.hidden<=1 OR auth.write>=a.hidden))) GROUP BY ms.sector_id, m.id, m.is_movie ORDER BY m.is_movie, m.id");
		ps.setInt(1, idUser);
		rst = ps.executeQuery();
		while (rst.next()) {
			int sectorId = rst.getInt("sector_id");
			com.buldreinfo.jersey.jaxb.model.app.Sector s = sectorMap.get(sectorId);
			if (s != null) {
				int id = rst.getInt("id");
				boolean isMovie = rst.getBoolean("is_movie");
				s.getMedia().add(new com.buldreinfo.jersey.jaxb.model.app.Media(id, isMovie, 0));
			}
		}
		rst.close();
		ps.close();
		// Media (problems)
		ps = c.getConnection().prepareStatement("SELECT mp.problem_id, m.id, m.is_movie, mp.milliseconds t FROM ((((((media m INNER JOIN media_problem mp ON m.id=mp.media_id AND m.deleted_user_id IS NULL) INNER JOIN problem p ON mp.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON r.id=auth.region_id WHERE rt.type_id=1 AND (a.hidden=0 OR (auth.user_id=? AND (a.hidden<=1 OR auth.write>=a.hidden))) GROUP BY mp.problem_id, m.id, m.is_movie, mp.milliseconds ORDER BY m.is_movie, m.id");
		ps.setInt(1, idUser);
		rst = ps.executeQuery();
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
		rst.close();
		ps.close();
		// Return
		return regionMap.values();
	}

	public List<Search> getSearch(int authUserId, int idRegion, SearchRequest sr) throws SQLException {
		List<Search> res = new ArrayList<>();
		// Areas
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, a.name, a.hidden FROM ((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON r.id=auth.region_id WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR auth.user_id IS NOT NULL) AND (a.name LIKE ? OR a.name LIKE ?) AND (a.hidden=0 OR (auth.user_id=? AND (a.hidden<=1 OR auth.write>=a.hidden))) GROUP BY a.id, a.name, a.hidden ORDER BY a.name LIMIT 18");
		ps.setInt(1, idRegion);
		ps.setInt(2, idRegion);
		ps.setString(3, sr.getValue() + "%");
		ps.setString(4, "% " + sr.getValue() + "%");
		ps.setInt(5, authUserId);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int id = rst.getInt("id");
			String name = rst.getString("name");
			int visibility = rst.getInt("hidden");
			res.add(new Search("A", "/area/" + id, name, visibility));
		}
		rst.close();
		ps.close();
		// Sectors
		ps = c.getConnection().prepareStatement("SELECT s.id, a.name area_name, s.name sector_name, s.hidden FROM (((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN permission auth ON r.id=auth.region_id WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR auth.user_id IS NOT NULL) AND (s.name LIKE ? OR s.name LIKE ?) AND (s.hidden=0 OR (auth.user_id=? AND (s.hidden<=1 OR auth.write>=s.hidden))) GROUP BY s.id, a.name, s.name, a.hidden ORDER BY a.name, s.name LIMIT 18");
		ps.setInt(1, idRegion);
		ps.setInt(2, idRegion);
		ps.setString(3, sr.getValue() + "%");
		ps.setString(4, "% " + sr.getValue() + "%");
		ps.setInt(5, authUserId);
		rst = ps.executeQuery();
		while (rst.next()) {
			int id = rst.getInt("id");
			String areaName = rst.getString("area_name");
			String sectorName = rst.getString("sector_name");
			int visibility = rst.getInt("hidden");
			res.add(new Search("S", "/sector/" + id, areaName + " | " + sectorName, visibility));
		}
		rst.close();
		ps.close();
		// Problems
		ps = c.getConnection().prepareStatement("SELECT p.id, p.name, p.grade, p.hidden FROM ((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN permission auth ON r.id=auth.region_id WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR auth.user_id IS NOT NULL) AND (p.name LIKE ? OR p.name LIKE ?) AND (p.hidden=0 OR (auth.user_id=? AND (p.hidden<=1 OR auth.write>=p.hidden))) GROUP BY p.id, p.name, a.hidden ORDER BY p.name, p.grade LIMIT 18");
		ps.setInt(1, idRegion);
		ps.setInt(2, idRegion);
		ps.setString(3, sr.getValue() + "%");
		ps.setString(4, "% " + sr.getValue() + "%");
		ps.setInt(5, authUserId);
		rst = ps.executeQuery();
		while (rst.next()) {
			int id = rst.getInt("id");
			String name = rst.getString("name");
			int grade = rst.getInt("grade");
			int visibility = rst.getInt("hidden");
			res.add(new Search(GradeHelper.intToString(idRegion, grade), "/problem/" + id, name, visibility));
		}
		rst.close();
		ps.close();
		// Truncate result to max 18
		while (res.size() > 18) {
			int numAreas = 0;
			int numSectors = 0;
			int numProblems = 0;
			Search lastArea = null;
			Search lastSector = null;
			Search lastProblem = null;
			for (Search s : res) {
				switch (s.getAvatar()) {
				case "A":
					numAreas++;
					lastArea = s;
					break;
				case "S":
					numSectors++;
					lastSector = s;
					break;
				default:
					numProblems++;
					lastProblem = s;
					break;
				}
			}
			if (numAreas > 4 || numSectors > 4) {
				res.remove(numAreas > numSectors? lastArea : lastSector);
			}
			else if (numProblems > 10) {
				res.remove(lastProblem);
			}
		}
		return res;
	}

	public Sector getSector(int authUserId, int regionId, int reqId) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		MarkerHelper markerHelper = new MarkerHelper();
		Sector s = null;
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id area_id, a.hidden area_hidden, a.name area_name, s.hidden, s.name, s.description, s.parking_latitude, s.parking_longitude, s.polygon_coords FROM (area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN permission auth ON a.region_id=auth.region_id WHERE s.id=? AND (s.hidden=0 OR (auth.user_id=? AND (s.hidden<=1 OR auth.write>=s.hidden))) GROUP BY a.id, a.hidden, a.name, s.hidden, s.name, s.description, s.parking_latitude, s.parking_longitude, s.polygon_coords"); 
		ps.setInt(1, reqId);
		ps.setInt(2, authUserId);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int areaId = rst.getInt("area_id");
			int areaVisibility = rst.getInt("area_hidden");
			String areaName = rst.getString("area_name");
			int visibility = rst.getInt("hidden");
			String name = rst.getString("name");
			String comment = rst.getString("description");
			LatLng l = markerHelper.getLatLng(rst.getDouble("parking_latitude"), rst.getDouble("parking_longitude"));
			String polygonCoords = rst.getString("polygon_coords");
			List<Media> media = getMediaSector(reqId, 0);
			media.addAll(getMediaArea(areaId));
			if (media.isEmpty()) {
				media = null;
			}
			s = new Sector(areaId, areaVisibility, areaName, reqId, visibility, name, comment, l.getLat(), l.getLng(), polygonCoords, media, null);
		}
		rst.close();
		ps.close();
		String sqlStr = "SELECT p.id, p.hidden, p.nr, p.name, p.description, ROUND((IFNULL(AVG(NULLIF(t.grade,0)), p.grade) + p.grade)/2) grade, p.latitude, p.longitude,"
				+ " COUNT(DISTINCT CASE WHEN m.is_movie=0 THEN m.id END) num_images,"
				+ " COUNT(DISTINCT CASE WHEN m.is_movie=1 THEN m.id END) num_movies,"
				+ " group_concat(DISTINCT CONCAT('{\"id\":', u.id, ',\"firstname\":\"', u.firstname, '\",\"surname\":\"', u.lastname, '\",\"initials\":\"', LEFT(u.firstname,1), LEFT(u.lastname,1), '\"}') ORDER BY u.firstname, u.lastname SEPARATOR ',') fa,"
				+ " COUNT(DISTINCT t.id) num_ticks, ROUND(ROUND(AVG(t.stars)*2)/2,1) stars,"
				+ " MAX(CASE WHEN (t.user_id=? OR u.id=?) THEN 1 END) ticked,"
				+ " ty.id type_id, ty.type, ty.subtype"
				+ " FROM (((((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN permission auth ON a.region_id=auth.region_id) LEFT JOIN (media_problem mp LEFT JOIN media m ON mp.media_id=m.id AND m.deleted_user_id IS NULL) ON p.id=mp.problem_id) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN tick t ON p.id=t.problem_id"
				+ " WHERE p.sector_id=?"
				+ "   AND (p.hidden=0 OR (auth.user_id=? AND (p.hidden<=1 OR auth.write>=p.hidden)))"
				+ " GROUP BY p.id, p.hidden, p.nr, p.name, p.description, p.grade, p.latitude, p.longitude, ty.id, ty.type, ty.subtype"
				+ " ORDER BY p.nr";
		ps = c.getConnection().prepareStatement(sqlStr);
		ps.setInt(1, authUserId);
		ps.setInt(2, authUserId);
		ps.setInt(3, reqId);
		ps.setInt(4, authUserId);
		rst = ps.executeQuery();
		while (rst.next()) {
			int id = rst.getInt("id");
			int visibility = rst.getInt("hidden");
			int nr = rst.getInt("nr");
			int grade = rst.getInt("grade");
			String name = rst.getString("name");
			String comment = rst.getString("description");
			String faStr = rst.getString("fa");
			List<FaUser> fa = Strings.isNullOrEmpty(faStr)? null: gson.fromJson("[" + faStr + "]", new TypeToken<ArrayList<FaUser>>(){}.getType());
			LatLng l = markerHelper.getLatLng(rst.getDouble("latitude"), rst.getDouble("longitude"));
			int numImages = rst.getInt("num_images");
			int numMovies = rst.getInt("num_movies");
			int numTicks = rst.getInt("num_ticks");
			double stars = rst.getDouble("stars");
			boolean ticked = rst.getBoolean("ticked");
			Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
			s.addProblem(id, visibility, nr, name, comment, GradeHelper.intToString(regionId, grade), fa, numImages, numMovies, l.getLat(), l.getLng(), numTicks, stars, ticked, t);
		}
		rst.close();
		ps.close();
		logger.debug("getSector(authUserId={}, reqId={}) - duration={}", authUserId, reqId, stopwatch);
		return s;
	}

	public String getSitemapTxt(Setup setup) throws SQLException {
		List<String> urls = new ArrayList<>();
		// Fixed urls
		urls.add(setup.getUrl(null));
		urls.add(setup.getUrl("/ethics"));
		urls.add(setup.getUrl("/gpl-3.0.txt"));
		urls.add(setup.getUrl("/browse"));
		// Users
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT f.user_id FROM area a, sector s, problem p, fa f WHERE a.region_id=? AND a.hidden=0 AND a.id=s.area_id AND s.hidden=0 AND s.id=p.sector_id AND p.hidden=0 AND p.id=f.problem_id GROUP BY f.user_id UNION SELECT t.user_id FROM area a, sector s, problem p, tick t WHERE a.region_id=? AND a.hidden=0 AND a.id=s.area_id AND s.hidden=0 AND s.id=p.sector_id AND p.hidden=0 AND p.id=t.problem_id GROUP BY t.user_id");
		ps.setInt(1, setup.getIdRegion());
		ps.setInt(2, setup.getIdRegion());
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int userId = rst.getInt("user_id");
			urls.add(setup.getUrl("/user/" + userId));
		}
		rst.close();
		ps.close();
		// Areas, sectors, problems
		ps = c.getConnection().prepareStatement("SELECT CONCAT('/area/', a.id) url FROM region r, area a WHERE r.id=? AND r.id=a.region_id AND a.hidden=0 UNION SELECT CONCAT('/sector/', s.id) url FROM region r, area a, sector s WHERE r.id=? AND r.id=a.region_id AND a.hidden=0 AND a.id=s.area_id AND s.hidden=0 UNION SELECT CONCAT('/problem/', p.id) url FROM region r, area a, sector s, problem p WHERE r.id=? AND r.id=a.region_id AND a.hidden=0 AND a.id=s.area_id AND s.hidden=0 AND s.id=p.sector_id AND p.hidden=0");
		ps.setInt(1, setup.getIdRegion());
		ps.setInt(2, setup.getIdRegion());
		ps.setInt(3, setup.getIdRegion());
		rst = ps.executeQuery();
		while (rst.next()) {
			urls.add(setup.getUrl(rst.getString("url")));
		}
		rst.close();
		ps.close();
		return Joiner.on("\r\n").join(urls);
	}

	public List<Type> getTypes(int regionId) throws SQLException {
		List<Type> res = new ArrayList<>();
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT t.id, t.type, t.subtype FROM type t, region_type rt WHERE t.id=rt.type_id AND rt.region_id=? GROUP BY t.id, t.type, t.subtype ORDER BY t.id, t.type, t.subtype");
		ps.setInt(1, regionId);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int id = rst.getInt("id");
			String type = rst.getString("type");
			String subtype = rst.getString("subtype");
			res.add(new Type(id, type, subtype));
		}
		rst.close();
		ps.close();
		return res;
	}

	public User getUser(int authUserId, int regionId, int reqId) throws SQLException {
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
		String sqlStr = "SELECT CONCAT(u.firstname, ' ', u.lastname) name, COUNT(DISTINCT CASE WHEN mC.is_movie=0 THEN mC.id END) num_images_created, COUNT(DISTINCT CASE WHEN mC.is_movie=1 THEN mC.id END) num_videos_created, COUNT(DISTINCT CASE WHEN mT.is_movie=0 THEN mT.id END) num_image_tags, COUNT(DISTINCT CASE WHEN mT.is_movie=1 THEN mT.id END) num_video_tags"
				+ " FROM ((user u LEFT JOIN media mC ON u.id=mC.photographer_user_id AND mC.deleted_user_id IS NULL) LEFT JOIN media_user mu ON u.id=mu.user_id) LEFT JOIN media mT ON mu.media_id=mT.id AND mT.deleted_user_id IS NULL WHERE u.id=? GROUP BY u.firstname, u.lastname";
		PreparedStatement ps = c.getConnection().prepareStatement(sqlStr);
		ps.setInt(1, reqId);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			String name = rst.getString("name");
			int numImagesCreated = rst.getInt("num_images_created");
			int numVideosCreated = rst.getInt("num_videos_created");
			int numImageTags = rst.getInt("num_image_tags");
			int numVideoTags = rst.getInt("num_video_tags");
			res = new User(readOnly, reqId, name, numImagesCreated, numVideosCreated, numImageTags, numVideoTags);
		}
		rst.close();
		ps.close();
		if (res == null) {
			return res;
		}

		sqlStr = "SELECT t.id id_tick, p.id id_problem, p.hidden, p.name, CASE WHEN (t.id IS NOT NULL) THEN t.comment ELSE p.description END comment, DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%Y-%m-%d') date, DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%d/%m-%y') date_hr, t.stars, CASE WHEN (f.user_id IS NOT NULL) THEN f.user_id ELSE 0 END fa, (CASE WHEN t.id IS NOT NULL THEN t.grade ELSE p.grade END) grade"
				+ " FROM ((((((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?)) LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?) LEFT JOIN fa f ON (p.id=f.problem_id AND f.user_id=?)"
				+ " WHERE (t.user_id IS NOT NULL OR f.user_id IS NOT NULL) AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR auth.user_id IS NOT NULL) AND (p.hidden=0 OR (auth.user_id IS NOT NULL AND (p.hidden<=1 OR auth.write>=p.hidden)))"
				+ " GROUP BY t.id, p.id, p.hidden, p.name, p.description, p.fa_date, t.date, t.stars, t.grade, p.grade"
				+ " ORDER BY CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END DESC, p.id DESC;";
		ps = c.getConnection().prepareStatement(sqlStr);
		ps.setInt(1, authUserId);
		ps.setInt(2, reqId);
		ps.setInt(3, reqId);
		ps.setInt(4, regionId);
		ps.setInt(5, regionId);
		rst = ps.executeQuery();
		while (rst.next()) {
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
			res.addTick(id, idProblem, visibility, name, comment, date, dateHr, stars, fa, GradeHelper.intToString(regionId, grade), grade);
		}
		rst.close();
		ps.close();
		logger.debug("getUser(authUserId={}, regionId={}, reqId={}) - duration={}", authUserId, regionId, reqId, stopwatch);
		return res;
	}

	public UserEdit getUserEdit(String token, int regionId, int id) throws SQLException {
		int loggedInUserId = getLoggedInUserId(token);
		if (loggedInUserId != id) {
			throw new SQLException("loggedInUserId != id");
		}
		UserEdit u = null;
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, username, firstname, lastname FROM user WHERE id=?");
		ps.setInt(1, id);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			id = rst.getInt("id");
			String username = rst.getString("username");
			String firstname = rst.getString("firstname");
			String lastname = rst.getString("lastname");
			u = new UserEdit(regionId, id, username, firstname, lastname, null, null);
		}
		rst.close();
		ps.close();
		Preconditions.checkNotNull(u);
		return u;
	}

	public List<User> getUserSearch(int authUserId, String value) throws SQLException {
		if (authUserId == -1) {
			throw new SQLException("User not logged in...");
		}
		List<User> res = new ArrayList<>();
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, CONCAT(firstname, ' ', lastname) name FROM user WHERE password IS NOT NULL AND (firstname LIKE ? OR lastname LIKE ? OR CONCAT(firstname, ' ', lastname) LIKE ?) ORDER BY firstname, lastname");
		ps.setString(1, value + "%");
		ps.setString(2, value + "%");
		ps.setString(3, value + "%");
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int id = rst.getInt("id");
			String name = rst.getString("name");
			res.add(new User(true, id, name, -1, -1, -1, -1));
		}
		rst.close();
		ps.close();
		return res;
	}

	public void registerUser(Register r) throws SQLException, NoSuchAlgorithmException {
		PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO user (firstname, lastname, username, password) VALUES (?, ?, ?, ?)");
		ps.setString(1, r.getFirstname());
		ps.setString(2, Strings.nullToEmpty(r.getLastname()));
		ps.setString(3, r.getUsername());
		ps.setString(4, hashPassword(r.getPassword()));
		ps.execute();
		ps.close();
	}

	public void resetPassword(String token, String password) throws SQLException, NoSuchAlgorithmException {
		PreparedStatement ps = c.getConnection().prepareStatement("UPDATE user SET password=?, recover_token=null WHERE recover_token=?");
		ps.setString(1, hashPassword(password));
		ps.setString(2, token);
		int res = ps.executeUpdate();
		ps.close();
		if (res == 0) {
			throw new SQLException("Invalid token. Password was not reset...");
		}
	}

	public Area setArea(int authUserId, int idRegion, Area a, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, InterruptedException {
		Preconditions.checkArgument(authUserId != -1, "Insufficient credentials");
		Preconditions.checkArgument(idRegion > 0, "Insufficient credentials");
		boolean writePermissions = false;
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT auth.write FROM permission auth WHERE auth.region_id=? AND auth.user_id=?");
		ps.setInt(1, idRegion);
		ps.setInt(2, authUserId);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int write = rst.getInt("write");
			writePermissions = write >= 1;
		}
		rst.close();
		ps.close();
		rst = null;
		ps = null;
		Preconditions.checkArgument(writePermissions, "Insufficient credentials");
		int idArea = -1;
		if (a.getId() > 0) {
			// Check if this actual area is writable for the user (remember that all regions can be visible to user, origin does not matter). Area region can be different to html-page region. Also check for admin/superadmin.
			int writable = 0;
			ps = c.getConnection().prepareStatement("SELECT 1 FROM area a, permission auth WHERE a.id=? AND a.region_id=auth.region_id AND auth.user_id=? AND auth.write>0 AND auth.write>=a.hidden");
			ps.setInt(1, a.getId());
			ps.setInt(2, authUserId);
			rst = ps.executeQuery();
			while (rst.next()) {
				writable = 1;
			}
			rst.close();
			ps.close();
			if (writable != 1) {
				throw new SQLException("Insufficient credentials");
			}
			ps = c.getConnection().prepareStatement("UPDATE area SET name=?, description=?, latitude=?, longitude=?, hidden=? WHERE id=?");
			ps.setString(1, a.getName());
			ps.setString(2, Strings.emptyToNull(a.getComment()));
			if (a.getLat() > 0) {
				ps.setDouble(3, a.getLat());
			}
			else {
				ps.setNull(3, Types.DOUBLE);
			}
			if (a.getLng() > 0) {
				ps.setDouble(4, a.getLng());
			}
			else {
				ps.setNull(4, Types.DOUBLE);
			}
			ps.setInt(5, a.getVisibility());
			ps.setInt(6, a.getId());
			ps.execute();
			ps.close();
			ps = null;
			idArea = a.getId();

			// Also update sectors and problems (last_updated and visibility [if >0])
			String sqlStr = null;
			if (a.getVisibility()>0) {
				sqlStr = "UPDATE (area a LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), a.hidden=?, s.last_updated=now(), s.hidden=?, p.last_updated=now(), p.hidden=? WHERE a.id=?";
				ps = c.getConnection().prepareStatement(sqlStr);
				ps.setInt(1, a.getVisibility());
				ps.setInt(2, a.getVisibility());
				ps.setInt(3, a.getVisibility());
				ps.setInt(4, idArea);
				ps.execute();
				ps.close();
			}
			else {
				sqlStr = "UPDATE (area a LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), p.last_updated=now() WHERE a.id=?";
				ps = c.getConnection().prepareStatement(sqlStr);
				ps.setInt(1, idArea);
				ps.execute();
				ps.close();
			}
		}
		else {
			ps = c.getConnection().prepareStatement("INSERT INTO area (android_id, region_id, name, description, latitude, longitude, hidden, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, now())", PreparedStatement.RETURN_GENERATED_KEYS);
			ps.setLong(1, System.currentTimeMillis());
			ps.setInt(2, idRegion);
			ps.setString(3, a.getName());
			ps.setString(4, Strings.emptyToNull(a.getComment()));
			if (a.getLat() > 0) {
				ps.setDouble(5, a.getLat());
			}
			else {
				ps.setNull(5, Types.DOUBLE);
			}
			if (a.getLng() > 0) {
				ps.setDouble(6, a.getLng());
			}
			else {
				ps.setNull(6, Types.DOUBLE);
			}
			ps.setInt(7, a.getVisibility());
			ps.executeUpdate();
			rst = ps.getGeneratedKeys();
			if (rst != null && rst.next()) {
				idArea = rst.getInt(1);
			}
			rst.close();
			ps.close();
		}
		if (idArea == -1) {
			throw new SQLException("idArea == -1");
		}
		// New media
		if (a.getNewMedia() != null) {
			for (NewMedia m : a.getNewMedia()) {
				final int idProblem = 0;
				final int idSector = 0;
				addNewMedia(authUserId, idProblem, idSector, a.getId(), m, multiPart);
			}
		}
		return getArea(authUserId, idArea);
	}

	public Problem setProblem(int authUserId, int regionId, Problem p, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, ParseException, InterruptedException {
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		int idProblem = -1;
		if (p.getId() > 0) {
			PreparedStatement ps = c.getConnection().prepareStatement("UPDATE ((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN permission auth ON (a.region_id=auth.region_id AND auth.user_id=? AND auth.write>0 AND auth.write>=p.hidden) SET p.name=?, p.description=?, p.grade=?, p.fa_date=?, p.latitude=?, p.longitude=?, p.hidden=?, p.nr=?, p.type_id=?, p.last_updated=now() WHERE p.id=?");
			ps.setInt(1, authUserId);
			ps.setString(2, p.getName());
			ps.setString(3, Strings.emptyToNull(p.getComment()));
			ps.setInt(4, GradeHelper.stringToInt(regionId, p.getOriginalGrade()));
			ps.setTimestamp(5, Strings.isNullOrEmpty(p.getFaDate())? null : new Timestamp(sdf.parse(p.getFaDate()).getTime()));
			if (p.getLat() > 0) {
				ps.setDouble(6, p.getLat());
			}
			else {
				ps.setNull(6, Types.DOUBLE);
			}
			if (p.getLng() > 0) {
				ps.setDouble(7, p.getLng());
			}
			else {
				ps.setNull(7, Types.DOUBLE);
			}
			ps.setInt(8, p.getVisibility());
			ps.setInt(9, p.getNr());
			ps.setInt(10, p.getT().getId());
			ps.setInt(11, p.getId());
			int res = ps.executeUpdate();
			ps.close();
			if (res != 1) {
				throw new SQLException("Insufficient credentials");
			}
			idProblem = p.getId();
		}
		else {
			PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO problem (android_id, sector_id, name, description, grade, fa_date, latitude, longitude, hidden, nr, type_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			ps.setLong(1, System.currentTimeMillis());
			ps.setInt(2, p.getSectorId());
			ps.setString(3, p.getName());
			ps.setString(4, Strings.emptyToNull(p.getComment()));
			ps.setInt(5, GradeHelper.stringToInt(regionId, p.getOriginalGrade()));
			ps.setTimestamp(6, Strings.isNullOrEmpty(p.getFaDate())? null : new Timestamp(sdf.parse(p.getFaDate()).getTime()));
			if (p.getLat() > 0) {
				ps.setDouble(7, p.getLat());
			}
			else {
				ps.setNull(7, Types.DOUBLE);
			}
			if (p.getLng() > 0) {
				ps.setDouble(8, p.getLng());
			}
			else {
				ps.setNull(8, Types.DOUBLE);
			}
			ps.setInt(9, p.getVisibility());
			ps.setInt(10, p.getNr() == 0? getSector(authUserId, regionId, p.getSectorId()).getProblems().stream().map(x -> x.getNr()).mapToInt(Integer::intValue).max().orElse(1) : p.getNr());
			ps.setInt(11, p.getT().getId());
			ps.executeUpdate();
			ResultSet rst = ps.getGeneratedKeys();
			if (rst != null && rst.next()) {
				idProblem = rst.getInt(1);
			}
			rst.close();
			ps.close();
		}
		if (idProblem == -1) {
			throw new SQLException("idProblem == -1");
		}
		// Also update last_updated on problem, sector and area (ALSO CHECKS PERMISSION [SINCE INSERT DOES NOT])
		String sqlStr = "UPDATE problem p, sector s, area a, permission auth SET p.last_updated=now(), s.last_updated=now(), a.last_updated=now() WHERE p.id=? AND p.sector_id=s.id AND s.area_id=a.id AND a.region_id=auth.region_id AND auth.user_id=? AND auth.write>0 AND auth.write>=p.hidden";
		PreparedStatement ps = c.getConnection().prepareStatement(sqlStr);
		ps.setInt(1, idProblem);
		ps.setInt(2, authUserId);
		int res = ps.executeUpdate();
		ps.close();
		ps = null;
		if (res == 0) {
			throw new SQLException("Insufficient credentials");
		}
		// New media
		if (p.getNewMedia() != null) {
			for (NewMedia m : p.getNewMedia()) {
				final int idSector = 0;
				final int idArea = 0;
				addNewMedia(authUserId, idProblem, idSector, idArea, m, multiPart);
			}
		}
		// FA
		if (p.getFa() != null) {
			Set<Integer> fas = new HashSet<>();
			ps = c.getConnection().prepareStatement("SELECT user_id FROM fa WHERE problem_id=?");
			ps.setInt(1, idProblem);
			ResultSet rst = ps.executeQuery();
			while (rst.next()) {
				fas.add(rst.getInt("user_id"));
			}
			rst.close();
			ps.close();
			for (FaUser x : p.getFa()) {
				Preconditions.checkArgument(x.getId() != 0);
				if (x.getId() > 0) { // Existing user
					boolean exists = fas.remove(x.getId());
					if (!exists) {
						PreparedStatement ps2 = c.getConnection().prepareStatement("INSERT INTO fa (problem_id, user_id) VALUES (?, ?)");
						ps2.setInt(1, idProblem);
						ps2.setInt(2, x.getId());
						ps2.execute();
						ps2.close();
					}
				}
				else { // New user
					int idUser = addUser(x.getFirstname(), x.getSurname());
					Preconditions.checkArgument(idUser>0);
					PreparedStatement ps2 = c.getConnection().prepareStatement("INSERT INTO fa (problem_id, user_id) VALUES (?, ?)");
					ps2.setInt(1, idProblem);
					ps2.setInt(2, idUser);
					ps2.execute();
					ps2.close();
				}
			}
			if (!fas.isEmpty()) {
				ps = c.getConnection().prepareStatement("DELETE FROM fa WHERE problem_id=? AND user_id=?");
				for (int x : fas) {
					ps.setInt(1, idProblem);
					ps.setInt(2, x);
					ps.addBatch();
				}
				ps.executeBatch();
				ps.close();
			}
		}
		else {
			ps = c.getConnection().prepareStatement("DELETE FROM fa WHERE problem_id=?");
			ps.setInt(1, idProblem);
			ps.close();
		}
		// Sections
		ps = c.getConnection().prepareStatement("DELETE FROM problem_section WHERE problem_id=?");
		ps.setInt(1, idProblem);
		ps.execute();
		ps.close();
		if (p.getSections() != null && p.getSections().size() > 1) {
			ps = c.getConnection().prepareStatement("INSERT INTO problem_section (problem_id, nr, description, grade) VALUES (?, ?, ?, ?)");
			for (Section s : p.getSections()) {
				ps.setInt(1, idProblem);
				ps.setInt(2, s.getNr());
				ps.setString(3, s.getDescription());
				ps.setInt(4, GradeHelper.stringToInt(regionId, s.getGrade()));
				ps.addBatch();
			}
			ps.executeBatch();
			ps.close();
		}
		return getProblem(authUserId, regionId, idProblem, 0).get(0);
	}

	public Sector setSector(int authUserId, int regionId, Sector s, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, InterruptedException {
		int idSector = -1;
		if (s.getId() > 0) {
			PreparedStatement ps = c.getConnection().prepareStatement("UPDATE sector s, area a, permission auth SET s.name=?, s.description=?, s.parking_latitude=?, s.parking_longitude=?, s.hidden=?, s.polygon_coords=? WHERE s.id=? AND s.area_id=a.id AND a.region_id=auth.region_id AND auth.user_id=? AND auth.write>0 AND auth.write>=s.hidden");
			ps.setString(1, s.getName());
			ps.setString(2, Strings.emptyToNull(s.getComment()));
			if (s.getLat() > 0) {
				ps.setDouble(3, s.getLat());
			}
			else {
				ps.setNull(3, Types.DOUBLE);
			}
			if (s.getLng() > 0) {
				ps.setDouble(4, s.getLng());
			}
			else {
				ps.setNull(4, Types.DOUBLE);
			}
			ps.setInt(5, s.getVisibility());
			ps.setString(6, Strings.emptyToNull(s.getPolygonCoords()));
			ps.setInt(7, s.getId());
			ps.setInt(8, authUserId);
			int res = ps.executeUpdate();
			ps.close();
			ps = null;
			if (res != 1) {
				throw new SQLException("Insufficient credentials");
			}
			idSector = s.getId();

			// Also update problems (last_updated and visibility [if >1]) + last_updated on area
			String sqlStr = null;
			if (s.getVisibility()>0) {
				sqlStr = "UPDATE (area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), s.hidden=?, p.last_updated=now(), p.hidden=? WHERE s.id=?";
				ps = c.getConnection().prepareStatement(sqlStr);
				ps.setInt(1, s.getVisibility());
				ps.setInt(2, s.getVisibility());
				ps.setInt(3, idSector);
				ps.execute();
				ps.close();
			}
			else {
				sqlStr = "UPDATE (area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), p.last_updated=now() WHERE s.id=?";
				ps = c.getConnection().prepareStatement(sqlStr);
				ps.setInt(1, idSector);
				ps.execute();
				ps.close();
			}
		}
		else {
			int writable = 0;
			PreparedStatement ps = c.getConnection().prepareStatement("SELECT 1 FROM area a, permission auth WHERE a.id=? AND a.region_id=auth.region_id AND auth.user_id=? AND auth.write>0 AND auth.write>=a.hidden");
			ps.setInt(1, s.getAreaId());
			ps.setInt(2, authUserId);
			ResultSet rst = ps.executeQuery();
			while (rst.next()) {
				writable = 1;
			}
			rst.close();
			ps.close();
			if (writable != 1) {
				throw new SQLException("Insufficient credentials");
			}
			ps = c.getConnection().prepareStatement("INSERT INTO sector (android_id, area_id, name, description, parking_latitude, parking_longitude, hidden, polygon_coords, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())", PreparedStatement.RETURN_GENERATED_KEYS);
			ps.setLong(1, System.currentTimeMillis());
			ps.setInt(2, s.getAreaId());
			ps.setString(3, s.getName());
			ps.setString(4, Strings.emptyToNull(s.getComment()));
			if (s.getLat() > 0) {
				ps.setDouble(5, s.getLat());
			}
			else {
				ps.setNull(5, Types.DOUBLE);
			}
			if (s.getLng() > 0) {
				ps.setDouble(6, s.getLng());
			}
			else {
				ps.setNull(6, Types.DOUBLE);
			}
			ps.setInt(7, s.getVisibility());
			ps.setString(8, Strings.emptyToNull(s.getPolygonCoords()));
			ps.executeUpdate();
			rst = ps.getGeneratedKeys();
			if (rst != null && rst.next()) {
				idSector = rst.getInt(1);
			}
			rst.close();
			ps.close();
		}
		if (idSector == -1) {
			throw new SQLException("idSector == -1");
		}
		// New media
		if (s.getNewMedia() != null) {
			for (NewMedia m : s.getNewMedia()) {
				final int idProblem = 0;
				final int idArea = 0;
				addNewMedia(authUserId, idProblem, idSector, idArea, m, multiPart);
			}
		}
		return getSector(authUserId, regionId, idSector);
	}

	public void setTick(int authUserId, int regionId, Tick t) throws SQLException, ParseException {
		Preconditions.checkArgument(authUserId != -1);
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		if (t.isDelete()) {
			Preconditions.checkArgument(t.getId()>0, "Cannot delete a tick without id");
			PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM tick WHERE id=? AND user_id=? AND problem_id=?");
			ps.setInt(1, t.getId());
			ps.setInt(2, authUserId);
			ps.setInt(3, t.getIdProblem());
			int res = ps.executeUpdate();
			ps.close();
			if (res != 1) {
				throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
			}
		}
		else if (t.getId() == -1) {
			PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO tick (problem_id, user_id, date, grade, comment, stars) VALUES (?, ?, ?, ?, ?, ?)");
			ps.setInt(1, t.getIdProblem());
			ps.setInt(2, authUserId);
			ps.setTimestamp(3, Strings.isNullOrEmpty(t.getDate())? null : new Timestamp(sdf.parse(t.getDate()).getTime()));
			ps.setInt(4, GradeHelper.stringToInt(regionId, t.getGrade()));
			ps.setString(5, Strings.emptyToNull(t.getComment()));
			ps.setDouble(6, t.getStars());
			ps.execute();
			ps.close();
		}
		else if (t.getId() > 0) {
			PreparedStatement ps = c.getConnection().prepareStatement("UPDATE tick SET date=?, grade=?, comment=?, stars=? WHERE id=? AND problem_id=? AND user_id=?");
			ps.setTimestamp(1, Strings.isNullOrEmpty(t.getDate())? null : new Timestamp(sdf.parse(t.getDate()).getTime()));
			ps.setInt(2, GradeHelper.stringToInt(regionId, t.getGrade()));
			ps.setString(3, Strings.emptyToNull(t.getComment()));
			ps.setDouble(4, t.getStars());
			ps.setInt(5, t.getId());
			ps.setInt(6, t.getIdProblem());
			ps.setInt(7, authUserId);
			int res = ps.executeUpdate();
			ps.close();
			if (res != 1) {
				throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
			}
		}
		else {
			throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
		}
	}

	public Permission setUser(String token, UserEdit u) throws SQLException, NoSuchAlgorithmException {
		Preconditions.checkArgument(u.getId()>0);
		final int loggedInUserId = getLoggedInUserId(token);
		if (loggedInUserId != u.getId()) {
			throw new SQLException("loggedInUserId != u.getId()");
		}
		Preconditions.checkNotNull(Strings.emptyToNull(u.getUsername()));
		Preconditions.checkNotNull(Strings.emptyToNull(u.getFirstname()));
		Preconditions.checkNotNull(Strings.emptyToNull(u.getLastname()));
		PreparedStatement ps = c.getConnection().prepareStatement("UPDATE user SET username=?, firstname=?, lastname=? WHERE id=?");
		ps.setString(1, u.getUsername());
		ps.setString(2, u.getFirstname());
		ps.setString(3, u.getLastname());
		ps.setInt(4, u.getId());
		ps.execute();
		ps.close();

		if (!Strings.isNullOrEmpty(u.getCurrentPassword()) && !Strings.isNullOrEmpty(u.getNewPassword())) {
			ps = c.getConnection().prepareStatement("UPDATE user SET password=? WHERE id=? AND password=?");
			ps.setString(1, hashPassword(u.getNewPassword()));
			ps.setInt(2, u.getId());
			ps.setString(3, hashPassword(u.getCurrentPassword()));
			int res = ps.executeUpdate();
			ps.close();
			if (res == 0) {
				throw new SQLException("Invalid current password");
			}
			ps = c.getConnection().prepareStatement("DELETE FROM user_token WHERE user_id=?");
			ps.setInt(1, u.getId());
			ps.execute();
			ps.close();
			return getPermission(null, u.getUsername(), u.getNewPassword());
		}
		return null;
	}

	public void upsertSvg(int authUserId, int problemId, int mediaId, Svg svg) throws SQLException {
		// Check for write permissions
		boolean ok = false;
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT 1 FROM ((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN permission auth ON (a.region_id=auth.region_id AND auth.user_id=? AND auth.write>0 AND auth.write>=p.hidden) WHERE p.id=?");
		ps.setInt(1, authUserId);
		ps.setInt(2, problemId);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			ok = true;
		}
		rst.close();
		ps.close();
		rst = null;
		ps = null;
		Preconditions.checkArgument(ok, "Insufficient credentials");
		// Delete/Insert/Update
		if (svg.isDelete() || Strings.emptyToNull(svg.getPath()) == null) {
			ps = c.getConnection().prepareStatement("DELETE FROM svg WHERE media_id=? AND problem_id=?");
			ps.setInt(1, mediaId);
			ps.setInt(2, problemId);
			ps.execute();
			ps.close();
			ps = null;
		}
		else if (svg.getId() <= 0) {
			ps = c.getConnection().prepareStatement("INSERT INTO svg (media_id, problem_id, path, has_anchor) VALUES (?, ?, ?, ?)");
			ps.setInt(1, mediaId);
			ps.setInt(2, problemId);
			ps.setString(3, svg.getPath());
			ps.setBoolean(4, svg.isHasAnchor());
			ps.execute();
			ps.close();
			ps = null;
		}
		else {
			ps = c.getConnection().prepareStatement("UPDATE svg SET media_id=?, problem_id=?, path=?, has_anchor=? WHERE id=?");
			ps.setInt(1, mediaId);
			ps.setInt(2, problemId);
			ps.setString(3, svg.getPath());
			ps.setBoolean(4, svg.isHasAnchor());
			ps.setInt(5, svg.getId());
			ps.execute();
			ps.close();
			ps = null;
		}
	}

	private int addNewMedia(int idUser, int idProblem, int idSector, int idArea, NewMedia m, FormDataMultiPart multiPart) throws SQLException, IOException, NoSuchAlgorithmException, InterruptedException {
		logger.debug("addNewMedia(idUser={}, idProblem={}, idSector={}, idArea={}, m={}) initialized", idUser, idProblem, idSector, m);
		Preconditions.checkArgument((idProblem > 0 && idSector == 0 && idArea == 0)
				|| (idProblem == 0 && idSector > 0 && idArea == 0)
				|| (idProblem == 0 && idSector == 0 && idArea > 0));
		try (InputStream is = multiPart.getField(m.getName()).getValueAs(InputStream.class)) {
			/**
			 * DB
			 */
			int idMedia = -1;
			final String suffix = "jpg";
			PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media (is_movie, suffix, photographer_user_id, uploader_user_id) VALUES (?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			ps.setBoolean(1, false);
			ps.setString(2, suffix);
			ps.setInt(3, getExistingOrInsertUser(m.getPhotographer()));
			ps.setInt(4, idUser);
			ps.executeUpdate();
			ResultSet rst = ps.getGeneratedKeys();
			if (rst != null && rst.next()) {
				idMedia = rst.getInt(1);
			}
			rst.close();
			ps.close();
			ps = null;
			Preconditions.checkArgument(idMedia>0);
			if (idProblem > 0) {
				ps = c.getConnection().prepareStatement("INSERT INTO media_problem (media_id, problem_id) VALUES (?, ?)");
				ps.setInt(1, idMedia);
				ps.setInt(2, idProblem);
				ps.execute();
				ps.close();
				ps = null;
			}
			else if (idSector > 0) {
				ps = c.getConnection().prepareStatement("INSERT INTO media_sector (media_id, sector_id) VALUES (?, ?)");
				ps.setInt(1, idMedia);
				ps.setInt(2, idSector);
				ps.execute();
				ps.close();
				ps = null;
			}
			else if (idArea > 0) {
				ps = c.getConnection().prepareStatement("INSERT INTO media_area (media_id, area_id) VALUES (?, ?)");
				ps.setInt(1, idMedia);
				ps.setInt(2, idArea);
				ps.execute();
				ps.close();
				ps = null;
			}
			if (!Strings.isNullOrEmpty(m.getInPhoto())) {
				ps = c.getConnection().prepareStatement("INSERT INTO media_user (media_id, user_id) VALUES (?, ?)");
				ps.setInt(1, idMedia);
				ps.setInt(2, getExistingOrInsertUser(m.getInPhoto()));
				ps.execute();
				ps.close();
				ps = null;
			}

			/**
			 * IO
			 */
			long ms = System.currentTimeMillis();

			// Save received file
			Path original = Paths.get(PATH + "temp");
			Files.createDirectories(original);
			original = original.resolve(ms + "_" + m.getName());
			Preconditions.checkArgument(Files.exists(original.getParent()), original.getParent().toString() + " does not exist");
			Preconditions.checkArgument(!Files.exists(original), original.toString() + " does already exist");
			Files.copy(is, original);
			Preconditions.checkArgument(Files.exists(original), original.toString() + " does not exist");

			final Path p = Paths.get(PATH + "original").resolve(String.valueOf(idMedia/100*100)).resolve(idMedia + "." + suffix);
			Files.createDirectories(p.getParent());
			Preconditions.checkArgument(!Files.exists(p), p.toString() + " does already exist");

			// If not JPG/JPEG --> convert to JPG, else --> copy to destination (ALWAYS JPG)
			final String inputExtension = com.google.common.io.Files.getFileExtension(original.getFileName().toString());
			if (!inputExtension.equalsIgnoreCase("jpg") && !inputExtension.equalsIgnoreCase("jpeg")) {
				BufferedImage src = ImageIO.read(original.toFile());
				BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
				dst.createGraphics().drawImage(src, 0, 0, Color.WHITE, null);
				ImageIO.write(dst, "jpg", p.toFile());
				src.flush();
				dst.flush();
			}
			else {
				Files.copy(original, p);
			}
			Preconditions.checkArgument(Files.exists(p), p.toString() + " does not exist");

			// Rotate (if EXIF-rotated)
			try (ThumbnailCreation creation = ThumbnailCreation.image(p.toFile())) {
				ExifOrientation orientation = creation.getExifRotation();

				if (orientation != null && orientation != ExifOrientation.HORIZONTAL_NORMAL) {
					logger.info("Rotating " + p.toString() + " using " + orientation);
					creation.
					rotate(orientation).
					preserveExif().
					saveTo(com.google.common.io.Files.asByteSink(p.toFile()));
				}
			}
			Preconditions.checkArgument(Files.exists(p), p.toString() + " does not exist");
			// Create scaled jpg and webp + update crc32 and dimentions in db
			createScaledImages(c, getDateTaken(p), idMedia, suffix);

			return idMedia;
		}
	}

	private int addUser(String firstname, String lastname) throws SQLException, NoSuchAlgorithmException {
		int idUser = -1;
		Preconditions.checkNotNull(Strings.emptyToNull(firstname));
		PreparedStatement ps2 = c.getConnection().prepareStatement("INSERT INTO user (username, firstname, lastname, password) VALUES (?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
		final String random = UUID.randomUUID().toString();
		ps2.setString(1, random);
		ps2.setString(2, firstname);
		ps2.setString(3, Strings.nullToEmpty(lastname));
		ps2.setString(4, hashPassword(random));
		ps2.executeUpdate();
		ResultSet rst2 = ps2.getGeneratedKeys();
		if (rst2 != null && rst2.next()) {
			idUser = rst2.getInt(1);
		}
		rst2.close();
		ps2.close();
		return idUser;
	}

	private void createScaledImages(DbConnection c, String dateTaken, int id, String suffix) throws IOException, InterruptedException, SQLException {
		final Path original = Paths.get(PATH + "original").resolve(String.valueOf(id/100*100)).resolve(id + "." + suffix);
		final Path webp = Paths.get(PATH + "web/webp").resolve(String.valueOf(id/100*100)).resolve(id + ".webp");
		final Path jpg = Paths.get(PATH + "web/jpg").resolve(String.valueOf(id/100*100)).resolve(id + ".jpg");
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
		String[] cmd = new String[]{"/bin/bash", "-c", "cwebp \"" + jpg.toString() + "\" -af -m 6 -o \"" + webp.toString() + "\""};
		Process process = Runtime.getRuntime().exec(cmd);
		process.waitFor();
		Preconditions.checkArgument(Files.exists(webp), "WebP does not exist. Command=" + Lists.newArrayList(cmd));
		final int crc32 = com.google.common.io.Files.hash(webp.toFile(), Hashing.crc32()).asInt();

		/**
		 * Final DB
		 */
		PreparedStatement ps = c.getConnection().prepareStatement("UPDATE media SET date_taken=?, checksum=?, width=?, height=? WHERE id=?");
		ps.setString(1, dateTaken);
		ps.setInt(2, crc32);
		ps.setInt(3, width);
		ps.setInt(4, height);
		ps.setInt(5, id);
		ps.execute();
		ps.close();
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
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT id FROM user WHERE CONCAT(firstname, ' ', lastname)=?");
		ps.setString(1, name);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			usId = rst.getInt("id");
		}
		rst.close();
		ps.close();
		if (usId == -1) {
			usId = addUser(name, null);
		}
		Preconditions.checkArgument(usId > 0);
		return usId;
	}

	private int getLoggedInUserId(String token) throws SQLException {
		int userId = -1;
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT user_id FROM user_token WHERE token=?");
		ps.setString(1, token);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			userId = rst.getInt("user_id");
		}
		rst.close();
		ps.close();
		return userId;
	}

	private List<Media> getMediaArea(int id) throws SQLException {
		List<Media> media = new ArrayList<>();
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.width, m.height, m.is_movie, TRIM(CONCAT(c.firstname, ' ', c.lastname)) creator, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', u.lastname)) ORDER BY u.firstname, u.lastname SEPARATOR ', ') in_photo FROM (((media m INNER JOIN media_area ma ON m.id=ma.media_id AND m.deleted_user_id IS NULL AND ma.area_id=?) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id GROUP BY m.id, m.width, m.height, m.is_movie, c.firstname, c.lastname ORDER BY m.is_movie, m.id");
		ps.setInt(1, id);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int itId = rst.getInt("id");
			int width = rst.getInt("width");
			int height = rst.getInt("height");
			int tyId = rst.getBoolean("is_movie")? 2 : 1;
			String creator = rst.getString("creator");
			String inPhoto = rst.getString("in_photo");
			String description = "photographer: " + creator;
			if (!Strings.isNullOrEmpty(inPhoto)) {
				description += ", in photo: " + inPhoto;
			}
			media.add(new Media(itId, width, height, description, tyId, null, 0, null));
		}
		rst.close();
		ps.close();
		return media;
	}

	private List<Media> getMediaProblem(int regionId, int sectorId, int problemId) throws SQLException {
		List<Media> media = regionId == 4? getMediaSector(sectorId, problemId) : Lists.newArrayList();
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.width, m.height, m.is_movie, ROUND(mp.milliseconds/1000) t, TRIM(CONCAT(c.firstname, ' ', c.lastname)) creator, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', u.lastname)) ORDER BY u.firstname, u.lastname SEPARATOR ', ') in_photo FROM (((media m INNER JOIN media_problem mp ON m.id=mp.media_id AND m.deleted_user_id IS NULL AND mp.problem_id=?) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id GROUP BY m.id, m.width, m.height, m.is_movie, mp.milliseconds, c.firstname, c.lastname ORDER BY m.is_movie, m.id");
		ps.setInt(1, problemId);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int itId = rst.getInt("id");
			int width = rst.getInt("width");
			int height = rst.getInt("height");
			int tyId = rst.getBoolean("is_movie")? 2 : 1;
			String t = rst.getString("t");
			String creator = rst.getString("creator");
			String inPhoto = rst.getString("in_photo");
			String description = "photographer: " + creator;
			if (!Strings.isNullOrEmpty(inPhoto)) {
				description += ", in photo: " + inPhoto;
			}
			media.add(new Media(itId, width, height, description, tyId, t, 0, null));
		}
		rst.close();
		ps.close();
		if (media.isEmpty()) {
			media = null;
		}
		return media;
	}

	private List<Media> getMediaSector(int idSector, int optionalIdProblem) throws SQLException {
		List<Media> media = new ArrayList<>();
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.width, m.height, m.is_movie, TRIM(CONCAT(c.firstname, ' ', c.lastname)) creator, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', u.lastname)) ORDER BY u.firstname, u.lastname SEPARATOR ', ') in_photo FROM (((media m INNER JOIN media_sector ms ON m.id=ms.media_id AND m.deleted_user_id IS NULL AND ms.sector_id=?) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id GROUP BY m.id, m.width, m.height, m.is_movie, c.firstname, c.lastname ORDER BY m.is_movie, m.id");
		ps.setInt(1, idSector);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int itId = rst.getInt("id");
			int width = rst.getInt("width");
			int height = rst.getInt("height");
			int tyId = rst.getBoolean("is_movie")? 2 : 1;
			String creator = rst.getString("creator");
			String inPhoto = rst.getString("in_photo");
			String description = "photographer: " + creator;
			if (!Strings.isNullOrEmpty(inPhoto)) {
				description += ", in photo: " + inPhoto;
			}
			List<Svg> svgs = getSvgs(itId);
			Media m = new Media(itId, width, height, description, tyId, null, optionalIdProblem, svgs);
			if (optionalIdProblem != 0 && svgs != null && svgs.stream().filter(svg -> svg.getProblemId() == optionalIdProblem).findAny().isPresent()) {
				media.clear();
				media.add(m);
				break;
			}
			else {
				media.add(m);
			}
		}
		rst.close();
		ps.close();
		return media;
	}

	private List<Svg> getSvgs(int idMedia) throws SQLException {
		List<Svg> res = null;
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT p.id problem_id, p.nr, s.id, s.path, s.has_anchor FROM svg s, problem p WHERE s.media_id=? AND s.problem_id=p.id");
		ps.setInt(1, idMedia);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			if (res == null) {
				res = new ArrayList<>();
			}
			int id = rst.getInt("id");
			int problemId = rst.getInt("problem_id");
			int nr = rst.getInt("nr");
			String path = rst.getString("path");
			boolean hasAnchor = rst.getBoolean("has_anchor");
			res.add(new Svg(false, id, problemId, nr, path, hasAnchor));
		}
		rst.close();
		ps.close();
		return res;
	}

	private String hashPassword(String password) throws NoSuchAlgorithmException {
		MessageDigest m = MessageDigest.getInstance("MD5");
		m.reset();
		m.update(password.getBytes());
		byte[] digest = m.digest();
		BigInteger bigInt = new BigInteger(1,digest);
		String hashtext = bigInt.toString(16);
		while (hashtext.length() < 32 ){
			hashtext = "0" + hashtext;
		}
		return hashtext;
	}

	private void setRandomMedia(Frontpage res, int authUserId, int regionId, boolean fallbackSolution) throws SQLException {
		String sqlStr = "SELECT m.id id_media, p.id id_problem, p.name problem, m.photographer_user_id id_creator, TRIM(CONCAT(u.firstname, ' ', u.lastname)) photographer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u2.firstname, ' ', u2.lastname)) ORDER BY u2.firstname, u2.lastname SEPARATOR ', ') in_photo, p.grade"
				+ " FROM ((((((((((media m INNER JOIN media_problem mp ON m.is_movie=0 AND m.id=mp.media_id) INNER JOIN problem p ON mp.problem_id=p.id AND p.hidden=0) INNER JOIN sector s ON p.sector_id=s.id AND s.hidden=0) INNER JOIN area a ON s.area_id=a.id AND a.hidden=0) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN user u ON m.photographer_user_id=u.id) INNER JOIN tick t ON p.id=t.problem_id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u2 ON mu.user_id=u2.id) LEFT JOIN permission auth ON (r.id=auth.region_id AND auth.user_id=?)"
				+ " WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR auth.user_id IS NOT NULL) AND m.deleted_user_id IS NULL"
				+ " GROUP BY m.id, p.id, p.name, m.photographer_user_id, u.firstname, u.lastname, u2.firstname, u2.lastname, p.grade"
				+ " HAVING AVG(t.stars)>=2"
				+ " ORDER BY rand()"
				+ " LIMIT 1";
		if (fallbackSolution) {
			sqlStr = sqlStr.replace("INNER JOIN tick", "LEFT JOIN tick");
			sqlStr = sqlStr.replace("HAVING AVG(t.stars)>=2", "");
		}
		PreparedStatement ps = c.getConnection().prepareStatement(sqlStr);
		ps.setInt(1, authUserId);
		ps.setInt(2, regionId);
		ps.setInt(3, regionId);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int idMedia = rst.getInt("id_media");
			int idProblem = rst.getInt("id_problem");
			String problem = rst.getString("problem");
			int grade = rst.getInt("grade");
			int idCreator = rst.getInt("id_creator");
			String photographer = rst.getString("photographer");
			String inPhoto = rst.getString("in_photo");
			res.setRandomMedia(idMedia, idProblem, problem, GradeHelper.intToString(regionId, grade), idCreator, photographer, inPhoto);
		}
		rst.close();
		ps.close();
	}

	private int upsertUserReturnId(String uniqueId) throws SQLException {
		int idUser = 0;
		if (Strings.isNullOrEmpty(uniqueId)) {
			return idUser;
		}
		String sqlStr = "INSERT INTO android_user (unique_id, last_sync) VALUES (?, now()) ON DUPLICATE KEY UPDATE last_sync=now()";
		PreparedStatement ps = c.getConnection().prepareStatement(sqlStr);
		ps.setString(1, uniqueId);
		ps.execute();
		ps.close();
		sqlStr = "SELECT user_id FROM android_user au WHERE unique_id=?";
		ps = c.getConnection().prepareStatement(sqlStr);
		ps.setString(1, uniqueId);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			idUser = rst.getInt(1);
		}
		rst.close();
		ps.close();
		return idUser;
	}
}