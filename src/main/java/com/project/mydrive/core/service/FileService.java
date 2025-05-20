package com.project.mydrive.core.service;

import com.project.mydrive.core.domain.Directory;
import com.project.mydrive.core.domain.File;
import com.project.mydrive.core.domain.FileMetadata;
import com.project.mydrive.core.domain.User;
import com.project.mydrive.core.repository.DirectoryRepository;
import com.project.mydrive.core.repository.FileRepository;
import com.project.mydrive.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final String LOCAL_STORE_DIR = "local-blob";
    private final FileRepository fileRepository;
    private final FileMeta fileRepository;
    private final DirectoryRepository directoryRepository;

    private final UserRepository userRepository;

    public File save(MultipartFile file, Long parentDirId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        User user = userRepository.findByUsername("karan").orElseThrow();
        Directory dir = directoryRepository.findById(parentDirId).orElseThrow();


        UUID blobRef = UUID.randomUUID();
        //TODO move to a client
        try {
            var storagePath = Paths.get(LOCAL_STORE_DIR).toAbsolutePath().normalize();
            Files.createDirectories(storagePath);
            Path target = storagePath.resolve(blobRef.toString());
            Files.copy(file.getInputStream(), target);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        var fileToSave = new File();

        fileToSave.setName(file.getOriginalFilename());
        fileToSave.setBlobReferenceId(blobRef);
        fileToSave.setOwner(user);
        fileToSave.setParentDirectory(dir);

        var fileMetadata = new FileMetadata();
        fileMetadata.setSize(BigInteger.valueOf(file.getSize()));
        fileMetadata.setMimeType(file.getContentType());
        fileMetadata.setExtension(getFileExtension(file.getOriginalFilename()));

        fileToSave.setFileMetadata(fileMetadata);

        return fileRepository.save(fileToSave);
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return null;
    }
}
