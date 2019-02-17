package com.boris.study.memory.utils;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.logic.sructure.StatelessBotScenario;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
public class BotUtils {

    @Getter
    @Value("${app.magic-chat}")
    private Long magicChatId;
    private BeanFactory beanFactory;

    public boolean containsText(Update update) {
        return update.hasMessage() && update.getMessage().hasText();
    }

    public boolean containsCommand(String text) {
        return text.matches("^(\\s*)?/\\w+(\\s*)$");
    }

    public SendMessage markdownMessage(String text, long chatId) {
        return new SendMessage()
                .setChatId(chatId)
                .setParseMode(ParseMode.MARKDOWN)
                .setText(text);
    }

    public SendMessage plainMessage(String text, long chatId) {
        return new SendMessage()
                .setChatId(chatId)
                .setText(text);
    }

    public EditMessageText plainEdit(Integer messageId, Long chatId, String text) {
        return new EditMessageText()
                .setMessageId(messageId)
                .setChatId(chatId)
                .setText(text);
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
            logger.error("Unknown update type");
            throw new IllegalStateException();
        }

        client.setId(Long.valueOf(user.getId()));
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
            logger.error("Wrong update type");
            throw new IllegalStateException();

        } else {
            logger.error("Unknown update type");
            throw new IllegalStateException();
        }

        return chat;
    }

    public StatelessBotScenario obtainStatelessScenario(Class<? extends StatelessBotScenario> scenarioClass,
                                                        Client client) {
        return (StatelessBotScenario) beanFactory.getBean("stateless", scenarioClass, client);
    }

    public BotScenario obtainScenario(Class<? extends BotScenario> scenarioClass, Client client) {
        return (BotScenario) beanFactory.getBean("stateful", scenarioClass, client);
    }

    @Autowired
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    private static Logger logger = LoggerFactory.getLogger(BotUtils.class);
}
