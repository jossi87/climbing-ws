package com.buldreinfo.jersey.jaxb.batch;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import com.buldreinfo.jersey.jaxb.infrastructure.DependencyBinder;

public class BatchBootstrapper {
    public static ServiceLocator createLocator() {
        var locator = ServiceLocatorFactory.getInstance().create("batch-locator-" + System.currentTimeMillis());
        ServiceLocatorUtilities.bind(locator, new DependencyBinder());
        return locator;
    }
}