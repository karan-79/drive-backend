package com.project.mydrive.api.v1;

import com.project.mydrive.api.v1.model.APIUser;
import com.project.mydrive.api.v1.model.CreateUserRequest;
import com.project.mydrive.core.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public APIUser createUser(@RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

}
