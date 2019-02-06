package com.boris.study.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

@EnableScheduling
@SpringBootApplication
public class MemoryApplication {

    public static void main(String[] args) {
        ApiContextInitializer.init();
        ConfigurableApplicationContext ctx = SpringApplication.run(MemoryApplication.class, args);
        try {
            new TelegramBotsApi().registerBot(ctx.getBean(MemOryBot.class));
        } catch (TelegramApiRequestException e) {
            logger.error("Exception occurred while registering bot", e);
        }
    }

    private static Logger logger = LoggerFactory.getLogger(MemoryApplication.class);
}

