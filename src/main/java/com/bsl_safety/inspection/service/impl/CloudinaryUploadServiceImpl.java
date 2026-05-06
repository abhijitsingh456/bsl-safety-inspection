package com.bsl_safety.inspection.service.impl;

import com.bsl_safety.inspection.service.CloudinaryUploadService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryUploadServiceImpl implements CloudinaryUploadService {

    private final Cloudinary cloudinary;

    @Override
    public List<String> uploadFromPaths(List<String> photoPaths) throws IOException {

        List<String> photoUrls = new ArrayList<>();

        for(String path: photoPaths){
            try{
                Map uploadResult = cloudinary.uploader().upload(
                        Files.readAllBytes(Paths.get(path)),
                        ObjectUtils.asMap(
                                "asset_folder","${cloudinary.asset-folder}",
                                "use_filename",true,
                                "unique_filename",true
                        )
                );
                photoUrls.add(uploadResult.get("secure_url").toString());
            }catch (IOException e){
                throw new RuntimeException("Cloudinary Upload Failed", e);
            }
        }

        return photoUrls;
    }
}
