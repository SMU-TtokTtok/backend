package org.project.ttokttok.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DummyDataLoader implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 개발 환경에서만 더미 데이터 로드
        if (isLocalOrDevProfile()) {
            log.info("더미 데이터 로딩 시작...");
            loadDummyData();
            log.info("더미 데이터 로딩 완료!");
        }
    }

    private boolean isLocalOrDevProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("local".equals(profile) || "dev".equals(profile)) {
                return true;
            }
        }
        return false;
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
        } catch (Exception e) {
            log.error("더미 데이터 로딩 중 오류 발생", e);
        }
    }

    private void loadAdminData() {
        log.info("Admin 데이터 로딩중...");
        String sql = """
                -- 1. Admin 데이터 30개 (동아리 관리자)
                INSERT INTO admins (id, username, password, created_at, updated_at) VALUES
                ('admin-001', 'admin_artclub', '$2a$10$dummyHashForPassword001', NOW(), NOW()),
                ('admin-002', 'admin_soccer', '$2a$10$dummyHashForPassword002', NOW(), NOW()),
                ('admin-003', 'admin_programming', '$2a$10$dummyHashForPassword003', NOW(), NOW()),
                ('admin-004', 'admin_volunteer', '$2a$10$dummyHashForPassword004', NOW(), NOW()),
                ('admin-005', 'admin_drama', '$2a$10$dummyHashForPassword005', NOW(), NOW()),
                ('admin-006', 'admin_basketball', '$2a$10$dummyHashForPassword006', NOW(), NOW()),
                ('admin-007', 'admin_english', '$2a$10$dummyHashForPassword007', NOW(), NOW()),
                ('admin-008', 'admin_photo', '$2a$10$dummyHashForPassword008', NOW(), NOW()),
                ('admin-009', 'admin_band', '$2a$10$dummyHashForPassword009', NOW(), NOW()),
                ('admin-010', 'admin_tennis', '$2a$10$dummyHashForPassword010', NOW(), NOW()),
                ('admin-011', 'admin_reading', '$2a$10$dummyHashForPassword011', NOW(), NOW()),
                ('admin-012', 'admin_dance', '$2a$10$dummyHashForPassword012', NOW(), NOW()),
                ('admin-013', 'admin_debate', '$2a$10$dummyHashForPassword013', NOW(), NOW()),
                ('admin-014', 'admin_environment', '$2a$10$dummyHashForPassword014', NOW(), NOW()),
                ('admin-015', 'admin_movie', '$2a$10$dummyHashForPassword015', NOW(), NOW()),
                ('admin-016', 'admin_guitar', '$2a$10$dummyHashForPassword016', NOW(), NOW()),
                ('admin-017', 'admin_badminton', '$2a$10$dummyHashForPassword017', NOW(), NOW()),
                ('admin-018', 'admin_cooking', '$2a$10$dummyHashForPassword018', NOW(), NOW()),
                ('admin-019', 'admin_volunteer2', '$2a$10$dummyHashForPassword019', NOW(), NOW()),
                ('admin-020', 'admin_chess', '$2a$10$dummyHashForPassword020', NOW(), NOW()),
                ('admin-021', 'admin_writing', '$2a$10$dummyHashForPassword021', NOW(), NOW()),
                ('admin-022', 'admin_running', '$2a$10$dummyHashForPassword022', NOW(), NOW()),
                ('admin-023', 'admin_science', '$2a$10$dummyHashForPassword023', NOW(), NOW()),
                ('admin-024', 'admin_travel', '$2a$10$dummyHashForPassword024', NOW(), NOW()),
                ('admin-025', 'admin_cafe', '$2a$10$dummyHashForPassword025', NOW(), NOW()),
                ('admin-026', 'admin_study', '$2a$10$dummyHashForPassword026', NOW(), NOW()),
                ('admin-027', 'admin_game', '$2a$10$dummyHashForPassword027', NOW(), NOW()),
                ('admin-028', 'admin_hiking', '$2a$10$dummyHashForPassword028', NOW(), NOW()),
                ('admin-029', 'admin_christian', '$2a$10$dummyHashForPassword029', NOW(), NOW()),
                ('admin-030', 'admin_etc', '$2a$10$dummyHashForPassword030', NOW(), NOW());             
                """;
        jdbcTemplate.execute(sql);
    }

    private void loadUserData() {
        log.info("User 데이터 로딩...");
        String encodedPassword = passwordEncoder.encode("TestPass123!");
        String sql = """
                -- 2. 사용자 데이터 50개 (동아리 멤버용)
                INSERT INTO users (id, email, password, name, is_email_verified, terms_agreed, created_at, updated_at) VALUES
                ('user-001', 'student001@sangmyung.kr', '$2a$10$dummyUserPassword001', '김철수', true, true, NOW(), NOW()),
                ('user-002', 'student002@sangmyung.kr', '$2a$10$dummyUserPassword002', '이영희', true, true, NOW(), NOW()),
                ('user-003', 'student003@sangmyung.kr', '$2a$10$dummyUserPassword003', '박민수', true, true, NOW(), NOW()),
                ('user-004', 'student004@sangmyung.kr', '$2a$10$dummyUserPassword004', '최지은', true, true, NOW(), NOW()),
                ('user-005', 'student005@sangmyung.kr', '$2a$10$dummyUserPassword005', '정한솔', true, true, NOW(), NOW()),
                ('user-006', 'student006@sangmyung.kr', '$2a$10$dummyUserPassword006', '강다은', true, true, NOW(), NOW()),
                ('user-007', 'student007@sangmyung.kr', '$2a$10$dummyUserPassword007', '윤서진', true, true, NOW(), NOW()),
                ('user-008', 'student008@sangmyung.kr', '$2a$10$dummyUserPassword008', '장민호', true, true, NOW(), NOW()),
                ('user-009', 'student009@sangmyung.kr', '$2a$10$dummyUserPassword009', '신예린', true, true, NOW(), NOW()),
                ('user-010', 'student010@sangmyung.kr', '$2a$10$dummyUserPassword010', '오성민', true, true, NOW(), NOW()),
                ('user-011', 'student011@sangmyung.kr', '$2a$10$dummyUserPassword011', '한지우', true, true, NOW(), NOW()),
                ('user-012', 'student012@sangmyung.kr', '$2a$10$dummyUserPassword012', '임도현', true, true, NOW(), NOW()),
                ('user-013', 'student013@sangmyung.kr', '$2a$10$dummyUserPassword013', '송하늘', true, true, NOW(), NOW()),
                ('user-014', 'student014@sangmyung.kr', '$2a$10$dummyUserPassword014', '배준영', true, true, NOW(), NOW()),
                ('user-015', 'student015@sangmyung.kr', '$2a$10$dummyUserPassword015', '노수빈', true, true, NOW(), NOW()),
                ('user-016', 'student016@sangmyung.kr', '$2a$10$dummyUserPassword016', '권태현', true, true, NOW(), NOW()),
                ('user-017', 'student017@sangmyung.kr', '$2a$10$dummyUserPassword017', '서채원', true, true, NOW(), NOW()),
                ('user-018', 'student018@sangmyung.kr', '$2a$10$dummyUserPassword018', '고민석', true, true, NOW(), NOW()),
                ('user-019', 'student019@sangmyung.kr', '$2a$10$dummyUserPassword019', '남가영', true, true, NOW(), NOW()),
                ('user-020', 'student020@sangmyung.kr', '$2a$10$dummyUserPassword020', '유준서', true, true, NOW(), NOW()),
                ('user-021', 'student021@sangmyung.kr', '$2a$10$dummyUserPassword021', '조은지', true, true, NOW(), NOW()),
                ('user-022', 'student022@sangmyung.kr', '$2a$10$dummyUserPassword022', '홍승우', true, true, NOW(), NOW()),
                ('user-023', 'student023@sangmyung.kr', '$2a$10$dummyUserPassword023', '문시연', true, true, NOW(), NOW()),
                ('user-024', 'student024@sangmyung.kr', '$2a$10$dummyUserPassword024', '안재혁', true, true, NOW(), NOW()),
                ('user-025', 'student025@sangmyung.kr', '$2a$10$dummyUserPassword025', '양소희', true, true, NOW(), NOW()),
                ('user-026', 'student026@sangmyung.kr', '$2a$10$dummyUserPassword026', '전우진', true, true, NOW(), NOW()),
                ('user-027', 'student027@sangmyung.kr', '$2a$10$dummyUserPassword027', '손나래', true, true, NOW(), NOW()),
                ('user-028', 'student028@sangmyung.kr', '$2a$10$dummyUserPassword028', '구자현', true, true, NOW(), NOW()),
                ('user-029', 'student029@sangmyung.kr', '$2a$10$dummyUserPassword029', '심예슬', true, true, NOW(), NOW()),
                ('user-030', 'student030@sangmyung.kr', '$2a$10$dummyUserPassword030', '복지훈', true, true, NOW(), NOW()),
                ('user-031', 'student031@sangmyung.kr', '$2a$10$dummyUserPassword031', '탁시은', true, true, NOW(), NOW()),
                ('user-032', 'student032@sangmyung.kr', '$2a$10$dummyUserPassword032', '금준혁', true, true, NOW(), NOW()),
                ('user-033', 'student033@sangmyung.kr', '$2a$10$dummyUserPassword033', '사다인', true, true, NOW(), NOW()),
                ('user-034', 'student034@sangmyung.kr', '$2a$10$dummyUserPassword034', '마세현', true, true, NOW(), NOW()),
                ('user-035', 'student035@sangmyung.kr', '$2a$10$dummyUserPassword035', '피아름', true, true, NOW(), NOW()),
                ('user-036', 'student036@sangmyung.kr', '$2a$10$dummyUserPassword036', '어민재', true, true, NOW(), NOW()),
                ('user-037', 'student037@sangmyung.kr', '$2a$10$dummyUserPassword037', '저수정', true, true, NOW(), NOW()),
                ('user-038', 'student038@sangmyung.kr', '$2a$10$dummyUserPassword038', '차동건', true, true, NOW(), NOW()),
                ('user-039', 'student039@sangmyung.kr', '$2a$10$dummyUserPassword039', '려하림', true, true, NOW(), NOW()),
                ('user-040', 'student040@sangmyung.kr', '$2a$10$dummyUserPassword040', '영민수', true, true, NOW(), NOW()),
                ('user-041', 'student041@sangmyung.kr', '$2a$10$dummyUserPassword041', '후예진', true, true, NOW(), NOW()),
                ('user-042', 'student042@sangmyung.kr', '$2a$10$dummyUserPassword042', '개태윤', true, true, NOW(), NOW()),
                ('user-043', 'student043@sangmyung.kr', '$2a$10$dummyUserPassword043', '빈서현', true, true, NOW(), NOW()),
                ('user-044', 'student044@sangmyung.kr', '$2a$10$dummyUserPassword044', '감우석', true, true, NOW(), NOW()),
                ('user-045', 'student045@sangmyung.kr', '$2a$10$dummyUserPassword045', '집소영', true, true, NOW(), NOW()),
                ('user-046', 'student046@sangmyung.kr', '$2a$10$dummyUserPassword046', '칠준호', true, true, NOW(), NOW()),
                ('user-047', 'student047@sangmyung.kr', '$2a$10$dummyUserPassword047', '합예나', true, true, NOW(), NOW()),
                ('user-048', 'student048@sangmyung.kr', '$2a$10$dummyUserPassword048', '목현수', true, true, NOW(), NOW()),
                ('user-049', 'student049@sangmyung.kr', '$2a$10$dummyUserPassword049', '욕지민', true, true, NOW(), NOW()),
                ('user-050', 'student050@sangmyung.kr', '$2a$10$dummyUserPassword050', '값태영', true, true, NOW(), NOW()),
                
                -- 테스트용 사용자 추가 (비밀번호: TestPass123!)
                ('test-user-001', 'test@sangmyung.kr', '%s', '테스트 사용자', true, true, NOW(), NOW()),
                ('test-user-002', 'admin@sangmyung.kr', '%s', '관리자 테스트', true, true, NOW(), NOW());
                """.formatted(encodedPassword, encodedPassword);
        jdbcTemplate.execute(sql);
    }

    private void loadClubData() {
        log.info("Club 데이터 로딩...");
        String sql = """
            -- 2. 동아리 데이터 30개 (다양한 카테고리, 타입, 모집상태, 모집대상)
            INSERT INTO clubs (id, name, profile_img, summary, club_type, club_category, custom_category, content, recruiting, admin_id, target_grades, created_at, updated_at) VALUES
            ('club-001', '그림사랑', 'https://example.com/art1.jpg', '미술을 사랑하는 사람들의 모임', 'CENTRAL', 'CULTURE', '회화', '함께 그림을 그리고 전시회도 개최하는 동아리입니다.', true, 'admin-001', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-002', '상명축구부', 'https://example.com/soccer.jpg', '축구를 통한 건강한 대학생활', 'CENTRAL', 'SPORTS', '축구', '매주 정기적인 훈련과 대회 참가를 통해 실력을 키웁니다.', true, 'admin-002', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-003', '코딩마스터', 'https://example.com/code.jpg', '프로그래밍 실력 향상을 위한 스터디', 'DEPARTMENT', 'ACADEMIC', '프로그래밍', '다양한 프로그래밍 언어를 배우고 프로젝트를 진행합니다.', true, 'admin-003', '["SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-004', '나눔봉사단', 'https://example.com/volunteer.jpg', '사회에 보탬이 되는 봉사활동', 'UNION', 'VOLUNTEER', '사회봉사', '지역사회를 위한 다양한 봉사활동을 함께 합니다.', true, 'admin-004', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-005', '연극반', 'https://example.com/drama.jpg', '무대 위에서 펼치는 우리들의 이야기', 'CENTRAL', 'CULTURE', '연극', '연기, 연출, 무대제작 등 연극의 모든 것을 배웁니다.', false, 'admin-005', '["SECOND_GRADE", "THIRD_GRADE"]', NOW(), NOW()),
            ('club-006', '농구동아리', 'https://example.com/basketball.jpg', '슛! 골! 함께하는 농구의 즐거움', 'CENTRAL', 'SPORTS', '농구', '농구 실력 향상과 팀워크를 기르는 동아리입니다.', true, 'admin-006', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-007', '영어회화', 'https://example.com/english.jpg', 'English Conversation Club', 'DEPARTMENT', 'ACADEMIC', '외국어', '원어민과 함께하는 영어회화 스터디입니다.', true, 'admin-007', '["FIRST_GRADE", "SECOND_GRADE"]', NOW(), NOW()),
            ('club-008', '포토클럽', 'https://example.com/photo.jpg', '셔터 속에 담는 아름다운 순간들', 'UNION', 'CULTURE', '사진', '사진 촬영 기법부터 보정까지 모든 것을 배웁니다.', true, 'admin-008', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-009', '밴드동아리', 'https://example.com/band.jpg', '음악으로 하나되는 우리', 'CENTRAL', 'CULTURE', '음악', '다양한 장르의 음악을 연주하는 밴드 동아리입니다.', false, 'admin-009', '["THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-010', '테니스부', 'https://example.com/tennis.jpg', '라켓과 공으로 만나는 우아한 스포츠', 'CENTRAL', 'SPORTS', '테니스', '테니스 기초부터 실전까지 체계적인 훈련을 합니다.', true, 'admin-010', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-011', '독서모임', 'https://example.com/book.jpg', '책 속에서 찾는 지혜와 감동', 'DEPARTMENT', 'CULTURE', '독서', '한 달에 한 권씩 책을 읽고 토론하는 모임입니다.', true, 'admin-011', '["FIRST_GRADE", "SECOND_GRADE"]', NOW(), NOW()),
            ('club-012', '댄스크루', 'https://example.com/dance.jpg', '리듬 속에서 찾는 자유로움', 'UNION', 'CULTURE', '댄스', '다양한 장르의 댄스를 배우고 공연도 합니다.', true, 'admin-012', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-013', '토론동아리', 'https://example.com/debate.jpg', '논리와 사고력을 기르는 토론', 'DEPARTMENT', 'ACADEMIC', '토론', '시사 이슈부터 철학적 주제까지 다양한 토론을 합니다.', false, 'admin-013', '["THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-014', '환경지키미', 'https://example.com/env.jpg', '지구를 위한 작은 실천', 'UNION', 'VOLUNTEER', '환경보호', '환경보호 캠페인과 실천 활동을 함께 합니다.', true, 'admin-014', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-015', '영화감상', 'https://example.com/movie.jpg', '스크린 속 이야기와 함께하는 시간', 'DEPARTMENT', 'CULTURE', '영화', '다양한 장르의 영화를 보고 감상을 나누는 모임입니다.', true, 'admin-015', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE"]', NOW(), NOW()),
            ('club-016', '기타동아리', 'https://example.com/guitar.jpg', '기타 선율로 전하는 마음', 'CENTRAL', 'CULTURE', '기타', '어쿠스틱 기타부터 일렉 기타까지 배웁니다.', true, 'admin-016', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-017', '배드민턴', 'https://example.com/badminton.jpg', '셔틀콕과 함께하는 즐거운 운동', 'UNION', 'SPORTS', '배드민턴', '배드민턴을 통한 건강관리와 친목도모를 합니다.', false, 'admin-017', '["SECOND_GRADE", "THIRD_GRADE"]', NOW(), NOW()),
            ('club-018', '요리연구회', 'https://example.com/cooking.jpg', '맛있는 요리로 행복한 시간', 'DEPARTMENT', 'CULTURE', '요리', '다양한 요리를 배우고 레시피를 공유합니다.', true, 'admin-018', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-019', '사랑나눔', 'https://example.com/love.jpg', '사랑을 실천하는 봉사활동', 'CENTRAL', 'VOLUNTEER', '복지봉사', '복지시설과 연계한 봉사활동을 정기적으로 합니다.', true, 'admin-019', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-020', '체스동아리', 'https://example.com/chess.jpg', '논리적 사고를 키우는 체스', 'DEPARTMENT', 'ACADEMIC', '체스', '체스 실력 향상과 대회 참가를 목표로 합니다.', false, 'admin-020', '["THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-021', '창작문예', 'https://example.com/writing.jpg', '글로 표현하는 나만의 세계', 'UNION', 'CULTURE', '문예창작', '소설, 시, 수필 등 다양한 장르의 글쓰기를 합니다.', true, 'admin-021', '["SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-022', '러닝크루', 'https://example.com/running.jpg', '건강한 달리기 문화 만들기', 'CENTRAL', 'SPORTS', '달리기', '함께 달리며 건강도 챙기고 친목도 다집니다.', true, 'admin-022', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-023', '과학탐구', 'https://example.com/science.jpg', '과학의 신비로운 세계 탐험', 'DEPARTMENT', 'ACADEMIC', '과학', '실험과 탐구를 통해 과학의 재미를 느껴봅시다.', true, 'admin-023', '["SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-024', '여행동아리', 'https://example.com/travel.jpg', '함께 떠나는 특별한 여행', 'UNION', 'ETC', '여행', '국내외 여행을 기획하고 추억을 만듭니다.', false, 'admin-024', '["THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-025', '카페동아리', 'https://example.com/cafe.jpg', '커피와 함께하는 여유로운 시간', 'DEPARTMENT', 'ETC', '카페', '커피에 대해 배우고 카페 문화를 즐깁니다.', true, 'admin-025', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-026', '스터디모임', 'https://example.com/study.jpg', '함께 공부하며 성장하기', 'UNION', 'ACADEMIC', '스터디', '전공 공부부터 자격증까지 함께 공부합니다.', true, 'admin-026', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-027', '게임동아리', 'https://example.com/game.jpg', '게임을 통한 즐거운 소통', 'CENTRAL', 'ETC', '게임', '다양한 게임을 즐기며 친목을 도모합니다.', true, 'admin-027', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-028', '등산동아리', 'https://example.com/hiking.jpg', '자연과 함께하는 건강한 등산', 'UNION', 'SPORTS', '등산', '정기적인 등산을 통해 자연을 느끼고 체력을 기릅니다.', false, 'admin-028', '["SECOND_GRADE", "THIRD_GRADE"]', NOW(), NOW()),
            ('club-029', '기독교동아리', 'https://example.com/christian.jpg', '믿음 안에서 함께하는 공동체', 'CENTRAL', 'RELIGION', '기독교', '신앙 생활과 봉사활동을 함께 하는 동아리입니다.', true, 'admin-029', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW()),
            ('club-030', '자유동아리', 'https://example.com/free.jpg', '자유로운 활동과 새로운 도전', 'DEPARTMENT', 'ETC', '자유활동', '정해진 틀 없이 자유롭게 활동하는 동아리입니다.', true, 'admin-030', '["FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"]', NOW(), NOW());
            """;
        jdbcTemplate.execute(sql);
    }

    private void loadClubMemberData() {
        log.info("ClubMember 데이터 로딩...");
        String sql = """
                -- 4. ClubMember 데이터 생성 (각 동아리별로 다양한 멤버 수)
                -- UUID 생성을 위한 함수 사용
                
                -- club-001 (그림사랑): 15명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-001', 'user-001', NOW(), NOW()),
                (gen_random_uuid(), 'club-001', 'user-002', NOW(), NOW()),
                (gen_random_uuid(), 'club-001', 'user-003', NOW(), NOW()),
                (gen_random_uuid(), 'club-001', 'user-004', NOW(), NOW()),
                (gen_random_uuid(), 'club-001', 'user-005', NOW(), NOW()),
                (gen_random_uuid(), 'club-001', 'user-006', NOW(), NOW()),
                (gen_random_uuid(), 'club-001', 'user-007', NOW(), NOW()),
                (gen_random_uuid(), 'club-001', 'user-008', NOW(), NOW()),
                (gen_random_uuid(), 'club-001', 'user-009', NOW(), NOW()),
                (gen_random_uuid(), 'club-001', 'user-010', NOW(), NOW()),
                (gen_random_uuid(), 'club-001', 'user-011', NOW(), NOW()),
                (gen_random_uuid(), 'club-001', 'user-012', NOW(), NOW()),
                (gen_random_uuid(), 'club-001', 'user-013', NOW(), NOW()),
                (gen_random_uuid(), 'club-001', 'user-014', NOW(), NOW()),
                (gen_random_uuid(), 'club-001', 'user-015', NOW(), NOW());
                
                -- club-002 (상명축구부): 25명 (가장 인기)
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-002', 'user-001', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-002', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-003', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-004', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-005', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-006', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-007', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-008', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-009', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-010', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-011', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-012', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-013', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-014', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-015', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-016', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-017', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-018', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-019', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-020', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-021', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-022', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-023', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-024', NOW(), NOW()),
                (gen_random_uuid(), 'club-002', 'user-025', NOW(), NOW());
                
                -- club-003 (코딩마스터): 18명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-003', 'user-026', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-027', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-028', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-029', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-030', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-031', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-032', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-033', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-034', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-035', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-036', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-037', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-038', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-039', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-040', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-041', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-042', NOW(), NOW()),
                (gen_random_uuid(), 'club-003', 'user-043', NOW(), NOW());
                
                -- club-004 (나눔봉사단): 12명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-004', 'user-044', NOW(), NOW()),
                (gen_random_uuid(), 'club-004', 'user-045', NOW(), NOW()),
                (gen_random_uuid(), 'club-004', 'user-046', NOW(), NOW()),
                (gen_random_uuid(), 'club-004', 'user-047', NOW(), NOW()),
                (gen_random_uuid(), 'club-004', 'user-048', NOW(), NOW()),
                (gen_random_uuid(), 'club-004', 'user-049', NOW(), NOW()),
                (gen_random_uuid(), 'club-004', 'user-050', NOW(), NOW()),
                (gen_random_uuid(), 'club-004', 'user-001', NOW(), NOW()),
                (gen_random_uuid(), 'club-004', 'user-002', NOW(), NOW()),
                (gen_random_uuid(), 'club-004', 'user-003', NOW(), NOW()),
                (gen_random_uuid(), 'club-004', 'user-004', NOW(), NOW()),
                (gen_random_uuid(), 'club-004', 'user-005', NOW(), NOW());
                
                
                -- club-005 (연극반): 8명 (모집마감)
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-005', 'user-001', NOW(), NOW()),
                (gen_random_uuid(), 'club-005', 'user-002', NOW(), NOW()),
                (gen_random_uuid(), 'club-005', 'user-003', NOW(), NOW()),
                (gen_random_uuid(), 'club-005', 'user-004', NOW(), NOW()),
                (gen_random_uuid(), 'club-005', 'user-005', NOW(), NOW()),
                (gen_random_uuid(), 'club-005', 'user-006', NOW(), NOW()),
                (gen_random_uuid(), 'club-005', 'user-007', NOW(), NOW()),
                (gen_random_uuid(), 'club-005', 'user-008', NOW(), NOW());
                
                -- club-006 (농구동아리): 14명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-006', 'user-009', NOW(), NOW()),
                (gen_random_uuid(), 'club-006', 'user-010', NOW(), NOW()),
                (gen_random_uuid(), 'club-006', 'user-011', NOW(), NOW()),
                (gen_random_uuid(), 'club-006', 'user-012', NOW(), NOW()),
                (gen_random_uuid(), 'club-006', 'user-013', NOW(), NOW()),
                (gen_random_uuid(), 'club-006', 'user-014', NOW(), NOW()),
                (gen_random_uuid(), 'club-006', 'user-015', NOW(), NOW()),
                (gen_random_uuid(), 'club-006', 'user-016', NOW(), NOW()),
                (gen_random_uuid(), 'club-006', 'user-017', NOW(), NOW()),
                (gen_random_uuid(), 'club-006', 'user-018', NOW(), NOW()),
                (gen_random_uuid(), 'club-006', 'user-019', NOW(), NOW()),
                (gen_random_uuid(), 'club-006', 'user-020', NOW(), NOW()),
                (gen_random_uuid(), 'club-006', 'user-021', NOW(), NOW()),
                (gen_random_uuid(), 'club-006', 'user-022', NOW(), NOW());
                
                -- club-007 (영어회화): 9명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-007', 'user-023', NOW(), NOW()),
                (gen_random_uuid(), 'club-007', 'user-024', NOW(), NOW()),
                (gen_random_uuid(), 'club-007', 'user-025', NOW(), NOW()),
                (gen_random_uuid(), 'club-007', 'user-026', NOW(), NOW()),
                (gen_random_uuid(), 'club-007', 'user-027', NOW(), NOW()),
                (gen_random_uuid(), 'club-007', 'user-028', NOW(), NOW()),
                (gen_random_uuid(), 'club-007', 'user-029', NOW(), NOW()),
                (gen_random_uuid(), 'club-007', 'user-030', NOW(), NOW()),
                (gen_random_uuid(), 'club-007', 'user-031', NOW(), NOW());
                
                -- club-008 (포토클럽): 11명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-008', 'user-032', NOW(), NOW()),
                (gen_random_uuid(), 'club-008', 'user-033', NOW(), NOW()),
                (gen_random_uuid(), 'club-008', 'user-034', NOW(), NOW()),
                (gen_random_uuid(), 'club-008', 'user-035', NOW(), NOW()),
                (gen_random_uuid(), 'club-008', 'user-036', NOW(), NOW()),
                (gen_random_uuid(), 'club-008', 'user-037', NOW(), NOW()),
                (gen_random_uuid(), 'club-008', 'user-038', NOW(), NOW()),
                (gen_random_uuid(), 'club-008', 'user-039', NOW(), NOW()),
                (gen_random_uuid(), 'club-008', 'user-040', NOW(), NOW()),
                (gen_random_uuid(), 'club-008', 'user-041', NOW(), NOW()),
                (gen_random_uuid(), 'club-008', 'user-042', NOW(), NOW());
                
                -- club-009 (밴드동아리): 6명 (모집마감)
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-009', 'user-043', NOW(), NOW()),
                (gen_random_uuid(), 'club-009', 'user-044', NOW(), NOW()),
                (gen_random_uuid(), 'club-009', 'user-045', NOW(), NOW()),
                (gen_random_uuid(), 'club-009', 'user-046', NOW(), NOW()),
                (gen_random_uuid(), 'club-009', 'user-047', NOW(), NOW()),
                (gen_random_uuid(), 'club-009', 'user-048', NOW(), NOW());
                
                -- club-010 (테니스부): 13명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-010', 'user-049', NOW(), NOW()),
                (gen_random_uuid(), 'club-010', 'user-050', NOW(), NOW()),
                (gen_random_uuid(), 'club-010', 'user-001', NOW(), NOW()),
                (gen_random_uuid(), 'club-010', 'user-002', NOW(), NOW()),
                (gen_random_uuid(), 'club-010', 'user-003', NOW(), NOW()),
                (gen_random_uuid(), 'club-010', 'user-004', NOW(), NOW()),
                (gen_random_uuid(), 'club-010', 'user-005', NOW(), NOW()),
                (gen_random_uuid(), 'club-010', 'user-006', NOW(), NOW()),
                (gen_random_uuid(), 'club-010', 'user-007', NOW(), NOW()),
                (gen_random_uuid(), 'club-010', 'user-008', NOW(), NOW()),
                (gen_random_uuid(), 'club-010', 'user-009', NOW(), NOW()),
                (gen_random_uuid(), 'club-010', 'user-010', NOW(), NOW()),
                (gen_random_uuid(), 'club-010', 'user-011', NOW(), NOW());
                
                -- club-011 (독서모임): 7명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-011', 'user-012', NOW(), NOW()),
                (gen_random_uuid(), 'club-011', 'user-013', NOW(), NOW()),
                (gen_random_uuid(), 'club-011', 'user-014', NOW(), NOW()),
                (gen_random_uuid(), 'club-011', 'user-015', NOW(), NOW()),
                (gen_random_uuid(), 'club-011', 'user-016', NOW(), NOW()),
                (gen_random_uuid(), 'club-011', 'user-017', NOW(), NOW()),
                (gen_random_uuid(), 'club-011', 'user-018', NOW(), NOW());
                
                -- club-012 (댄스크루): 16명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-012', 'user-019', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-020', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-021', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-022', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-023', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-024', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-025', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-026', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-027', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-028', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-029', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-030', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-031', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-032', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-033', NOW(), NOW()),
                (gen_random_uuid(), 'club-012', 'user-034', NOW(), NOW());
                
                -- club-013 (토론동아리): 5명 (모집마감)
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-013', 'user-035', NOW(), NOW()),
                (gen_random_uuid(), 'club-013', 'user-036', NOW(), NOW()),
                (gen_random_uuid(), 'club-013', 'user-037', NOW(), NOW()),
                (gen_random_uuid(), 'club-013', 'user-038', NOW(), NOW()),
                (gen_random_uuid(), 'club-013', 'user-039', NOW(), NOW());
                
                -- club-014 (환경지키미): 10명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-014', 'user-040', NOW(), NOW()),
                (gen_random_uuid(), 'club-014', 'user-041', NOW(), NOW()),
                (gen_random_uuid(), 'club-014', 'user-042', NOW(), NOW()),
                (gen_random_uuid(), 'club-014', 'user-043', NOW(), NOW()),
                (gen_random_uuid(), 'club-014', 'user-044', NOW(), NOW()),
                (gen_random_uuid(), 'club-014', 'user-045', NOW(), NOW()),
                (gen_random_uuid(), 'club-014', 'user-046', NOW(), NOW()),
                (gen_random_uuid(), 'club-014', 'user-047', NOW(), NOW()),
                (gen_random_uuid(), 'club-014', 'user-048', NOW(), NOW()),
                (gen_random_uuid(), 'club-014', 'user-049', NOW(), NOW());
                
                -- club-015 (영화감상): 8명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-015', 'user-050', NOW(), NOW()),
                (gen_random_uuid(), 'club-015', 'user-001', NOW(), NOW()),
                (gen_random_uuid(), 'club-015', 'user-002', NOW(), NOW()),
                (gen_random_uuid(), 'club-015', 'user-003', NOW(), NOW()),
                (gen_random_uuid(), 'club-015', 'user-004', NOW(), NOW()),
                (gen_random_uuid(), 'club-015', 'user-005', NOW(), NOW()),
                (gen_random_uuid(), 'club-015', 'user-006', NOW(), NOW()),
                (gen_random_uuid(), 'club-015', 'user-007', NOW(), NOW());
                
                -- club-016 (기타동아리): 12명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-016', 'user-008', NOW(), NOW()),
                (gen_random_uuid(), 'club-016', 'user-009', NOW(), NOW()),
                (gen_random_uuid(), 'club-016', 'user-010', NOW(), NOW()),
                (gen_random_uuid(), 'club-016', 'user-011', NOW(), NOW()),
                (gen_random_uuid(), 'club-016', 'user-012', NOW(), NOW()),
                (gen_random_uuid(), 'club-016', 'user-013', NOW(), NOW()),
                (gen_random_uuid(), 'club-016', 'user-014', NOW(), NOW()),
                (gen_random_uuid(), 'club-016', 'user-015', NOW(), NOW()),
                (gen_random_uuid(), 'club-016', 'user-016', NOW(), NOW()),
                (gen_random_uuid(), 'club-016', 'user-017', NOW(), NOW()),
                (gen_random_uuid(), 'club-016', 'user-018', NOW(), NOW()),
                (gen_random_uuid(), 'club-016', 'user-019', NOW(), NOW());
                
                -- club-017 (배드민턴): 4명 (모집마감, 인기도 낮음)
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-017', 'user-020', NOW(), NOW()),
                (gen_random_uuid(), 'club-017', 'user-021', NOW(), NOW()),
                (gen_random_uuid(), 'club-017', 'user-022', NOW(), NOW()),
                (gen_random_uuid(), 'club-017', 'user-023', NOW(), NOW());
                
                -- club-018 (요리연구회): 9명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-018', 'user-024', NOW(), NOW()),
                (gen_random_uuid(), 'club-018', 'user-025', NOW(), NOW()),
                (gen_random_uuid(), 'club-018', 'user-026', NOW(), NOW()),
                (gen_random_uuid(), 'club-018', 'user-027', NOW(), NOW()),
                (gen_random_uuid(), 'club-018', 'user-028', NOW(), NOW()),
                (gen_random_uuid(), 'club-018', 'user-029', NOW(), NOW()),
                (gen_random_uuid(), 'club-018', 'user-030', NOW(), NOW()),
                (gen_random_uuid(), 'club-018', 'user-031', NOW(), NOW()),
                (gen_random_uuid(), 'club-018', 'user-032', NOW(), NOW());
                
                -- club-019 (사랑나눔): 11명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-019', 'user-033', NOW(), NOW()),
                (gen_random_uuid(), 'club-019', 'user-034', NOW(), NOW()),
                (gen_random_uuid(), 'club-019', 'user-035', NOW(), NOW()),
                (gen_random_uuid(), 'club-019', 'user-036', NOW(), NOW()),
                (gen_random_uuid(), 'club-019', 'user-037', NOW(), NOW()),
                (gen_random_uuid(), 'club-019', 'user-038', NOW(), NOW()),
                (gen_random_uuid(), 'club-019', 'user-039', NOW(), NOW()),
                (gen_random_uuid(), 'club-019', 'user-040', NOW(), NOW()),
                (gen_random_uuid(), 'club-019', 'user-041', NOW(), NOW()),
                (gen_random_uuid(), 'club-019', 'user-042', NOW(), NOW()),
                (gen_random_uuid(), 'club-019', 'user-043', NOW(), NOW());
                
                -- club-020 (체스동아리): 3명 (모집마감, 인기도 낮음)
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-020', 'user-044', NOW(), NOW()),
                (gen_random_uuid(), 'club-020', 'user-045', NOW(), NOW()),
                (gen_random_uuid(), 'club-020', 'user-046', NOW(), NOW());
                
                -- club-021 (창작문예): 7명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-021', 'user-047', NOW(), NOW()),
                (gen_random_uuid(), 'club-021', 'user-048', NOW(), NOW()),
                (gen_random_uuid(), 'club-021', 'user-049', NOW(), NOW()),
                (gen_random_uuid(), 'club-021', 'user-050', NOW(), NOW()),
                (gen_random_uuid(), 'club-021', 'user-001', NOW(), NOW()),
                (gen_random_uuid(), 'club-021', 'user-002', NOW(), NOW()),
                (gen_random_uuid(), 'club-021', 'user-003', NOW(), NOW());
                
                -- club-022 (러닝크루): 15명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-022', 'user-004', NOW(), NOW()),
                (gen_random_uuid(), 'club-022', 'user-005', NOW(), NOW()),
                (gen_random_uuid(), 'club-022', 'user-006', NOW(), NOW()),
                (gen_random_uuid(), 'club-022', 'user-007', NOW(), NOW()),
                (gen_random_uuid(), 'club-022', 'user-008', NOW(), NOW()),
                (gen_random_uuid(), 'club-022', 'user-009', NOW(), NOW()),
                (gen_random_uuid(), 'club-022', 'user-010', NOW(), NOW()),
                (gen_random_uuid(), 'club-022', 'user-011', NOW(), NOW()),
                (gen_random_uuid(), 'club-022', 'user-012', NOW(), NOW()),
                (gen_random_uuid(), 'club-022', 'user-013', NOW(), NOW()),
                (gen_random_uuid(), 'club-022', 'user-014', NOW(), NOW()),
                (gen_random_uuid(), 'club-022', 'user-015', NOW(), NOW()),
                (gen_random_uuid(), 'club-022', 'user-016', NOW(), NOW()),
                (gen_random_uuid(), 'club-022', 'user-017', NOW(), NOW()),
                (gen_random_uuid(), 'club-022', 'user-018', NOW(), NOW());
                
                -- club-023 (과학탐구): 6명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-023', 'user-019', NOW(), NOW()),
                (gen_random_uuid(), 'club-023', 'user-020', NOW(), NOW()),
                (gen_random_uuid(), 'club-023', 'user-021', NOW(), NOW()),
                (gen_random_uuid(), 'club-023', 'user-022', NOW(), NOW()),
                (gen_random_uuid(), 'club-023', 'user-023', NOW(), NOW()),
                (gen_random_uuid(), 'club-023', 'user-024', NOW(), NOW());
                
                -- club-024 (여행동아리): 2명 (모집마감, 인기도 낮음)
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-024', 'user-025', NOW(), NOW()),
                (gen_random_uuid(), 'club-024', 'user-026', NOW(), NOW());
                
                -- club-025 (카페동아리): 8명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-025', 'user-027', NOW(), NOW()),
                (gen_random_uuid(), 'club-025', 'user-028', NOW(), NOW()),
                (gen_random_uuid(), 'club-025', 'user-029', NOW(), NOW()),
                (gen_random_uuid(), 'club-025', 'user-030', NOW(), NOW()),
                (gen_random_uuid(), 'club-025', 'user-031', NOW(), NOW()),
                (gen_random_uuid(), 'club-025', 'user-032', NOW(), NOW()),
                (gen_random_uuid(), 'club-025', 'user-033', NOW(), NOW()),
                (gen_random_uuid(), 'club-025', 'user-034', NOW(), NOW());
                
                -- club-026 (스터디모임): 13명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-026', 'user-035', NOW(), NOW()),
                (gen_random_uuid(), 'club-026', 'user-036', NOW(), NOW()),
                (gen_random_uuid(), 'club-026', 'user-037', NOW(), NOW()),
                (gen_random_uuid(), 'club-026', 'user-038', NOW(), NOW()),
                (gen_random_uuid(), 'club-026', 'user-039', NOW(), NOW()),
                (gen_random_uuid(), 'club-026', 'user-040', NOW(), NOW()),
                (gen_random_uuid(), 'club-026', 'user-041', NOW(), NOW()),
                (gen_random_uuid(), 'club-026', 'user-042', NOW(), NOW()),
                (gen_random_uuid(), 'club-026', 'user-043', NOW(), NOW()),
                (gen_random_uuid(), 'club-026', 'user-044', NOW(), NOW()),
                (gen_random_uuid(), 'club-026', 'user-045', NOW(), NOW()),
                (gen_random_uuid(), 'club-026', 'user-046', NOW(), NOW()),
                (gen_random_uuid(), 'club-026', 'user-047', NOW(), NOW());
                
                -- club-027 (게임동아리): 10명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-027', 'user-048', NOW(), NOW()),
                (gen_random_uuid(), 'club-027', 'user-049', NOW(), NOW()),
                (gen_random_uuid(), 'club-027', 'user-050', NOW(), NOW()),
                (gen_random_uuid(), 'club-027', 'user-001', NOW(), NOW()),
                (gen_random_uuid(), 'club-027', 'user-002', NOW(), NOW()),
                (gen_random_uuid(), 'club-027', 'user-003', NOW(), NOW()),
                (gen_random_uuid(), 'club-027', 'user-004', NOW(), NOW()),
                (gen_random_uuid(), 'club-027', 'user-005', NOW(), NOW()),
                (gen_random_uuid(), 'club-027', 'user-006', NOW(), NOW()),
                (gen_random_uuid(), 'club-027', 'user-007', NOW(), NOW());
                
                -- club-028 (등산동아리): 1명 (모집마감, 인기도 낮음)
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-028', 'user-008', NOW(), NOW());
                
                -- club-029 (기독교동아리): 9명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-029', 'user-009', NOW(), NOW()),
                (gen_random_uuid(), 'club-029', 'user-010', NOW(), NOW()),
                (gen_random_uuid(), 'club-029', 'user-011', NOW(), NOW()),
                (gen_random_uuid(), 'club-029', 'user-012', NOW(), NOW()),
                (gen_random_uuid(), 'club-029', 'user-013', NOW(), NOW()),
                (gen_random_uuid(), 'club-029', 'user-014', NOW(), NOW()),
                (gen_random_uuid(), 'club-029', 'user-015', NOW(), NOW()),
                (gen_random_uuid(), 'club-029', 'user-016', NOW(), NOW()),
                (gen_random_uuid(), 'club-029', 'user-017', NOW(), NOW());
                
                -- club-030 (자유동아리): 11명
                INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
                (gen_random_uuid(), 'club-030', 'user-018', NOW(), NOW()),
                (gen_random_uuid(), 'club-030', 'user-019', NOW(), NOW()),
                (gen_random_uuid(), 'club-030', 'user-020', NOW(), NOW()),
                (gen_random_uuid(), 'club-030', 'user-021', NOW(), NOW()),
                (gen_random_uuid(), 'club-030', 'user-022', NOW(), NOW()),
                (gen_random_uuid(), 'club-030', 'user-023', NOW(), NOW()),
                (gen_random_uuid(), 'club-030', 'user-024', NOW(), NOW()),
                (gen_random_uuid(), 'club-030', 'user-025', NOW(), NOW()),
                (gen_random_uuid(), 'club-030', 'user-026', NOW(), NOW()),
                (gen_random_uuid(), 'club-030', 'user-027', NOW(), NOW()),
                (gen_random_uuid(), 'club-030', 'user-028', NOW(), NOW());
                -- (더 많이 만들 수도 있지만 우선 이 4개로 테스트)
                """;
        log.info("동아리 멤버 데이터 입력 완료");
        jdbcTemplate.execute(sql);
    }

    private void loadFavoriteData() {
        log.info("Favorite 데이터 로딩...");
        String sql = """
                -- 5. 즐겨찾기 데이터 (수정된 버전)
                INSERT INTO user_favorites (id, user_id, club_id, created_at, updated_at) VALUES
                
                -- club-002 (상명축구부): 즐겨찾기 15개
                (gen_random_uuid(), 'user-001', 'club-002', NOW(), NOW()),
                (gen_random_uuid(), 'user-002', 'club-002', NOW(), NOW()),
                (gen_random_uuid(), 'user-003', 'club-002', NOW(), NOW()),
                (gen_random_uuid(), 'user-004', 'club-002', NOW(), NOW()),
                (gen_random_uuid(), 'user-005', 'club-002', NOW(), NOW()),
                (gen_random_uuid(), 'user-006', 'club-002', NOW(), NOW()),
                (gen_random_uuid(), 'user-007', 'club-002', NOW(), NOW()),
                (gen_random_uuid(), 'user-008', 'club-002', NOW(), NOW()),
                (gen_random_uuid(), 'user-009', 'club-002', NOW(), NOW()),
                (gen_random_uuid(), 'user-010', 'club-002', NOW(), NOW()),
                (gen_random_uuid(), 'user-026', 'club-002', NOW(), NOW()),
                (gen_random_uuid(), 'user-027', 'club-002', NOW(), NOW()),
                (gen_random_uuid(), 'user-028', 'club-002', NOW(), NOW()),
                (gen_random_uuid(), 'user-029', 'club-002', NOW(), NOW()),
                (gen_random_uuid(), 'user-030', 'club-002', NOW(), NOW()),
                
                -- club-003 (코딩마스터): 즐겨찾기 3개
                (gen_random_uuid(), 'user-011', 'club-003', NOW(), NOW()),
                (gen_random_uuid(), 'user-012', 'club-003', NOW(), NOW()),
                (gen_random_uuid(), 'user-013', 'club-003', NOW(), NOW()),
                
                -- club-001 (그림사랑): 즐겨찾기 8개
                (gen_random_uuid(), 'user-014', 'club-001', NOW(), NOW()),
                (gen_random_uuid(), 'user-015', 'club-001', NOW(), NOW()),
                (gen_random_uuid(), 'user-016', 'club-001', NOW(), NOW()),
                (gen_random_uuid(), 'user-017', 'club-001', NOW(), NOW()),
                (gen_random_uuid(), 'user-018', 'club-001', NOW(), NOW()),
                (gen_random_uuid(), 'user-031', 'club-001', NOW(), NOW()),
                (gen_random_uuid(), 'user-032', 'club-001', NOW(), NOW()),
                (gen_random_uuid(), 'user-033', 'club-001', NOW(), NOW()),
                
                -- club-012 (댄스크루): 즐겨찾기 12개
                (gen_random_uuid(), 'user-019', 'club-012', NOW(), NOW()),
                (gen_random_uuid(), 'user-020', 'club-012', NOW(), NOW()),
                (gen_random_uuid(), 'user-021', 'club-012', NOW(), NOW()),
                (gen_random_uuid(), 'user-022', 'club-012', NOW(), NOW()),
                (gen_random_uuid(), 'user-023', 'club-012', NOW(), NOW()),
                (gen_random_uuid(), 'user-034', 'club-012', NOW(), NOW()),
                (gen_random_uuid(), 'user-035', 'club-012', NOW(), NOW()),
                (gen_random_uuid(), 'user-036', 'club-012', NOW(), NOW()),
                (gen_random_uuid(), 'user-037', 'club-012', NOW(), NOW()),
                (gen_random_uuid(), 'user-038', 'club-012', NOW(), NOW()),
                (gen_random_uuid(), 'user-039', 'club-012', NOW(), NOW()),
                (gen_random_uuid(), 'user-040', 'club-012', NOW(), NOW()),
                
                -- club-022 (러닝크루): 즐겨찾기 4개
                (gen_random_uuid(), 'user-041', 'club-022', NOW(), NOW()),
                (gen_random_uuid(), 'user-042', 'club-022', NOW(), NOW()),
                (gen_random_uuid(), 'user-043', 'club-022', NOW(), NOW()),
                (gen_random_uuid(), 'user-044', 'club-022', NOW(), NOW()),
                
                -- club-006 (농구동아리): 즐겨찾기 6개
                (gen_random_uuid(), 'user-045', 'club-006', NOW(), NOW()),
                (gen_random_uuid(), 'user-046', 'club-006', NOW(), NOW()),
                (gen_random_uuid(), 'user-047', 'club-006', NOW(), NOW()),
                (gen_random_uuid(), 'user-048', 'club-006', NOW(), NOW()),
                (gen_random_uuid(), 'user-049', 'club-006', NOW(), NOW()),
                (gen_random_uuid(), 'user-050', 'club-006', NOW(), NOW()),
                
                -- club-010 (테니스부): 즐겨찾기 10개
                (gen_random_uuid(), 'user-001', 'club-010', NOW(), NOW()),
                (gen_random_uuid(), 'user-002', 'club-010', NOW(), NOW()),
                (gen_random_uuid(), 'user-003', 'club-010', NOW(), NOW()),
                (gen_random_uuid(), 'user-004', 'club-010', NOW(), NOW()),
                (gen_random_uuid(), 'user-005', 'club-010', NOW(), NOW()),
                (gen_random_uuid(), 'user-024', 'club-010', NOW(), NOW()),
                (gen_random_uuid(), 'user-025', 'club-010', NOW(), NOW()),
                (gen_random_uuid(), 'user-026', 'club-010', NOW(), NOW()),
                (gen_random_uuid(), 'user-027', 'club-010', NOW(), NOW()),
                (gen_random_uuid(), 'user-028', 'club-010', NOW(), NOW()),
                
                -- club-004 (나눔봉사단): 즐겨찾기 2개
                (gen_random_uuid(), 'user-006', 'club-004', NOW(), NOW()),
                (gen_random_uuid(), 'user-007', 'club-004', NOW(), NOW()),
                
                -- club-016 (기타동아리): 즐겨찾기 5개
                (gen_random_uuid(), 'user-008', 'club-016', NOW(), NOW()),
                (gen_random_uuid(), 'user-009', 'club-016', NOW(), NOW()),
                (gen_random_uuid(), 'user-010', 'club-016', NOW(), NOW()),
                (gen_random_uuid(), 'user-029', 'club-016', NOW(), NOW()),
                (gen_random_uuid(), 'user-030', 'club-016', NOW(), NOW()),
                
                -- club-026 (스터디모임): 즐겨찾기 7개
                (gen_random_uuid(), 'user-011', 'club-026', NOW(), NOW()),
                (gen_random_uuid(), 'user-012', 'club-026', NOW(), NOW()),
                (gen_random_uuid(), 'user-013', 'club-026', NOW(), NOW()),
                (gen_random_uuid(), 'user-014', 'club-026', NOW(), NOW()),
                (gen_random_uuid(), 'user-015', 'club-026', NOW(), NOW()),
                (gen_random_uuid(), 'user-031', 'club-026', NOW(), NOW()),
                (gen_random_uuid(), 'user-032', 'club-026', NOW(), NOW()),
                
                -- club-008 (포토클럽): 즐겨찾기 9개
                (gen_random_uuid(), 'user-016', 'club-008', NOW(), NOW()),
                (gen_random_uuid(), 'user-017', 'club-008', NOW(), NOW()),
                (gen_random_uuid(), 'user-018', 'club-008', NOW(), NOW()),
                (gen_random_uuid(), 'user-019', 'club-008', NOW(), NOW()),
                (gen_random_uuid(), 'user-020', 'club-008', NOW(), NOW()),
                (gen_random_uuid(), 'user-033', 'club-008', NOW(), NOW()),
                (gen_random_uuid(), 'user-034', 'club-008', NOW(), NOW()),
                (gen_random_uuid(), 'user-035', 'club-008', NOW(), NOW()),
                (gen_random_uuid(), 'user-036', 'club-008', NOW(), NOW()),
                
                -- club-019 (사랑나눔): 즐겨찾기 4개
                (gen_random_uuid(), 'user-021', 'club-019', NOW(), NOW()),
                (gen_random_uuid(), 'user-022', 'club-019', NOW(), NOW()),
                (gen_random_uuid(), 'user-023', 'club-019', NOW(), NOW()),
                (gen_random_uuid(), 'user-037', 'club-019', NOW(), NOW()),
                
                -- club-030 (자유동아리): 즐겨찾기 6개
                (gen_random_uuid(), 'user-024', 'club-030', NOW(), NOW()),
                (gen_random_uuid(), 'user-025', 'club-030', NOW(), NOW()),
                (gen_random_uuid(), 'user-026', 'club-030', NOW(), NOW()),
                (gen_random_uuid(), 'user-038', 'club-030', NOW(), NOW()),
                (gen_random_uuid(), 'user-039', 'club-030', NOW(), NOW()),
                (gen_random_uuid(), 'user-040', 'club-030', NOW(), NOW()),
                
                -- club-027 (게임동아리): 즐겨찾기 3개
                (gen_random_uuid(), 'user-027', 'club-027', NOW(), NOW()),
                (gen_random_uuid(), 'user-028', 'club-027', NOW(), NOW()),
                (gen_random_uuid(), 'user-041', 'club-027', NOW(), NOW()),
                
                -- club-029 (기독교동아리): 즐겨찾기 2개
                (gen_random_uuid(), 'user-029', 'club-029', NOW(), NOW()),
                (gen_random_uuid(), 'user-042', 'club-029', NOW(), NOW()),
                
                -- club-025 (카페동아리): 즐겨찾기 1개
                (gen_random_uuid(), 'user-030', 'club-025', NOW(), NOW()),
                
                -- club-018 (요리연구회): 즐겨찾기 3개
                (gen_random_uuid(), 'user-043', 'club-018', NOW(), NOW()),
                (gen_random_uuid(), 'user-044', 'club-018', NOW(), NOW()),
                (gen_random_uuid(), 'user-045', 'club-018', NOW(), NOW()),
                
                -- club-007 (영어회화): 즐겨찾기 2개
                (gen_random_uuid(), 'user-046', 'club-007', NOW(), NOW()),
                (gen_random_uuid(), 'user-047', 'club-007', NOW(), NOW()),
                
                -- club-014 (환경지키미): 즐겨찾기 3개
                (gen_random_uuid(), 'user-048', 'club-014', NOW(), NOW()),
                (gen_random_uuid(), 'user-049', 'club-014', NOW(), NOW()),
                (gen_random_uuid(), 'user-050', 'club-014', NOW(), NOW()),
                
                -- 나머지 동아리들은 즐겨찾기 0-1개
                -- club-011 (독서모임): 즐겨찾기 1개
                (gen_random_uuid(), 'user-001', 'club-011', NOW(), NOW()),
                
                -- club-015 (영화감상): 즐겨찾기 1개
                (gen_random_uuid(), 'user-002', 'club-015', NOW(), NOW()),
                
                -- club-021 (창작문예): 즐겨찾기 1개
                (gen_random_uuid(), 'user-003', 'club-021', NOW(), NOW()),
                
                -- club-023 (과학탐구): 즐겨찾기 1개
                (gen_random_uuid(), 'user-004', 'club-023', NOW(), NOW());
                """;
        jdbcTemplate.execute(sql);
    }
}