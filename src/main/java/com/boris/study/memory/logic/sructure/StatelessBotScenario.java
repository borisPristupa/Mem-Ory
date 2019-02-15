package com.boris.study.memory.logic.sructure;

import com.boris.study.memory.data.entity.ScenarioState;

public abstract class StatelessBotScenario extends BotScenario {
    public abstract void processStateless(Request request);

    @Override
    public Boolean process(Request request, boolean forceRestart) {
        processStateless(request);
        return true;
    }

    public StatelessBotScenario(ScenarioState scenarioState) {
        super(scenarioState);
    }
}
