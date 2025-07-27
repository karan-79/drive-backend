package com.project.mydrive.api.v1;

import com.project.mydrive.api.v1.model.APIUser;
import com.project.mydrive.api.v1.model.AuthResponse;
import com.project.mydrive.api.v1.model.FirebaseAuthRequest;
import com.project.mydrive.core.domain.User;
import com.project.mydrive.core.exception.UserNotFoundException;
import com.project.mydrive.core.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Value("${http-only-cookie.https-enabled:true}")
    private boolean httpsCookieScheme;

    @SneakyThrows
    @PostMapping("/register")
    public APIUser createUser(@RequestBody FirebaseAuthRequest request) {
        return userService.createUser(request);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody FirebaseAuthRequest request, HttpServletResponse response) {

        var token = userService.processLogin(request.idToken());

        var cookie = new Cookie("jwt_token", token);
        cookie.setHttpOnly(true);

        cookie.setSecure(httpsCookieScheme);
        cookie.setPath("/");
        cookie.setMaxAge(600); // 10hrs

        String cookieHeader = String.format(
                "jwt_token=%s; Path=%s; Max-Age=%d; HttpOnly; Secure; SameSite=" + (httpsCookieScheme ? "None" : "Lax"),
                cookie.getValue(),
                cookie.getPath(),
                cookie.getMaxAge());
        response.setHeader("Set-Cookie", cookieHeader);
        return ResponseEntity.ok(new AuthResponse(
                true,
                token,
                "Login Successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> checkAuth(@AuthenticationPrincipal User user, HttpServletRequest request) {

        if (!userService.doesUserExists(user.getId()))
            throw new UserNotFoundException("User with ID " + user.getId() + " does not exist.");

        String jwtToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt_token".equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    break;
                }
            }
        }

        return ResponseEntity.ok(
                new AuthResponse(
                        true,
                        jwtToken,
                        "User is authenticated"));

    }

}
