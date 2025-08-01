package com.project.mydrive.core.service;

import com.project.mydrive.api.v1.model.APIDirectory;
import com.project.mydrive.core.domain.Directory;
import com.project.mydrive.core.domain.User;
import com.project.mydrive.core.exception.DirectoryNotFoundException;
import com.project.mydrive.core.exception.UserNotFoundException;
import com.project.mydrive.core.repository.DirectoryRepository;
import com.project.mydrive.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DirectoryService {

    private final DirectoryRepository directoryRepository;
    private final UserRepository userRepository;

    public APIDirectory createDir(String name, Long parentDir, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));
        var dir = parentDir == null ? getRootDirForUser(user) : directoryRepository.getDirectoryByOwnerAndId(user, parentDir).orElseThrow(() -> new DirectoryNotFoundException("Parent directory with ID " + parentDir + " not found."));

        var newDir = new Directory();
        newDir.setName(name);
        newDir.setOwner(user);
        newDir.setParentDirectory(dir);

        var savedDir = directoryRepository.save(newDir);

        return toApiDir(savedDir);
    }

    private APIDirectory toApiDir(Directory savedDir) {
        return new APIDirectory(savedDir.getId(), savedDir.getName(), Optional.ofNullable(savedDir.getParentDirectory()).map(Directory::getId).orElse(null));
    }

    public Directory createRootDirForUser(User user) {
        var dir = new Directory();

        dir.setName("My Drive");
        dir.setOwner(user);

        return directoryRepository.save(dir);

    }

    public Directory getRootDirForUser(User user) {
        return directoryRepository.getDirectoryByOwnerAndParentDirectoryIsNull(user);
    }

    public APIDirectory updateDir(Long dirId, String newDirName, Long newParentDirId, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));
        var dir = directoryRepository.getDirectoryByOwnerAndId(user, dirId).orElseThrow(() -> new DirectoryNotFoundException("Directory with ID " + dirId + " not found."));

        if (!newDirName.equals(dir.getName())) {
            dir.setName(newDirName);
        }

        // only if not root
        if (dir.getParentDirectory() != null
                && newParentDirId != null
                && !dir.getParentDirectory().getId().equals(newParentDirId)
        ) {
            var newParentDir = directoryRepository.getDirectoryByOwnerAndId(user, newParentDirId).orElseThrow(() -> new DirectoryNotFoundException("New parent directory with ID " + newParentDirId + " not found."));
            dir.setParentDirectory(newParentDir);
        }

        var savedDir = directoryRepository.save(dir);

        return toApiDir(savedDir);
    }

    public List<APIDirectory> getAllDirsUnder(Long parentDir, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));
        Directory dir = null;
        if (parentDir == null) {
            dir = getRootDirForUser(user);
        } else {
            dir = directoryRepository.getDirectoryByOwnerAndId(user, parentDir).orElseThrow();
        }

        return dir.getSubDirectories().stream().map(this::toApiDir).toList();
    }

    public List<APIDirectory> getAllDirs(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));

        return directoryRepository.findAllByOwner(user).stream().map(this::toApiDir).toList();

    }
}