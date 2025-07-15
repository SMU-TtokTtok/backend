package org.project.ttokttok.domain.memo.repository;

import org.project.ttokttok.domain.memo.domain.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoRepository extends JpaRepository<Memo, String> {
}

