package com.boris.study.memory.logic.data;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.Data;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.data.repository.LabelRepository;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.logic.sructure.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DataSaver extends BotScenario {
    public enum Result {
        NEW_DATA_URL
    }

    private DataRepository dataRepository;
    private LabelRepository labelRepository;

    @Override
    public Boolean process(Request request, boolean forceRestart) {
        if (!continueProcessing(request, forceRestart))
            return false;

        Client client = getClient();
        int stageDescriptionSaved = 0;


        if (null == getStage() || getStage() < stageDescriptionSaved) {

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

                request.put(LabelAssigner.Key.URL.name(), data.getUrl());
                request.put(LabelAssigner.Key.LABEL_NAME.name(), "all data");
                processStateless(LabelAssigner.class, request);

                setState(getState().put(Result.NEW_DATA_URL.name(), data.getUrl()));
                request.put(Descriptioner.Key.DATA_URL.name(), data.getUrl());
                if (!processOther(Descriptioner.class, request)) {
                    setStage(stageDescriptionSaved);
                    return false;
                }

            } catch (Exception e) {
                logger.error("Failed to save data in request " + request, e);
            }
        }

        request.put(Result.NEW_DATA_URL.name(), getState().get(Result.NEW_DATA_URL.name()).toString());
        setStage(null);
        logger.info("DataSaver - Finished for client " + client);
        return true;
    }

    public DataSaver(ScenarioState state) {
        super(state);
    }

    @Autowired
    public void setDataRepository(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Autowired
    public void setLabelRepository(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    private final Logger logger = LoggerFactory.getLogger(DataSaver.class);
}