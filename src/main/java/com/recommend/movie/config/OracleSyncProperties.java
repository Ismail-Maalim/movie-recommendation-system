package com.recommend.movie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "oracle.apex")
public class OracleSyncProperties {

    private final Api api = new Api();
    private final Sync sync = new Sync();

    public Api getApi() {
        return api;
    }

    public String getApiUrl() {
        return api.getUrl();
    }

    public Sync getSync() {
        return sync;
    }

    public static class Api {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class Sync {
        private boolean enabled = true;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 10000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }
}
