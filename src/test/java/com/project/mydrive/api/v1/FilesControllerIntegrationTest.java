package com.project.mydrive.api.v1;

import com.project.mydrive.BaseIntegrationTests;
import com.project.mydrive.api.v1.model.APIFile;
import com.project.mydrive.api.v1.model.APIUser;
import com.project.mydrive.api.v1.model.UpdateFileRequest;
import com.project.mydrive.core.domain.Directory;
import com.project.mydrive.core.domain.User;
import com.project.mydrive.core.repository.DirectoryRepository;
import com.project.mydrive.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class FilesControllerIntegrationTest extends BaseIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DirectoryRepository directoryRepository;

    private User testUser;
    private Directory rootDirectory;

    private WebTestClient authenticatedWebTestClient;

    @BeforeEach
    void setupUserAndDirectory() throws Exception {
        super.setup();

        String uid = "filetestuser_" + System.currentTimeMillis();
        String email = "filetest_" + System.currentTimeMillis() + "@example.com";
        String firstName = "File";
        String lastName = "Test";

        APIUser apiUser = registerAndLoginUser(uid, email, firstName, lastName);
        authenticatedWebTestClient = getAuthenticatedWebTestClient(uid, email);

        testUser = userRepository.findById(apiUser.id()).orElseThrow();
        rootDirectory = directoryRepository.getDirectoryByOwnerAndParentDirectoryIsNull(testUser);
    }

    @Test
    void shouldUploadFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello, World!".getBytes());

        authenticatedWebTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
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
                });
    }

    @Test
    void shouldDownloadFile() {
        // Upload file
        MockMultipartFile file = new MockMultipartFile("file", "download_test.txt", "text/plain", "Downloadable content.".getBytes());

        APIFile uploadedFile = authenticatedWebTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
                        .queryParam("parentDirId", rootDirectory.getId())
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(toMultipartData(file)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIFile.class)
                .returnResult().getResponseBody();

        authenticatedWebTestClient.get().uri("/v1/files/{blobRef}", uploadedFile.blobRef())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class)
                .isEqualTo("Downloadable content.");
    }

    @Test
    void shouldUpdateFile() {
        MockMultipartFile file = new MockMultipartFile("file", "update_test.txt", "text/plain", "Content to be updated.".getBytes());

        APIFile uploadedFile = authenticatedWebTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
                        .queryParam("parentDirId", rootDirectory.getId())
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(toMultipartData(file)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIFile.class)
                .returnResult().getResponseBody();

        UpdateFileRequest updateRequest = new UpdateFileRequest("updated_name.txt", rootDirectory.getId());

        authenticatedWebTestClient.put().uri("/v1/files/{fileId}", uploadedFile.id())
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
            authenticatedWebTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
                            .queryParam("parentDirId", rootDirectory.getId())
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(toMultipartData(file)))
                    .exchange()
                    .expectStatus().isOk();
        }

        authenticatedWebTestClient.get().uri(uriBuilder -> uriBuilder.path("/v1/files")
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

        authenticatedWebTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
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

        authenticatedWebTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
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
        UUID nonExistentBlobRef = UUID.randomUUID();

        authenticatedWebTestClient.get().uri("/v1/files/{blobRef}", nonExistentBlobRef)
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
        registerAndLoginUser(otherUid, otherEmail, "Other", "User");
        WebTestClient otherUserClient = getAuthenticatedWebTestClient(otherUid, otherEmail);

        MockMultipartFile otherUserFile = new MockMultipartFile("file", "other_file.txt", "text/plain", "Content from other user.".getBytes());
        APIFile uploadedFile = otherUserClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
                        .queryParam("parentDirId", directoryRepository.getDirectoryByOwnerAndParentDirectoryIsNull(userRepository.findByEmail(otherEmail).orElseThrow()).getId())
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(toMultipartData(otherUserFile)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIFile.class)
                .returnResult().getResponseBody();

        // Try to download the other user's file with the current authenticated user
        authenticatedWebTestClient.get().uri("/v1/files/{blobRef}", uploadedFile.blobRef())
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Unauthorized File Access");
    }

    @Test
    void shouldReturnFileNotFoundExceptionWhenUpdatingNonExistentFile() throws Exception {
        Long nonExistentFileId = 9999L;
        UpdateFileRequest updateRequest = new UpdateFileRequest("new_name.txt", rootDirectory.getId());

        authenticatedWebTestClient.put().uri("/v1/files/{fileId}", nonExistentFileId)
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
        APIFile uploadedFile = authenticatedWebTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/files")
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

        authenticatedWebTestClient.put().uri("/v1/files/{fileId}", uploadedFile.id())
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

        authenticatedWebTestClient.get().uri(uriBuilder -> uriBuilder.path("/v1/files")
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