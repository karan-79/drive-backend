package com.project.mydrive.core.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.project.mydrive.api.v1.model.APIUser;
import com.project.mydrive.api.v1.model.CreateUserRequest;
import com.project.mydrive.api.v1.model.FirebaseAuthRequest;
import com.project.mydrive.core.domain.User;
import com.project.mydrive.core.exception.FirebaseTokenException;
import com.project.mydrive.core.exception.UserAlreadyExistsException;
import com.project.mydrive.core.exception.UserNotFoundException;
import com.project.mydrive.core.repository.UserRepository;
import com.project.mydrive.utils.JwtUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DirectoryService directoryService;
    private final JwtUtils jwtUtils;
    private final FirebaseAuth firebaseAuth;

    private FirebaseToken getFireBaseToken(String idToken) {
        try {

            return firebaseAuth.verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            throw new FirebaseTokenException("Invalid Firebase ID token: " + e.getMessage());
        }
    }

    @Transactional
    public APIUser createUser(FirebaseAuthRequest request) {

        var decodedToken = getFireBaseToken(request.idToken());

        var email = decodedToken.getEmail();
        var firstName = request.firstName();
        var lastName = request.lastName();
        var uid = decodedToken.getUid();

        // TODO validate
        userRepository.findByEmail(email.trim())
                .ifPresent((u) -> {
                    throw new UserAlreadyExistsException("User already exists with email " + email);
                });

        var user = new User();
        user.setUId(uid);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        User u = userRepository.save(user);

        directoryService.createRootDirForUser(u);

        return mapToAPIUser(u);
    }

    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    private static APIUser mapToAPIUser(User user) {
        return new APIUser(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                String.valueOf(user.getStorageUsed()),
                String.valueOf(user.getStorageLimit())
        );
    }


    public String processLogin(String uId) {
        var decoded = getFireBaseToken(uId);

        var email = decoded.getEmail();

        var user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new UserNotFoundException("User does not exist with email " + email));

        return jwtUtils.generateToken(user.getId().toString());
    }

    public boolean doesUserExists(UUID userId) {
        return userRepository.findById(userId).isPresent();
    }

}
