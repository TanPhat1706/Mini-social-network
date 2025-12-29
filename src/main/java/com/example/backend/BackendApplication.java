package com.example.backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableJpaAuditing
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@PostConstruct
    public void init() {
        // Thiết lập múi giờ mặc định là Asia/Ho_Chi_Minh (GMT+7)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        System.out.println("Spring boot application running in UTC+7 timezone :" + new java.util.Date());
    }
}
