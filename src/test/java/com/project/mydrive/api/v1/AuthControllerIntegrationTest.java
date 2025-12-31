package com.project.mydrive.api.v1;

import com.google.firebase.auth.FirebaseToken;
import com.project.mydrive.BaseIntegrationTests;
import com.project.mydrive.api.v1.model.APIUser;
import com.project.mydrive.api.v1.model.FirebaseAuthRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class AuthControllerIntegrationTest extends BaseIntegrationTests {

    @Test
    void shouldRegisterUser() throws Exception {
        String uid = "testUid1";
        String email = "test1@example.com";
        String firstName = "Test";
        String lastName = "User";

        FirebaseToken mockToken = mockFirebaseToken(uid, email);
        when(firebaseAuth.verifyIdToken(mockToken.getUid())).thenReturn(mockToken);

        FirebaseAuthRequest authRequest = new FirebaseAuthRequest(mockToken.getUid(), firstName, lastName);

        APIUser apiUser = webTestClient.post().uri("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIUser.class)
                .returnResult().getResponseBody();

        assertThat(apiUser).isNotNull();
        assertThat(apiUser.email()).isEqualTo(email);
        assertThat(apiUser.firstName()).isEqualTo(firstName);
        assertThat(apiUser.lastName()).isEqualTo(lastName);
    }

    @Test
    void shouldLoginUser() {
        String uid = "testUid2";
        String email = "test2@example.com";
        String firstName = "Test";
        String lastName = "User";

        createTestUser(uid, email, firstName, lastName);

        webTestClient.post().uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new FirebaseAuthRequest(uid, null, null))
                .exchange()
                .expectStatus().isOk()
                .expectCookie().exists("jwt_token");
    }


    //    @Test
    void shouldReturnUserNotFoundWhenLoggingInWithNonExistentUser() throws Exception {
        String uid = "nonExistentUid";
        String email = "nonexistent@example.com";

        doReturn(mockFirebaseToken(uid, email))
                .when(firebaseAuth)
                .verifyIdToken(uid);

        webTestClient.post().uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new FirebaseAuthRequest(uid, null, null))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody() // Expect a JSON response for the error
                .jsonPath("$.error").isEqualTo("User Not Found");
    }

    //    @Test
    void shouldReturnFirebaseTokenExceptionWhenFirebaseTokenIsInvalid() throws Exception {
        String invalidToken = "invalidFirebaseToken";
//        doThrow(new FirebaseAuthException(ErrorCode.ALREADY_EXISTS, "invalid-id-token", "Invalid ID token"))
//                .when(firebaseAuth).verifyIdToken(invalidToken);

        webTestClient.post().uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new FirebaseAuthRequest(invalidToken, null, null))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Firebase Token Error");
    }

    @Test
    void shouldReturnUserAlreadyExistsWhenRegisteringExistingUser() throws Exception {
        String uid = "existingUid";
        String email = "existing@example.com";
        String firstName = "Existing";
        String lastName = "User";

        createTestUser(uid, email, firstName, lastName);

        // Try to register again
        webTestClient.post().uri("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new FirebaseAuthRequest(uid, firstName, lastName))
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody()
                .jsonPath("$.error").isEqualTo("User Already Exists");
    }

    @Test
    void shouldReturnUserNotFoundWhenCheckingAuthForNonExistentUser() throws Exception {
        String uid = "nonExistentUserForAuthCheck";
        String email = "nonexistentauth@example.com";

        // Mock Firebase to return a valid token, but the user won't be in our DB
        doReturn(mockFirebaseToken(uid, email)).when(firebaseAuth).verifyIdToken(uid);

        // Manually create a JWT for a non-existent user (as if it was generated before user deletion)
        String jwtToken = jwtUtils.generateToken(java.util.UUID.randomUUID().toString());

        webTestClient.get().uri("/v1/auth/me")
                .cookie("jwt_token", jwtToken)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("User Not Found");
    }
}
