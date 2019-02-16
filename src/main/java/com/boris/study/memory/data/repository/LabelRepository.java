package com.boris.study.memory.data.repository;

import com.boris.study.memory.data.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LabelRepository extends JpaRepository<Label, Integer> {
    Optional<Label> findByNameAndClientId(String name, Long clientId);
}
