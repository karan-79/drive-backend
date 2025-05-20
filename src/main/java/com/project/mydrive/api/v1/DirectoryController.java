package com.project.mydrive.api.v1;

import com.project.mydrive.api.v1.model.CreateDirectoryRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/v1/directories")
@RestController
public class DirectoryController {


    @PostMapping
    public void createDir(CreateDirectoryRequest request) {

    }

}
