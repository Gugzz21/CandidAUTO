package com.jobassistant;

import com.jobassistant.shared.PostgresUnixSocketBridge;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JobAssistantApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(JobAssistantApplication.class);
        app.addInitializers(new PostgresUnixSocketBridge());
        app.run(args);
    }
}
