package com.buldreinfo.jersey.jaxb.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Metadata;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.User;
import com.google.common.base.Strings;

import jersey.repackaged.com.google.common.base.Joiner;

public class MetaHelper {
	private List<Setup> setups = new ArrayList<>();

	public MetaHelper() {
		setups.add(new Setup(1, true, "Buldreinfo", "buldreinfo.com", "Bouldering in Rogaland (Stavanger, Western Norway)"));
		setups.add(new Setup(2, true, "Buldring i Hordaland", "buldring.bergen-klatreklubb.no", "Bouldering in Hordaland (Bergen, Western Norway)"));
		setups.add(new Setup(3, true, "Buldring i Fredrikstad", "buldring.fredrikstadklatreklubb.org", "Bouldering in Fredrikstad (Eastern Norway)"));
		setups.add(new Setup(4, false, "Bratte Linjer", "brattelinjer.no", "Climbing in Rogaland (Stavanger, Western Norway)"));
		setups.add(new Setup(5, true, "Buldring i Jotunheimen", "buldring.jotunheimenfjellsport.com", "Bouldering in Jotunheimen (Norway)"));
		setups.add(new Setup(6, false, "Klatring i Jotunheimen", "klatring.jotunheimenfjellsport.com", "Climbing in Jotunheimen (Norway)"));
	}

	public Setup getSetup(String base) {
		Optional<Setup> s = setups.stream().filter(x -> base.contains(x.getDomain())).findAny();
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
			a.setMetadata(new Metadata(setup.getTitle(a.getName()), description));
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
			f.setMetadata(new Metadata(setup.getTitle(null), description));
		}
		else if (m instanceof Problem) {
			Problem p = (Problem)m;
			String title = String.format("%s [%s] (%s / %s)", p.getName(), p.getGrade(), p.getAreaName(), p.getSectorName());
			String description = p.getComment();
			if (p.getFa() != null && !p.getFa().isEmpty()) {
				String fa = Joiner.on(", ").join(p.getFa().stream().map(x -> (x.getFirstname() + " " + x.getSurname()).trim()).collect(Collectors.toList()));
				description = (!Strings.isNullOrEmpty(description)? description + " | " : "") + "First ascent by " + fa + (!Strings.isNullOrEmpty(p.getFaDateHr())? " (" + p.getFaDate() + ")" : "");
			}
			p.setMetadata(new Metadata(setup.getTitle(title), description));
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
			s.setMetadata(new Metadata(setup.getTitle(title), description));
		}
		else if (m instanceof User) {
			User u = (User)m;
			String title = String.format("%s (log book)", u.getName());
			String description = String.format("%d ascents, %d pictures taken, %d appearance in pictures, %d videos created, %d appearance in videos", u.getTicks().size(), u.getNumImagesCreated(), u.getNumImageTags(), u.getNumVideosCreated(), u.getNumVideoTags());
			u.setMetadata(new Metadata(setup.getTitle(title), description));
		}
	}
}