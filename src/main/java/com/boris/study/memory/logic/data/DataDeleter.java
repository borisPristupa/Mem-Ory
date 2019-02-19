package com.boris.study.memory.logic.data;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.Data;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.logic.sructure.Request;
import com.boris.study.memory.logic.sructure.StatelessBotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DataDeleter extends StatelessBotScenario {
    public enum Key {
        DATA_URL
    }

    private DataRepository dataRepository;

    @Override
    public void processStateless(Request request) {
        Client client = getClient();
        logger.info("DataDeleter - Starting for " + client);

        try {
            String url = request.get(Key.DATA_URL.name());
            Data data = dataRepository.findByUrl(url).orElseThrow(
                    () -> new IllegalArgumentException("Wrong url passed to DataDeleter: " + url + ", " + request)
            );
            dataRepository.delete(data);

            bot.execute(botUtils.plainMessage(
                    "Deleted that data", botUtils.retrieveChat(request.update).getId()
            ));
        } catch (Exception e) {
            logger.error("Failed to process DataDeleter in update " + request.update, e);
        }

        logger.info("DataDeleter - Finished for " + client);
    }

    public DataDeleter(ScenarioState state) {
        super(state);
    }

    @Autowired
    public void setDataRepository(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    private final Logger logger = LoggerFactory.getLogger(DataDeleter.class);
}