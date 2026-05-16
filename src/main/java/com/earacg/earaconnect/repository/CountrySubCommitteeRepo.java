package com.earacg.earaconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.earacg.earaconnect.model.CountrySubCommittee;

@Repository
public interface CountrySubCommitteeRepo extends JpaRepository<CountrySubCommittee, Long> {
    CountrySubCommittee findByCountryIdAndSubCommitteeId(Long countryId, Long subCommitteeId);
    List<CountrySubCommittee> findByCountryId(Long countryId);
}
