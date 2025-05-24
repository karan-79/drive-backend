package com.project.mydrive.api.v1;

import com.project.mydrive.api.v1.model.APIFile;
import com.project.mydrive.api.v1.model.UpdateFileRequest;
import com.project.mydrive.core.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
public class FilesController {

    // TODO temporary
    private final FileService fileService;

    UUID userId = UUID.fromString("7e68c8cf-9e4e-42f1-bb19-51b3cf9d676f");

    @PostMapping
    public APIFile uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "parentDirId", required = false) Long parentDirId
    ) throws IOException {
        return fileService.save(file, parentDirId, userId);
    }

    @PutMapping("/{fileId}")
    public APIFile updateFile(@PathVariable("fileId") Long fileId, @RequestBody UpdateFileRequest fileRequest) {
        return fileService.update(fileId, fileRequest, userId);
    }

    @GetMapping
    public List<APIFile> getFiles(@RequestParam(value = "parentDirId", required = false) Long parentDirId) {
        return fileService.getFilesUnder(parentDirId, userId);
    }

}
