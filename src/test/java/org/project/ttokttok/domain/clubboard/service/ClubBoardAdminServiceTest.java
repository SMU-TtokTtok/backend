package org.project.ttokttok.domain.clubboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubboard.repository.ClubBoardRepository;
import org.project.ttokttok.domain.clubboard.service.dto.request.CreateBoardServiceRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ClubBoardAdminServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubBoardRepository clubBoardRepository;

    @InjectMocks
    private ClubBoardAdminService clubBoardService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("createBoard(): 요청 관리자 명과 저장된 관리자 명이 다르면 예외 발생")
    void createBoardAdminNameNotMatch() {

    }


    @Test
    @DisplayName("createBoard(): 동아리 ID가 존재하지 않으면 예외 발생")
    void createBoardClubNotFound() {
        // given
        String clubId = "invalidClubId";
        CreateBoardServiceRequest request = new CreateBoardServiceRequest("admin", clubId, "title", "content");

        // when & then
        assertThatThrownBy(() -> clubBoardService.createBoard(request))
                .isInstanceOf(ClubNotFoundException.class);
    }
}