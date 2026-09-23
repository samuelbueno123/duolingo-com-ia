package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.Institution;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findByCode(String code);

    Optional<Institution> findByEmail(String email);

    Optional<Institution> findByPhone(String phone);

    boolean existsByCode(String code);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<Institution> findByNameContainingIgnoreCase(String name);

    List<Institution> findByCityContainingIgnoreCase(String city);

    List<Institution> findByStateContainingIgnoreCase(String state);

    List<Institution> findByCountryContainingIgnoreCase(String country);

    List<Institution> findByActive(Boolean active);
}