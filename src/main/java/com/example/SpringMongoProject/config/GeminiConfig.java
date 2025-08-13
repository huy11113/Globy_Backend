// PATH: src/main/java/com/example/SpringMongoProject/config/GeminiConfig.java
package com.example.SpringMongoProject.config;

import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {
    @Bean
    public Client geminiClient() {
        return new Client();
    }
}