package com.example.xhscopywriting;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class XhsCopywritingApplication {

    public static void main(String[] args) {
        configureDotenvDirectory();
        SpringApplication.run(XhsCopywritingApplication.class, args);
    }

    static void configureDotenvDirectory() {
        if (System.getProperty("springdotenv.directory") != null
                || System.getenv("SPRINGDOTENV_DIRECTORY") != null) {
            return;
        }

        Path workingDirectory = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath()
                .normalize();
        System.setProperty(
                "springdotenv.directory",
                findDotenvDirectory(workingDirectory).toString());
    }

    static Path findDotenvDirectory(Path workingDirectory) {
        if (Files.isRegularFile(workingDirectory.resolve(".env"))) {
            return workingDirectory;
        }

        Path parentDirectory = workingDirectory.getParent();
        if (parentDirectory != null && Files.isRegularFile(parentDirectory.resolve(".env"))) {
            return parentDirectory;
        }

        return workingDirectory;
    }
}
