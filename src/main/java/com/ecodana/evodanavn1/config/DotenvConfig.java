package com.ecodana.evodanavn1.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import jakarta.annotation.PostConstruct;
import java.io.File;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DotenvConfig {

    static {
        // Load .env trước khi Spring Boot khởi tạo
        loadEnvironmentVariables();
    }

    private static void loadEnvironmentVariables() {
        try {
            // Kiểm tra file .env tồn tại
            File envFile = new File(".env");
            if (!envFile.exists()) {
                System.out.println("⚠️  .env file not found at: " + envFile.getAbsolutePath());
                return;
            }

            Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMissing()
                    .load();

            // Set system properties
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();

                // Loại bỏ dấu ngoặc kép nếu có
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                // Chỉ set nếu chưa có system property
                if (System.getProperty(key) == null) {
                    System.setProperty(key, value);
                    System.out.println("✅ Loaded: " + key + " = " +
                            (key.toLowerCase().contains("password") ||
                                    key.toLowerCase().contains("key") ||
                                    key.toLowerCase().contains("secret") ?
                                    "***HIDDEN***" : value));
                }
            });

            System.out.println("✅ Successfully loaded " + dotenv.entries().size() + " environment variables");

        } catch (Exception e) {
            System.err.println("❌ Failed to load .env file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PostConstruct
    public void validateConfiguration() {
        System.out.println("🔍 Validating environment configuration...");

        // Validate critical properties
        validateProperty("DB_URL");
        validateProperty("DB_USERNAME");
        validateProperty("GOOGLE_API_KEY");
        validateProperty("MAIL_USERNAME");
        validateProperty("MAIL_PASSWORD");
        validateProperty("CLOUDFLARE_ACCOUNT_ID");
        validateProperty("CLOUDFLARE_API_TOKEN");

        System.out.println("✅ Environment validation completed");
    }

    private void validateProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.trim().isEmpty()) {
            System.err.println("⚠️  Missing or empty property: " + propertyName);
        } else {
            System.out.println("✅ " + propertyName + " is configured");
        }
    }
}