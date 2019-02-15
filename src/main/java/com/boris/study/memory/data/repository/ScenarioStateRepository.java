package com.boris.study.memory.data.repository;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.entity.ScenarioStatePK;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioStateRepository extends JpaRepository<ScenarioState, ScenarioStatePK> {
    List<ScenarioState> findAllByClient(Client client);
    List<ScenarioState> findAllByName(String name);
    Optional<ScenarioState> findByNameAndClientId(String name, Long clientId);
}
