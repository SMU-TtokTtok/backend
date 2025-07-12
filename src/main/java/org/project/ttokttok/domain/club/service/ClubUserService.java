package org.project.ttokttok.domain.club.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.service.dto.response.ClubDetailServiceResponse;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClubUserService {

    private final ClubRepository clubRepository;

    private final S3Service s3Service;

    public ClubDetailServiceResponse getClubIntroduction(String username, String clubId) {
        if (!clubRepository.existsById(clubId))
            throw new ClubNotFoundException();

        return ClubDetailServiceResponse.from(clubRepository.getClubIntroduction(clubId, username));
    }
}
