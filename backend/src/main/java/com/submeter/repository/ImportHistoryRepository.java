package com.submeter.repository;

import com.submeter.entity.ImportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImportHistoryRepository extends JpaRepository<ImportHistory, UUID> {
}
