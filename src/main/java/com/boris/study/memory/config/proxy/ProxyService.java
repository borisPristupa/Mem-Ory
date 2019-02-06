package com.boris.study.memory.config.proxy;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

@Component
@ConfigurationProperties(prefix = "bot.proxy")
public class ProxyService {
    static final String CONNECTION_DESTINATION = "https://api.telegram.org";

    @Setter
    private List<String> list = new ArrayList<>(); // is read from property file
    private TreeSet<ProxyResponse> proxyResponses;
    private ProxyUpdater updater;

    private ProxyConfig proxyConfig;

    @PostConstruct
    void initEnvironment() {
        logger.info(String.format("Received proxy config: connectTimeout = %sms, updateDelay = %ss",
                proxyConfig.getConnectTimeout(),
                proxyConfig.getUpdateDelay()));
        proxyResponses = new TreeSet<>();
        updater = new ProxyUpdater(proxyConfig.getUpdateDelay(), proxyConfig.getConnectTimeout());
        if (list.size() == 0) {
            logger.warn("Empty proxy list");
            return;
        }

        logger.info(String.format("Resolving %s proxies - Starting...", list.size()));

        for (String proxy : list) {
            try {
                proxyResponses.add(new ProxyResponse(proxy, proxyConfig.getConnectTimeout()));
            } catch (IllegalArgumentException e) {
                logger.warn(String.format("Proxy %s is invalid", proxy));
            }
        }
        updater.setLastUpdated(System.currentTimeMillis());
        logger.info("Resolving proxies - Successfully finished");
    }

    // Returns Optional.empty() if no working proxy found
    public Optional<String> getFastestProxy(boolean forceUpdate) {
        if (forceUpdate || updater.isUpdateNeeded()) {
            if (!updateProxies())
                return Optional.empty();
        }

        for (ProxyResponse response : proxyResponses)
            if (ProxyResponse.NOT_RESPONDING != response.getConnectTime())
                return Optional.ofNullable(response.getProxy());

        return Optional.empty();
    }

    private boolean updateProxies() {
        logger.info("Updating proxies' response times - Starting...");
        boolean anyWorking = false;
        for (ProxyResponse response : proxyResponses) {
            anyWorking |= updater.updateProxy(response);
            updater.setLastUpdated(System.currentTimeMillis());
        }
        logger.info("Updating proxies' response times - Successfully finished");
        return anyWorking;
    }

    @Scheduled(fixedDelay = 10000)
    private void startUpdate() {
        if (list.size() > 0 && updater.isUpdateNeeded()) {
            logger.info("Scheduled update of proxies - Starting...");
            updateProxies();
        }
    }

    @Autowired
    public ProxyService(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler();
    }

    private static final Logger logger = LoggerFactory.getLogger(ProxyService.class);
}
