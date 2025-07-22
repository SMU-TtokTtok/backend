package org.project.ttokttok.infrastructure.s3.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface ContentValidatable {
    void validateContent(MultipartFile content);

    void validateSize(long size);

    void validateType(String type);

    void validateFileName(String fileName);
}
