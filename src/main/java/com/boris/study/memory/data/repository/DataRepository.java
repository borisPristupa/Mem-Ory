package com.boris.study.memory.data.repository;

import com.boris.study.memory.data.entity.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface DataRepository extends JpaRepository<Data, String> {
    @Query(value = "SELECT d.* FROM data d " +
            "JOIN label_for_data lfd on url = data_url " +
            "JOIN label l on lfd.label_id = l.id AND l.client_id = ?2 " +
            "WHERE l.name IN (?1) " +
            "GROUP BY d.url", nativeQuery = true)
    List<Data> findAllByLabelNamesAndClientId(Set<String> labels, Integer clientId);

    @Query(value = "SELECT d.* FROM data d " +
            "JOIN label_for_data lfd on d.url = lfd.data_url " +
            "JOIN label l on lfd.label_id = l.id " +
            "JOIN client c on l.client_id = c.id " +
            "WHERE client_id = ?1 " +
            "GROUP BY d.url", nativeQuery = true)
    List<Data> findAllByClientId(Integer clientId);
}
