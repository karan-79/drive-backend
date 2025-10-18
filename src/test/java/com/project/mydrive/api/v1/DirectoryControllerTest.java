package com.project.mydrive.api.v1;

import com.project.mydrive.BaseIntegrationTests;
import com.project.mydrive.api.v1.model.APIDirectory;
import com.project.mydrive.api.v1.model.APIUser;
import com.project.mydrive.api.v1.model.CreateDirectoryRequest;
import com.project.mydrive.core.domain.Directory;
import com.project.mydrive.core.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoryControllerTest extends BaseIntegrationTests {


    private User testUser;
    private Directory rootDirectory;

    private WebTestClient authenticatedWebTestClient;

    @BeforeEach
    void setupUserAndDirectory() throws Exception {
        super.setup();

        String uid = "dirtestuser_" + System.currentTimeMillis();
        String email = "dirtest_" + System.currentTimeMillis() + "@example.com";
        String firstName = "Dir";
        String lastName = "Test";

        APIUser apiUser = registerAndLoginUser(uid, email, firstName, lastName);
        authenticatedWebTestClient = getAuthenticatedWebTestClient(uid, email);

        testUser = userRepository.findById(apiUser.id()).orElseThrow();
        rootDirectory = directoryRepository.getDirectoryByOwnerAndParentDirectoryIsNullAndIsDeletedIsFalse(testUser);
    }

    @Test
    void createDir() {
        CreateDirectoryRequest request = new CreateDirectoryRequest("new-dir", rootDirectory.getId());

        authenticatedWebTestClient.post().uri("/v1/directories")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIDirectory.class)
                .value(apiDirectory -> {
                    assertThat(apiDirectory.name()).isEqualTo("new-dir");
                    assertThat(apiDirectory.parentDirId()).isEqualTo(rootDirectory.getId());
                    assertTrue(directoryRepository.findById(apiDirectory.id()).isPresent());
                });
    }

    @Test
    void createDir_shouldFailForNonExistentParent() {
        CreateDirectoryRequest request = new CreateDirectoryRequest("new-dir", 9999L);

        authenticatedWebTestClient.post().uri("/v1/directories")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateDir_shouldRename() {
        // Create a directory first
        APIDirectory createdDir = createDirectory("dir-to-rename", rootDirectory.getId());

        CreateDirectoryRequest updateRequest = new CreateDirectoryRequest("renamed-dir", rootDirectory.getId());

        authenticatedWebTestClient.put().uri("/v1/directories/{dirId}", createdDir.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIDirectory.class)
                .value(apiDirectory -> {
                    assertThat(apiDirectory.name()).isEqualTo("renamed-dir");
                    assertThat(apiDirectory.id()).isEqualTo(createdDir.id());
                });

        Directory updatedDir = directoryRepository.findById(createdDir.id()).orElseThrow();
        assertThat(updatedDir.getName()).isEqualTo("renamed-dir");
    }

    @Test
    void updateDir_shouldMove() {
        APIDirectory dir1 = createDirectory("dir1", rootDirectory.getId());
        APIDirectory dir2 = createDirectory("dir2", rootDirectory.getId());

        CreateDirectoryRequest updateRequest = new CreateDirectoryRequest(dir2.name(), dir1.id());

        authenticatedWebTestClient.put().uri("/v1/directories/{dirId}", dir2.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIDirectory.class)
                .value(apiDirectory -> {
                    assertThat(apiDirectory.parentDirId()).isEqualTo(dir1.id());
                });

        Directory movedDir = directoryRepository.findById(dir2.id()).orElseThrow();
        assertThat(movedDir.getParentDirectory().getId()).isEqualTo(dir1.id());
    }

    @Test
    void updateDir_shouldFailForNonExistentDir() {
        CreateDirectoryRequest updateRequest = new CreateDirectoryRequest("new-name", rootDirectory.getId());
        authenticatedWebTestClient.put().uri("/v1/directories/{dirId}", 9999L)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getDirs() {
        APIDirectory dir1 = createDirectory("dir1", rootDirectory.getId());
        APIDirectory dir2 = createDirectory("dir2", rootDirectory.getId());

        authenticatedWebTestClient.get().uri(uriBuilder -> uriBuilder.path("/v1/directories")
                        .queryParam("parentDirId", rootDirectory.getId())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(APIDirectory.class)
                .hasSize(2)
                .value(apiDirectories -> assertThat(apiDirectories).extracting(APIDirectory::name).containsExactlyInAnyOrder("dir1", "dir2"));
    }

    @Test
    void getDirs_shouldReturnEmptyListForEmptyDirectory() {
        APIDirectory newDir = createDirectory("new-empty-dir", rootDirectory.getId());

        authenticatedWebTestClient.get().uri(uriBuilder -> uriBuilder.path("/v1/directories")
                        .queryParam("parentDirId", newDir.id())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(APIDirectory.class)
                .hasSize(0);
    }

    @Test
    void getAllDirs() {
        APIDirectory dir1 = createDirectory("dir1", rootDirectory.getId());
        APIDirectory dir1_1 = createDirectory("dir1_1", dir1.id());

        authenticatedWebTestClient.get().uri("/v1/directories/all")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(APIDirectory.class)
                .value(apiDirectories -> {
                    // Should contain root ("My Drive"), dir1, and dir1_1
                    assertThat(apiDirectories).hasSize(3);
                    assertThat(apiDirectories).extracting(APIDirectory::name)
                            .containsExactlyInAnyOrder("My Drive", "dir1", "dir1_1");
                });
    }

    @Test
    void deleteDir() {
        APIDirectory dirToDelete = createDirectory("to-delete", rootDirectory.getId());

        authenticatedWebTestClient.delete().uri("/v1/directories/{dirId}", dirToDelete.id())
                .exchange()
                .expectStatus().isOk();

        Directory deletedDir = directoryRepository.findById(dirToDelete.id()).orElseThrow();
        assertTrue(deletedDir.isDeleted());

        // Verify it's not returned by getDirs
        authenticatedWebTestClient.get().uri(uriBuilder -> uriBuilder.path("/v1/directories")
                        .queryParam("parentDirId", rootDirectory.getId())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(APIDirectory.class)
                .value(apiDirectories -> assertThat(apiDirectories).extracting(APIDirectory::id).doesNotContain(dirToDelete.id()));
    }

    @Test
    void deleteDir_shouldFailForRootDir() {
        authenticatedWebTestClient.delete().uri("/v1/directories/{dirId}", rootDirectory.getId())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void deleteDir_shouldFailForNonExistentDir() {
        authenticatedWebTestClient.delete().uri("/v1/directories/{dirId}", 9999L)
                .exchange()
                .expectStatus().isNotFound();
    }

    private APIDirectory createDirectory(String name, Long parentId) {
        CreateDirectoryRequest request = new CreateDirectoryRequest(name, parentId);
        return authenticatedWebTestClient.post().uri("/v1/directories")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(APIDirectory.class)
                .returnResult()
                .getResponseBody();
    }
}
