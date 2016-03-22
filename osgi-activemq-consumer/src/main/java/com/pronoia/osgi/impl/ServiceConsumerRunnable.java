package com.pronoia.osgi.impl;

import java.util.TimerTask;

import com.pronoia.osgi.service.DynamicService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceConsumerRunnable extends TimerTask {
    Logger log = LoggerFactory.getLogger(this.getClass());

    BundleContext context;

    public ServiceConsumerRunnable(BundleContext context, ServiceReference parentServiceReference) {
        this.context = context;
        this.parentServiceReference = parentServiceReference;
    }

    ServiceReference<DynamicService> parentServiceReference;
    DynamicService cachedServiceObject;

    @Override
    public void run() {
        runWithLocalServiceReference();
    }

    void runWithLocalServiceReference() {
        ServiceReference<DynamicService> serviceReference = null;
        DynamicService serviceObject = null;

        log.info("runWithLocalServiceReference");
        try {
            // lookup the current "best" DynamicService each time, just before we need to use it
            serviceReference = context.getServiceReference(DynamicService.class);

            // if the service reference is null then we know there's no log service available
            if (null == serviceReference) {
                log.warn("getServiceReference( {} ) returned null", DynamicService.class.getName());
                return;
            }

            serviceObject = context.getService(serviceReference);

            // if the dereferenced instance is null then we know the service has been removed
            if (null == serviceObject) {
                log.warn("getService returned null");
                return;
            }

            serviceObject.execute("ping");
        } catch (Exception ex) {
            log.error("Unexpected exception", ex);
        } finally {
            if (null != serviceObject) {
                serviceObject = null;
            }

            if (null != serviceReference) {
                // let the OSGi framework know we're done with the serviceObject
                context.ungetService(serviceReference);
                serviceReference = null;
            }
        }

    }

    void runWithParentServiceReference() {
        DynamicService serviceObject = null;

        log.info("runWithParentServiceReference");
        try {
            // if the service reference is null then we know there's no log service available
            if (null == parentServiceReference) {
                log.warn("parentServiceReference is null");
                return;
            }

            serviceObject = context.getService(parentServiceReference);

            // if the dereferenced instance is null then we know the service has been removed
            if (null == serviceObject) {
                log.warn("getService returned null");
                return;
            }

            serviceObject.execute("ping");
        } catch (Exception ex) {
            log.error("Unexpected exception", ex);
        } finally {
            if (null != serviceObject) {
                serviceObject = null;
            }
        }

    }

    void runWithCachedServiceObject() {
        log.info("runWithCachedServiceObject");
        try {
            if (null == cachedServiceObject) {
                log.info( "No service object - retrieving from service reference");
                if (null == parentServiceReference) {
                    log.error("Unable to retrieve service object - parentServiceReference is null");
                    return;
                }
                cachedServiceObject = context.getService( parentServiceReference);
                // if the dereferenced instance is null then we know the service has been removed
                if (null == cachedServiceObject) {
                    log.warn("getService returned null");
                    return;
                }
            }

             cachedServiceObject.execute("ping");
        } catch (Exception ex) {
            log.error("Unexpected exception", ex);
        }

    }
}
