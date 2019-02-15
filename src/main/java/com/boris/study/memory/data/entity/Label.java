package com.boris.study.memory.data.entity;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.Set;

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
    private Long clientId;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false, insertable = false, updatable = false)
    private Client client;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "label_for_data",
            joinColumns = {@JoinColumn(name = "label_id")},
            inverseJoinColumns = {@JoinColumn(name = "data_url")})
    private Set<Data> data;
}
