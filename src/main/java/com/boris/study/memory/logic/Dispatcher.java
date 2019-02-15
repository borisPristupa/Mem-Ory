package com.boris.study.memory.logic;

import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.logic.data.DataSaver;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.logic.sructure.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dispatcher extends BotScenario {

    @Override
    public Boolean process(Request request, boolean forceRestart) {
        if (!continueProcessing(request, forceRestart))
            return false;

        logger.info("Dispatching - Starting for client " + getClient());

        if (request.update.hasMessage() && request.update.getMessage().hasText()) {

            String text = request.update.getMessage().getText();
            if (containsCommand(text)) {
                try {
                    if (!processOther(CommandHandler.class, request))
                        return false;
                } catch (Exception e) {
                    logger.error("Failed to handle commands in request " + request, e);
                }
            } else {
                try {
                    if (!processOther(DataSaver.class, request))
                        return false;
                } catch (Exception e) {
                    logger.error("Failed to handle data saving in request " + request, e);
                }
            }

        } else {
            logger.info("No message in request.update! Ignoring unexpected request.update for " + getClient() + ": " + request.update);
        }

        setStage(null);
        logger.info("Dispatching - Finished");
        return true;
    }

    private boolean containsCommand(String text) {
        return text.matches("(.*\\s)?/\\w+(\\s|$).*");
    }

    public Dispatcher(ScenarioState scenarioState) {
        super(scenarioState);
    }

    private static Logger logger = LoggerFactory.getLogger(Dispatcher.class);
}
