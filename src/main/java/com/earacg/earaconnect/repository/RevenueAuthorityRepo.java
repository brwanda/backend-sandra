package com.earacg.earaconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.earacg.earaconnect.model.RevenueAuthority;

@Repository
public interface RevenueAuthorityRepo extends JpaRepository<RevenueAuthority, Long>{
    RevenueAuthority findByName(String name);
    List<RevenueAuthority> findByCountryId(Long countryId);
    @Modifying
    @Query("DELETE FROM RevenueAuthority ra WHERE ra.country.id = :countryId")
    void deleteByCountryId(@Param("countryId") Long countryId);
    
    long countByCountryId(Long countryId);
}
