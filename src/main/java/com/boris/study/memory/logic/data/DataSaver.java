package com.boris.study.memory.logic.data;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.Data;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.logic.sructure.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DataSaver extends BotScenario {
    private DataRepository dataRepository;

    @Override
    public Boolean process(Request request, boolean forceRestart) {
        Client client = getClient();
        logger.info("DataSaver - Starting for " + client);

        Data data = new Data();
        try {
            processStateless(DataForwarder.class, request);

            data.setUrl(dataUtils.generateUrl());
            data.setMagicId(Integer.valueOf(request.get(DataForwarder.RESULT_MAGIC_ID)));
            dataRepository.save(data);

            bot.execute(botUtils.plainMessage(
                    "Successfully saved. New data URL: " + data.getUrl(),
                    botUtils.retrieveChat(request.update).getId()
            ));

        } catch (Exception e) {
            logger.error("Failed to save data in request " + request, e);
        }

        logger.info(String.format(
                "DataSaver - Finished with dataUrl %s for %s",
                data.getUrl(), client));
        return true;
    }

    public DataSaver(ScenarioState state) {
        super(state);
    }

    @Autowired
    public void setDataRepository(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    private final Logger logger = LoggerFactory.getLogger(DataSaver.class);
}