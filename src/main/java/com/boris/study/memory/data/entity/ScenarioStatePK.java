package com.boris.study.memory.data.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class ScenarioStatePK implements Serializable {
    private Integer client_id;
    private String name;
}
