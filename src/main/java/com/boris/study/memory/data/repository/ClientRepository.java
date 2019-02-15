package com.boris.study.memory.data.repository;

import com.boris.study.memory.data.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}
