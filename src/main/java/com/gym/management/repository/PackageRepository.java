package com.gym.management.repository;

import com.gym.management.entity.Package;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PackageRepository extends JpaRepository<Package, UUID> {
    List<Package> findByActiveTrue();

    Page<Package> findByActiveTrue(Pageable pageable);
}
