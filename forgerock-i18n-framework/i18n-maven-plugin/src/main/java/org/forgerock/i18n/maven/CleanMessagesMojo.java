/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 *      Copyright 2011 ForgeRock AS
 */
package org.forgerock.i18n.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which cleans unused messages files from a property file.
 *
 * @Checkstyle:ignore
 * @goal clean-messages
 * @threadSafe
 */
@SuppressWarnings("resource")
public final class CleanMessagesMojo extends AbstractMojo {

    /**
     * A task which searches a single source file for any messages.
     */
    private final class SourceFileTask implements Runnable {

        private final File sourceFile;

        private SourceFileTask(final File sourceFile) {
            this.sourceFile = sourceFile;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            // Cache the keys that we want to check so that we avoid excessive
            // contention on the CHM.
            final List<MessagePropertyKey> keys = new LinkedList<MessagePropertyKey>(
                    unreferencedProperties.keySet());

            try {
                final FileInputStream s = new FileInputStream(sourceFile);
                try {
                    final LineNumberReader reader = new LineNumberReader(
                            new InputStreamReader(s, encoding));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        final Iterator<MessagePropertyKey> i = keys.iterator();
                        while (i.hasNext()) {
                            final MessagePropertyKey key = i.next();
                            if (key.isPresent(line)) {
                                i.remove();
                                unreferencedProperties.remove(key);
                                referencedProperties.put(key, ""); // Dummy
                                                                   // value.
                            }
                        }
                    }
                } finally {
                    try {
                        s.close();
                    } catch (final Exception ignored) {
                        // Ignore.
                    }
                }

                fileCount.incrementAndGet();
            } catch (final IOException e) {
                getLog().error(
                        "An error occurred while reading source file "
                                + sourceFile.getName(), e);
            }
        }

    }

    /**
     * The target directory in which the source files should be generated.
     *
     * @parameter default-value="${project.build.sourceDirectory}"
     * @required
     */
    private File sourceDirectory;

    /**
     * The message message file to be cleaned.
     *
     * @parameter
     * @required
     */
    private File messageFile;

    /**
     * The encoding argument used by Java source files.
     *
     * @parameter default-value="${project.build.sourceEncoding}"
     * @required
     */
    private String encoding;

    private final Map<MessagePropertyKey, String> unreferencedProperties =
            new ConcurrentHashMap<MessagePropertyKey, String>();
    private final Map<MessagePropertyKey, String> referencedProperties =
            new ConcurrentHashMap<MessagePropertyKey, String>();
    private final AtomicInteger fileCount = new AtomicInteger();

    /**
     * {@inheritDoc}
     */
    // @Checkstyle:ignore
    public void execute() throws MojoExecutionException {
        if (!sourceDirectory.exists()) {
            throw new MojoExecutionException("Source directory "
                    + sourceDirectory.getPath() + " does not exist");
        } else if (!sourceDirectory.isDirectory()) {
            throw new MojoExecutionException("Source directory "
                    + sourceDirectory.getPath() + " is not a directory");
        }

        if (!messageFile.exists()) {
            throw new MojoExecutionException("Message file "
                    + messageFile.getPath() + " does not exist");
        } else if (!messageFile.isFile()) {
            throw new MojoExecutionException("Message file "
                    + messageFile.getPath() + " is not a file");
        }

        final Properties properties = new Properties();

        try {
            final FileInputStream propertiesFile = new FileInputStream(
                    messageFile);
            try {
                properties.load(propertiesFile);
            } finally {
                try {
                    propertiesFile.close();
                } catch (final Exception ignored) {
                    // Ignore.
                }
            }
        } catch (final IOException e) {
            throw new MojoExecutionException(
                    "An IO error occurred while reading the message property file: "
                            + e);
        }

        // Initially all properties are deemed to have no references.
        for (final Map.Entry<Object, Object> property : properties.entrySet()) {
            final String propKey = property.getKey().toString();
            final MessagePropertyKey key = MessagePropertyKey.valueOf(propKey);
            unreferencedProperties.put(key, property.getValue().toString());
        }

        final int messageCount = unreferencedProperties.size();

        // Process source files in parallel.
        final ExecutorService executor = Executors.newFixedThreadPool(Runtime
                .getRuntime().availableProcessors() * 2);

        // Recursively add source files to task queue.
        processSourceDirectory(executor, sourceDirectory);

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException(
                    "Interrupted while processing source files");
        }

        if ((unreferencedProperties.size() + referencedProperties.size()) != messageCount) {
            throw new IllegalStateException("Message table sizes are invalid");
        }

        getLog().info("Processed " + fileCount.get() + " source files");
        getLog().info(
                "Found " + unreferencedProperties.size() + " / " + messageCount
                        + " unreferenced properties");

        // All source files processed and each message is known to be either
        // referenced or not. Re-write the property file including only the
        // referenced properties.
        int cleanedMessageCount = 0;
        int savedMessageCount = 0;

        final List<String> lines = new ArrayList<String>(10000);
        try {
            final FileInputStream propertiesFile = new FileInputStream(
                    messageFile);
            try {
                final LineNumberReader reader = new LineNumberReader(
                        new InputStreamReader(propertiesFile, "ISO-8859-1"));

                String line;
                boolean inValue = false;
                boolean lineNeedsRemoving = false;
                boolean foundErrors = false;
                while ((line = reader.readLine()) != null) {
                    if (!inValue) {
                        // This could be a comment or the start of a new
                        // property.
                        final String trimmedLine = line.trim();
                        if (trimmedLine.length() == 0) {
                            lines.add(line);
                        } else if (trimmedLine.startsWith("#")) {
                            lines.add(line);
                        } else {
                            final String key;
                            final int separator = trimmedLine.indexOf('=');
                            if (separator < 0) {
                                key = trimmedLine;
                            } else {
                                key = trimmedLine.substring(0, separator)
                                        .trim();
                            }

                            MessagePropertyKey mpk;
                            try {
                                mpk = MessagePropertyKey.valueOf(key);
                            } catch (IllegalArgumentException e) {
                                getLog().error(
                                        "Unable to decode line "
                                                + reader.getLineNumber() + ": "
                                                + line, e);
                                lines.add(line);
                                lineNeedsRemoving = false;
                                inValue = isContinuedOnNextLine(line);
                                foundErrors = true;
                                continue;
                            }

                            if (referencedProperties.containsKey(mpk)) {
                                savedMessageCount++;
                                lines.add(line);
                                lineNeedsRemoving = false;
                            } else {
                                if (!unreferencedProperties.containsKey(mpk)) {
                                    throw new IllegalStateException(
                                            "Unregistered message key");
                                }
                                cleanedMessageCount++;
                                lineNeedsRemoving = true;
                            }
                            inValue = isContinuedOnNextLine(line);
                        }
                    } else {
                        // This is a continuation line.
                        if (!lineNeedsRemoving) {
                            lines.add(line);
                        }

                        inValue = isContinuedOnNextLine(line);
                    }
                }

                if (foundErrors) {
                    throw new MojoExecutionException(
                            "Aborting because the message file could not be parsed");
                }
            } finally {
                try {
                    propertiesFile.close();
                } catch (final Exception ignored) {
                    // Ignore.
                }
            }
        } catch (final IOException e) {
            throw new MojoExecutionException(
                    "An IO error occurred while reading the message property file: "
                            + e);
        }

        // Now write out the cleaned message file.
        if (cleanedMessageCount == 0) {
            // Nothing to do.
            getLog().info(
                    "Message file " + messageFile.getName()
                            + " unchanged: no messages were cleaned");
        } else {
            try {
                final FileOutputStream propertiesFile = new FileOutputStream(
                        messageFile);
                try {
                    final OutputStreamWriter writer = new OutputStreamWriter(
                            propertiesFile, "ISO-8859-1");
                    final String eol = System.getProperty("line.separator");
                    for (final String line : lines) {
                        writer.write(line);
                        writer.write(eol);
                    }

                    writer.close();
                } finally {
                    try {
                        propertiesFile.close();
                    } catch (final Exception ignored) {
                        // Ignore.
                    }
                }
            } catch (final IOException e) {
                throw new MojoExecutionException(
                        "An IO error occurred while writing the message property file: "
                                + e);
            }

            getLog().info(
                    "Message file " + messageFile.getName() + " cleaned: "
                            + cleanedMessageCount + " messages removed and "
                            + savedMessageCount + " kept");
        }
    }

    // Check to see if this line ends with a continuation character (odd
    // number of consecutive back-slash).
    boolean isContinuedOnNextLine(final String line) {
        int bsCount = 0;
        for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            bsCount++;
        }

        return ((bsCount % 2) == 1);
    }

    // Recursively create tasks for each Java source file in the provided source
    // directory and sub-directories.
    private void processSourceDirectory(final ExecutorService executor,
            final File s) {
        for (final File f : s.listFiles()) {
            if (f.isDirectory()) {
                processSourceDirectory(executor, f);
            } else if (f.isFile()) {
                if (f.getName().endsWith(".java")) {
                    // Add a task for processing this source file.
                    executor.execute(new SourceFileTask(f));
                }
            }
        }
    }
}
