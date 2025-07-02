package org.project.ttokttok.domain.club.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.ImageMaxSizeOverException;
import org.project.ttokttok.domain.club.exception.InvalidImageTypeException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.service.dto.request.ClubContentUpdateServiceRequest;
import org.project.ttokttok.domain.club.service.mapper.ClubMapper;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClubAdminService {

    private final ClubRepository clubRepository;
    private final ClubMapper mapper;
    private final S3Service s3Service;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L; // 5MB

    @Transactional
    public void updateContent(String username, ClubContentUpdateServiceRequest request) {
        Club club = clubRepository.findById(request.clubId())
                .orElseThrow(ClubNotFoundException::new);

        // 요청한 이가 이 동아리의 관리자가 맞는지 검증
        validateAdmin(username, club.getAdmin().getUsername());

        if (request.profileImage() != null && request.profileImage().isPresent()) {
            // s3에 저장하고 url를 받아와서 저장.
            MultipartFile profileImage = request.profileImage().get();
            String profileImgKey = s3Service.uploadFile(profileImage);

            // 이미지 검증
            validateImageContentType(profileImage);
            validateImageSize(profileImage);

            // 기존 프로필 이미지가 있다면 삭제
            validateProfileImgExist(club, profileImgKey);

            club.setProfileImageUrl(profileImgKey);
        }

        // 요청에서 null 값만 확인 후, mapper를 통해 업데이트
        mapper.updateClubFromRequest(club, request);
    }

    private void validateImageContentType(MultipartFile profileImage) {
        if (!ALLOWED_CONTENT_TYPES.contains(profileImage.getContentType())) {
            throw new InvalidImageTypeException();
        }
    }

    private void validateImageSize(MultipartFile profileImage) {
        if (profileImage.getSize() > MAX_IMAGE_SIZE) {
            throw new ImageMaxSizeOverException();
        }
    }

    private void validateProfileImgExist(Club club, String profileImgKey) {
        if (club.getProfileImageUrl() != null && !club.getProfileImageUrl().equals(profileImgKey)) {
            s3Service.deleteFile(club.getProfileImageUrl());
        }
    }

    private void validateAdmin(String username, String targetAdminUsername) {
        if (!username.equals(targetAdminUsername))
            throw new NotClubAdminException();
    }
}
