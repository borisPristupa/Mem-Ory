package com.boris.study.memory.logic.data;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.Data;
import com.boris.study.memory.data.entity.Label;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.data.repository.LabelRepository;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.logic.sructure.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

public class DataSaver extends BotScenario {
    public enum Key {
        DONT_SAVE
    }

    private DataRepository dataRepository;
    private LabelRepository labelRepository;

    @Override
    public Boolean process(Request request, boolean forceRestart) {
        if (!continueProcessing(request, forceRestart))
            return false;

        if (request.containsKey(Key.DONT_SAVE.name()))
            return true;

        Client client = getClient();
        logger.info("DataSaver - Starting for " + client);

        Data data = new Data();
        try {
            processStateless(DataForwarder.class, request);

            data.setUrl(dataUtils.generateUrl());
            data.setMagicId(Integer.valueOf(request.get(DataForwarder.RESULT_MAGIC_ID)));

            bot.execute(botUtils.plainMessage(
                    "Successfully saved. New data URL: " + data.getUrl(),
                    botUtils.retrieveChat(request.update).getId()
            ));

            Optional<Label> allData = labelRepository.findByNameAndClientId("all data", getClient().getId());
            Label allDataLabel = allData.orElseThrow(() -> new IllegalStateException("'all data' label is missing"));
            data.setLabels(new HashSet<>(Collections.singleton(allDataLabel)));
            dataRepository.save(data);

            request.put(Descriptioner.Key.DATA_URL.name(), data.getUrl());
            if (!processOther(Descriptioner.class, request))
                return false;

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

    @Autowired
    public void setLabelRepository(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    private final Logger logger = LoggerFactory.getLogger(DataSaver.class);
}