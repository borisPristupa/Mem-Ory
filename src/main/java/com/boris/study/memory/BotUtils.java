package com.boris.study.memory;

import com.boris.study.memory.data.entity.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

public class BotUtils {
    static Client retrieveClient(Update update) {
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

    public static Chat retrieveChat(Update update) {
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

    private static Logger logger = LoggerFactory.getLogger(BotUtils.class);
}
