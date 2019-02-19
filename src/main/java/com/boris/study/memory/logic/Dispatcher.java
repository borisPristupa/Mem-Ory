package com.boris.study.memory.logic;

import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.logic.data.DataSaver;
import com.boris.study.memory.logic.helpers.HelpShower;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.logic.sructure.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dispatcher extends BotScenario {

    @Override
    public Boolean process(Request request, boolean forceRestart) {
        if (request.update.hasMessage() && request.update.getMessage().hasText()
                && "/help".equals(request.update.getMessage().getText())) {
            processStateless(HelpShower.class, request);
            return true;
        }

        if (!continueProcessing(request, forceRestart))
            return false;

        int stageDispatched = 0;

        if (null == getStage() || getStage() < stageDispatched) {
            logger.info("Dispatching - Starting for client " + getClient());

            if (request.update.hasMessage()) {
                if (request.update.getMessage().hasText() &&
                        botUtils.containsCommand(request.update.getMessage().getText())) {
                    try {
                        if (!processOther(CommandHandler.class, request)) {
                            setStage(stageDispatched);
                            return false;
                        }
                    } catch (Exception e) {
                        logger.error("Failed to handle commands in request " + request, e);
                    }
                } else {
                    try {
                        if (!processOther(DataSaver.class, request)) {
                            setStage(stageDispatched);
                            return false;
                        }
                    } catch (Exception e) {
                        logger.error("Failed to handle data saving in request " + request, e);
                    }
                }
            } else {
                logger.info("No message in request.update! Ignoring unexpected request.update for " + getClient() + ": " + request.update);
            }
        }

        setStage(null);
        logger.info("Dispatching - Finished");
        return true;
    }

    public Dispatcher(ScenarioState scenarioState) {
        super(scenarioState);
    }

    private static Logger logger = LoggerFactory.getLogger(Dispatcher.class);
}
