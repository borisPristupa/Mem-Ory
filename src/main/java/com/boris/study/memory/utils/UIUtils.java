package com.boris.study.memory.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@Setter
@Getter
@PropertySource(value = "messages_ru.properties", encoding = "UTF-8")
@ConfigurationProperties(prefix = "messages", ignoreUnknownFields = false)
public class UIUtils {
    private Errors errors;
    private Config config;
    private String help;
    private String commands;
    private String greeting;

    private String labelNavigationInfo;

    public String getGreeting(String name) {
        return String.format(greeting, name);
    }

    public String getLabelNavigationInfo() {
        return String.format(labelNavigationInfo, DataUtils.LABEL_NAME_LENGTH);
    }

    @Setter
    @Getter
    @ToString
    public static class Errors {
        private String wrongChatType;
        private String unknownCommand;
        private String needlessStart;
        private String noDataByUrl;
    }

    @Setter
    @Getter
    @ToString
    public static class Config {
        private String url, txt, click;
    }
}
