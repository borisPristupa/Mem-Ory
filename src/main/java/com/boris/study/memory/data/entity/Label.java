package com.boris.study.memory.data.entity;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;

@Entity
@lombok.Data
@Table(name = "label", schema = "public", catalog = "mem_ory")
public class Label {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = -1, nullable = false)
    private String name;

    @Column(name = "client_id", nullable = false)
    private Integer clientId;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false, insertable = false, updatable = false)
    private Client client;
}