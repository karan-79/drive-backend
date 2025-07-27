package com.project.mydrive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.project.mydrive.api.v1.model.APIUser;
import com.project.mydrive.api.v1.model.FirebaseAuthRequest;
import com.project.mydrive.config.TestConfig;
import com.project.mydrive.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestConfig.class)
public class BaseIntegrationTests {

    @Value("${local.server.port}")
    protected int port;

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected FirebaseAuth firebaseAuth;

    @Autowired
    protected JwtUtils jwtUtils;

    @BeforeEach
    public void setup() {
        this.webTestClient = webTestClient.mutate()
                .baseUrl("http://localhost:" + port + "/drive")
                .build();
    }

    protected FirebaseToken mockFirebaseToken(String uid, String email) {
        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn(uid);
        when(mockToken.getEmail()).thenReturn(email);
        return mockToken;
    }

    protected WebTestClient getAuthenticatedWebTestClient(String uid, String email) throws Exception {
        FirebaseToken mockToken = mockFirebaseToken(uid, email);
        when(firebaseAuth.verifyIdToken(mockToken.getUid())).thenReturn(mockToken);

        FirebaseAuthRequest authRequest = new FirebaseAuthRequest(mockToken.getUid(), "", "");

        String jwtToken = webTestClient.post().uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult().getResponseBody().get("token").toString();

        return webTestClient.mutate()
                .defaultCookie("jwt_token", jwtToken)
                .build();
    }

    protected APIUser registerAndLoginUser(String uid, String email, String firstName, String lastName) throws Exception {
        FirebaseToken mockToken = mockFirebaseToken(uid, email);
        when(firebaseAuth.verifyIdToken(mockToken.getUid())).thenReturn(mockToken);

        FirebaseAuthRequest authRequest = new FirebaseAuthRequest(mockToken.getUid(), firstName, lastName);

        return webTestClient.post().uri("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIUser.class)
                .returnResult().getResponseBody();
    }

}