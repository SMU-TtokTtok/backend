package org.project.ttokttok.domain.notice.repository;

import org.project.ttokttok.domain.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, String>, NoticeCustomRepository {

    // 조회수 원자적 증가 (동시 상세 조회 시 read-modify-write 경쟁으로 인한 조회수 유실 방지)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notice n SET n.viewCount = n.viewCount + 1 WHERE n.id = :noticeId")
    int increaseViewCount(@Param("noticeId") String noticeId);
}
