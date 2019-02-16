package com.boris.study.memory;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.repository.ClientRepository;
import com.boris.study.memory.logic.Dispatcher;
import com.boris.study.memory.logic.helpers.Greeter;
import com.boris.study.memory.logic.sructure.Request;
import com.boris.study.memory.utils.BotUtils;
import com.boris.study.memory.utils.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class MemOryBot extends TelegramLongPollingBot {
    @Value("${bot.token}")
    private String token;
    @Value("${bot.username}")
    private String username;

    private UIUtils uiUtils;
    private BotUtils botUtils;

    private ClientRepository clientRepository;

    @Override
    public void onUpdateReceived(Update update) {
        Client client = botUtils.retrieveClient(update);

        if (!botUtils.retrieveChat(update).isUserChat()) {
            SendMessage errorMessage = new SendMessage()
                    .setChatId(botUtils.retrieveChat(update).getId())
                    .setText(uiUtils.getErrors().getWrongChatType());
            try {
                execute(errorMessage);
            } catch (TelegramApiException e) {
                logger.error("Exception while informing about wrong type of chat, " + client, e);
            }
            return;
        }

        logger.info("Received update! From " + client);
        if (!clientRepository.existsById(client.getId())) {
            clientRepository.save(client);
            botUtils.obtainStatelessScenario(Greeter.class, client).processStateless(new Request(update));
        } else
            botUtils.obtainScenario(Dispatcher.class, client).process(new Request(update), false);

    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Autowired
    public void setBeanFactory(BotUtils botUtils) {
        this.botUtils = botUtils;
    }

    @Autowired
    public void setClientRepository(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Autowired
    public void setUiUtils(UIUtils uiUtils) {
        this.uiUtils = uiUtils;
    }

    private static Logger logger = LoggerFactory.getLogger(MemOryBot.class);

}
