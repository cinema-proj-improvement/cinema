package com.elice.cinema.global.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    private Storage storage = new Storage();
    private Upload upload = new Upload();

    @Getter @Setter
    public static class Storage {
        private String type;         // local | s3
        private String s3Bucket;
        private String s3Region;
        private String imageBaseUrl; // 이미지 URL prefix: local=/uploads, s3=CDN 도메인
    }

    @Getter @Setter
    public static class Upload {
        private String basePath;   // file.upload.base-path
        private String urlPrefix;  // file.upload.url-prefix
    }
}