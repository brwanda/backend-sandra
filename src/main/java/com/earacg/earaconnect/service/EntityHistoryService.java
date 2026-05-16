package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.EntityHistory;
import com.earacg.earaconnect.repository.EntityHistoryRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class EntityHistoryService {

    @Autowired
    private EntityHistoryRepo entityHistoryRepo;

    private final ObjectMapper objectMapper;

    public EntityHistoryService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * Record an entity snapshot before deletion.
     * @param entityType The type name (e.g., "Meeting", "Committee")
     * @param entityId The original entity ID
     * @param entity The entity object to serialize
     * @param deletedBy The user ID who performed the deletion (nullable)
     * @param reason Optional reason for deletion
     */
    public void recordDeletion(String entityType, Long entityId, Object entity, Long deletedBy, String reason) {
        try {
            String jsonData;
            try {
                jsonData = objectMapper.writeValueAsString(entity);
            } catch (Exception e) {
                // Fallback: store basic info if serialization fails
                jsonData = String.format("{\"id\": %d, \"type\": \"%s\", \"serializationError\": \"%s\"}",
                        entityId, entityType, e.getMessage().replace("\"", "'"));
                log.warn("Could not fully serialize {} with id {}: {}", entityType, entityId, e.getMessage());
            }

            EntityHistory history = EntityHistory.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityData(jsonData)
                    .deletedBy(deletedBy)
                    .deletedAt(LocalDateTime.now())
                    .deletionReason(reason)
                    .build();

            entityHistoryRepo.save(history);
            log.info("Recorded deletion history for {} with id {}", entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to record deletion history for {} with id {}: {}", entityType, entityId, e.getMessage());
            // Don't prevent the actual deletion from proceeding
        }
    }

    /**
     * Get all deletion history records for a specific entity type.
     */
    public List<EntityHistory> getHistoryByType(String entityType) {
        return entityHistoryRepo.findByEntityTypeOrderByDeletedAtDesc(entityType);
    }

    /**
     * Get deletion history for a specific entity.
     */
    public List<EntityHistory> getHistoryByTypeAndId(String entityType, Long entityId) {
        return entityHistoryRepo.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Get all deletion history.
     */
    public List<EntityHistory> getAllHistory() {
        return entityHistoryRepo.findAll();
    }
}
