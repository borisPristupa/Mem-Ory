package com.boris.study.memory.data.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class ScenarioStatePK implements Serializable {
    private Long client_id;
    private String name;
}
