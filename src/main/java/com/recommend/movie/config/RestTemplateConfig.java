package com.recommend.movie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private final OracleSyncProperties properties;

    public RestTemplateConfig(OracleSyncProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "oracleRestTemplate")
    public RestTemplate oracleRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getSync().getConnectTimeoutMs());
        factory.setReadTimeout(properties.getSync().getReadTimeoutMs());
        return new RestTemplate(factory);
    }
}
