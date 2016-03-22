package com.pronoia.osgi.impl;

import java.util.Timer;

import com.pronoia.osgi.service.DynamicService;

import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ServiceConsumerActivator implements BundleActivator {
    Logger _log = LoggerFactory.getLogger(this.getClass());

    BundleContext context;

    Timer scheduler = new Timer();

    // So we can hold on to these for testing
    ServiceReference<DynamicService> serviceReference;
    DynamicService serviceObject;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        context = bundleContext;
        _log.info("start called");

        getService();

        ServiceConsumerRunnable task = new ServiceConsumerRunnable(context, serviceReference);

        scheduler.scheduleAtFixedRate(task, 5000, 5000);
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        _log.info("stop called");

        if (null != scheduler) {
            scheduler.cancel();
            scheduler = null;
        }

        if (null != serviceReference) {
            context.ungetService(serviceReference);
            serviceReference = null;
        }

        if (null != serviceObject) {
            serviceObject = null;
        }
    }

    void getService() {
        _log.info( "Holding on to ServiceReference and ServiceObject in activator");
        serviceReference = context.getServiceReference(DynamicService.class);
        serviceObject = context.getService(serviceReference);
    }
}
