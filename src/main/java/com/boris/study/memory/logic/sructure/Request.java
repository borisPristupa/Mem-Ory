package com.boris.study.memory.logic.sructure;

import lombok.ToString;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;

@ToString
public class Request extends HashMap<String, String> {
    public final Update update;

    public Request(Update update) {
        this.update = update;
    }

}
