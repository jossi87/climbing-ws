package com.buldreinfo.jersey.jaxb.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.helpers.GradeHelper;
import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.buldreinfo.jersey.jaxb.metadata.jsonld.JsonLdCreator;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Browse;
import com.buldreinfo.jersey.jaxb.model.Finder;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Grade;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Metadata;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.User;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;

import jersey.repackaged.com.google.common.base.Joiner;

public class MetaHelper {
	private static Logger logger = LogManager.getLogger();
	private List<Setup> setups = new ArrayList<>();

	public MetaHelper() {
		setups.add(new Setup(1)
				.setBouldering(true)
				.setTitle("Buldreinfo")
				.setDomain("buldreinfo.com")
				.setDescription("Bouldering in Rogaland (Stavanger, Western Norway)")
				.setShowLogoPlay(true).setShowLogoSis(true).setShowLogoBrv(true)
				.setLatLng(58.78119, 5.86361).setDefaultZoom(7));
		setups.add(new Setup(2)
				.setBouldering(true)
				.setTitle("Buldring i Hordaland")
				.setDomain("buldring.bergen-klatreklubb.no")
				.setDescription("Bouldering in Hordaland (Bergen, Western Norway)")
				.setShowLogoPlay(true).setShowLogoSis(false).setShowLogoBrv(false)
				.setLatLng(60.47521, 6.83169).setDefaultZoom(7));
		setups.add(new Setup(3)
				.setBouldering(true)
				.setTitle("Buldring i Fredrikstad")
				.setDomain("buldring.fredrikstadklatreklubb.org")
				.setDescription("Bouldering in Fredrikstad (Eastern Norway)")
				.setShowLogoPlay(true).setShowLogoSis(false).setShowLogoBrv(false)
				.setLatLng(59.22844, 10.91722).setDefaultZoom(7));
		setups.add(new Setup(4)
				.setBouldering(false)
				.setTitle("Bratte Linjer")
				.setDomain("brattelinjer.no")
				.setDescription("Climbing in Rogaland (Stavanger, Western Norway)")
				.setShowLogoPlay(false).setShowLogoSis(false).setShowLogoBrv(true)
				.setLatLng(58.78119, 5.86361).setDefaultZoom(9));
		setups.add(new Setup(5)
				.setBouldering(true)
				.setTitle("Buldring i Jotunheimen")
				.setDomain("buldring.jotunheimenfjellsport.com")
				.setDescription("Bouldering in Jotunheimen (Norway)")
				.setShowLogoPlay(true).setShowLogoSis(false).setShowLogoBrv(false)
				.setLatLng(61.60500, 8.47750).setDefaultZoom(7));
		setups.add(new Setup(6)
				.setBouldering(false)
				.setTitle("Klatring i Jotunheimen")
				.setDomain("klatring.jotunheimenfjellsport.com")
				.setDescription("Climbing in Jotunheimen (Norway)")
				.setShowLogoPlay(false).setShowLogoSis(false).setShowLogoBrv(false)
				.setLatLng(61.60500, 8.47750).setDefaultZoom(9));
	}
	
	public Setup getSetup(HttpServletRequest request) {
		Preconditions.checkNotNull(request);
		String origin = request.getHeader(HttpHeaders.ORIGIN);
		if (!Strings.isNullOrEmpty(origin)) {
			Optional<Setup> s = setups.stream().filter(x -> origin.contains(x.getDomain())).findAny();
			if (s.isPresent()) {
				return s.get();
			}
			logger.warn("Unknown origin=" + origin);
		}
		String serverName = Strings.emptyToNull(request.getServerName());
		Preconditions.checkNotNull(serverName, "Invalid request=" + request);
		Optional<Setup> s = setups.stream().filter(x -> serverName.equalsIgnoreCase(x.getDomain())).findAny();
		if (s.isPresent()) {
			return s.get();
		}
		return setups.get(0);
	}

	public Setup getSetup(int regionId) {
		Optional<Setup> s = setups.stream().filter(x -> x.getIdRegion() == regionId).findAny();
		if (s.isPresent()) {
			return s.get();
		}
		return setups.get(0);
	}

	public Setup getSetup(String base) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(base), "Invalid base=" + base);
		Optional<Setup> s = setups.stream().filter(x -> base.contains(x.getDomain())).findAny();
		if (s.isPresent()) {
			return s.get();
		}
		return setups.get(0);
	}

	public List<Setup> getSetups() {
		return setups;
	}

	public void updateMetadata(IMetadata m, Setup setup) {
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
			a.setMetadata(new Metadata(setup.getTitle(a.getName()))
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
			f.setMetadata(new Metadata(setup.getTitle(null))
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
			List<Grade> grades = new ArrayList<>();
			Map<Integer, String> lookup = GradeHelper.getGrades(setup.getIdRegion());
			for (int id : lookup.keySet()) {
				grades.add(new Grade(id, lookup.get(id)));
			}
			p.setMetadata(new Metadata(setup.getTitle(title))
					.setDescription(description)
					.setJsonLd(JsonLdCreator.getJsonLd(setup, p))
					.setIsBouldering(setup.isBouldering())
					.setGrades(grades));
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
			s.setMetadata(new Metadata(setup.getTitle(title))
					.setDescription(description)
					.setJsonLd(JsonLdCreator.getJsonLd(setup, s))
					.setDefaultCenter(setup.getDefaultCenter())
					.setDefaultZoom(setup.getDefaultZoom())
					.setIsBouldering(setup.isBouldering()));
		}
		else if (m instanceof User) {
			User u = (User)m;
			String title = String.format("%s (log book)", u.getName());
			String description = String.format("%d ascents, %d pictures taken, %d appearance in pictures, %d videos created, %d appearance in videos", u.getTicks().size(), u.getNumImagesCreated(), u.getNumImageTags(), u.getNumVideosCreated(), u.getNumVideoTags());
			List<Grade> grades = new ArrayList<>();
			Map<Integer, String> lookup = GradeHelper.getGrades(setup.getIdRegion());
			for (int id : lookup.keySet()) {
				grades.add(new Grade(id, lookup.get(id)));
			}
			u.setMetadata(new Metadata(setup.getTitle(title))
					.setDescription(description)
					.setGrades(grades));
		}
		else if (m instanceof Meta) {
			Meta x = (Meta)m;
			x.setMetadata(new Metadata(setup.getTitle())
					.setDefaultCenter(setup.getDefaultCenter())
					.setDefaultZoom(setup.getDefaultZoom()));
		}
		else if (m instanceof Browse) {
			Browse b = (Browse)m;
			String description = String.format("%d areas, %d sectors, %d %s",
					b.getAreas().size(),
					b.getAreas().stream().map(x -> x.getNumSectors()).mapToInt(Integer::intValue).sum(),
					b.getAreas().stream().map(x -> x.getNumProblems()).mapToInt(Integer::intValue).sum(),
					setup.isBouldering()? "boulders" : "routes");
			b.setMetadata(new Metadata(setup.getTitle("Browse"))
					.setDescription(description)
					.setDefaultCenter(setup.getDefaultCenter())
					.setDefaultZoom(setup.getDefaultZoom()));
		}
		else if (m instanceof Finder) {
			Finder f = (Finder)m;
			String title = setup.getTitle("Finder [" + f.getGrade() + "]");
			String description = String.format("%d %s",
					f.getProblems().size(),
					(setup.isBouldering()? "problems" : "routes"));
			f.setMetadata(new Metadata(title)
					.setDescription(description)
					.setDefaultCenter(setup.getDefaultCenter())
					.setIsBouldering(setup.isBouldering()));
		}
		else {
			throw new RuntimeException("Invalid m=" + m);
		}
	}
}