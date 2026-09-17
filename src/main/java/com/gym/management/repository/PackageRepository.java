package com.gym.management.repository;

import com.gym.management.entity.Package;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PackageRepository extends JpaRepository<Package, UUID> {
}
