package com.boris.study.memory;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.repository.ClientRepository;
import com.boris.study.memory.logic.Dispatcher;
import com.boris.study.memory.logic.Greeter;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.ui.UIData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
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

    private UIData uiData;
    private BeanFactory beanFactory;

    private ClientRepository clientRepository;

    @Override
    public void onUpdateReceived(Update update) {
        Client client = BotUtils.retrieveClient(update);

        if (!BotUtils.retrieveChat(update).isUserChat()) {
            SendMessage errorMessage = new SendMessage()
                    .setChatId(BotUtils.retrieveChat(update).getId())
                    .setText(uiData.getErrors().getWrongChatType());
            try {
                execute(errorMessage);
            } catch (TelegramApiException e) {
                logger.trace("Exception while informing about wrong type of chat, " + client, e);
            }
            return;
        }

        BotScenario greeter;
        logger.info("Received update! From " + client);
        if (!clientRepository.existsById(client.getId())) {
            clientRepository.save(client);
            greeter = beanFactory.getBean(BotScenario.class, Greeter.class, client);
            if (!greeter.process(update, false)) {
                return;
            }
        } else
            greeter = beanFactory.getBean(BotScenario.class, Greeter.class, client);

        if (!greeter.hasFinished())
            if (!greeter.process(update, false))
                return;

        beanFactory.getBean(BotScenario.class, Dispatcher.class, client).process(update);
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
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Autowired
    public void setClientRepository(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Autowired
    public void setUiData(UIData uiData) {
        this.uiData = uiData;
    }

    private static Logger logger = LoggerFactory.getLogger(MemOryBot.class);
}
