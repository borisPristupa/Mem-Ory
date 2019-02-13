package com.boris.study.memory.logic;

import com.boris.study.memory.BotUtils;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.logic.sructure.BotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Dispatcher extends BotScenario {

    @Override
    public Boolean process(Update update, boolean forceRestart) {

        if (!continueProcessing(update, forceRestart))
            return false;

        logger.info("Dispatching - Starting for client " + getClient());
        try {
            bot.execute(defaultMessage(
                    "No logic yet",
                    BotUtils.retrieveChat(update).getId()
            ));
        } catch (TelegramApiException e) {
            logger.trace("Exception occurred while dispatching", e);
        }
        logger.info("Dispatching - Finished");
        return true;
    }


    public Dispatcher(ScenarioState scenarioState) {
        super(scenarioState);
    }

    private static Logger logger = LoggerFactory.getLogger(Dispatcher.class);
}
