package com.boris.study.memory;

import com.boris.study.memory.repository.SampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class MemOryBot extends TelegramLongPollingBot {
    private static Logger logger = LoggerFactory.getLogger(MemOryBot.class);

    @Value("${bot.token}")
    private String token;
    @Value("${bot.username}")
    private String username;

    private SampleRepository sampleRepository;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            SendMessage response = new SendMessage();
            response.setChatId(update.getMessage().getChatId());

            response.setText(String.format("Hello, creature №%s! My name is %s. Not very nice to meet you...",
                    update.getMessage().getFrom().getId(),
                    username));
            try {
                execute(response);
            } catch (TelegramApiException e) {
                logger.trace("Couldn't execute response", e);
            }

            response.setText("Current data: " + sampleRepository.findAll());
            try {
                execute(response);
            } catch (TelegramApiException e) {
                logger.trace("Couldn't execute response", e);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Autowired
    public void setSampleRepository(SampleRepository sampleRepository) {
        this.sampleRepository = sampleRepository;
    }

}
