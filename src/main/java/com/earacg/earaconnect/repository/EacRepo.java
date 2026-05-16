package com.earacg.earaconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.earacg.earaconnect.model.Eac;

@Repository
public interface EacRepo extends JpaRepository<Eac, Long>{
    Eac findByName(String name);
}
