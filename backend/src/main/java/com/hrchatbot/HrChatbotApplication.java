package com.hrchatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class HrChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrChatbotApplication.class, args);
    }
}
