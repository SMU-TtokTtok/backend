package org.project.ttokttok.domain.club.service.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;

public record ClubPatchRequest(
        JsonNullable<String> name,
        JsonNullable<ClubType> clubType,
        JsonNullable<ClubCategory> clubCategory,
        JsonNullable<ClubUniv> clubUniv,
        JsonNullable<String> customCategory,
        JsonNullable<String> summary,
        JsonNullable<String> content
) {
}
