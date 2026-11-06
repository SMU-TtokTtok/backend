package org.project.ttokttok.domain.favorite.repository;

import org.project.ttokttok.domain.favorite.domain.Favorite;

import java.util.List;

public interface FavoriteCustomRepository {
    List<Favorite> findFavorites(String userEmail, String cursor, int size, String sort);
}
