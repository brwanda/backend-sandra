package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.Committee;
import com.earacg.earaconnect.service.CommitteeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/committees")
@RequiredArgsConstructor
@Slf4j
public class CommitteeController {

    private final CommitteeService committeeService;

    @GetMapping
    public ResponseEntity<List<Committee>> getAllCommittees() {
        List<Committee> committees = committeeService.getAllCommittees();
        return ResponseEntity.ok(committees);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Committee> getCommitteeById(@PathVariable Long id) {
        Committee committee = committeeService.getCommitteeById(id);
        return ResponseEntity.ok(committee);
    }

    @PostMapping
    public ResponseEntity<Committee> createCommittee(@RequestBody Committee committee) {
        Committee createdCommittee = committeeService.createCommittee(committee);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCommittee);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Committee> updateCommittee(@PathVariable Long id, @RequestBody Committee committeeDetails) {
        Committee updatedCommittee = committeeService.updateCommittee(id, committeeDetails);
        return ResponseEntity.ok(updatedCommittee);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommittee(@PathVariable Long id) {
        log.info("🔍 CommitteeController: DELETE request received for committee ID: {}", id);
        
        try {
            committeeService.deleteCommittee(id);
            log.info("✅ CommitteeController: Successfully deleted committee with ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("❌ CommitteeController: Error deleting committee with ID {}: {}", id, e.getMessage());
            log.error("❌ CommitteeController: Exception type: {}", e.getClass().getSimpleName());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}