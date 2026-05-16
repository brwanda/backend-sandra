package com.earacg.earaconnect.repository;

import com.earacg.earaconnect.model.EntityHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EntityHistoryRepo extends JpaRepository<EntityHistory, Long> {
    List<EntityHistory> findByEntityType(String entityType);
    List<EntityHistory> findByEntityTypeAndEntityId(String entityType, Long entityId);
    List<EntityHistory> findByDeletedBy(Long userId);
    List<EntityHistory> findByEntityTypeOrderByDeletedAtDesc(String entityType);
}
