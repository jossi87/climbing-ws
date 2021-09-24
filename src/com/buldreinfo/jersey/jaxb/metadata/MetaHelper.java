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
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup.GRADE_SYSTEM;
import com.buldreinfo.jersey.jaxb.metadata.jsonld.JsonLdCreator;
import com.buldreinfo.jersey.jaxb.model.About;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Browse;
import com.buldreinfo.jersey.jaxb.model.Cameras;
import com.buldreinfo.jersey.jaxb.model.ContentGraph;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Frontpage.RandomMedia;
import com.buldreinfo.jersey.jaxb.model.LatLng;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.MediaSvg;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Metadata;
import com.buldreinfo.jersey.jaxb.model.OpenGraph;
import com.buldreinfo.jersey.jaxb.model.Permissions;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Dangerous;
import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Sites;
import com.buldreinfo.jersey.jaxb.model.TableOfContents;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Trash;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import jersey.repackaged.com.google.common.base.Joiner;

public class MetaHelper {
	private List<Setup> setups = new ArrayList<>();

	public MetaHelper() {
		setups.add(new Setup("buldreinfo.com", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(1)
				.setTitle("Buldreinfo")
				.setDescription("Bouldering in Rogaland (Stavanger, Western Norway)")
				.setLatLng(58.72, 6.62).setDefaultZoom(8));
		setups.add(new Setup("buldring.bergen-klatreklubb.no", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(2)
				.setTitle("Buldring i Hordaland")
				.setDescription("Bouldering in Hordaland (Bergen, Western Norway)")
				.setLatLng(60.89, 5.84).setDefaultZoom(8));
		setups.add(new Setup("buldre.forer.no", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(3)
				.setTitle("Buldring i Fredrikstad")
				.setDescription("Bouldering in Fredrikstad (Eastern Norway)")
				.setLatLng(59.15, 10.92).setDefaultZoom(11));
		setups.add(new Setup("brattelinjer.no", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(4)
				.setTitle("Bratte Linjer")
				.setDescription("Climbing in Rogaland (Stavanger, Western Norway)")
				.setLatLng(58.72, 6.62).setDefaultZoom(8));
		setups.add(new Setup("buldring.jotunheimenfjellsport.com", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(5)
				.setTitle("Buldring i Jotunheimen")
				.setDescription("Bouldering in Jotunheimen (Norway)")
				.setLatLng(61.875, 9.086).setDefaultZoom(9));
		setups.add(new Setup("klatring.jotunheimenfjellsport.com", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(6)
				.setTitle("Klatring i Jotunheimen")
				.setDescription("Climbing in Jotunheimen (Norway)")
				.setLatLng(61.875, 9.086).setDefaultZoom(9));
		setups.add(new Setup("buldreforer.tromsoklatring.no", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(7)
				.setTitle("Buldring i Tromsø")
				.setDescription("Bouldering in Tromsø (Norway)")
				.setLatLng(69.73, 18.49).setDefaultZoom(11));
		setups.add(new Setup("klatreforer.tromsoklatring.no", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(8)
				.setTitle("Klatring i Tromsø")
				.setDescription("Climbing in Tromsø (Norway)")
				.setLatLng(69.77, 18.78).setDefaultZoom(9));
		setups.add(new Setup("klatreforer.narvikklatreklubb.no", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(9)
				.setTitle("Klatring i Narvik")
				.setDescription("Climbing in Narvik (Norway)")
				.setLatLng(68.41312, 17.54277).setDefaultZoom(9));
		setups.add(new Setup("tau.forer.no", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(10)
				.setTitle("Klatring i Fredrikstad")
				.setDescription("Climbing in Fredrikstad (Eastern Norway)")
				.setLatLng(59.15, 10.92).setDefaultZoom(11));
		setups.add(new Setup("hkl.brattelinjer.no", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(11)
				.setTitle("Klatring på Haugalandet")
				.setDescription("Climbing in Haugaland (Western Norway)")
				.setLatLng(59.51196, 5.76736).setDefaultZoom(9));
		setups.add(new Setup("hkl.buldreinfo.com", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(12)
				.setTitle("Buldring på Haugalandet")
				.setDescription("Bouldering in Haugaland (Western Norway)")
				.setLatLng(59.67, 5.38).setDefaultZoom(8));
		setups.add(new Setup("buldring.flatangeradventure.no", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(13)
				.setTitle("Buldring i Trøndelag")
				.setDescription("Bouldering in Trøndelag (Norway)")
				.setLatLng(64.06897, 10.50973).setDefaultZoom(8));
		setups.add(new Setup("klatring.flatangeradventure.no", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(14)
				.setTitle("Klatring i Trøndelag")
				.setDescription("Climbing in Trøndelag (Norway)")
				.setLatLng(64.06897, 10.50973).setDefaultZoom(8));
		setups.add(new Setup("buldring.narvikklatreklubb.no", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(15)
				.setTitle("Buldring i Narvik")
				.setDescription("Bouldering in Narvik (Norway)")
				.setLatLng(68.41312, 17.54277).setDefaultZoom(9));
		setups.add(new Setup("is.brattelinjer.no", Setup.GRADE_SYSTEM.ICE)
				.setIdRegion(16)
				.setTitle("Bratte Linjer (isklatring)")
				.setDescription("Ice climbing in Rogaland (Stavanger, Western Norway)")
				.setLatLng(58.72, 6.62).setDefaultZoom(8));
		setups.add(new Setup("is.forer.no", Setup.GRADE_SYSTEM.ICE)
				.setIdRegion(17)
				.setTitle("Is-klatring i Fredrikstad")
				.setDescription("Ice climbing in Fredrikstad (Eastern Norway)")
				.setLatLng(59.15, 10.92).setDefaultZoom(11));
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
			String info = a.getTypeNumTicked() == null || a.getTypeNumTicked().isEmpty()? null : a.getTypeNumTicked()
					.stream()
					.map(tnt -> tnt.getNum() + " " + tnt.getType().toLowerCase())
					.collect(Collectors.joining(", "));
			if (setup.isBouldering()) {
				description = String.format("Bouldering in %s (%s)", a.getName(), info);
			}
			else {
				description = String.format("Climbing in %s (%s)", a.getName(), info);
			}

			OpenGraph og = getOg(setup, "/area/" + a.getId(), a.getMedia(), requestedIdMedia);
			a.setMetadata(new Metadata(c, setup, authUserId, a.getName(), og)
					.setCanonical(a.getCanonical())
					.setDescription(description)
					.setJsonLd(JsonLdCreator.getJsonLd(setup, a))
					.setDefaultCenter(setup.getDefaultCenter())
					.setDefaultZoom(setup.getDefaultZoom()));
		}
		else if (m instanceof Cameras) {
			Cameras f = (Cameras)m;
			String title =  "Weather map";
			String description = "Weather map";
			OpenGraph og = getOg(setup, "/weather", null, requestedIdMedia);
			f.setMetadata(new Metadata(c, setup, authUserId, title, og)
					.setDescription(description)
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
		else if (m instanceof Dangerous) {
			Dangerous hse = (Dangerous)m;
			int numProblems = 0;
			for (Dangerous.Area a : hse.getAreas()) {
				for (Dangerous.Sector s : a.getSectors()) {
					numProblems += s.getProblems().size();
				}
			}
			String description = numProblems + (setup.isBouldering()? " problems" : " routes") + " marked as dangerous.";
			OpenGraph og = getOg(setup, "/hse", null, requestedIdMedia);
			hse.setMetadata(new Metadata(c, setup, authUserId, "Health and Safety Executive (HSE)", og).setDescription(description));
		}
		else if (m instanceof Profile) {
			Profile p = (Profile)m;
			String title = !Strings.isNullOrEmpty(p.getLastname())? p.getFirstname() + " " + p.getLastname() : p.getFirstname();
			OpenGraph og = getOg(setup, "/profile/" + p.getId(), null, requestedIdMedia);
			p.setMetadata(new Metadata(c, setup, authUserId, title, og)
					.setDefaultCenter(setup.getDefaultCenter())
					.setDefaultZoom(setup.getDefaultZoom()));
		}
		else if (m instanceof TableOfContents) {
			TableOfContents toc = (TableOfContents)m;
			int numAreas = 0;
			int numSectors = 0;
			int numProblems = 0;
			for (TableOfContents.Area a : toc.getAreas()) {
				numAreas++;
				for (TableOfContents.Sector s : a.getSectors()) {
					numSectors++;
					numProblems += s.getProblems().size();
				}
			}
			String description = String.format("%d areas, %d sectors, %d %s",
					numAreas, numSectors, numProblems,
					setup.isBouldering()? "boulders" : "routes");
			OpenGraph og = getOg(setup, "/toc", null, requestedIdMedia);
			toc.setMetadata(new Metadata(c, setup, authUserId, "Table of Contents", og).setDescription(description));
		}
		else if (m instanceof ContentGraph) {
			ContentGraph cg = (ContentGraph)m;
			OpenGraph og = getOg(setup, "/cg", null, requestedIdMedia);
			cg.setMetadata(new Metadata(c, setup, authUserId, "Content Graph", og).setDescription("Content Graph"));
		}
		else if (m instanceof Trash) {
			Trash t = (Trash)m;
			String description = t.getTrash().size() + " items in trash";
			OpenGraph og = getOg(setup, "/trash", null, requestedIdMedia);
			t.setMetadata(new Metadata(c, setup, authUserId, "Trash", og).setDescription(description));
		}
		else if (m instanceof Sites) {
			Sites s = (Sites)m;
			int total = s.getRegions().stream().mapToInt(r -> r.getNumProblems()).sum();
			String title = null;
			String url = null;
			if (s.getType().equals(GRADE_SYSTEM.BOULDER)) {
				title = "Map of bouldering in Norway";
				url = "/sites/boulder";
			} else if (s.getType().equals(GRADE_SYSTEM.CLIMBING)) {
				title = "Map of route climbing in Norway";
				url = "/sites/climbing";
			} else if (s.getType().equals(GRADE_SYSTEM.ICE)) {
				title = "Map of ice climbing in Norway";
				url = "/sites/ice";
			} else {
				throw new RuntimeException("Invalid type:" + s.getType());
			}
			String description = title + " (" + total + (s.getType().equals(GRADE_SYSTEM.BOULDER)? " boulders)" : " routes)");
			OpenGraph og = getOg(setup, url, null, requestedIdMedia);
			s.setMetadata(new Metadata(c, setup, authUserId, title, og)
					.setDescription(description)
					.setDefaultCenter(new LatLng(65.27462, 18.55251))
					.setDefaultZoom(5));
		}
		else if (m instanceof Ticks) {
			Ticks t = (Ticks)m;
			String description = String.format("Page %d/%d", t.getCurrPage(), t.getNumPages());
			OpenGraph og = getOg(setup, "/ticks/" + t.getCurrPage(), null, requestedIdMedia);
			t.setMetadata(new Metadata(c, setup, authUserId, "Public ascents", og).setDescription(description));
		}
		else if (m instanceof Permissions) {
			Permissions p = (Permissions)m;
			OpenGraph og = getOg(setup, "/permissions", null, requestedIdMedia);
			p.setMetadata(new Metadata(c, setup, authUserId, "Permissions", og));
		}
		else if (m instanceof About) {
			About a = (About)m;
			OpenGraph og = getOg(setup, "/about", null, requestedIdMedia);
			a.setMetadata(new Metadata(c, setup, authUserId, "About", og));
		}
		else if (m instanceof MediaSvg) {
			MediaSvg x = (MediaSvg)m;
			OpenGraph og = getOg(setup, "/", null, requestedIdMedia);
			x.setMetadata(new Metadata(c, setup, authUserId, "Media SVG", og));
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