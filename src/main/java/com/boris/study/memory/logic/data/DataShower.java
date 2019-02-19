package com.boris.study.memory.logic.data;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.logic.sructure.Request;
import com.boris.study.memory.logic.sructure.StatelessBotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DataShower extends StatelessBotScenario {
    public static final String KEY_URL = "url";

    private DataRepository dataRepository;

    @Override
    public void processStateless(Request request) {
        Client client = getClient();
        logger.info("DataShower - Starting for " + client);

        try {
            if (!request.containsKey(KEY_URL)) {
                bot.execute(botUtils.markdownMessage(
                        "Well, we only support links for data, sorry",
                        botUtils.retrieveChat(request.update).getId()
                ));
                return;
            }
            String dataUrl = request.get(KEY_URL);
            if (!dataRepository.existsById(dataUrl)) {
                bot.execute(botUtils.markdownMessage(
                        uiUtils.getErrors().getNoDataByUrl(),
                        botUtils.retrieveChat(request.update).getId()
                ));
                return;
            } else {
                processStateless(DataForwarder.class, new Request(request.update) {{
                    put(DataForwarder.KEY_URL, dataUrl);
                    put(DataForwarder.KEY_TO_CLIENT, getClient().getId().toString());
                }});
            }

        } catch (Exception e) {
            logger.error("Failed to process DataShower in request " + request, e);
        }

        logger.info("DataShower - Finished for " + client);
    }

    public DataShower(ScenarioState state) {
        super(state);
    }

    @Autowired
    public void setDataRepository(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    private final Logger logger = LoggerFactory.getLogger(DataShower.class);
}