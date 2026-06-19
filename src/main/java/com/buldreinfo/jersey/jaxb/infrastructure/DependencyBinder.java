package com.buldreinfo.jersey.jaxb.infrastructure;

import javax.sql.DataSource;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import com.buldreinfo.jersey.jaxb.dao.ActivityRepository;
import com.buldreinfo.jersey.jaxb.dao.AreaRepository;
import com.buldreinfo.jersey.jaxb.dao.ExternalLinksRepository;
import com.buldreinfo.jersey.jaxb.dao.FrontpageRepository;
import com.buldreinfo.jersey.jaxb.dao.GeoRepository;
import com.buldreinfo.jersey.jaxb.dao.HierarchyRepository;
import com.buldreinfo.jersey.jaxb.dao.MediaRepository;
import com.buldreinfo.jersey.jaxb.dao.ProblemRepository;
import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.buldreinfo.jersey.jaxb.dao.SectorRepository;
import com.buldreinfo.jersey.jaxb.dao.TickRepository;
import com.buldreinfo.jersey.jaxb.dao.TodoRepository;
import com.buldreinfo.jersey.jaxb.dao.TrashRepository;
import com.buldreinfo.jersey.jaxb.dao.UserRepository;
import com.buldreinfo.jersey.jaxb.resources.ActivityResource;
import com.buldreinfo.jersey.jaxb.resources.AdministratorsResource;
import com.buldreinfo.jersey.jaxb.resources.AreasResource;
import com.buldreinfo.jersey.jaxb.resources.ElevationResource;
import com.buldreinfo.jersey.jaxb.resources.FrontpageResource;
import com.buldreinfo.jersey.jaxb.resources.InteractionResource;
import com.buldreinfo.jersey.jaxb.resources.MediaResource;
import com.buldreinfo.jersey.jaxb.resources.MetaResource;
import com.buldreinfo.jersey.jaxb.resources.ProblemsResource;
import com.buldreinfo.jersey.jaxb.resources.ProfilesResource;
import com.buldreinfo.jersey.jaxb.resources.SectorsResource;
import com.buldreinfo.jersey.jaxb.resources.UsersResource;
import com.buldreinfo.jersey.jaxb.resources.WebcamsResource;
import com.buldreinfo.jersey.jaxb.resources.WithoutJsResource;

import jakarta.inject.Singleton;

public class DependencyBinder extends AbstractBinder {
    @Override
    protected void configure() {
        bindFactory(DataSourceFactory.class).to(DataSource.class).in(Singleton.class);
        bindAsContract(TransactionManager.class).in(Singleton.class);
        
        // Bind Repositories as Singletons
        bindAsContract(ActivityRepository.class).in(Singleton.class);
        bindAsContract(AreaRepository.class).in(Singleton.class);
        bindAsContract(ExternalLinksRepository.class).in(Singleton.class);
        bindAsContract(FrontpageRepository.class).in(Singleton.class);
        bindAsContract(GeoRepository.class).in(Singleton.class);
        bindAsContract(HierarchyRepository.class).in(Singleton.class);
        bindAsContract(MediaRepository.class).in(Singleton.class);
        bindAsContract(ProblemRepository.class).in(Singleton.class);
        bindAsContract(RegionRepository.class).in(Singleton.class);
        bindAsContract(SectorRepository.class).in(Singleton.class);
        bindAsContract(TickRepository.class).in(Singleton.class);
        bindAsContract(TodoRepository.class).in(Singleton.class);
        bindAsContract(TrashRepository.class).in(Singleton.class);
        bindAsContract(UserRepository.class).in(Singleton.class);

        // Bind Resources as Singletons
        bindAsContract(ActivityResource.class).in(Singleton.class);
        bindAsContract(AdministratorsResource.class).in(Singleton.class);
        bindAsContract(AreasResource.class).in(Singleton.class);
        bindAsContract(ElevationResource.class).in(Singleton.class);
        bindAsContract(FrontpageResource.class).in(Singleton.class);
        bindAsContract(InteractionResource.class).in(Singleton.class);
        bindAsContract(MediaResource.class).in(Singleton.class);
        bindAsContract(MetaResource.class).in(Singleton.class);
        bindAsContract(ProblemsResource.class).in(Singleton.class);
        bindAsContract(ProfilesResource.class).in(Singleton.class);
        bindAsContract(SectorsResource.class).in(Singleton.class);
        bindAsContract(UsersResource.class).in(Singleton.class);
        bindAsContract(WebcamsResource.class).in(Singleton.class);
        bindAsContract(WithoutJsResource.class).in(Singleton.class);
    }
}