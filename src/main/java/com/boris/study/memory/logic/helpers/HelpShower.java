package com.boris.study.memory.logic.helpers;

import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.logic.sructure.Request;
import com.boris.study.memory.logic.sructure.StatelessBotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class HelpShower extends StatelessBotScenario {

    @Override
    public void processStateless(Request request) {
        logger.info("Showing help - Starting for " + getClient());
        try {
            bot.execute(botUtils.markdownMessage(
                    uiUtils.getHelp(),
                    botUtils.retrieveChat(request.update).getId()
            ));
        } catch (TelegramApiException e) {
            logger.error("Failed to show help in request " + request, e);
        }
        logger.info("Showing help - Finished for " + getClient());
    }

    public HelpShower(ScenarioState state) {
        super(state);
    }

    private final Logger logger = LoggerFactory.getLogger(HelpShower.class);
}