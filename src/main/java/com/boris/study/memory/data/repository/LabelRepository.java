package com.boris.study.memory.data.repository;

import com.boris.study.memory.data.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LabelRepository extends JpaRepository<Label, Integer> {
    @Query(value = "SELECT l.* FROM label_hierarchy lh " +
            "JOIN label l on l.id = lh.son AND ?1 = lh.parent " +
            "GROUP BY l.id", nativeQuery = true)
    List<Label> findAllByParentId(Integer parent);

    @Query(value = "SELECT l.* FROM label l " +
            "JOIN label_for_data lfd on l.id = lfd.label_id " +
            "WHERE lfd.data_url = ?1 " +
            "GROUP BY l.id", nativeQuery = true)
    List<Label> findAllByDataUrl(String dataUrl);

    Optional<Label> findByNameAndClientId(String name, Long clientId);
}
