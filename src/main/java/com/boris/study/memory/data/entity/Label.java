package com.boris.study.memory.data.entity;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

@Entity
@lombok.Data
@Table(name = "label", schema = "public", catalog = "mem_ory")
public class Label {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = -1, nullable = false)
    private String name;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    public Label() {
    }

    public Label(String name, Client client) {
        this.name = name;
        this.client = client;
        this.clientId = client.getId();
    }

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false, insertable = false, updatable = false)
    private Client client;

    public void addSon(Label son) {
        getSons().add(son);
    }

    public Set<Label> getSons() {
        if (null == sons)
            sons = new HashSet<>();
        return sons;
    }

    public Set<Label> getAllSonsRecursively() {
        Set<Label> result = new HashSet<>();
        LinkedList<Label> checkerList = new LinkedList<>(result);
        checkerList.add(this);
        while (!checkerList.isEmpty()) {
            checkerList.removeFirst().getSons().forEach(label -> {
                result.add(label);
                checkerList.addLast(label);
            });
        }
        return result;
    }

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "label_hierarchy",
            joinColumns = {@JoinColumn(name = "parent")},
            inverseJoinColumns = {@JoinColumn(name = "son")})
    private Set<Label> sons;

    public Set<Label> getParents() {
        if (null == parents)
            parents = new HashSet<>();
        return parents;
    }

    public Set<Label> getAllParentsRecursively() {
        if (null == parents)
            parents = new HashSet<>();

        Set<Label> result = new HashSet<>();
        LinkedList<Label> checkerList = new LinkedList<>(result);
        checkerList.add(this);
        while (!checkerList.isEmpty()) {
            checkerList.removeFirst().getParents().forEach(label -> {
                result.add(label);
                checkerList.addLast(label);
            });
        }
        return result;
    }

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "label_hierarchy",
            joinColumns = {@JoinColumn(name = "son")},
            inverseJoinColumns = {@JoinColumn(name = "parent")})
    private Set<Label> parents;
}
