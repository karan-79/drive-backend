package com.project.mydrive.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.project.mydrive.external.document.DocumentClient;
import com.project.mydrive.external.document.S3DocumentClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@Profile(value = "default")
public class AppConfig {


    @Value("${firebase.credentials.json}")
    private String credentialsJson;

    @Bean
    public FirebaseAuth firebaseAuth() throws IOException {

        ByteArrayInputStream serviceAccount = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));

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
