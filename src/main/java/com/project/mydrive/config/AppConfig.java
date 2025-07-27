package com.project.mydrive.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.project.mydrive.external.document.DocumentClient;
import com.project.mydrive.external.document.S3DocumentClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@Profile(value = "default")
public class AppConfig {


    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    @PostConstruct
    public void init() throws IOException {
        FileInputStream serviceAccount = new FileInputStream(credentialsPath);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    public S3Client s3Client(
            @Value("${aws.bucket-name}") String bucketName,
            @Value("${aws.region}") String region
    ) {

        return S3Client.builder()
                // EC2 instance profile gets picked automatically
                .region(Region.EU_NORTH_1)
                .build();
    }

    @Bean
    public DocumentClient documentClient(@Value("${aws.bucket-name}") String bucketName, S3Client client) {
        return new S3DocumentClient(client, bucketName);
    }
}
