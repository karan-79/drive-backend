package com.project.mydrive.core.service;

import com.project.mydrive.core.domain.Directory;
import com.project.mydrive.core.domain.User;
import com.project.mydrive.core.repository.DirectoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DirectoryService {
    private final DirectoryRepository directoryRepository;

    public Long createDir(String name, Long parentDir, UUID userId) {
        return null;
    }

    public Directory createRootDirForUser(User user) {
        var dir = new Directory();
        dir.setName("/");
        dir.setOwner(user);

        return directoryRepository.save(dir);
    }



}
