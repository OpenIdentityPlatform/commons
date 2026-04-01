package com.savage7.maven.plugin.dependency;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the maven-external-dependency-plugin correctly downloaded
 * and staged the declared artifact items.
 *
 * The {@code resolve-external} and {@code install-external} goals run in the
 * {@code process-resources} phase, which precedes the {@code test} phase, so
 * the staging directory is populated by the time these tests execute.
 *
 * The staging directory path is injected via the {@code staging.directory}
 * system property configured in the maven-surefire-plugin.
 */
public class AppTest {

    /** Staging directory configured in pom.xml and passed as a system property. */
    private static final String STAGING_DIR =
            System.getProperty("staging.directory", "target/dependencies/");

    @Test
    public void testStagingDirectoryExists() {
        File stagingDir = new File(STAGING_DIR);
        assertTrue("Staging directory should exist after resolve-external goal: " + stagingDir.getAbsolutePath(),
                stagingDir.exists() && stagingDir.isDirectory());
    }

    @Test
    public void testStagingDirectoryIsNotEmpty() {
        File stagingDir = new File(STAGING_DIR);
        if (stagingDir.exists()) {
            String[] files = stagingDir.list();
            assertNotNull("Staging directory listing should not be null", files);
            assertTrue("Staging directory should contain at least one downloaded artifact",
                    files.length > 0);
        }
    }

    /** Example 1 – basic JAR downloaded via URL template. */
    @Test
    public void testCommonsIoDownloaded() {
        assertStagedFileExists("commons-io-2.11.0.jar");
    }

    /** Example 2 – JAR downloaded with explicit SHA-1 checksum verification. */
    @Test
    public void testHamcrestCoreDownloaded() {
        assertStagedFileExists("hamcrest-core-1.3.jar");
    }

    /** Example 3 – download-only artifact (install=false, deploy=false). */
    @Test
    public void testSlf4jApiDownloaded() {
        assertStagedFileExists("slf4j-api-1.7.36.jar");
    }

    /** Example 4 – JAR with sources classifier. */
    @Test
    public void testCommonsLangSourcesDownloaded() {
        assertStagedFileExists("commons-lang-2.6-sources.jar");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private void assertStagedFileExists(String fileName) {
        File artifact = new File(STAGING_DIR, fileName);
        assertTrue("Expected staged artifact to exist and be non-empty: " + artifact.getAbsolutePath(),
                artifact.exists() && artifact.length() > 0);
    }
}
