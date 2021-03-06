package com.boris.study.memory.logic.sructure;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.ClientRepository;
import com.boris.study.memory.data.repository.ScenarioStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

@Configuration
public class BotScenarioConfiguration {
    private final ScenarioStateRepository stateRepository;
    private final ClientRepository clientRepository;

    @Bean("stateless")
    @Scope(value = "prototype")
    public StatelessBotScenario getStatelessScenario(Class<? extends StatelessBotScenario> scenarioClass,
                                                     Client client) {
        return (StatelessBotScenario) getScenario(scenarioClass, client);
    }

    @Bean("stateful")
    @Scope(value = "prototype")
    public BotScenario getScenario(Class<? extends BotScenario> scenarioClass, Client client) {
        if (!clientRepository.existsById(client.getId())) {
            throw new IllegalArgumentException("There is no such client in database: " + client);
        }

        ScenarioState scenarioState;
        Optional<ScenarioState> scenarioStateOptional =
                stateRepository.findByNameAndClientId(BotScenario.getName(scenarioClass), client.getId());
        scenarioState = scenarioStateOptional.orElseGet(() -> {

            ScenarioState newState = new ScenarioState();
            newState.setClient_id(client.getId());
            newState.setClient(client);
            newState.setName(BotScenario.getName(scenarioClass));

            stateRepository.save(newState);
            return newState;
        });
        try {
            return scenarioClass.getConstructor(ScenarioState.class).newInstance(scenarioState);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            logger.error(
                    String.format(
                            "Failed instantiate %s. " +
                                    "Maybe there is no constructor %s(ScenarioState) ?",
                            scenarioClass.getName(),
                            scenarioClass.getName()),
                    e);
            throw new IllegalStateException(e);
        }
    }

    @Autowired
    public BotScenarioConfiguration(ScenarioStateRepository stateRepository, ClientRepository clientRepository) {
        this.stateRepository = stateRepository;
        this.clientRepository = clientRepository;
    }

    private static Logger logger = LoggerFactory.getLogger(BotScenarioConfiguration.class);
}
