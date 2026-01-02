package com.project.mydrive.api.v1;

import com.project.mydrive.api.v1.model.APIFile;
import com.project.mydrive.api.v1.model.UpdateFileRequest;
import com.project.mydrive.core.domain.User;
import com.project.mydrive.core.service.CleanUpService;
import com.project.mydrive.core.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping
    public APIFile uploadFile(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "parentDirId", required = false) Long parentDirId
    ) throws IOException {
        return fileService.save(file, parentDirId, user);
    }

    @PutMapping("/{fileId}")
    public APIFile updateFile(
            @AuthenticationPrincipal User user,
            @PathVariable("fileId") Long fileId,
            @RequestBody UpdateFileRequest fileRequest
    ) {
        return fileService.update(fileId, fileRequest, user);
    }

    @GetMapping("/{fileId}/thumbnail")
    public ResponseEntity<Resource> loadThumbnail(
            @AuthenticationPrincipal User user,
            @PathVariable("fileId") Long fileId
    ) {
        var file = fileService.loadThumbnail(fileId, user);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .body(file.resource());
    }

    @GetMapping
    public List<APIFile> getFiles(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "parentDirId", required = false) Long parentDirId
    ) {
        return fileService.getFilesUnder(parentDirId, user);
    }

    // TODO: I think you should take in just file id and resolve blobRef internally?? Don't know need to think more.
    @GetMapping("/{blobRef}")
    public ResponseEntity<Resource> downloadFile(
            @AuthenticationPrincipal User user,
            @PathVariable("blobRef") UUID blobRef
    ) {
        var fileResource = fileService.downloadFile(blobRef, user);

        String contentType = fileResource.mimeType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileResource.filename() + "\""
                )
                .contentType(MediaType.parseMediaType(contentType))
                .body(fileResource.resource());
    }


    @DeleteMapping("/{blobRef}")
    public void deleteFile(
            @AuthenticationPrincipal User user,
            @PathVariable("blobRef") UUID blobRef
    ) {
        fileService.deleteFile(blobRef, user);
    }

}
