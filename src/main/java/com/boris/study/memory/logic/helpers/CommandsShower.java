package com.boris.study.memory.logic.helpers;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.logic.CommandHandler;
import com.boris.study.memory.logic.sructure.Request;
import com.boris.study.memory.logic.sructure.StatelessBotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.stream.Collectors;

public class CommandsShower extends StatelessBotScenario {

    @Override
    public void processStateless(Request request) {
        Client client = getClient();
        logger.info("CommandsShower - Starting for " + client);

        SendMessage commandsMessage = botUtils.markdownMessage(
                uiUtils.getCommands(),
                botUtils.retrieveChat(request.update).getId()
        );
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();

        markup.setResizeKeyboard(true);
        markup.setKeyboard(CommandHandler.getKnownCommands()
                .stream()
                .map(command -> {
                    KeyboardRow row = new KeyboardRow();
                    row.add(command);
                    return row;
                })
                .collect(Collectors.toList()));

        commandsMessage.setReplyMarkup(markup);
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