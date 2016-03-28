package com.pronoia.osgi.impl;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveMQServiceManagerFactory implements ManagedServiceFactory{
    Logger log = LoggerFactory.getLogger(this.getClass());

    BundleContext context;
    ScheduledExecutorService scheduler;
    Map<String, ActiveMQServiceManager> managerMap = new HashMap<>();

    public void init() {
        log.info( "init");
        scheduler = new ScheduledThreadPoolExecutor(5);

    }

    public void destroy() {
        log.info("destroy");
        for (ActiveMQServiceManager manager: managerMap.values()) {
            manager.destroy();
        }
        scheduler.shutdown();
    }

    public void setContext(BundleContext context) {
        log.info( "setContext");
        this.context = context;
    }

    @Override
    public String getName() {
        log.info( "getName");

        return "Factory to create ActiveMQServiceManagers";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> dictionary) throws ConfigurationException {
        log.info( "updated( {}, {} )", pid, dictionary);
        if ( managerMap.containsKey(pid)) {
            managerMap.get(pid).updated( dictionary );
        } else {
            ActiveMQServiceManager answer = new ActiveMQServiceManager();
            answer.setContext(this.context);
            answer.setScheduler(this.scheduler);
            answer.updated(dictionary);
            answer.init();
            managerMap.put(pid, answer);
        }
    }

    @Override
    public void deleted(String pid) {
        log.info( "deleted( {} )", pid);
        if ( managerMap.containsKey(pid)) {
            ActiveMQServiceManager activeMQServiceManager = managerMap.remove(pid);
            activeMQServiceManager.destroy();
        }
    }
}
