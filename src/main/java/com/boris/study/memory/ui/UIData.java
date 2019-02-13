package com.boris.study.memory.ui;

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
public class UIData {
    private String help;
    private Errors errors;
    private String greeting;

    public String getGreeting(String name) {
        return String.format(greeting, name);
    }

    @Setter
    @Getter
    @ToString
    public static class Errors {
        private String wrongChatType;
    }
}
