package com.project.mydrive.api.v1;

import com.project.mydrive.api.v1.model.APIDirectory;
import com.project.mydrive.api.v1.model.CreateDirectoryRequest;
import com.project.mydrive.core.service.DirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/v1/directories")
@RestController
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryService directoryService;

    UUID userId = UUID.fromString("7e68c8cf-9e4e-42f1-bb19-51b3cf9d676f");

    @PostMapping
    public APIDirectory createDir(@RequestBody CreateDirectoryRequest request) {
        return directoryService.createDir(request.name(), request.parentDirectoryId(), userId);
    }


    @PutMapping("/{dirId}")
    public APIDirectory updateDir(@PathVariable("dirId") Long dirId, @RequestBody CreateDirectoryRequest request) {
        return directoryService.updateDir(dirId, request.name(), request.parentDirectoryId(), userId);
    }

    @GetMapping
    public List<APIDirectory> getDirs(@RequestParam(value = "parentDirId", required = false) Long parentDir) {
        return directoryService.getAllDirsUnder(parentDir, userId);
    }

    @GetMapping("/all")
    public List<APIDirectory> getAllDirs() {
        return directoryService.getAllDirs(userId);
    }


}
