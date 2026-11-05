package org.project.ttokttok.domain.favorite.repository;

import org.project.ttokttok.domain.favorite.domain.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FavoriteCustomRepository {
    /* *
     * 사용자의 즐겨찾기 목록을 페이징 및 정렬하여 조호
     * */
    // Page<Favorite> findFavoritesByUserEmail(String userEmail, Pageable pageable);
    List<Favorite> findFavorites(String userEmail, String cursor, int size, String sort);
}
