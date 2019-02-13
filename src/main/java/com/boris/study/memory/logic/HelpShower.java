package com.boris.study.memory.logic;

import com.boris.study.memory.BotUtils;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.logic.sructure.BotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class HelpShower extends BotScenario {

    @Override
    public Boolean process(Update update, boolean forceRestart) {
        logger.info("Showing help - Starting for " + getClient());
        try {
            bot.execute(defaultMessage(
                    uiData.getHelp(),
                    BotUtils.retrieveChat(update).getId()
            ));
        } catch (TelegramApiException e) {
            logger.trace("Failed to show help", e);
        }
        logger.info("Showing help - Finished for " + getClient());
        return true;
    }

    public HelpShower(ScenarioState state) {
        super(state);
    }

    private final Logger logger = LoggerFactory.getLogger(HelpShower.class);
}