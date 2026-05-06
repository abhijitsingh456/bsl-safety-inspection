package com.bsl_safety.inspection.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface CloudinaryUploadService {
    List<String> uploadFromPaths(List<String> photoPaths) throws IOException;
}
