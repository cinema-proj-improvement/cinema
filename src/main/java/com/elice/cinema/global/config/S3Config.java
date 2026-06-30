package com.elice.cinema.global.config;

import com.elice.cinema.global.config.properties.FileProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "s3")
public class S3Config {

    @Bean
    public S3Client s3Client(FileProperties fileProperties) {
        return S3Client.builder()
                .region(Region.of(fileProperties.getStorage().getS3Region()))
                .build();
    }
}
