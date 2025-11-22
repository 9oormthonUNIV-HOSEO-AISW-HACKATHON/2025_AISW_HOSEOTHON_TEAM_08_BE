package com.sedroad;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SedroadApplication {
    private static final Logger log = LoggerFactory.getLogger(SedroadApplication.class);

    public static void main(String[] args) {
        loadEnvFile();
        SpringApplication.run(SedroadApplication.class, args);
    }

    private static void loadEnvFile() {
        File envFile = new File(".env");
        if (envFile.exists() && envFile.isFile()) {
            try {
                Files.readAllLines(Paths.get(".env")).forEach(line -> {
                    if (line != null && !line.trim().isEmpty() && !line.trim().startsWith("#")) {
                        int equalsIndex = line.indexOf('=');
                        if (equalsIndex > 0) {
                            String key = line.substring(0, equalsIndex).trim();
                            String value = line.substring(equalsIndex + 1).trim();
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }
                            if (!key.isEmpty() && !value.isEmpty()) {
                                if (System.getenv(key) == null && System.getProperty(key) == null) {
                                    System.setProperty(key, value);
                                }
                            }
                        }
                    }
                });
            } catch (IOException e) {
                log.warn("Failed to load .env file: {}", e.getMessage());
            }
        }
    }
}

