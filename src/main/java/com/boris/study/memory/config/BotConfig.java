package com.boris.study.memory.config;

import com.boris.study.memory.config.proxy.ProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;

import java.util.Optional;

@Configuration
public class BotConfig {
    @Value("${bot.proxy.needed}")
    private boolean proxyNeeded = false;

    @Bean
    public DefaultBotOptions getDefaultBotOptions(ProxyService proxyService) {
        DefaultBotOptions options = ApiContext.getInstance(DefaultBotOptions.class);

        if (proxyNeeded) {
            Optional<String> proxy = proxyService.getFastestProxy(false);
            if (proxy.isPresent()) {
                String proxyHost = proxy.get().split(":")[0];
                int proxyPort = Integer.parseInt(proxy.get().split(":")[1]);

                options.setProxyHost(proxyHost);
                options.setProxyPort(proxyPort);
                options.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
                logger.info(String.format("Enabling %s proxy %s:%s",
                        options.getProxyType(),
                        proxyHost,
                        proxyPort));
            } else {
                logger.warn("No available proxy found");
            }
        }
        return options;
    }

    private static Logger logger = LoggerFactory.getLogger(BotConfig.class);
}
