package com.boris.study.memory;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.repository.ClientRepository;
import com.boris.study.memory.logic.Dispatcher;
import com.boris.study.memory.logic.Greeter;
import com.boris.study.memory.logic.sructure.BotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
public class MemOryBot extends TelegramLongPollingBot {
    @Value("${bot.token}")
    private String token;
    @Value("${bot.username}")
    private String username;

    private BeanFactory beanFactory;

    private ClientRepository clientRepository;

    @Override
    public void onUpdateReceived(Update update) {
        Client current = retrieveClient(update);
        logger.info("Received update! From " + current);
        if (!clientRepository.existsById(current.getId())) {
            clientRepository.save(current);
            BotScenario greeter = beanFactory.getBean(BotScenario.class, Greeter.class, current);
            logger.info("Obtained a greeter");
            greeter.process(update);
        }

        BotScenario dispatcher = beanFactory.getBean(BotScenario.class, Dispatcher.class, current);
        dispatcher.process(update);
    }

    public Client retrieveClient(Update update) {
        Client client = new Client();
        User user;
        if (update.hasMessage()) {
            user = update.getMessage().getFrom();

        } else if (update.hasCallbackQuery()) {
            user = update.getCallbackQuery().getFrom();

        } else if (update.hasChannelPost()) {
            user = update.getChannelPost().getFrom();

        } else if (update.hasChosenInlineQuery()) {
            user = update.getChosenInlineQuery().getFrom();

        } else if (update.hasEditedChannelPost()) {
            user = update.getEditedChannelPost().getFrom();

        } else if (update.hasEditedMessage()) {
            user = update.getEditedMessage().getFrom();

        } else if (update.hasInlineQuery()) {
            user = update.getInlineQuery().getFrom();

        } else if (update.hasPreCheckoutQuery()) {
            user = update.getPreCheckoutQuery().getFrom();

        } else if (update.hasShippingQuery()) {
            user = update.getShippingQuery().getFrom();
        } else {
            logger.trace("Unknown update type");
            throw new IllegalStateException();
        }

        client.setId(user.getId());
        client.setFirstName(user.getFirstName());
        client.setLastName(user.getLastName());
        client.setUsername(user.getUserName());
        return client;
    }

    public Chat retrieveChat(Update update) {
        Chat chat;
        if (update.hasMessage()) {
            chat = update.getMessage().getChat();

        } else if (update.hasCallbackQuery()) {
            chat = update.getCallbackQuery().getMessage().getChat();

        } else if (update.hasChannelPost()) {
            chat = update.getChannelPost().getChat();

        } else if (update.hasEditedChannelPost()) {
            chat = update.getEditedChannelPost().getChat();

        } else if (update.hasEditedMessage()) {
            chat = update.getEditedMessage().getChat();

        } else if (update.hasChosenInlineQuery() ||
                update.hasInlineQuery() ||
                update.hasPreCheckoutQuery() ||
                update.hasShippingQuery()) {
            logger.trace("Wrong update type");
            throw new IllegalStateException();

        } else {
            logger.trace("Unknown update type");
            throw new IllegalStateException();
        }

        return chat;
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

    private static Logger logger = LoggerFactory.getLogger(MemOryBot.class);
}
