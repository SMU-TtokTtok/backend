package org.project.ttokttok.domain.favorite.repository;

import org.project.ttokttok.domain.favorite.domain.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, String> {
}
