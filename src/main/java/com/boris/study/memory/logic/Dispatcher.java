package com.boris.study.memory.logic;

import com.boris.study.memory.MemOryBot;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.logic.sructure.BotScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Dispatcher extends BotScenario {
    private MemOryBot bot;
    private BeanFactory beanFactory;

    @Override
    public Boolean process(Update update) {
        logger.info("Dispatching - Starting...");
        if (update.hasMessage()) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(bot.retrieveChat(update).getId());
            sendMessage.setText("My state is: " + state.getState());
            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                logger.trace("Exception while sending test message", e);
            }
        }
        logger.info("Dispatching - Finished");
        return true;
    }

    public Dispatcher(ScenarioState scenarioState) {
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

    private static Logger logger = LoggerFactory.getLogger(Dispatcher.class);
}
