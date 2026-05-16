package com.earacg.earaconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.earacg.earaconnect.model.Country;

@Repository
public interface CountryRepo extends JpaRepository<Country, Long>{
    Country findByName(String name);
    Country findByIsCode(String isCode);
    boolean existsByName(String name);
    boolean existsByEmail(String email);
    boolean existsByIsCode(String isCode);
}
