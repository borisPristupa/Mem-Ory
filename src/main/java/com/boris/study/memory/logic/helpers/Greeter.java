package com.boris.study.memory.logic.helpers;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.logic.label.LabelsInitializer;
import com.boris.study.memory.logic.sructure.Request;
import com.boris.study.memory.logic.sructure.StatelessBotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Greeter extends StatelessBotScenario {

    @Override
    public void processStateless(Request request) {
        Client client = getClient();
        logger.info("Greeting - Starting for " + getClient());
        try {
            bot.execute(botUtils.markdownMessage(
                    uiUtils.getGreeting(client.getFirstName()),
                    botUtils.retrieveChat(request.update).getId()
            ));

            processStateless(LabelsInitializer.class, request);
            processStateless(HelpShower.class, request);
            processStateless(CommandsShower.class, request);
        } catch (TelegramApiException e) {
            logger.error("Exception while greeting in request " + request, e);
        }
        logger.info("Greeting - Finished for " + getClient());
    }

    public Greeter(ScenarioState scenarioState) {
        super(scenarioState);
    }

    private static Logger logger = LoggerFactory.getLogger(Greeter.class);
}
