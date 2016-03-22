package com.pronoia.osgi.impl;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;
import java.util.Properties;

public class ManagedControlServiceClientActivator implements BundleActivator {
    Logger _log = LoggerFactory.getLogger(this.getClass());

    BundleContext _context;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        _context = bundleContext;
        _log.info("start called");

        ServiceReference configAdminServiceRef = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        ConfigurationAdmin configAdminService = (ConfigurationAdmin) bundleContext.getService( configAdminServiceRef );

        Configuration configuration = configAdminService.createFactoryConfiguration("com.pronoia.controlservice");

        Properties props = new Properties();
        props.put( "control.name", "FredControlOne" );
        configuration.update( (Dictionary)props );

        configuration = configAdminService.createFactoryConfiguration( "com.pronoia.controlservice");
        props = new Properties();
        props.put( "control.name", "FredControlTwo" );
        configuration.update( (Dictionary)props );
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        _log.info("stop called");
        _context = null;
    }
}
