package com.boris.study.memory.logic;

import com.boris.study.memory.BotUtils;
import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.logic.sructure.BotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Greeter extends BotScenario {

    @Override
    public Boolean process(Update update, boolean forceRestart) {
        if (!continueProcessing(update, forceRestart))
            return false;

        int stageFinished = 0;

        if (null == getStage() || getStage() < stageFinished) {
            Client client = getClient();
            logger.info("Greeting - Starting for " + getClient());
            try {
                bot.execute(defaultMessage(
                        uiData.getGreeting(client.getFirstName()),
                        BotUtils.retrieveChat(update).getId()
                ));

                if (!processOther(HelpShower.class, update)) {
                    setSubscenario(BotScenario.getName(HelpShower.class));
                    setStage(stageFinished);
                    return false;
                }
            } catch (TelegramApiException e) {
                logger.trace("Exception while greeting " + client, e);
            }
        }
        setStage(null);

        logger.info("Greeting - Finished for " + getClient());
        return true;
    }

    public Greeter(ScenarioState scenarioState) {
        super(scenarioState);
    }

    private static Logger logger = LoggerFactory.getLogger(Greeter.class);
}
