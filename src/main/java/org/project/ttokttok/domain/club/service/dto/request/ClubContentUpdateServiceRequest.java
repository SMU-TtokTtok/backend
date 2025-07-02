package org.project.ttokttok.domain.club.service.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.springframework.web.multipart.MultipartFile;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClubContentUpdateServiceRequest(
        String clubId,
        JsonNullable<String> name,
        JsonNullable<ClubType> clubType,
        JsonNullable<ClubCategory> clubCategory,
        JsonNullable<String> customCategory,
        JsonNullable<String> summary,
        JsonNullable<MultipartFile> profileImage,
        JsonNullable<String> content,
        JsonNullable<Boolean> recruiting
) {
}