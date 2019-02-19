package com.boris.study.memory.logic.data;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.Data;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.data.repository.LabelRepository;
import com.boris.study.memory.logic.sructure.Request;
import com.boris.study.memory.logic.sructure.StatelessBotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;

public class LabelAssigner extends StatelessBotScenario {
    public enum Key {
        URL, LABEL_NAME
    }

    private DataRepository dataRepository;
    private LabelRepository labelRepository;

    @Override
    public void processStateless(Request request) {
        Client client = getClient();
        logger.info("LabelAssigner - Starting for " + client);

        try {
            Data data = dataRepository.findByUrl(request.get(Key.URL.name())).orElseThrow(() ->
                    new IllegalStateException("Bad url passed to LabelAssigner: " + request.get(Key.URL.name()) +
                            " in request " + request));

            if (null == data.getLabels()) {
                data.setLabels(new HashSet<>());
            }

            data.getLabels().add(labelRepository.findByNameAndClientId(
                    request.get(Key.LABEL_NAME.name()), client.getId())
                    .orElseThrow(() -> new IllegalStateException(String.format(
                            "Couldn't assign unknown label '%s' to data %s in request %s",
                            request.get(Key.LABEL_NAME.name()), data, request))));

            dataRepository.save(data);

            bot.execute(botUtils.plainMessage("Assigned label '" +
                            request.get(Key.LABEL_NAME.name()) + "' to this data",
                    botUtils.retrieveChat(request.update).getId()
            ));
        } catch (Exception e) {
            logger.error("Did not succeed in assigning label to data", e);
        }
        logger.info("LabelAssigner - Finished for " + client);
    }

    public LabelAssigner(ScenarioState state) {
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

    private final Logger logger = LoggerFactory.getLogger(LabelAssigner.class);
}