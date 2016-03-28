package com.pronoia.osgi.impl;

import java.util.Dictionary;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveMQServiceManager implements Runnable, BlueprintListener {
    Logger log = LoggerFactory.getLogger(this.getClass());

    ScheduledExecutorService scheduler;
    ScheduledFuture<?> scheduledFuture;

    BundleContext context;
    long checkInterval = 5;

    String brokerUrl = "tcp://localhost:61616";
    String userName = "admin";
    String password = "admin";
    ActiveMQConnectionFactory connectionFactory;

    ServiceRegistration<ConnectionFactory> serviceRegistration;

    public void init() {
        log.info( "init");
        connectionFactory = new ActiveMQConnectionFactory(brokerUrl, userName, password);
        checkConnection();

        scheduledFuture = scheduler.scheduleWithFixedDelay(this, checkInterval, checkInterval, TimeUnit.SECONDS);
    }

    public void destroy() {
        log.info("destroy");
        scheduledFuture.cancel(true);
        scheduledFuture = null;
        scheduler = null;
        unregisterService();
        connectionFactory = null;
    }

    public void setContext(BundleContext context) {
        this.context = context;
    }

    public void setScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    public long getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(long checkInterval) {
        log.info( "setCheckInterval {}", checkInterval);
        this.checkInterval = checkInterval;
        if (null != scheduledFuture) {
            log.info( "Resetting service Timer");
            scheduledFuture.cancel(false);
            scheduledFuture = scheduler.scheduleWithFixedDelay(this, checkInterval, checkInterval, TimeUnit.SECONDS);
        }
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public void run() {
        checkConnection();
    }

    synchronized void checkConnection() {
        log.info("Checking the status of the ActiveMQ Broker: {}", brokerUrl);
        Connection connection = null;
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
            connection = factory.createConnection(userName, password);
            log.info( "Connection succeeded");
            registerService();
        } catch (Exception ex) {
            log.warn( "Connection failed");
            unregisterService();
        } finally {
            if (null != connection) {
                try {
                    connection.close();
                } catch (JMSException jmxEx) {
                    log.warn( "Closing connection failed");
                } finally {
                    connection = null;
                }
            }
        }
    }

    void registerService() {
        if (null == serviceRegistration) {
            log.info( "Registering Service");
            serviceRegistration = context.registerService(ConnectionFactory.class, connectionFactory.copy(), null);
        }
    }

    void unregisterService() {
        if (null != serviceRegistration) {
            log.info( "Unregistering Service");
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }

    void reRegisterService() {
        if (null != serviceRegistration) {
            unregisterService();
            registerService();
        }
    }

    @Override
    public void blueprintEvent(BlueprintEvent blueprintEvent) {
        if ( blueprintEvent.getBundle().getBundleId() == context.getBundle().getBundleId() ) {
            switch (blueprintEvent.getType()) {
                case BlueprintEvent.CREATING:
                    log.info( "CREATING: {}", brokerUrl);
                    break;
                case BlueprintEvent.CREATED:
                    log.info( "CREATED: {}", brokerUrl);
                    break;
                case BlueprintEvent.DESTROYING:
                    log.info( "DESTROYING: {}", brokerUrl);
                    this.destroy();
                    break;
                case BlueprintEvent.DESTROYED:
                    log.info( "DESTROYED: {}", brokerUrl);
                    break;
                case BlueprintEvent.FAILURE:
                    log.info( "FAILURE: {}", brokerUrl);
                    break;
                case BlueprintEvent.GRACE_PERIOD:
                    log.info( "GRACE_PERIOD: {}", brokerUrl);
                    break;
                case BlueprintEvent.WAITING:
                    log.info( "WAITING: {}, brokerUrl");
                    break;
                default:
                    log.warn( "Unknown blueprint event type");
            }
        }
    }

    public void updated(Dictionary<String,?> props) {
        if (null == props) {
            log.info( "DELETED: {}", brokerUrl);
            this.destroy();
        } else {
            log.info( "UPDATED: {}", brokerUrl);
            String answer = (String) props.get("brokerUrl");
            if (null != answer) {
                brokerUrl = answer;
            }
            answer = (String)props.get("userName");
            if (null != answer) {
                userName = answer;
            }
            answer = (String)props.get("password");
            if (null != answer) {
                password = answer;
            }
            propertyChanged();
        }
    }

    synchronized void propertyChanged() {
        if (null != connectionFactory) {
            if (null != serviceRegistration) {
                unregisterService();
                connectionFactory = new ActiveMQConnectionFactory(getBrokerUrl(), getUserName(), getPassword());
                checkConnection();
            } else {
                connectionFactory = new ActiveMQConnectionFactory(getBrokerUrl(), getUserName(), getPassword());
            }
        }
    }

}
