package com.opensurveys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the OpenSurveys API (auth, forms, answers, SPA forwarding).
 * Local secrets come from {@code .env}, loaded by
 * {@link com.opensurveys.config.DotenvEnvironmentPostProcessor} so both {@code main}
 * and Maven/{@code @SpringBootTest} resolve {@code application.properties} placeholders.
 */
@SpringBootApplication
public class OpenSurveysApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenSurveysApplication.class, args);
	}
}
