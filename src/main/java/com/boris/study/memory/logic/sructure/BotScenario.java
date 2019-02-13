package com.boris.study.memory.logic.sructure;

import com.boris.study.memory.MemOryBot;
import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.ScenarioStateRepository;
import com.boris.study.memory.ui.UIData;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

public abstract class BotScenario implements Scenario<Update, Boolean> {
    @Autowired
    protected MemOryBot bot;
    @Autowired
    protected BeanFactory beanFactory;
    @Autowired
    protected UIData uiData;
    @Autowired
    private ScenarioStateRepository stateRepository;

    private final ScenarioState state;

    /**
     * @param update The update MemOryBot gets from user
     * @return Has the scenario finished yet?
     */
    @Override
    public abstract Boolean process(Update update, boolean forceRestart);

    /**
     * Process the update <b>with</b> forcing restart.
     *
     * @param update The update MemOryBot gets from user
     * @return Has the scenario finished yet?
     */
    public Boolean process(Update update) {
        return process(update, true);
    }

    public Boolean processOther(Class<? extends BotScenario> scenarioClass, Update update) {
        return beanFactory.getBean(BotScenario.class, scenarioClass, state.getClient()).process(update);
    }

    protected Boolean continueProcessing(Update update, boolean forceRestart) {

        if (forceRestart) {
            state.setSubscenario(null);
            state.setStage(null);
            return true;
        }

        if (null != state.getSubscenario()) {

            Class<? extends BotScenario> subscenarioClass =
                    BotScenario.getScenarioClass(state.getSubscenario());
            if (!beanFactory.getBean(BotScenario.class, subscenarioClass, state.getClient())
                    .process(update, false))
                return false;
            state.setSubscenario(null);
        }
        return true;
    }

    protected void setSubscenario(String subscenario) {
        state.setSubscenario(subscenario);
        stateRepository.save(state);
    }

    protected void setStage(Integer stage) {
        state.setStage(stage);
        stateRepository.save(state);
    }

    protected String getSubscenario() {
        return state.getSubscenario();
    }

    protected Integer getStage() {
        return state.getStage();
    }

    protected Client getClient() {
        return state.getClient();
    }

    public boolean hasFinished() {
        return null == getStage();
    }

    protected SendMessage defaultMessage(String text, long chatId) {
        return new SendMessage()
                .setChatId(chatId)
                .setParseMode(ParseMode.MARKDOWN)
                .setText(text);
    }

    /**
     * @param t   Class<? extends BotScenario> instance
     * @param <T> Any Scenario, extending BotScenario
     * @return The name of the Scenario, used in ScenarioState to store each Scenario's state in DB
     */
    public static <T extends BotScenario> String getName(Class<T> t) {
        return t.getName();
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends BotScenario> getScenarioClass(String name) throws IllegalArgumentException {
        try {
            Class scenarioClass = Class.forName(name);
            if (BotScenario.class.isAssignableFrom(scenarioClass))
                return scenarioClass;
            throw new IllegalArgumentException(String.format("Class %s does not extend BotScenario", name));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public BotScenario(ScenarioState scenarioState) {
        this.state = scenarioState;
    }

}
