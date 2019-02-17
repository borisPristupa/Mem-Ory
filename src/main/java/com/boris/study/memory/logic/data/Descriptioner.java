package com.boris.study.memory.logic.data;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.Data;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.logic.sructure.Request;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public class Descriptioner extends BotScenario {

    public enum Key {
        DATA_URL
    }

    private DataRepository dataRepository;

    @Override
    public Boolean process(Request request, boolean forceRestart) {
        if (!continueProcessing(request, forceRestart))
            return false;

        Client client = getClient();

        int stageRequestedForDesc = 0;

        try {
            if (null == getStage() || getStage() < stageRequestedForDesc) {
                logger.info("Descriptioner - Starting for " + client);

                if (!request.containsKey(Key.DATA_URL.name()))
                    throw new IllegalArgumentException("No data url passed");

                bot.execute(botUtils.plainMessage(
                        "Send some short description for this data",
                        botUtils.retrieveChat(request.update).getId()
                ));

                setState(getState().put(Key.DATA_URL.name(), request.get(Key.DATA_URL.name())));
                setStage(stageRequestedForDesc);
                return false;
            }

            logger.info("Descriptioner - Continuing for client " + client);
            if (!botUtils.containsText(request.update))
                return true;

            String description = request.update.getMessage().getText();
            Optional<Data> dataOptional = dataRepository.findByUrl(getState().get(Key.DATA_URL.name()).toString());
            Data data = dataOptional.orElseThrow(() ->
                    new IllegalArgumentException("No data found by url " +
                            getState().get(Key.DATA_URL.name()).toString()));
            data.setDescription(description);
            dataRepository.save(data);

            bot.execute(botUtils.plainMessage(
                    "Description saved successfully",
                    botUtils.retrieveChat(request.update).getId()
            ));

        } catch (Exception e) {
            logger.error("Failed to process Descriptioner in update " + request.update, e);
        }

        setStage(null);
        setState(new JSONObject());
        logger.info("Descriptioner - Finished for " + client);
        return true;
    }

    public Descriptioner(ScenarioState state) {
        super(state);
    }

    @Autowired
    public void setDataRepository(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    private final Logger logger = LoggerFactory.getLogger(Descriptioner.class);
}