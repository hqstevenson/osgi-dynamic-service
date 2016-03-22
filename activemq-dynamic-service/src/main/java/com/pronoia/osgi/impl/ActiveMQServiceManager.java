package com.pronoia.osgi.impl;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.RandomAccess;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class ActiveMQServiceManager implements Runnable {
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

        scheduler = new ScheduledThreadPoolExecutor(1);
        scheduledFuture = scheduler.scheduleWithFixedDelay(this, checkInterval, checkInterval, TimeUnit.SECONDS);
    }

    public void destroy() {
        log.info("destroy");
        scheduledFuture.cancel(true);
        scheduledFuture = null;
        scheduler.shutdown();
        scheduler = null;
        unregisterService();
        connectionFactory = null;
    }

    public void setContext(BundleContext context) {
        log.info( "setContext");
        this.context = context;
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

    public void setBrokerUrl(String brokerUrl) {
        log.info("setBrokerUrl");
        this.brokerUrl = brokerUrl;
        propertyChanged();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        log.info( "setUserName");
        this.userName = userName;
        propertyChanged();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        log.info( "setPassword");
        this.password = password;
        propertyChanged();
    }

    @Override
    public void run() {
        checkConnection();
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
}
