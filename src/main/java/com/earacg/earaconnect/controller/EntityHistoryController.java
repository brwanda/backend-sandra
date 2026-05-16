package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.EntityHistory;
import com.earacg.earaconnect.service.EntityHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entity-history")
public class EntityHistoryController {

    @Autowired
    private EntityHistoryService entityHistoryService;

    @GetMapping
    public ResponseEntity<List<EntityHistory>> getAllHistory() {
        return ResponseEntity.ok(entityHistoryService.getAllHistory());
    }

    @GetMapping("/type/{entityType}")
    public ResponseEntity<List<EntityHistory>> getHistoryByType(@PathVariable String entityType) {
        return ResponseEntity.ok(entityHistoryService.getHistoryByType(entityType));
    }

    @GetMapping("/type/{entityType}/{entityId}")
    public ResponseEntity<List<EntityHistory>> getHistoryByTypeAndId(
            @PathVariable String entityType, @PathVariable Long entityId) {
        return ResponseEntity.ok(entityHistoryService.getHistoryByTypeAndId(entityType, entityId));
    }
}
