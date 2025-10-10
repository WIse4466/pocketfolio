package com.example.pocketfolio.repository;

import com.example.pocketfolio.entity.Recurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecurrenceRepository extends JpaRepository<Recurrence, UUID> {
    List<Recurrence> findByActiveTrue();
}

