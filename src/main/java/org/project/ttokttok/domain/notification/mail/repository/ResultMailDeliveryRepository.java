package org.project.ttokttok.domain.notification.mail.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.project.ttokttok.domain.notification.mail.domain.ResultMailDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResultMailDeliveryRepository extends JpaRepository<ResultMailDelivery, String> {

    // 임대가 만료된 SENDING 행도 가져와 워커가 죽어 멈춘 작업을 되살린다. 다른 워커가 잠근 행은 건너뛴다. 반드시 트랜잭션 안에서 불러야 잠금이 유지된다.
    @Query(value = """
            SELECT * FROM result_mail_deliveries
            WHERE (status = 'PENDING' AND next_attempt_at <= :now)
               OR (status = 'SENDING' AND lease_until <= :now)
            ORDER BY next_attempt_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<ResultMailDelivery> findDueForUpdateSkipLocked(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
