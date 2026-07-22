package org.project.ttokttok.domain.superadmin.repository;

import org.project.ttokttok.domain.superadmin.domain.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuperAdminRepository extends JpaRepository<SuperAdmin, String> {
    Optional<SuperAdmin> findByUsername(String username);
}
