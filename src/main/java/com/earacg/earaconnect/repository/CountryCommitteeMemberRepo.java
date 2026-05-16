package com.earacg.earaconnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.earacg.earaconnect.model.CountryCommitteeMember;

@Repository
public interface CountryCommitteeMemberRepo extends JpaRepository<CountryCommitteeMember, Long> {
    List<CountryCommitteeMember> findByCountryId(Long countryId);
    
    List<CountryCommitteeMember> findByCommitteeId(Long committeeId);
    
    List<CountryCommitteeMember> findByCountryIdAndCommitteeId(Long countryId, Long committeeId);
    
    List<CountryCommitteeMember> findByIsChairTrue();
    
    List<CountryCommitteeMember> findByIsViceChairTrue();
    
    List<CountryCommitteeMember> findByIsCommitteeSecretaryTrue();

    @Query("SELECT c FROM CountryCommitteeMember c WHERE lower(c.committee.name) LIKE lower(concat('%', :keyword, '%'))")
    List<CountryCommitteeMember> findByCommitteeNameContaining(@Param("keyword") String keyword);

    @Query("SELECT c FROM CountryCommitteeMember c WHERE c.country.id = :countryId AND c.isDelegationSecretary = true")
    List<CountryCommitteeMember> findDelegationSecretariesByCountryId(@Param("countryId") Long countryId);
    
    Optional<CountryCommitteeMember> findByEmail(String email);

    List<CountryCommitteeMember> findByEmailIgnoreCase(String email);
    
    @Query("SELECT c FROM CountryCommitteeMember c WHERE c.name LIKE %:name%")
    List<CountryCommitteeMember> findByNameContaining(@Param("name") String name);
    
    // Find all members by committee ID with specific roles
    @Query("SELECT c FROM CountryCommitteeMember c WHERE c.committee.id = :committeeId AND c.isChair = true")
    List<CountryCommitteeMember> findChairsByCommitteeId(@Param("committeeId") Long committeeId);
    
    @Query("SELECT c FROM CountryCommitteeMember c WHERE c.committee.id = :committeeId AND c.isViceChair = true")
    List<CountryCommitteeMember> findViceChairsByCommitteeId(@Param("committeeId") Long committeeId);
    
    @Query("SELECT c FROM CountryCommitteeMember c WHERE c.committee.id = :committeeId AND c.isCommitteeSecretary = true")
    List<CountryCommitteeMember> findSecretariesByCommitteeId(@Param("committeeId") Long committeeId);
    
    @Query("SELECT c FROM CountryCommitteeMember c WHERE c.committee.id = :committeeId AND c.isCommitteeMember = true")
    List<CountryCommitteeMember> findRegularMembersByCommitteeId(@Param("committeeId") Long committeeId);
}
