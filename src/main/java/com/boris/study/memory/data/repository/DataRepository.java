package com.boris.study.memory.data.repository;

import com.boris.study.memory.data.entity.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface DataRepository extends JpaRepository<Data, String> {
    @Query(value = "SELECT d.* FROM data d " +
            "JOIN label_for_data lfd on url = data_url " +
            "JOIN label l on lfd.label_id = l.id AND l.client_id = ?2 " +
            "WHERE l.name IN (?1) " +
            "GROUP BY d.url", nativeQuery = true)
    Set<Data> findAllByLabelNamesAndClientId(Set<String> labels, Long clientId);

    Optional<Data> findByUrl(String url);
}
