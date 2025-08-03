package org.project.ttokttok.infrastructure.s3.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum S3FileDirectory {
    PROFILE_IMAGE("/profile-images/"),
    BOARD_FILE("/board-files/"),
    BOARD_IMAGE("/board-images/"),
    INTRODUCTION_IMAGE("/introduction-images/");

    final String directoryName;
}
