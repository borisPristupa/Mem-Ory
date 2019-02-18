package com.boris.study.memory.logic.helpers;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.logic.CommandHandler;
import com.boris.study.memory.logic.sructure.Request;
import com.boris.study.memory.logic.sructure.StatelessBotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class CommandsShower extends StatelessBotScenario {

    @Override
    public void processStateless(Request request) {
        Client client = getClient();
        logger.info("CommandsShower - Starting for " + client);

        SendMessage commandsMessage = botUtils.markdownMessage(
                uiUtils.getCommands(),
                botUtils.retrieveChat(request.update).getId()
        );
        commandsMessage.setReplyMarkup(botUtils.oneColumnReplyMarkup(CommandHandler.getKnownCommands()));
        try {
            bot.execute(commandsMessage);
        } catch (TelegramApiException e) {
            logger.error("Failed to send known commands in request " + request, e);
        }

        logger.info("CommandsShower - Finished for " + client);
    }

    public CommandsShower(ScenarioState state) {
        super(state);
    }

    private final Logger logger = LoggerFactory.getLogger(CommandsShower.class);
}