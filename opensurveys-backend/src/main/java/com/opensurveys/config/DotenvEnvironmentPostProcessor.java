package com.opensurveys.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads {@code .env} into the Spring {@link ConfigurableEnvironment} before
 * {@code application.properties} placeholders are resolved. Runs for both
 * {@code main} and {@code @SpringBootTest} (unlike code in {@code main} alone).
 * Registered in {@code META-INF/spring.factories} for Spring Boot 4.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final String PROPERTY_SOURCE_NAME = "dotenv";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
			return;
		}

		Path envDir = resolveEnvDirectory();
		Dotenv dotenv = Dotenv.configure()
				.directory(envDir.toString())
				.ignoreIfMissing()
				.load();

		Map<String, Object> values = new HashMap<>();
		dotenv.entries().forEach(entry -> {
			String key = entry.getKey();
			// Prefer real OS env / already-set Spring properties over .env.
			if (environment.getProperty(key) == null && System.getenv(key) == null) {
				values.put(key, entry.getValue());
			}
		});

		if (!values.isEmpty()) {
			environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, values));
		}
	}

	/**
	 * JAR directory when packaged; module root when running from {@code target/classes}
	 * (Maven tests / IDE); otherwise the process working directory.
	 */
	private static Path resolveEnvDirectory() {
		try {
			URI location = DotenvEnvironmentPostProcessor.class
					.getProtectionDomain()
					.getCodeSource()
					.getLocation()
					.toURI();
			Path path = Path.of(location);
			if (path.toString().endsWith(".jar")) {
				Path parent = path.getParent();
				if (parent != null) {
					return parent;
				}
			}
			// target/classes -> module root (where .env lives for local dev / tests)
			if ("classes".equals(path.getFileName() != null ? path.getFileName().toString() : null)) {
				Path target = path.getParent();
				if (target != null && "target".equals(target.getFileName().toString())) {
					Path moduleRoot = target.getParent();
					if (moduleRoot != null && Files.isRegularFile(moduleRoot.resolve(".env"))) {
						return moduleRoot;
					}
				}
			}
		} catch (Exception ignored) {
			// Fall through to working directory.
		}
		return Path.of(System.getProperty("user.dir"));
	}
}
