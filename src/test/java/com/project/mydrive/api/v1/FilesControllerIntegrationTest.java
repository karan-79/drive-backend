package com.project.mydrive.api.v1;

import com.project.mydrive.BaseIntegrationTests;
import com.project.mydrive.api.v1.model.APIFile;
import com.project.mydrive.api.v1.model.UpdateFileRequest;
import com.project.mydrive.core.domain.Directory;
import com.project.mydrive.core.domain.User;
import org.assertj.core.internal.Bytes;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilesControllerIntegrationTest extends BaseIntegrationTests {


    private User testUser;
    private Directory rootDirectory;

    @BeforeEach
    void setupUserAndDirectory() throws Exception {
        super.setup();

        String uid = "filetestuser_" + System.currentTimeMillis();
        String email = "filetest_" + System.currentTimeMillis() + "@example.com";
        String firstName = "File";
        String lastName = "Test";

        // if present then test user is already logged in.
        if (userRepository.findByuId(uid).isPresent()) return;

        var token = registerAndLoginUser(uid, email, firstName, lastName);

        this.webTestClient = webTestClient.mutate()
                .defaultCookie("jwt_token", token)
                .build();

        testUser = userRepository.findByuId(uid).orElseThrow();
        rootDirectory = directoryRepository.getDirectoryByOwnerAndParentDirectoryIsNullAndIsDeletedIsFalse(testUser);
    }

    @Test
    void shouldUploadFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello, World!".getBytes());

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
                        .queryParam("parentDirId", rootDirectory.getId())
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(toMultipartData(file)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIFile.class)
                .value(apiFile -> {
                    assertThat(apiFile.name()).isEqualTo("test.txt");
                    assertThat(apiFile.size()).isEqualTo(13L);
                    assertTrue(fileRepository.findById(apiFile.id()).isPresent());
                });
    }

    @Test
    void shouldDownloadFile() {
        // Upload file
        MockMultipartFile file = new MockMultipartFile("file", "download_test.txt", "text/plain", "Downloadable content.".getBytes());

        APIFile uploadedFile = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
                        .queryParam("parentDirId", rootDirectory.getId())
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(toMultipartData(file)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIFile.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(uploadedFile);

        webTestClient.get().uri("/v1/files/{fileId}/download", uploadedFile.id())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectHeader().cacheControl(CacheControl.maxAge(Duration.ofDays(20)).cachePrivate().immutable())
                .expectHeader().contentDisposition(ContentDisposition.attachment().filename(uploadedFile.name()).build())
                .expectBody(String.class)
                .isEqualTo("Downloadable content.");
    }


    @Test
    void shouldPreview() {
        // Upload file
        byte[] bytes = createImage("jpeg");
        MockMultipartFile file = new MockMultipartFile("file", "preview_test.jpeg", "image/jpeg", bytes);

        APIFile uploadedFile = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
                        .queryParam("parentDirId", rootDirectory.getId())
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(toMultipartData(file)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIFile.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(uploadedFile);

        webTestClient.get().uri("/v1/files/{fileId}/preview", uploadedFile.id())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_JPEG_VALUE)
                .expectHeader().contentDisposition(ContentDisposition.inline().filename(uploadedFile.name()).build())
                .expectHeader().cacheControl(CacheControl.maxAge(Duration.ofDays(20)).cachePrivate().immutable())
                .expectBody(byte[].class)
                .isEqualTo(bytes);
    }

    @Test void  testThumbnailGenerates() throws InterruptedException {
        byte[] bytes = createImage("jpeg");
        MockMultipartFile file = new MockMultipartFile("file", "preview_test.jpeg", "image/jpeg", bytes);

        var uploadedFile = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
                        .queryParam("parentDirId", rootDirectory.getId())
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(toMultipartData(file)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIFile.class)
                .value(apiFile -> {
                    assertThat(apiFile.thumbnailUrl()).isNull(); //as it won't be returned in first call
                }).returnResult().getResponseBody();

        Assertions.assertNotNull(uploadedFile);
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            webTestClient.get().uri("/v1/files/{fileId}/thumbnail", uploadedFile.id())
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentDisposition(ContentDisposition.inline().filename(uploadedFile.name()).build())
                    .expectHeader().cacheControl(CacheControl.maxAge(Duration.ofDays(20)).cachePrivate().immutable())
                    .expectHeader().contentType(MediaType.IMAGE_PNG)
                    .expectBody(byte[].class)
                    .value(body -> {
                        assertThat(body.length).isLessThan(bytes.length);
                    });
        });
    }

    @Test
    void fileLoadingEndpointThrows_IllegalArgument_when_invalidAction() {
        var invalidAction = "destroy";
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Argument is invalid");
        errorResponse.put("message", "Action not supported for string: " + invalidAction);

        webTestClient.get().uri("/v1/files/{fileId}/{action}", 213, invalidAction)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map.class)
                .isEqualTo(errorResponse);
    }

    private byte[] createImage(String ext) {
        try {
            var image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
            var os = new ByteArrayOutputStream();
            ImageIO.write(image, ext, os);
            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldUpdateFile() {
        MockMultipartFile file = new MockMultipartFile("file", "update_test.txt", "text/plain", "Content to be updated.".getBytes());

        APIFile uploadedFile = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
                        .queryParam("parentDirId", rootDirectory.getId())
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(toMultipartData(file)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIFile.class)
                .returnResult().getResponseBody();

        UpdateFileRequest updateRequest = new UpdateFileRequest("updated_name.txt", rootDirectory.getId());

        webTestClient.put().uri("/v1/files/{fileId}", uploadedFile.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIFile.class)
                .value(apiFile -> {
                    assertThat(apiFile.name()).isEqualTo("updated_name.txt");
                });
    }

    @Test
    void shouldGetFilesUnderDirectory() {
        MockMultipartFile file1 = new MockMultipartFile("file", "file1.txt", "text/plain", "Content of file 1.".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "file2.txt", "text/plain", "Content of file 2.".getBytes());

        for (MockMultipartFile file : List.of(file1, file2)) {
            webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
                            .queryParam("parentDirId", rootDirectory.getId())
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(toMultipartData(file)))
                    .exchange()
                    .expectStatus().isOk();
        }

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/v1/files")
                        .queryParam("parentDirId", rootDirectory.getId())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(APIFile.class)
                .hasSize(2)
                .value(apiFiles -> assertThat(apiFiles)
                        .extracting(APIFile::name)
                        .containsExactlyInAnyOrder("file1.txt", "file2.txt"));
    }

    // Exception Tests
    @Test
    void shouldReturnEmptyFileExceptionWhenUploadingEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
                        .queryParam("parentDirId", rootDirectory.getId())
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(toMultipartData(emptyFile)))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Empty File");
    }

    @Test
    void shouldReturnDirectoryNotFoundExceptionWhenUploadingToNonExistentDirectory() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello, World!".getBytes());
        Long nonExistentDirId = 9999L;

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
                        .queryParam("parentDirId", nonExistentDirId)
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(toMultipartData(file)))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Directory Not Found");
    }

    @Test
    void shouldReturnFileNotFoundExceptionWhenDownloadingNonExistentFile() throws Exception {

        webTestClient.get().uri("/v1/files/{fileId}/download", 999969)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("File Not Found");
    }


    @Test
    void shouldReturnUnauthorizedFileAccessExceptionWhenDownloadingOthersFile() throws Exception {
        // Create a file with another user
        String otherUid = "otherUser_" + System.currentTimeMillis();
        String otherEmail = "other_" + System.currentTimeMillis() + "@example.com";

        var token = registerAndLoginUser(otherUid, otherEmail, "Other", "User");

        WebTestClient otherUserClient = this.cleanWebTestClient.mutate()
                .baseUrl("http://localhost:" + port + "/drive")
                .defaultCookie("jwt_token", token)
                .build();

        MockMultipartFile otherUserFile = new MockMultipartFile("file", "other_file.txt", "text/plain", "Content from other user.".getBytes());
        APIFile uploadedFile = otherUserClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
                        .queryParam("parentDirId", directoryRepository.getDirectoryByOwnerAndParentDirectoryIsNullAndIsDeletedIsFalse(userRepository.findByEmail(otherEmail).orElseThrow()).getId())
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(toMultipartData(otherUserFile)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIFile.class)
                .returnResult().getResponseBody();

        // Try to download the other user's file with the current authenticated user
        webTestClient.get().uri("/v1/files/{fileId}/download", uploadedFile.id())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("File Not Found");
    }

    @Test
    void shouldReturnFileNotFoundExceptionWhenUpdatingNonExistentFile() throws Exception {
        Long nonExistentFileId = 9999L;
        UpdateFileRequest updateRequest = new UpdateFileRequest("new_name.txt", rootDirectory.getId());

        webTestClient.put().uri("/v1/files/{fileId}", nonExistentFileId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("File Not Found");
    }

    @Test
    void shouldReturnDirectoryNotFoundExceptionWhenUpdatingFileWithNonExistentParentDirectory() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "update_parent.txt", "text/plain", "Content.".getBytes());
        APIFile uploadedFile = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
                        .queryParam("parentDirId", rootDirectory.getId())
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(toMultipartData(file)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIFile.class)
                .returnResult().getResponseBody();

        Long nonExistentDirId = 9999L;
        UpdateFileRequest updateRequest = new UpdateFileRequest("update_parent.txt", nonExistentDirId);

        webTestClient.put().uri("/v1/files/{fileId}", uploadedFile.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Directory Not Found");
    }

    @Test
    void shouldReturnDirectoryNotFoundExceptionWhenGettingFilesUnderNonExistentDirectory() throws Exception {
        Long nonExistentDirId = 9999L;

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/v1/files")
                        .queryParam("parentDirId", nonExistentDirId)
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Directory Not Found");
    }

    private MultiValueMap<String, HttpEntity<?>> toMultipartData(MockMultipartFile file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.getContentType()));
        headers.setContentDispositionFormData("file", file.getOriginalFilename());
        ByteArrayResource resource = null;
        try {

            resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

        } catch (Exception e) {
            e.printStackTrace();
        }

        HttpEntity<ByteArrayResource> entity = new HttpEntity<>(resource, headers);
        MultiValueMap<String, HttpEntity<?>> map = new LinkedMultiValueMap<>();
        map.add("file", entity);
        return map;
    }
}