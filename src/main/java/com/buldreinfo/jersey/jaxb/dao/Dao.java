package com.buldreinfo.jersey.jaxb.dao;

import com.buldreinfo.jersey.jaxb.dao.repositories.ActivityRepository;
import com.buldreinfo.jersey.jaxb.dao.repositories.AreaRepository;
import com.buldreinfo.jersey.jaxb.dao.repositories.ExternalLinksRepository;
import com.buldreinfo.jersey.jaxb.dao.repositories.FrontpageRepository;
import com.buldreinfo.jersey.jaxb.dao.repositories.GeoRepository;
import com.buldreinfo.jersey.jaxb.dao.repositories.HierarchyRepository;
import com.buldreinfo.jersey.jaxb.dao.repositories.MediaRepository;
import com.buldreinfo.jersey.jaxb.dao.repositories.ProblemRepository;
import com.buldreinfo.jersey.jaxb.dao.repositories.RegionRepository;
import com.buldreinfo.jersey.jaxb.dao.repositories.SectorRepository;
import com.buldreinfo.jersey.jaxb.dao.repositories.TickRepository;
import com.buldreinfo.jersey.jaxb.dao.repositories.TodoRepository;
import com.buldreinfo.jersey.jaxb.dao.repositories.TrashRepository;
import com.buldreinfo.jersey.jaxb.dao.repositories.UserRepository;

public class Dao {
	private final ActivityRepository activityRepo;
	private final AreaRepository areaRepo;
	private final ExternalLinksRepository externalLinksRepo;
	private final FrontpageRepository frontpageRepo;
	private final GeoRepository geoRepo;
	private final HierarchyRepository hierarchyRepo;
	private final MediaRepository mediaRepo;
	private final ProblemRepository problemRepo;
	private final RegionRepository regionRepo;
	private final SectorRepository sectorRepo;
	private final TickRepository tickRepo;
	private final TodoRepository todoRepo;
	private final TrashRepository trashRepo;
	private final UserRepository userRepo;
	
	public Dao() {
		this.activityRepo = new ActivityRepository();
		this.areaRepo = new AreaRepository(this);
		this.externalLinksRepo = new ExternalLinksRepository();
		this.frontpageRepo = new FrontpageRepository();
		this.geoRepo = new GeoRepository();
		this.hierarchyRepo = new HierarchyRepository(this);
		this.mediaRepo = new MediaRepository(this);
		this.problemRepo = new ProblemRepository(this);
		this.regionRepo = new RegionRepository(this);
		this.sectorRepo = new SectorRepository(this);
		this.tickRepo = new TickRepository(this);
		this.todoRepo = new TodoRepository();
		this.trashRepo = new TrashRepository(this);
		this.userRepo = new UserRepository(this);
	}
	
	public FrontpageRepository getFrontpageRepo() {
		return frontpageRepo;
	}
	
	public HierarchyRepository getHierarchyRepo() {
		return hierarchyRepo;
	}
	
	public TickRepository getTickRepo() {
		return tickRepo;
	}
	
	public TrashRepository getTrashRepo() {
		return trashRepo;
	}
	
	public TodoRepository getTodoRepo() {
		return todoRepo;
	}
	
	public AreaRepository getAreaRepo() {
		return areaRepo;
	}
	
	public RegionRepository getRegionRepo() {
		return regionRepo;
	}
	
	public ExternalLinksRepository getExternalLinksRepo() {
		return externalLinksRepo;
	}
	
	public GeoRepository getGeoRepo() {
		return geoRepo;
	}
	
	public ProblemRepository getProblemRepo() {
		return problemRepo;
	}

	public SectorRepository getSectorRepo() {
		return sectorRepo;
	}
	
	public ActivityRepository getActivityRepo() {
		return activityRepo;
	}
	
	public MediaRepository getMediaRepo() {
		return mediaRepo;
	}
	
	public UserRepository getUserRepo() {
		return userRepo;
	}
}