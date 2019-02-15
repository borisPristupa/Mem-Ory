package com.boris.study.memory.logic.sructure;

import com.boris.study.memory.MemOryBot;
import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.ScenarioStateRepository;
import com.boris.study.memory.utils.BotUtils;
import com.boris.study.memory.utils.DataUtils;
import com.boris.study.memory.utils.UIUtils;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BotScenario implements Scenario<Request, Boolean> {
    @Autowired
    protected MemOryBot bot;
    @Autowired
    protected BotUtils botUtils;
    @Autowired
    protected DataUtils dataUtils;
    @Autowired
    protected UIUtils uiUtils;
    @Autowired
    private ScenarioStateRepository stateRepository;

    private final ScenarioState state;

    /**
     * @param request The update MemOryBot gets from user + metadata
     * @return Has the scenario finished yet?
     */
    @Override
    public abstract Boolean process(Request request, boolean forceRestart);

    /**
     * Process the request <b>with</b> forcing restart.
     *
     * @param request The update MemOryBot gets from user + metadata
     * @return Has the scenario finished yet?
     */
    public Boolean process(Request request) {
        return process(request, true);
    }

    public Boolean processOther(Class<? extends BotScenario> scenarioClass, Request request) {
        if (botUtils.obtainScenario(scenarioClass, getClient()).process(request))
            return true;
        setSubscenario(getName(scenarioClass));
        return false;
    }

    // FIXME: 13.02.19 I don't like that a superclass is dependent on its subclass
    public void processStateless(Class<? extends StatelessBotScenario> scenarioClass, Request request) {
        botUtils.obtainStatelessScenario(scenarioClass, getClient()).processStateless(request);
    }

    protected Boolean continueProcessing(Request request, boolean forceRestart) {

        if (forceRestart) {
            state.setSubscenario(null);
            state.setStage(null);
            return true;
        }

        if (null != state.getSubscenario()) {

            Class<? extends BotScenario> subscenarioClass =
                    BotScenario.getScenarioClass(state.getSubscenario());
            if (!botUtils.obtainScenario(subscenarioClass, state.getClient())
                    .process(request, false))
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
