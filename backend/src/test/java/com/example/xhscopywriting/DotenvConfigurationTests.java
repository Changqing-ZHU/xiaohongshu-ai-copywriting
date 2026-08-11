package com.example.xhscopywriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

class DotenvConfigurationTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void findsDotenvInParentProjectDirectory() throws Exception {
        Path projectRoot = temporaryDirectory.resolve("project");
        Path backendDirectory = projectRoot.resolve("backend");
        Files.createDirectories(backendDirectory);
        Files.createFile(projectRoot.resolve(".env"));

        assertEquals(
                projectRoot,
                XhsCopywritingApplication.findDotenvDirectory(backendDirectory));
    }

    @Test
    void loadsDotenvAndResolvesApplicationPlaceholders() throws Exception {
        String databaseUrl = "jdbc:mysql://localhost:3306/dotenv_test";
        Files.writeString(temporaryDirectory.resolve(".env"), """
                DB_URL=jdbc:mysql://localhost:3306/dotenv_test
                DB_USERNAME=dotenv_user
                MYSQL_ROOT_PASSWORD=dotenv_password
                UPLOAD_DIR=dotenv_uploads
                AI_PROVIDER=mock
                QWEN_API_KEY=dotenv_test_key
                QWEN_BASE_URL=https://dashscope.test/compatible-mode/v1
                QWEN_MODEL=dotenv_test_model
                """);

        String previousDirectory = System.getProperty("springdotenv.directory");
        System.setProperty("springdotenv.directory", temporaryDirectory.toString());

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                DotenvTestApplication.class)
                .web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off")
                .run()) {
            ConfigurableEnvironment environment = context.getEnvironment();
            String resolvedDatasourceUrl = environment.getProperty("spring.datasource.url");

            assertEquals(databaseUrl, environment.getProperty("DB_URL"));
            assertEquals("dotenv_user", environment.getProperty("DB_USERNAME"));
            assertEquals("dotenv_password", environment.getProperty("MYSQL_ROOT_PASSWORD"));
            assertEquals("dotenv_uploads", environment.getProperty("UPLOAD_DIR"));
            assertEquals("mock", environment.getProperty("AI_PROVIDER"));
            assertEquals("dotenv_test_key", environment.getProperty("QWEN_API_KEY"));
            assertEquals(
                    "https://dashscope.test/compatible-mode/v1",
                    environment.getProperty("QWEN_BASE_URL"));
            assertEquals("dotenv_test_model", environment.getProperty("QWEN_MODEL"));
            assertEquals(databaseUrl, resolvedDatasourceUrl);
            assertNotEquals("${DB_URL}", resolvedDatasourceUrl);
            assertFalse(resolvedDatasourceUrl == null || resolvedDatasourceUrl.contains("${"));
        } finally {
            if (previousDirectory == null) {
                System.clearProperty("springdotenv.directory");
            } else {
                System.setProperty("springdotenv.directory", previousDirectory);
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class
    })
    static class DotenvTestApplication {
    }
}
