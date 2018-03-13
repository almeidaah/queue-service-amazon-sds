package com.example.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties("server")
public class ServerProperties {

    private String env;
    private Integer queueSize;

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

    @Override
    public String toString() {
        return "ServerProperties{" +
                "env='" + env + '\'' +
                "qSize='" + queueSize + '\'' +
                '}';
    }
}
