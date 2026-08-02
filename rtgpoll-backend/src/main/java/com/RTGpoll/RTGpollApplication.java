package com.RTGpoll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the RTGpoll API (auth, forms, answers, SPA forwarding).
 * Local secrets come from {@code .env}, loaded by
 * {@link com.RTGpoll.config.DotenvEnvironmentPostProcessor} so both {@code main}
 * and Maven/{@code @SpringBootTest} resolve {@code application.properties} placeholders.
 */
@SpringBootApplication
public class RTGpollApplication {

	public static void main(String[] args) {
		SpringApplication.run(RTGpollApplication.class, args);
	}
}
