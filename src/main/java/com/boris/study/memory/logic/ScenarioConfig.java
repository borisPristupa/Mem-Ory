package com.boris.study.memory.logic;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ScenarioConfig {
    private String configName;
    private String description;
    private List<String> parameters;
}
