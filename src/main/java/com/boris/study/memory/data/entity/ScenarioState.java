package com.boris.study.memory.data.entity;

import com.boris.study.memory.data.StateConverter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.json.JSONObject;

import javax.persistence.*;

/**
 * This entity represents a scenario and its current state for a certain client. Remembering states
 * is needed due to Scenario's concept: it may return a value even if it is not finished - such
 * happens when there are some processes the scenario needs to finish before its parent decides
 * what to do next. For example, when a scenario has an ongoing dialog with a client, it can only
 * send him messages, but the Bot is the only who can receive messages, so that it should somehow
 * remember whom to send that messages, and scenarios should remember the place they have stopped
 * at - that is what the state stands for.
 */
@Entity
@Data
@IdClass(ScenarioStatePK.class)
@Table(name = "scenario_state", schema = "public", catalog = "mem_ory")
public class ScenarioState {
    @Id
    @Column(name = "client_id")
    private Long client_id;

    @Id
    @Column(name = "name")
    private String name;

    @Column(name = "stage")
    private Integer stage;

    @Column(name = "subscenario", length = -1)
    private String subscenario;

    @Convert(converter = StateConverter.class)
    @Column(name = "state", nullable = false, length = -1)
    private JSONObject state = new JSONObject();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false, insertable = false, updatable = false)
    private Client client;
}
