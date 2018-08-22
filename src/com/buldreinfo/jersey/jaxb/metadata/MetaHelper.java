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
import com.buldreinfo.jersey.jaxb.model.Finder;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Frontpage.RandomMedia;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Metadata;
import com.buldreinfo.jersey.jaxb.model.OpenGraph;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Sector;
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
				.setShowLogoPlay(true).setShowLogoSis(true).setShowLogoBrv(true)
				.setLatLng(58.78119, 5.86361).setDefaultZoom(7));
		setups.add(new Setup("buldring.bergen-klatreklubb.no")
				.setIdRegion(2)
				.setBouldering(true)
				.setTitle("Buldring i Hordaland")
				.setDescription("Bouldering in Hordaland (Bergen, Western Norway)")
				.setShowLogoPlay(true).setShowLogoSis(false).setShowLogoBrv(false)
				.setLatLng(60.47521, 6.83169).setDefaultZoom(7));
		setups.add(new Setup("buldring.fredrikstadklatreklubb.org")
				.setIdRegion(3)
				.setBouldering(true)
				.setTitle("Buldring i Fredrikstad")
				.setDescription("Bouldering in Fredrikstad (Eastern Norway)")
				.setShowLogoPlay(true).setShowLogoSis(false).setShowLogoBrv(false)
				.setLatLng(59.22844, 10.91722).setDefaultZoom(7));
		setups.add(new Setup("brattelinjer.no")
				.setIdRegion(4)
				.setBouldering(false)
				.setTitle("Bratte Linjer")
				.setDescription("Climbing in Rogaland (Stavanger, Western Norway)")
				.setShowLogoPlay(false).setShowLogoSis(false).setShowLogoBrv(true)
				.setLatLng(58.78119, 5.86361).setDefaultZoom(9));
		setups.add(new Setup("buldring.jotunheimenfjellsport.com")
				.setIdRegion(5)
				.setBouldering(true)
				.setTitle("Buldring i Jotunheimen")
				.setDescription("Bouldering in Jotunheimen (Norway)")
				.setShowLogoPlay(true).setShowLogoSis(false).setShowLogoBrv(false)
				.setLatLng(61.60500, 8.47750).setDefaultZoom(7));
		setups.add(new Setup("klatring.jotunheimenfjellsport.com")
				.setIdRegion(6)
				.setBouldering(false)
				.setTitle("Klatring i Jotunheimen")
				.setDescription("Climbing in Jotunheimen (Norway)")
				.setShowLogoPlay(false).setShowLogoSis(false).setShowLogoBrv(false)
				.setLatLng(61.60500, 8.47750).setDefaultZoom(9));
		// DEV
		setups.add(new Setup("dev.jossi.org")
				.setIdRegion(4)
				.setBouldering(false)
				.setTitle("DEV")
				.setDescription("DEV")
				.setShowLogoPlay(false).setShowLogoSis(false).setShowLogoBrv(true)
				.setLatLng(58.78119, 5.86361).setDefaultZoom(9)
				.setSetRobotsDenyAll());
		setups.add(new Setup("localhost:3000")
				.setIdRegion(1)
				.setBouldering(true)
				.setTitle("DEV")
				.setDescription("DEV")
				.setShowLogoPlay(true).setShowLogoSis(true).setShowLogoBrv(true)
				.setLatLng(58.78119, 5.86361).setDefaultZoom(7)
				.setSetRobotsDenyAll());
	}
	
	public Setup getSetup(HttpServletRequest request) {
		Preconditions.checkNotNull(request);
		String serverName = Strings.emptyToNull(request.getServerName());
		Preconditions.checkNotNull(serverName, "Invalid request=" + request);
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
	
	public void updateMetadata(DbConnection c, IMetadata m, Setup setup, int authUserId) throws SQLException {
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
			
			OpenGraph og = getOg(setup, "/area/" + a.getId(), a.getMedia());
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
				og = new OpenGraph(setup.getUrl(null), image, x.getWidth(), x.getHeight());
			}
			else {
				og = getOg(setup, null, null);
			}
			f.setMetadata(new Metadata(c, setup, authUserId, null, og)
					.setDescription(description));
		}
		else if (m instanceof Problem) {
			Problem p = (Problem)m;
			String title = String.format("%s [%s] (%s / %s)", p.getName(), p.getGrade(), p.getAreaName(), p.getSectorName());
			String description = p.getComment();
			if (p.getFa() != null && !p.getFa().isEmpty()) {
				String fa = Joiner.on(", ").join(p.getFa().stream().map(x -> (x.getFirstname() + " " + x.getSurname()).trim()).collect(Collectors.toList()));
				description = (!Strings.isNullOrEmpty(description)? description + " | " : "") + "First ascent by " + fa + (!Strings.isNullOrEmpty(p.getFaDateHr())? " (" + p.getFaDate() + ")" : "");
			}
			OpenGraph og = getOg(setup, "/problem/" + p.getId(), p.getMedia());
			p.setMetadata(new Metadata(c, setup, authUserId, title, og)
					.setCanonical(p.getCanonical())
					.setDescription(description)
					.setJsonLd(JsonLdCreator.getJsonLd(setup, p))
					.setIsBouldering(setup.isBouldering())
					.setTypes(c.getBuldreinfoRepo().getTypes(setup.getIdRegion())));
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
			OpenGraph og = getOg(setup, "/sector/" + s.getId(), s.getMedia());
			s.setMetadata(new Metadata(c, setup, authUserId, title, og)
					.setCanonical(s.getCanonical())
					.setDescription(description)
					.setJsonLd(JsonLdCreator.getJsonLd(setup, s))
					.setDefaultCenter(setup.getDefaultCenter())
					.setDefaultZoom(setup.getDefaultZoom())
					.setIsBouldering(setup.isBouldering()));
		}
		else if (m instanceof User) {
			User u = (User)m;
			String title = String.format("%s", u.getName());
			String description = String.format("%d ascents, %d pictures taken, %d appearance in pictures, %d videos created, %d appearance in videos", u.getTicks().size(), u.getNumImagesCreated(), u.getNumImageTags(), u.getNumVideosCreated(), u.getNumVideoTags());
			OpenGraph og = getOg(setup, "/user/" + u.getId(), null);
			u.setMetadata(new Metadata(c, setup, authUserId, title, og)
					.setDescription(description));
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
			OpenGraph og = getOg(setup, "/browse", null);
			b.setMetadata(new Metadata(c, setup, authUserId, "Browse", og)
					.setDescription(description)
					.setDefaultCenter(setup.getDefaultCenter())
					.setDefaultZoom(setup.getDefaultZoom()));
		}
		else if (m instanceof Finder) {
			Finder f = (Finder)m;
			String title = String.format("Finder [%s]", f.getGrade());
			String description = String.format("%d %s",
					f.getProblems().size(),
					(setup.isBouldering()? "problems" : "routes"));
			OpenGraph og = getOg(setup, "/finder/" + f.getIdGrade(), null);
			f.setMetadata(new Metadata(c, setup, authUserId, title, og)
					.setDescription(description)
					.setDefaultCenter(setup.getDefaultCenter())
					.setIsBouldering(setup.isBouldering()));
		}
		else {
			throw new RuntimeException("Invalid m=" + m);
		}
	}
	
	private OpenGraph getOg(Setup setup, String suffix, List<Media> media) {
		String url = setup.getUrl(suffix);
		if (media != null) {
			Optional<Media> optMedia = media.stream().filter(x -> x.getIdType()==1).reduce((a, b) -> b);
			if (optMedia.isPresent()) {
				Media m = optMedia.get();
				String image = setup.getUrl("/buldreinfo_media/jpg/" + String.valueOf(m.getId()/100*100) + "/" + m.getId() + ".jpg");
				return new OpenGraph(url, image, m.getWidth(), m.getHeight());
			}
		}
		return new OpenGraph(url, setup.getUrl("/png/buldreinfo_black.png"), 136, 120);
	}
}