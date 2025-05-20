package com.project.mydrive.api.v1;

import com.project.mydrive.core.domain.User;
import com.project.mydrive.core.repository.UserRepository;
import com.project.mydrive.core.service.FileService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/v1/files")
public class FilesController {

    // TODO temporary
    private final FileService fileService;

    public String uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("parentDirId") Long parentDirId
    ) throws IOException {

        return fileService.save(file, parentDirId);
    }

}
