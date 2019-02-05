package com.boris.study.memory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;

@Component
public class BotConfig {
    private static Logger logger = LoggerFactory.getLogger(BotConfig.class);

    @Value("${proxy.host}")
    private String proxyHost;
    @Value("${proxy.port}")
    private Integer proxyPort;

    @Bean
    public DefaultBotOptions getDefaultBotOptions() {
        DefaultBotOptions options = ApiContext.getInstance(DefaultBotOptions.class);
        options.setProxyHost(proxyHost);
        options.setProxyPort(proxyPort);
        options.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
        logger.info(String.format("Enabling %s proxy %s:%s",
                options.getProxyType(),
                proxyHost,
                proxyPort));
        return options;
    }
}
