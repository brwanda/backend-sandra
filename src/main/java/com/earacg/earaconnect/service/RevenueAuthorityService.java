package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.RevenueAuthority;
import com.earacg.earaconnect.repository.RevenueAuthorityRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RevenueAuthorityService {

    private final RevenueAuthorityRepo revenueAuthorityRepository;

    public RevenueAuthorityService(RevenueAuthorityRepo revenueAuthorityRepository) {
        this.revenueAuthorityRepository = revenueAuthorityRepository;
    }

    public List<RevenueAuthority> getAllRevenueAuthorities() {
        return revenueAuthorityRepository.findAll();
    }

    public RevenueAuthority getRevenueAuthorityById(Long id) {
        return revenueAuthorityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RevenueAuthority not found with id: " + id));
    }

    public RevenueAuthority createRevenueAuthority(RevenueAuthority revenueAuthority) {
        return revenueAuthorityRepository.save(revenueAuthority);
    }

    public RevenueAuthority updateRevenueAuthority(Long id, RevenueAuthority revenueAuthorityDetails) {
        RevenueAuthority revenueAuthority = getRevenueAuthorityById(id);
        revenueAuthority.setName(revenueAuthorityDetails.getName());
        revenueAuthority.setCountry(revenueAuthorityDetails.getCountry());
        return revenueAuthorityRepository.save(revenueAuthority);
    }

    public void deleteRevenueAuthority(Long id) {
        RevenueAuthority revenueAuthority = getRevenueAuthorityById(id);
        revenueAuthorityRepository.delete(revenueAuthority);
    }

    public List<RevenueAuthority> getRevenueAuthoritiesByCountryId(Long countryId) {
        return revenueAuthorityRepository.findByCountryId(countryId);
    }
}