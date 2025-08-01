package org.project.ttokttok.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class DummyDataLoader implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 임시로 더미 데이터 로딩 비활성화
        //log.info("더미 데이터 로딩 비활성화됨");
        //return;

        // 개발 또는 운영(테스트용) 환경에서 더미 데이터 로드
        if (shouldLoadDummyData()) {
            log.info("더미 데이터 로딩 시작 (Profile: {})", String.join(", ", environment.getActiveProfiles()));
            loadDummyData();
            log.info("더미 데이터 로딩 완료!");
        }
    }

    private boolean shouldLoadDummyData() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("local".equals(profile) || "dev".equals(profile) || "prod".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    private String readSqlFile(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }

    private void loadDummyData() {
        try {
            // 데이터 존재 확인 (중복 방지)
            Long adminCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admins", Long.class);
            if (adminCount != null && adminCount > 0) {
                log.info("더미 데이터가 이미 존재합니다. 로딩을 건너뜁니다.");
                return;
            }

            // 1. Admin 데이터
            loadAdminData();

            // 2. User 데이터
            loadUserData();

            // 3. Club 데이터
            loadClubData();

            // 4. ClubMember 데이터
            loadClubMemberData();

            // 5. Favorite 데이터
            loadFavoriteData();

            // 6. ClubBoard 데이터
            loadClubBoardData();

            // 7. ApplyForm 데이터
            loadApplyFormData();

            // 8. 테스트용 ApplyForm 데이터 (확실히 들어가도록)
            loadTestApplyFormData();

            // 9. 추가 엣지 케이스 ApplyForm 데이터
            loadEdgeCaseApplyFormData();

            // 10. 지원자 데이터 로딩
            loadApplicantData();

            // 11. DocumentPhase 데이터 로딩
            loadDocumentPhaseData();

            // 12. InterviewPhase 데이터 로딩
            loadInterviewPhaseData();
        } catch (Exception e) {
            log.error("더미 데이터 로딩 중 오류 발생", e);
        }
    }

    private void loadAdminData() throws IOException {
        log.info("Admin 데이터 로딩중...");
        String sql = readSqlFile("testdata/1_admin.sql");
        jdbcTemplate.execute(sql);
    }

    private void loadUserData() throws IOException {
        log.info("User 데이터 로딩...");
        String encodedPassword = passwordEncoder.encode("TestPass123!");
        String sql = readSqlFile("testdata/2_user.sql");
        jdbcTemplate.execute(String.format(sql, encodedPassword, encodedPassword));
    }

    private void loadClubData() throws IOException {
        log.info("Club 데이터 로딩...");
        String sql = readSqlFile("testdata/3_club.sql");
        jdbcTemplate.execute(sql);
    }

    private void loadClubMemberData() throws IOException {
        log.info("ClubMember 데이터 로딩...");
        String sql = readSqlFile("testdata/4_club_member.sql");
        jdbcTemplate.execute(sql);
    }

    private void loadFavoriteData() throws IOException {
        log.info("Favorite 데이터 로딩...");
        String sql = readSqlFile("testdata/5_favorite.sql");
        jdbcTemplate.execute(sql);
    }

    private void loadClubBoardData() throws IOException {
        log.info("ClubBoard 데이터 로딩...");
        String sql = readSqlFile("testdata/6_club_board.sql");
        jdbcTemplate.execute(sql);
    }

    private void loadApplyFormData() throws IOException {
        log.info("ApplyForm 데이터 로딩...");
        String sql = readSqlFile("testdata/7_apply_form.sql");
        jdbcTemplate.execute(sql);

        // ApplyForm grades 데이터 추가
        loadApplyFormGrades();
    }

    private void loadApplyFormGrades() throws IOException {
        log.info("ApplyForm Grades 데이터 로딩 (다양한 학년 조합)...");
        String sql = readSqlFile("testdata/8_apply_form_grades.sql");
        jdbcTemplate.execute(sql);
    }

    private void loadTestApplyFormData() throws IOException {
        log.info("테스트용 ApplyForm 데이터 로딩...");

        // 먼저 기존 데이터 삭제
        jdbcTemplate.execute(readSqlFile("testdata/9_test_apply_form_delete.sql"));

        // club-001용 ApplyForm 생성
        String applyFormId = "test-applyform-001";
        String sql1 = readSqlFile("testdata/10_test_apply_form_insert.sql");
        jdbcTemplate.update(sql1, applyFormId);

        // grades 데이터 추가 (1학년만 - 신입생 전용)
        String sql2 = readSqlFile("testdata/11_test_apply_form_grades_insert.sql");
        jdbcTemplate.update(sql2, applyFormId);

        log.info("테스트용 ApplyForm 데이터 로딩 완료: {}", applyFormId);
    }

    private void loadEdgeCaseApplyFormData() throws IOException {
        log.info("엣지 케이스 ApplyForm 데이터 로딩...");

        // 마감된 모집 (INACTIVE 상태) - recruiting: false가 되어야 함
        String inactiveFormId = "inactive-applyform-001";
        String sql1 = readSqlFile("testdata/12_edge_case_apply_form_inactive.sql");
        jdbcTemplate.update(sql1, inactiveFormId);

        // 2학년만 모집하는 특별한 케이스
        String specialFormId = "special-applyform-001";
        String sql2 = readSqlFile("testdata/12_edge_case_apply_form_special.sql");
        jdbcTemplate.update(sql2, specialFormId);

        // grades 데이터 추가
        String sql3 = readSqlFile("testdata/13_edge_case_apply_form_grades.sql");
        jdbcTemplate.update(sql3, specialFormId);

        log.info("엣지 케이스 ApplyForm 데이터 로딩 완료");
    }

    private void loadApplicantData() throws IOException {
        log.info("Applicant 데이터 로딩...");
        String sql = readSqlFile("testdata/14_applicant.sql");
        jdbcTemplate.execute(sql);

        log.info("Applicant 데이터 로딩 완료");
    }

    private void loadDocumentPhaseData() throws IOException {
        log.info("InterviewPhase 데이터 로딩...");
        String sql = readSqlFile("testdata/15_document_phase.sql");
        jdbcTemplate.execute(sql);

        log.info("DocumentPhase 데이터 로딩 완료");
    }

    private void loadInterviewPhaseData() throws IOException {
        log.info("InterviewPhase 데이터 로딩...");
        String sql = readSqlFile("testdata/16_interview_phase.sql");
        jdbcTemplate.execute(sql);

        log.info("InterviewPhase 데이터 로딩 완료");
    }
}
