package com.project.mydrive.config;

import com.google.firebase.auth.FirebaseAuth;
import com.project.mydrive.external.document.DocumentClient;
import com.project.mydrive.external.document.S3DocumentClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import javax.sql.DataSource;
import java.net.URI;
import java.time.Duration;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public FirebaseAuth firebaseAuth() {
        return mock(FirebaseAuth.class);
    }

    @Bean
    public DataSource getDs(Environment environment) {
        var container = new PostgreSQLContainer<>("postgres:latest")
                .withDatabaseName("testdb")
                .withUsername("sa")
                .withStartupTimeout(Duration.ofSeconds(30))
                .withPassword("test");
        container.start();

        return DataSourceBuilder
                .create()
                .url(container.getJdbcUrl())
                .username(container.getUsername())
                .password(container.getPassword())
                .build();
    }

    @Bean
    public S3Client s3Client(@Value("${aws.bucket-name}") String bucketName) {

        var uri = startLocalStack();

        var s3Client = S3Client.builder()
                // EC2 instance profile gets picked automatically
                .endpointOverride(uri)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .region(Region.EU_NORTH_1)
                .build();

        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName).build());
            System.out.println("-------- Bucket created Successfully !!! -----------");
        } catch (BucketAlreadyExistsException ex) {
            System.out.println("-------- Bucket already exists, just continue !!! -----------");
        } catch (Exception e) {
            System.err.println("❌ Failed to create test bucket: " + e.getMessage());
        }

        return s3Client;
    }

    private URI startLocalStack() {
        var container = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
                .withServices(LocalStackContainer.Service.S3);

        container.start();

        return container.getEndpoint();
    }

    @Bean
    public DocumentClient documentClient(@Value("${aws.bucket-name}") String bucketName, S3Client client) {
        return new S3DocumentClient(client, bucketName);
    }
}
