package org.project.ttokttok.domain.clubboard.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.clubboard.repository.ClubBoardRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClubBoardUserService {

    private final ClubBoardRepository clubBoardRepository;

}
