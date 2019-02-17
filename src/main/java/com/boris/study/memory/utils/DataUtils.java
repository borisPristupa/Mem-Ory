package com.boris.study.memory.utils;

import com.boris.study.memory.data.repository.DataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class DataUtils {
    public static final int LABEL_NAME_LENGTH = 30;

    private final DataRepository dataRepository;

    public String generateUrl() {
        final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345689";
        final int URL_LENGTH = 40;

        StringBuilder url;
        Random random = new Random();
        do {
            url = new StringBuilder("/d_");

            for (int i = 0; i < URL_LENGTH; i++) {
                char c = CHARS.charAt(random.nextInt(CHARS.length()));
                if (random.nextBoolean())
                    url.append(String.valueOf(c).toLowerCase());
                else
                    url.append(c);
            }

        } while (dataRepository.existsById(url.toString()));
        return url.toString();
    }

    @Autowired
    public DataUtils(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    public boolean isValidLabelName(String name) {
        return null != name && name.length() < LABEL_NAME_LENGTH && !name.contains("\n");
    }

    public boolean isValidDataUrl(String text) {
        return text.matches("/d_[A-Za-z0-9]+");
    }
}
