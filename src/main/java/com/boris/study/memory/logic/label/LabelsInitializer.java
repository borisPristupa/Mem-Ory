package com.boris.study.memory.logic.label;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.Label;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.ClientRepository;
import com.boris.study.memory.data.repository.LabelRepository;
import com.boris.study.memory.logic.sructure.Request;
import com.boris.study.memory.logic.sructure.StatelessBotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashSet;

public class LabelsInitializer extends StatelessBotScenario {
    private LabelRepository labelRepository;
    private ClientRepository clientRepository;

    @Override
    public void processStateless(Request request) {
        Client client = getClient();
        logger.info("LabelsInitializer - Starting for " + client);

        try {
            Label allData = new Label("all data", client),
                    audio = new Label("audio", client),
                    photo = new Label("photo", client),
                    video = new Label("video", client),
                    media = new Label("media", client),
                    contact = new Label("contact", client);
            media.setSons(new HashSet<>(Arrays.asList(audio, photo, video)));

            labelRepository.save(allData);
            labelRepository.save(audio);
            labelRepository.save(photo);
            labelRepository.save(video);
            labelRepository.save(contact);
            labelRepository.saveAndFlush(media);

            client.setLabels(new HashSet<>(Arrays.asList(
                    allData, audio, photo, video, media, contact)));
            clientRepository.save(client);

        } catch (Exception e) {
            logger.error("Failed to process LabelsInitializer in update " + request.update, e);
        }

        logger.info("LabelsInitializer - Finished for " + client);
    }

    public LabelsInitializer(ScenarioState state) {
        super(state);
    }

    @Autowired
    public void setLabelRepository(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    @Autowired
    public void setClientRepository(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    private final Logger logger = LoggerFactory.getLogger(LabelsInitializer.class);
}