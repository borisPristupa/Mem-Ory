package com.boris.study.memory.data.entity;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.Set;

@Entity
@lombok.Data
@Table(name = "data", schema = "public", catalog = "mem_ory")
public class Data {
    @Id
    @Column(name = "url", length = -1)
    private String url;

    @Column(name = "magic_id", unique = true, nullable = false)
    private Integer magicId;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "label_for_data",
            joinColumns = {@JoinColumn(name = "data_url")},
            inverseJoinColumns = {@JoinColumn(name = "label_id")})
    private Set<Label> labels;
}

