package com.boris.study.memory.logic.data;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.Data;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.ClientRepository;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.logic.sructure.Request;
import com.boris.study.memory.logic.sructure.StatelessBotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

public class DataForwarder extends StatelessBotScenario {
    public static final String KEY_TO_CLIENT = "to user", KEY_URL = "url";
    public static final String RESULT_MAGIC_ID = "magic id";

    private DataRepository dataRepository;
    private ClientRepository clientRepository;

    @Override
    public void processStateless(Request request) {
        Client client = getClient();
        logger.info("DataForwarder - Starting for " + client);

        try {
            if (request.containsKey(KEY_TO_CLIENT)) {

                Optional<Data> optionalData = dataRepository.findById(request.get(KEY_URL));
                if (!optionalData.isPresent()) {
                    sendSendingError(request);
                    throw new IllegalArgumentException("Wrong url + " + request.get(KEY_URL));
                }

                Optional<Client> optionalClient =
                        clientRepository.findById(Long.parseLong(request.get(KEY_TO_CLIENT)));

                if (!optionalClient.isPresent()) {
                    sendSendingError(request);
                    throw new IllegalArgumentException("Wrong client id + " + request.get(KEY_TO_CLIENT));
                }

                Data data = optionalData.get();
                Client destination = optionalClient.get();
                bot.execute(new ForwardMessage()
                        .setFromChatId(botUtils.getMagicChatId())
                        .setChatId(destination.getId())
                        .setMessageId(data.getMagicId()));
            } else {

                Message forwarded = bot.execute(new ForwardMessage()
                        .setFromChatId(botUtils.retrieveChat(request.update).getId())
                        .setChatId(botUtils.getMagicChatId())
                        .setMessageId(request.update.getMessage().getMessageId()));
                request.put(RESULT_MAGIC_ID, forwarded.getMessageId().toString());
            }
        } catch (Exception e) {
            sendSendingError(request);
            logger.error("Failed to process DataForwarder in request " + request, e);
        }

        logger.info("DataForwarder - Finished for " + client);
    }

    private void sendSendingError(Request request) {
        try {
            bot.execute(botUtils.markdownMessage(
                    "Unable to send that data, sorry", botUtils.retrieveChat(request.update).getId()
            ));
        } catch (TelegramApiException e) {
            logger.error("Exception while sending error message about sending data in request " + request, e);
        }
    }

    @Autowired
    public void setDataRepository(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Autowired
    public void setClientRepository(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public DataForwarder(ScenarioState state) {
        super(state);
    }

    private final Logger logger = LoggerFactory.getLogger(DataForwarder.class);
}