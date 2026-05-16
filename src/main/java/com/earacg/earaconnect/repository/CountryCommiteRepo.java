package com.earacg.earaconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.earacg.earaconnect.model.CountryCommitte;

@Repository
public interface CountryCommiteRepo extends JpaRepository<CountryCommitte, Long> {
    CountryCommitte findByCountryIdAndCommitteeId(Long countryId, Long committeeId);
}
