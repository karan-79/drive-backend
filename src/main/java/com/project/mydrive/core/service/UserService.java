package com.project.mydrive.core.service;

import com.project.mydrive.api.v1.model.APIUser;
import com.project.mydrive.api.v1.model.CreateUserRequest;
import com.project.mydrive.core.domain.Directory;
import com.project.mydrive.core.domain.User;
import com.project.mydrive.core.repository.DirectoryRepository;
import com.project.mydrive.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DirectoryService directoryService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public APIUser createUser(CreateUserRequest createUserRequest) {

        // TODO validate
        userRepository.findByEmail(createUserRequest.email().trim())
                .map((u) -> {
                    throw new RuntimeException("User already exist with email" + createUserRequest.email());
                });
        userRepository.findByUsername(createUserRequest.username().trim())
                .map((u) -> {
                    throw new RuntimeException("User already exist with email" + createUserRequest.email());
                });

        var password = bCryptPasswordEncoder.encode(createUserRequest.password());

        var user = new User();
        user.setUsername(createUserRequest.username().trim());
        user.setPassword(password);
        user.setName(createUserRequest.name().trim());
        user.setEmail(createUserRequest.email().trim());
        User u = userRepository.save(user);

        return mapToAPIUser(u);
    }

    private static APIUser mapToAPIUser(User user) {
        return new APIUser(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                String.valueOf(user.getStorageUsed()),
                String.valueOf(user.getStorageLimit())
        );
    }


}
