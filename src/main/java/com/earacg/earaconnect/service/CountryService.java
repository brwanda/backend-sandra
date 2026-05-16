package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.Country;
import com.earacg.earaconnect.model.Eac;
import com.earacg.earaconnect.repository.CountryRepo;
import com.earacg.earaconnect.repository.EacRepo;
import com.earacg.earaconnect.repository.RevenueAuthorityRepo;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepo countryRepository;
    private final EacRepo eacRepository;
    private final RevenueAuthorityRepo revenueAuthorityRepository;

    @Transactional(readOnly = true)
    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Country getCountryById(Long id) {
        return countryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Country not found with id: " + id));
    }

    @Transactional
    public Country createCountry(Country country) {
        // Check if name, email, or iso code already exists
        if (countryRepository.existsByName(country.getName())) {
            throw new IllegalArgumentException("Country with name " + country.getName() + " already exists");
        }
        if (countryRepository.existsByEmail(country.getEmail())) {
            throw new IllegalArgumentException("Email " + country.getEmail() + " is already in use");
        }
        if (countryRepository.existsByIsCode(country.getIsCode())) {
            throw new IllegalArgumentException("ISO code " + country.getIsCode() + " is already in use");
        }
        
        // Verify EAC exists
        Eac eac = eacRepository.findById(country.getEac().getId())
                .orElseThrow(() -> new EntityNotFoundException("EAC not found with id: " + country.getEac().getId()));
        country.setEac(eac);
        
        return countryRepository.save(country);
    }

    @Transactional
    public Country updateCountry(Long id, Country countryDetails) {
        Country country = getCountryById(id);
        
        // Check if new name is unique (if changed)
        if (!country.getName().equals(countryDetails.getName()) && 
            countryRepository.existsByName(countryDetails.getName())) {
            throw new IllegalArgumentException("Country with name " + countryDetails.getName() + " already exists");
        }
        
        // Check if new email is unique (if changed)
        if (!country.getEmail().equals(countryDetails.getEmail()) && 
            countryRepository.existsByEmail(countryDetails.getEmail())) {
            throw new IllegalArgumentException("Email " + countryDetails.getEmail() + " is already in use");
        }
        
        // Check if new ISO code is unique (if changed)
        if (!country.getIsCode().equals(countryDetails.getIsCode()) && 
            countryRepository.existsByIsCode(countryDetails.getIsCode())) {
            throw new IllegalArgumentException("ISO code " + countryDetails.getIsCode() + " is already in use");
        }
        
        // Update EAC if changed
        if (!country.getEac().getId().equals(countryDetails.getEac().getId())) {
            Eac eac = eacRepository.findById(countryDetails.getEac().getId())
                    .orElseThrow(() -> new EntityNotFoundException("EAC not found with id: " + countryDetails.getEac().getId()));
            country.setEac(eac);
        }
        
        country.setName(countryDetails.getName());
        country.setEmail(countryDetails.getEmail());
        country.setIsCode(countryDetails.getIsCode());
        
        return countryRepository.save(country);
    }

    @Transactional
    public void deleteCountry(Long id) {
        // Check if country exists
        Country country = getCountryById(id);
        
        // First delete all revenue authorities for this country
        revenueAuthorityRepository.deleteByCountryId(id);
        
        // Then delete the country
        countryRepository.delete(country);
    }

    public long getRevenueAuthorityCountByCountryId(Long countryId) {
        return revenueAuthorityRepository.countByCountryId(countryId);
    }
}