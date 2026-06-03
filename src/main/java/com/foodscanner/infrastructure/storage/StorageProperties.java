package com.foodscanner.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Слой: infrastructure
 * Конфигурация объектного хранилища (профиль storage.*).
 */
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** Провайдер: minio | s3 (сейчас реализован minio). */
    private String provider = "minio";

    private final Minio minio = new Minio();

    public String getProvider()   { return provider; }
    public void   setProvider(String v) { this.provider = v; }
    public Minio  getMinio()      { return minio; }

    public static class Minio {
        private String endpoint  = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private String bucket    = "food-scanner-photos";

        public String getEndpoint()  { return endpoint; }
        public String getAccessKey() { return accessKey; }
        public String getSecretKey() { return secretKey; }
        public String getBucket()    { return bucket; }
        public void setEndpoint(String v)  { this.endpoint = v; }
        public void setAccessKey(String v) { this.accessKey = v; }
        public void setSecretKey(String v) { this.secretKey = v; }
        public void setBucket(String v)    { this.bucket = v; }
    }
}
