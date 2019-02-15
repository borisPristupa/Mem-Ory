package com.boris.study.memory.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@lombok.Data
@Table(name = "data", schema = "public", catalog = "mem_ory")
public class Data {
    @Id
    @Column(name = "url", length = -1)
    private String url;

    @Column(name = "magic_id", unique = true, nullable = false)
    private Integer magicId;

}

