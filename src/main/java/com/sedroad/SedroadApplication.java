package com.sedroad;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SedroadApplication {
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
                            // 따옴표 제거
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }
                            if (!key.isEmpty() && !value.isEmpty()) {
                                // 이미 환경 변수로 설정되어 있지 않은 경우만 설정
                                if (System.getenv(key) == null && System.getProperty(key) == null) {
                                    System.setProperty(key, value);
                                }
                            }
                        }
                    }
                });
                System.out.println("Loaded environment variables from .env file");
            } catch (IOException e) {
                System.err.println("WARNING: Failed to load .env file: " + e.getMessage());
            }
        } else {
            System.out.println("WARNING: .env file not found. Ensure environment variables are set.");
        }
    }
}

