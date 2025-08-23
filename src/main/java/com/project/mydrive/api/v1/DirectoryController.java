package com.project.mydrive.api.v1;

import com.project.mydrive.api.v1.model.APIDirectory;
import com.project.mydrive.api.v1.model.CreateDirectoryRequest;
import com.project.mydrive.core.domain.User;
import com.project.mydrive.core.service.DirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/v1/directories")
@RestController
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryService directoryService;

    @PostMapping
    public APIDirectory createDir(
            @AuthenticationPrincipal User user,
            @RequestBody CreateDirectoryRequest request
    ) {
        return directoryService.createDir(request.name(), request.parentDirectoryId(), user.getId());
    }


    @PutMapping("/{dirId}")
    public APIDirectory updateDir(
            @AuthenticationPrincipal User user,
            @PathVariable("dirId") Long dirId,
            @RequestBody CreateDirectoryRequest request
    ) {
        return directoryService.updateDir(dirId, request.name(), request.parentDirectoryId(), user.getId());
    }

    @GetMapping
    public List<APIDirectory> getDirs(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "parentDirId", required = false) Long parentDir
    ) {
        return directoryService.getAllDirsUnder(parentDir, user.getId());
    }

    @GetMapping("/all")
    public List<APIDirectory> getAllDirs(
            @AuthenticationPrincipal User user
    ) {
        return directoryService.getAllDirs(user.getId());
    }

    @DeleteMapping("/{dirId}")
    public void deleteDir(
            @AuthenticationPrincipal User user,
            @PathVariable("dirId") Long dirId
    ) {
        directoryService.deleteDir(dirId, user);
    }


}
