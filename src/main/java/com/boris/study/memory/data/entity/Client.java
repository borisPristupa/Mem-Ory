package com.boris.study.memory.data.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.Set;

@Entity
@Data
public class Client {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "first_name", nullable = false, length = -1)
    private String firstName;

    @Column(name = "last_name", length = -1)
    private String lastName;

    @Column(name = "username", length = -1)
    private String username;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "client", fetch = FetchType.EAGER)
    private Set<ScenarioState> scenarioStatesById;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "client", fetch = FetchType.EAGER)
    private Set<Label> labels;
}
