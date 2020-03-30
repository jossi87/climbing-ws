package com.buldreinfo.jersey.jaxb.metadata;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.buldreinfo.jersey.jaxb.metadata.jsonld.JsonLdCreator;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Browse;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Frontpage.RandomMedia;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Metadata;
import com.buldreinfo.jersey.jaxb.model.OpenGraph;
import com.buldreinfo.jersey.jaxb.model.Permissions;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.TodoUser;
import com.buldreinfo.jersey.jaxb.model.User;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import jersey.repackaged.com.google.common.base.Joiner;

public class MetaHelper {
	private List<Setup> setups = new ArrayList<>();

	public MetaHelper() {
		setups.add(new Setup("buldreinfo.com")
				.setIdRegion(1)
				.setBouldering(true)
				.setTitle("Buldreinfo")
				.setDescription("Bouldering in Rogaland (Stavanger, Western Norway)")
				.setLatLng(58.78119, 5.86361).setDefaultZoom(10));
		setups.add(new Setup("buldring.bergen-klatreklubb.no")
				.setIdRegion(2)
				.setBouldering(true)
				.setTitle("Buldring i Hordaland")
				.setDescription("Bouldering in Hordaland (Bergen, Western Norway)")
				.setLatLng(60.37, 5.96).setDefaultZoom(7));
		setups.add(new Setup("buldring.fredrikstadklatreklubb.org")
				.setIdRegion(3)
				.setBouldering(true)
				.setTitle("Buldring i Fredrikstad")
				.setDescription("Bouldering in Fredrikstad (Eastern Norway)")
				.setLatLng(59.22844, 10.91722).setDefaultZoom(10));
		setups.add(new Setup("brattelinjer.no")
				.setIdRegion(4)
				.setBouldering(false)
				.setTitle("Bratte Linjer")
				.setDescription("Climbing in Rogaland (Stavanger, Western Norway)")
				.setLatLng(58.78119, 5.86361).setDefaultZoom(9));
		setups.add(new Setup("buldring.jotunheimenfjellsport.com")
				.setIdRegion(5)
				.setBouldering(true)
				.setTitle("Buldring i Jotunheimen")
				.setDescription("Bouldering in Jotunheimen (Norway)")
				.setLatLng(61.60500, 8.47750).setDefaultZoom(7));
		setups.add(new Setup("klatring.jotunheimenfjellsport.com")
				.setIdRegion(6)
				.setBouldering(false)
				.setTitle("Klatring i Jotunheimen")
				.setDescription("Climbing in Jotunheimen (Norway)")
				.setLatLng(61.60500, 8.47750).setDefaultZoom(7));
		setups.add(new Setup("buldreforer.tromsoklatring.no")
				.setIdRegion(7)
				.setBouldering(true)
				.setTitle("Buldring i Tromsø")
				.setDescription("Bouldering in Tromsø (Norway)")
				.setLatLng(69.65994, 18.66755).setDefaultZoom(7));
		setups.add(new Setup("klatreforer.tromsoklatring.no")
				.setIdRegion(8)
				.setBouldering(false)
				.setTitle("Klatring i Tromsø")
				.setDescription("Climbing in Tromsø (Norway)")
				.setLatLng(69.65994, 18.66755).setDefaultZoom(7));
		setups.add(new Setup("klatreforer.narvikklatreklubb.no")
				.setIdRegion(9)
				.setBouldering(false)
				.setTitle("Klatring i Narvik")
				.setDescription("Climbing in Narvik (Norway)")
				.setLatLng(68.41312, 17.54277).setDefaultZoom(7));
		setups.add(new Setup("tau.fredrikstadklatreklubb.org")
				.setIdRegion(10)
				.setBouldering(false)
				.setTitle("Klatring i Fredrikstad")
				.setDescription("Climbing in Fredrikstad (Eastern Norway)")
				.setLatLng(59.22844, 10.91722).setDefaultZoom(10));
		setups.add(new Setup("hkl.brattelinjer.no")
				.setIdRegion(11)
				.setBouldering(false)
				.setTitle("Klatring på Haugalandet")
				.setDescription("Climbing in Haugaland (Western Norway)")
				.setLatLng(59.51196, 5.76736).setDefaultZoom(8));
		// DEV
		setups.add(new Setup("dev.jossi.org")
				.setIdRegion(4)
				.setBouldering(false)
				.setTitle("DEV")
				.setDescription("DEV")
				.setLatLng(58.78119, 5.86361).setDefaultZoom(10)
				.setSetRobotsDenyAll());
	}

	public Setup getSetup(HttpServletRequest request) {
		Preconditions.checkNotNull(request);
		Preconditions.checkNotNull(request.getServerName(), "Invalid request=" + request);
		final String serverName = request.getServerName().toLowerCase().replace("www.", "");
		return setups
				.stream()
				.filter(x -> serverName.equalsIgnoreCase(x.getDomain()))
				.findAny()
				.orElseThrow(() -> new RuntimeException("Invalid serverName=" + serverName));
	}

	public Setup getSetup(int regionId) {
		return setups
				.stream()
				.filter(x -> x.getIdRegion() == regionId)
				.findAny()
				.orElseThrow(() -> new RuntimeException("Invalid regionId=" + regionId));
	}

	public List<Setup> getSetups() {
		return setups;
	}

	public void updateMetadata(DbConnection c, IMetadata m, Setup setup, int authUserId, int requestedIdMedia) throws SQLException {
		if (m == null) {
			return;
		}
		if (m instanceof Area) {
			Area a = (Area)m;
			String description = null;
			if (setup.isBouldering()) {
				description = String.format("Bouldering in %s (%d sectors, %d boulders)", a.getName(), a.getSectors().size(), a.getSectors().stream().map(x -> x.getNumProblems()).mapToInt(Integer::intValue).sum());
			}
			else {
				description = String.format("Climbing in %s (%d sectors, %d routes)", a.getName(), a.getSectors().size(), a.getSectors().stream().map(x -> x.getNumProblems()).mapToInt(Integer::intValue).sum());
			}

			OpenGraph og = getOg(setup, "/area/" + a.getId(), a.getMedia(), requestedIdMedia);
			a.setMetadata(new Metadata(c, setup, authUserId, a.getName(), og)
					.setCanonical(a.getCanonical())
					.setDescription(description)
					.setJsonLd(JsonLdCreator.getJsonLd(setup, a))
					.setDefaultCenter(setup.getDefaultCenter())
					.setDefaultZoom(setup.getDefaultZoom()));
		}
		else if (m instanceof Frontpage) {
			Frontpage f = (Frontpage)m;
			String description = String.format("%s - %d %s, %d public ascents, %d images, %d ascents on video",
					setup.getDescription(),
					f.getNumProblems(),
					(setup.isBouldering()? "boulders" : "routes"),
					f.getNumTicks(),
					f.getNumImages(),
					f.getNumMovies());
			OpenGraph og = null;
			if (f.getRandomMedia() != null) {
				RandomMedia x = f.getRandomMedia();
				String image = setup.getUrl("/buldreinfo_media/jpg/" + String.valueOf(x.getIdMedia()/100*100) + "/" + x.getIdMedia() + ".jpg");
				og = new OpenGraph(setup.getUrl(null), image, x.getWidth(), x.getHeight(), null);
			}
			else {
				og = getOg(setup, null, null, requestedIdMedia);
			}
			f.setMetadata(new Metadata(c, setup, authUserId, null, og)
					.setDescription(description));
		}
		else if (m instanceof Problem) {
			Problem p = (Problem)m;
			String title = String.format("%s [%s] (%s / %s)", p.getName(), p.getGrade(), p.getAreaName(), p.getSectorName());
			String description = p.getComment();
			if (p.getFa() != null && !p.getFa().isEmpty()) {
				String fa = Joiner.on(", ").join(p.getFa().stream().map(x -> x.getName().trim()).collect(Collectors.toList()));
				description = (!Strings.isNullOrEmpty(description)? description + " | " : "") + "First ascent by " + fa + (!Strings.isNullOrEmpty(p.getFaDateHr())? " (" + p.getFaDate() + ")" : "");
			}
			OpenGraph og = getOg(setup, "/problem/" + p.getId(), p.getMedia(), requestedIdMedia);
			p.setMetadata(new Metadata(c, setup, authUserId, title, og)
					.setCanonical(p.getCanonical())
					.setDescription(description)
					.setJsonLd(JsonLdCreator.getJsonLd(setup, p))
					.setTypes(c.getBuldreinfoRepo().getTypes(setup.getIdRegion()))
					.setDefaultCenter(setup.getDefaultCenter())
					.setDefaultZoom(setup.getDefaultZoom()));
		}
		else if (m instanceof Sector) {
			Sector s = (Sector)m;
			String title = String.format("%s (%s)", s.getName(), s.getAreaName());
			String description = String.format("%s in %s / %s (%d %s)%s",
					(setup.isBouldering()? "Bouldering" : "Climbing"),
					s.getAreaName(),
					s.getName(),
					(s.getProblems() != null? s.getProblems().size() : 0),
					(setup.isBouldering()? "boulders" : "routes"),
					(!Strings.isNullOrEmpty(s.getComment())? " | " + s.getComment() : ""));
			OpenGraph og = getOg(setup, "/sector/" + s.getId(), s.getMedia(), requestedIdMedia);
			s.setMetadata(new Metadata(c, setup, authUserId, title, og)
					.setCanonical(s.getCanonical())
					.setDescription(description)
					.setJsonLd(JsonLdCreator.getJsonLd(setup, s))
					.setDefaultCenter(setup.getDefaultCenter())
					.setDefaultZoom(setup.getDefaultZoom())
					.setTypes(c.getBuldreinfoRepo().getTypes(setup.getIdRegion())));
		}
		else if (m instanceof User) {
			User u = (User)m;
			String title = String.format("%s", u.getName());
			String description = String.format("%d ascents, %d pictures taken, %d appearance in pictures, %d videos created, %d appearance in videos", u.getTicks().size(), u.getNumImagesCreated(), u.getNumImageTags(), u.getNumVideosCreated(), u.getNumVideoTags());
			OpenGraph og = getOg(setup, "/user/" + u.getId(), null, requestedIdMedia);
			u.setMetadata(new Metadata(c, setup, authUserId, title, og).setDescription(description));
		}
		else if (m instanceof Meta) {
			Meta x = (Meta)m;
			x.setMetadata(new Metadata(c, setup, authUserId, null, null)
					.setDefaultCenter(setup.getDefaultCenter())
					.setDefaultZoom(setup.getDefaultZoom())
					.setTypes(c.getBuldreinfoRepo().getTypes(setup.getIdRegion())));
		}
		else if (m instanceof Browse) {
			Browse b = (Browse)m;
			String description = String.format("%d areas, %d sectors, %d %s",
					b.getAreas().size(),
					b.getAreas().stream().map(x -> x.getNumSectors()).mapToInt(Integer::intValue).sum(),
					b.getAreas().stream().map(x -> x.getNumProblems()).mapToInt(Integer::intValue).sum(),
					setup.isBouldering()? "boulders" : "routes");
			OpenGraph og = getOg(setup, "/browse", null, requestedIdMedia);
			b.setMetadata(new Metadata(c, setup, authUserId, "Browse", og)
					.setDescription(description)
					.setDefaultCenter(setup.getDefaultCenter())
					.setDefaultZoom(setup.getDefaultZoom()));
		}
		else if (m instanceof Ticks) {
			Ticks t = (Ticks)m;
			String description = String.format("Page %d/%d", t.getCurrPage(), t.getNumPages());
			OpenGraph og = getOg(setup, "/ticks/" + t.getCurrPage(), null, requestedIdMedia);
			t.setMetadata(new Metadata(c, setup, authUserId, "Public ascents", og).setDescription(description));
		}
		else if (m instanceof TodoUser) {
			TodoUser u = (TodoUser)m;
			String title = String.format("%s (To-do list)", u.getName());
			OpenGraph og = getOg(setup, "/todo/" + u.getId(), null, requestedIdMedia);
			u.setMetadata(new Metadata(c, setup, authUserId, title, og)
					.setDefaultCenter(setup.getDefaultCenter())
					.setDefaultZoom(setup.getDefaultZoom()));
		}
		else if (m instanceof Permissions) {
			Permissions p = (Permissions)m;
			OpenGraph og = getOg(setup, "/permissions", null, requestedIdMedia);
			p.setMetadata(new Metadata(c, setup, authUserId, "Permissions", og));
		}
		else {
			throw new RuntimeException("Invalid m=" + m);
		}
	}

	private OpenGraph getOg(Setup setup, String suffix, List<Media> media, int requestedIdMedia) {
		String url = setup.getUrl(suffix + (requestedIdMedia>0? "?idMedia=" + requestedIdMedia : ""));
		if (media != null) {
			Optional<Media> optMedia = null;
			if (requestedIdMedia > 0) {
				optMedia = media.stream().filter(x -> x.getId() == requestedIdMedia).findAny();
			}
			if (optMedia == null) {
				optMedia = media.stream().filter(x -> x.getIdType()==1).reduce((a, b) -> b);
			}
			if (optMedia.isPresent()) {
				Media m = optMedia.get();
				String image = setup.getUrl("/buldreinfo_media/jpg/" + String.valueOf(m.getId()/100*100) + "/" + m.getId() + ".jpg");
				String video = null;
				if (m.getIdType()!=1) {
					video = setup.getUrl("/buldreinfo_media/mp4/" + String.valueOf(m.getId()/100*100) + "/" + m.getId() + ".mp4");
				}
				return new OpenGraph(url, image, m.getWidth(), m.getHeight(), video);
			}
		}
		return new OpenGraph(url, setup.getUrl("/png/buldreinfo_black.png"), 136, 120, null);
	}
}