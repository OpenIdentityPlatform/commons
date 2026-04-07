package com.savage7.maven.plugin.dependency;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Demonstrates that an externally-downloaded artifact (commons-io) is available
 * on the classpath after the maven-external-dependency-plugin has resolved and
 * installed it during the {@code process-resources} phase.
 */
public class App {
    public static void main(String[] args) throws Exception {
        // Write a temporary file and read it back using commons-io,
        // which was downloaded by the external-dependency plugin.
        File temp = File.createTempFile("external-dep-demo", ".txt");
        temp.deleteOnExit();

        FileUtils.writeStringToFile(temp, "Hello from commons-io!", StandardCharsets.UTF_8);
        String content = FileUtils.readFileToString(temp, StandardCharsets.UTF_8);

        System.out.println("commons-io is on the classpath: " + content);
    }
}
