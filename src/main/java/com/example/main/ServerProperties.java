package com.example.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("server")
public class ServerProperties {

    private String env;
    private Integer queueSize;
    private Long visibilityTimeout;

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public Integer getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
    }

    public Long getVisibilityTimeout() {
        return visibilityTimeout;
    }

    public void setVisibilityTimeout(Long visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
    }

    @Override
    public String toString() {
        return String.format("ServerProperties{ envvironment = %s, queueSize = %d, visibilityTimeout = %d", env, queueSize, visibilityTimeout);
    }
}
