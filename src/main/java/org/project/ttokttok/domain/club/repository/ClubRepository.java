package org.project.ttokttok.domain.club.repository;

import org.project.ttokttok.domain.club.domain.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, String>, ClubCustomRepository{
    @Query("SELECT c from Club c where c.admin.username = :username")
    Optional<Club> findByAdminUsername(String username);
}
