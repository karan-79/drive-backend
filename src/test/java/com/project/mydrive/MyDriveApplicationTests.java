package com.project.mydrive;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Testcontainers
class MyDriveApplicationTests {

	@Container
	public static PostgreSQLContainer<?> testcontainer = new PostgreSQLContainer<>("postgres:17")
			.withDatabaseName("drive_clone")
			.withUsername("postgres")
			.withPassword("postgres");

//	@Test
	void contextLoads() {
	}

}
