# Drive Backend

This is the backend component for the Drive application, built using Spring Boot. It provides RESTful APIs for user authentication, directory management, and file operations.

## Project Overview

The Drive application consists of two main parts:
- **Frontend (drive):** A Next.js application responsible for the user interface.
- **Backend (drive-backend):** A Spring Boot application that handles business logic, data storage, and integration with external services like Firebase for authentication and S3 for file storage.

## Test Architecture

The `drive-backend` project utilizes integration tests to ensure the correct functioning of its APIs and services. The testing setup is designed to provide a realistic environment while isolating external dependencies.

### Key Components:

1.  **`BaseIntegrationTests.java`**:
    *   Serves as the base class for all integration tests.
    *   Configured with `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` to start the application on a random port, preventing port conflicts during parallel test execution.
    *   Uses `@ActiveProfiles("test")` to activate a test-specific Spring profile, allowing for different bean configurations (e.g., mocking external services).
    *   Injects `WebTestClient` for making HTTP requests to the running application.
    *   **Firebase Authentication Mocking**: It integrates with `TestSecurityConfig` to mock `FirebaseAuth`. This is crucial for testing authentication-related endpoints without relying on an actual Firebase project. The `firebaseAuth` bean is mocked using Mockito, allowing test methods to control the behavior of `verifyIdToken()`.
    *   **JWT Handling**: Provides helper methods (`mockFirebaseToken`, `getAuthenticatedWebTestClient`, `registerAndLoginUser`) to:
        *   Generate mock `FirebaseToken` instances with controlled UIDs and emails.
        *   Simulate user registration and login flows.
        *   Obtain and use JWTs in subsequent authenticated requests by setting them as cookies in the `WebTestClient`.

2.  **`TestSecurityConfig.java`**:
    *   A `@TestConfiguration` class that provides a `@Primary` mocked `FirebaseAuth` bean. This ensures that during tests, the application uses our mocked Firebase instance instead of attempting to connect to a real one.

3.  **Controller Integration Tests (e.g., `AuthControllerIntegrationTest.java`, `FilesControllerIntegrationTest.java`)**:
    *   Extend `BaseIntegrationTests` to leverage the shared testing infrastructure.
    *   Use the `authenticatedWebTestClient` (a `WebTestClient` instance pre-configured with a JWT for an authenticated user) to make requests to protected endpoints.
    *   **Exception Testing**: Includes dedicated test cases to verify that the custom exceptions (e.g., `UserNotFoundException`, `DirectoryNotFoundException`, `EmptyFileException`, `UnauthorizedFileAccessException`) are correctly thrown by the service layer and translated into appropriate HTTP error responses by the `GlobalExceptionHandler`. These tests assert on the HTTP status code and the error message in the response body.

### Dependencies Used in Testing:

*   **Spring Boot Test**: Provides the core testing framework for Spring Boot applications.
*   **WebTestClient**: A non-blocking, reactive client for testing RESTful APIs. Ideal for integration tests.
*   **JUnit 5**: The testing framework used for writing test cases (`@Test`, `@BeforeEach`, etc.).
*   **AssertJ**: A fluent assertion library used for writing more readable and expressive assertions (`assertThat`).
*   **Mockito**: Used for mocking the `FirebaseAuth` dependency, allowing us to control its behavior and isolate our application logic during tests.
*   **Testcontainers**: Enables the use of Docker containers for integration tests (e.g., for a real database or S3 mock), ensuring a consistent and isolated testing environment.

This test architecture ensures that the application's core functionalities are thoroughly tested, including authentication and error handling, in an environment that closely resembles production while maintaining test isolation and efficiency.
