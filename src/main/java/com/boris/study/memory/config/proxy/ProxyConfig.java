package com.boris.study.memory.config.proxy;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bot.proxy.config")
public class ProxyConfig {
    private int updateDelay = 600; // in seconds, updated from property file
    private int connectTimeout = 5000; // in milliseconds, updated from property file
}
