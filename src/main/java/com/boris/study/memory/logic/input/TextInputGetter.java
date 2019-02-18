package com.boris.study.memory.logic.input;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.logic.sructure.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextInputGetter extends BotScenario {

    public enum Result {
        TEXT
    }

    @Override
    public Boolean process(Request request, boolean forceRestart) {
        if (!continueProcessing(request, forceRestart))
            return false;

        Client client = getClient();
        int stageWaiting = 0;

        if (null == getStage() || getStage() < stageWaiting) {
            logger.info("TextInputGetter - Starting for " + client);
            setStage(stageWaiting);
            return false;
        }

        if (request.update.hasMessage() && request.update.getMessage().hasText()) {
            request.put(Result.TEXT.name(), request.update.getMessage().getText());
            setStage(null);
            return true;
        }

        try {
            bot.execute(botUtils.markdownMessage(
                    "Excuse me, I need you to send me text",
                    client.getId()));
        } catch (Exception e) {
            logger.error("Failed to inform user about bad input in request " + request, e);
        }

        setStage(null);
        logger.info("TextInputGetter - Finished for " + client);
        return true;
    }

    public TextInputGetter(ScenarioState state) {
        super(state);
    }

    private final Logger logger = LoggerFactory.getLogger(TextInputGetter.class);
}