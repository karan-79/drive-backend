package com.project.mydrive.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.project.mydrive.external.document.DocumentClient;
import com.project.mydrive.external.document.S3DocumentClient;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;

@Configuration
@Profile("local")
public class LocalAppConfig {


    @Value("${firebase.credentials.path}")
    private String credentialsPath;


    @Bean
    public FirebaseAuth firebaseAuth() throws IOException {

        FileInputStream serviceAccount = new FileInputStream(credentialsPath);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
        var apps = FirebaseApp.getApps();
        return apps.isEmpty()
                ? FirebaseAuth.getInstance(FirebaseApp.initializeApp(options))
                : FirebaseAuth.getInstance(apps.get(0));
    }


    @Bean
    public S3Client s3Client(
            @Value("${localstack.s3.endpoint}") String s3Endpoint,
            @Value("${localstack.s3.bucket-name}") String bucketName,
            @Value("${localstack.s3.aws.region}") String region
    ) {

        var uri = URI.create(s3Endpoint);

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

    @Bean
    public DocumentClient documentClient(@Value("${localstack.s3.bucket-name}") String bucketName, S3Client client) {
        return new S3DocumentClient(client, bucketName);
    }


    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
