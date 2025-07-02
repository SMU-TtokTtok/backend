package org.project.ttokttok.domain.club.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.service.dto.request.ClubContentUpdateServiceRequest;


@Mapper(componentModel = "spring")
public interface ClubMapper {

    /**
     * ClubContentUpdateServiceRequest의 각 필드를 Club 엔티티에 매핑.
     * JsonNullable로 감싼 필드는 unwrapNullable 메서드를 통해 언랩.
     */
    @Mapping(target = "name", source = "name", qualifiedByName = "unwrapNullable")
    @Mapping(target = "clubType", source = "clubType", qualifiedByName = "unwrapNullable")
    @Mapping(target = "clubCategory", source = "clubCategory", qualifiedByName = "unwrapNullable")
    @Mapping(target = "customCategory", source = "customCategory", qualifiedByName = "unwrapNullable")
    @Mapping(target = "summary", source = "summary", qualifiedByName = "unwrapNullable")
    @Mapping(target = "content", source = "content", qualifiedByName = "unwrapNullable")
    @Mapping(target = "recruiting", source = "recruiting", qualifiedByName = "unwrapNullable")
    void updateClubFromRequest(@MappingTarget Club club, ClubContentUpdateServiceRequest request);

    /**
     * JsonNullable 타입의 값을 언랩하여 실제 값 또는 null을 반환.
     */
    @Named("unwrapNullable")
    static <T> T unwrap(JsonNullable<T> value) {
        return (value != null && value.isPresent()) ? value.get() : null;
    }
}