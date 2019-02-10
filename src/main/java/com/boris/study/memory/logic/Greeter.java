package com.boris.study.memory.logic;

import com.boris.study.memory.MemOryBot;
import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.logic.sructure.BotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Greeter extends BotScenario {
    private MemOryBot bot;
    private BeanFactory beanFactory;

    @Override
    public Boolean process(Update update) {
        Client client = bot.retrieveClient(update);
        logger.info("Gonna greet " + client);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(bot.retrieveChat(update).getId());
        sendMessage.setText(String.format("Hello, %s!\n" +
                        "Seems like you're for the first time here." +
                        " No help for you right now, just be lucky",
                client.getFirstName()));
        try {
            logger.info("Executing greeting with " + bot + " ...");
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.trace("Exception while sending text message", e);
        }

        //TODO greet and show help...

        logger.info("Greeting - Finished");
        return true;
    }

    public Greeter(ScenarioState scenarioState) {
        super(scenarioState);
    }

    @Autowired
    public void setBot(MemOryBot bot) {
        this.bot = bot;
    }

    @Autowired
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    private static Logger logger = LoggerFactory.getLogger(Greeter.class);
}
