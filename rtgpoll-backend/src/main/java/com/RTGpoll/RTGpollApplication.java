package com.RTGpoll;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;
import java.nio.file.Path;

/**
 * Spring Boot entry point for the RTGpoll API (auth, forms, answers, SPA forwarding).
 * Loads a local {@code .env} into system properties so {@code application.properties}
 * can resolve secrets via {@code ${...}} without committing them to git.
 * When running from a JAR, {@code .env} is read from the same folder as the JAR;
 * otherwise (IDE / {@code spring-boot:run}) it uses the process working directory.
 */
@SpringBootApplication
public class RTGpollApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(RTGpollApplication.class, args);
	}

	private static void loadDotEnv() {
		Path envDir = resolveEnvDirectory();
		Dotenv dotenv = Dotenv.configure()
				.directory(envDir.toString())
				.ignoreIfMissing()
				.load();
		dotenv.entries().forEach(entry -> {
			String key = entry.getKey();
			if (System.getenv(key) == null && System.getProperty(key) == null) {
				System.setProperty(key, entry.getValue());
			}
		});
	}

	/**
	 * Directory that should contain {@code .env}: the JAR's parent folder when packaged,
	 * otherwise the current working directory for local development.
	 */
	private static Path resolveEnvDirectory() {
		try {
			URI location = RTGpollApplication.class
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
		} catch (Exception ignored) {
			// Fall through to working directory (IDE, spring-boot:run, or unusual classloaders).
		}
		return Path.of(System.getProperty("user.dir"));
	}
}
