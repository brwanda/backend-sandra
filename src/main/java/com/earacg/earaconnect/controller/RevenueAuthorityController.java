package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.RevenueAuthority;
import com.earacg.earaconnect.service.RevenueAuthorityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/revenue-authorities")
public class RevenueAuthorityController {

    private final RevenueAuthorityService revenueAuthorityService;

    public RevenueAuthorityController(RevenueAuthorityService revenueAuthorityService) {
        this.revenueAuthorityService = revenueAuthorityService;
    }

    @GetMapping
    public List<RevenueAuthority> getAllRevenueAuthorities() {
        return revenueAuthorityService.getAllRevenueAuthorities();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RevenueAuthority> getRevenueAuthorityById(@PathVariable Long id) {
        return ResponseEntity.ok(revenueAuthorityService.getRevenueAuthorityById(id));
    }

    @PostMapping
    public RevenueAuthority createRevenueAuthority(@RequestBody RevenueAuthority revenueAuthority) {
        return revenueAuthorityService.createRevenueAuthority(revenueAuthority);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RevenueAuthority> updateRevenueAuthority(
            @PathVariable Long id, @RequestBody RevenueAuthority revenueAuthorityDetails) {
        return ResponseEntity.ok(revenueAuthorityService.updateRevenueAuthority(id, revenueAuthorityDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRevenueAuthority(@PathVariable Long id) {
        revenueAuthorityService.deleteRevenueAuthority(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/country/{countryId}")
    public List<RevenueAuthority> getRevenueAuthoritiesByCountryId(@PathVariable Long countryId) {
        return revenueAuthorityService.getRevenueAuthoritiesByCountryId(countryId);
    }
}