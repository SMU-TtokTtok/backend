package org.project.ttokttok.domain.notice.repository;

import org.project.ttokttok.domain.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, String>, NoticeCustomRepository {
}
