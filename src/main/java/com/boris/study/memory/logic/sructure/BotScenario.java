package com.boris.study.memory.logic.sructure;

import com.boris.study.memory.data.entity.ScenarioState;
import org.telegram.telegrambots.meta.api.objects.Update;

public abstract class BotScenario implements Scenario<Update, Boolean> {
    protected final ScenarioState state;

    /**
     * @param update The update MemOryBot gets from user
     * @return Has the scenario finished yet?
     */
    @Override
    public abstract Boolean process(Update update);


    static <T extends BotScenario> String getName(Class<T> t) {
        return t.getName();
    }

    public BotScenario(ScenarioState scenarioState) {
        this.state = scenarioState;
    }

}
