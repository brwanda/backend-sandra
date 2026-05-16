package com.earacg.earaconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.earacg.earaconnect.model.CSubCommitteeMembers;

@Repository
public interface CSubCommitteeMembersRepo extends JpaRepository<CSubCommitteeMembers, Long> {
    // Find by country
    List<CSubCommitteeMembers> findByCountryId(Long countryId);
    
    // Find by sub-committee
    List<CSubCommitteeMembers> findBySubCommitteeId(Long subCommitteeId);
    
    // Find chairs
    List<CSubCommitteeMembers> findByIsChairTrue();
    
    // Find vice chairs
    List<CSubCommitteeMembers> findByIsViceChairTrue();
    
    // Find delegation secretaries
    List<CSubCommitteeMembers> findByIsDelegationSecretaryTrue();

    List<CSubCommitteeMembers> findByCountryIdAndIsDelegationSecretaryTrue(Long countryId);
    
    // Find committee secretaries
    List<CSubCommitteeMembers> findByIsCommitteeSecretaryTrue();
    
    // Find committee members
    List<CSubCommitteeMembers> findByIsCommitteeMemberTrue();
    
    // Search by name (case insensitive)
    List<CSubCommitteeMembers> findByNameContainingIgnoreCase(String name);
    
    // Find by email
    List<CSubCommitteeMembers> findByEmail(String email);

    // Find by email (case-insensitive) for matching User to member record
    List<CSubCommitteeMembers> findByEmailIgnoreCase(String email);

    /**
     * Find by email with normalized comparison: trims spaces and optional surrounding
     * double quotes from DB column, case-insensitive. Use to match User email to
     * member record regardless of storage quirks. Table name matches DB (csub_committee_members).
     */
    @Query(value = "SELECT * FROM csub_committee_members WHERE LOWER(TRIM(BOTH '\"' FROM email)) = LOWER(TRIM(:email))", nativeQuery = true)
    List<CSubCommitteeMembers> findByEmailNormalized(@Param("email") String email);
    
    // Check if email exists
    boolean existsByEmail(String email);
    
    // Find by phone
    List<CSubCommitteeMembers> findByPhone(String phone);
    
    // Custom query to find members with specific roles in a country
    @Query("SELECT c FROM CSubCommitteeMembers c WHERE c.country.id = :countryId AND " +
           "(c.isChair = true OR c.isViceChair = true OR c.isDelegationSecretary = true)")
    List<CSubCommitteeMembers> findLeadersByCountry(@Param("countryId") Long countryId);
    
    // Custom query to find members with specific roles in a sub-committee
    @Query("SELECT c FROM CSubCommitteeMembers c WHERE c.subCommittee.id = :subCommitteeId AND " +
           "(c.isChair = true OR c.isViceChair = true OR c.isCommitteeSecretary = true)")
    List<CSubCommitteeMembers> findLeadersBySubCommittee(@Param("subCommitteeId") Long subCommitteeId);
    
    // Find all members by sub-committee ID with specific roles
    @Query("SELECT c FROM CSubCommitteeMembers c WHERE c.subCommittee.id = :subCommitteeId AND c.isChair = true")
    List<CSubCommitteeMembers> findChairsBySubCommitteeId(@Param("subCommitteeId") Long subCommitteeId);
    
    @Query("SELECT c FROM CSubCommitteeMembers c WHERE c.subCommittee.id = :subCommitteeId AND c.isViceChair = true")
    List<CSubCommitteeMembers> findViceChairsBySubCommitteeId(@Param("subCommitteeId") Long subCommitteeId);
    
    @Query("SELECT c FROM CSubCommitteeMembers c WHERE c.subCommittee.id = :subCommitteeId AND c.isCommitteeSecretary = true")
    List<CSubCommitteeMembers> findSecretariesBySubCommitteeId(@Param("subCommitteeId") Long subCommitteeId);
    
    @Query("SELECT c FROM CSubCommitteeMembers c WHERE c.subCommittee.id = :subCommitteeId AND c.isCommitteeMember = true")
    List<CSubCommitteeMembers> findRegularMembersBySubCommitteeId(@Param("subCommitteeId") Long subCommitteeId);
    
    @Query("SELECT c FROM CSubCommitteeMembers c WHERE c.subCommittee.id = :subCommitteeId AND c.isDelegationSecretary = true")
    List<CSubCommitteeMembers> findDelegationSecretariesBySubCommitteeId(@Param("subCommitteeId") Long subCommitteeId);

    // --- Role uniqueness: one Chair / Vice Chair / Committee Secretary per (country, subcommittee) ---
    boolean existsByCountryIdAndSubCommitteeIdAndIsChairTrue(Long countryId, Long subCommitteeId);
    boolean existsByCountryIdAndSubCommitteeIdAndIsViceChairTrue(Long countryId, Long subCommitteeId);
    boolean existsByCountryIdAndSubCommitteeIdAndIsCommitteeSecretaryTrue(Long countryId, Long subCommitteeId);
    boolean existsByCountryIdAndSubCommitteeIdAndIsChairTrueAndIdNot(Long countryId, Long subCommitteeId, Long excludeMemberId);
    boolean existsByCountryIdAndSubCommitteeIdAndIsViceChairTrueAndIdNot(Long countryId, Long subCommitteeId, Long excludeMemberId);
    boolean existsByCountryIdAndSubCommitteeIdAndIsCommitteeSecretaryTrueAndIdNot(Long countryId, Long subCommitteeId, Long excludeMemberId);

    // --- Delegation Secretary unique per country ---
    boolean existsByCountryIdAndIsDelegationSecretaryTrue(Long countryId);
    boolean existsByCountryIdAndIsDelegationSecretaryTrueAndIdNot(Long countryId, Long excludeMemberId);
}
