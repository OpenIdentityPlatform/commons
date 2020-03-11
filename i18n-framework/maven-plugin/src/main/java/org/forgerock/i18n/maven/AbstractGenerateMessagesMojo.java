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
 *      Copyright 2011-2013 ForgeRock AS
 */
package org.forgerock.i18n.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UnknownFormatConversionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Abstract Mojo implementation for generating message source files from a one
 * or more property files.
 */
abstract class AbstractGenerateMessagesMojo extends AbstractMojo {
    /**
     * A message file descriptor passed in from the POM configuration.
     */
    static final class MessageFile {
        /**
         * The name of the message property file to be processed.
         */
        private final String name;

        /**
         * Creates a new message file.
         *
         * @param name
         *            The name of the message file relative to the resource
         *            directory.
         */
        MessageFile(final String name) {
            this.name = name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return name;
        }

        /**
         * Returns the class name (without package) which will contain the
         * generated message descriptors.
         *
         * @return The class name (without package) which will contain the
         *         generated message descriptors.
         */
        String getClassName() {
            final StringBuilder builder = new StringBuilder();
            final String shortName = getShortName();
            boolean upperCaseNextChar = true;
            for (final char c : shortName.toCharArray()) {
                if (c == '_' || c == '-') {
                    upperCaseNextChar = true;
                    continue;
                }

                if (upperCaseNextChar) {
                    builder.append(Character.toUpperCase(c));
                    upperCaseNextChar = false;
                } else {
                    builder.append(c);
                }
            }
            builder.append("Messages");
            return builder.toString();
        }

        /**
         * The name of the message file relative to the resource directory.
         *
         * @return The name of the message file relative to the resource
         *         directory.
         */
        String getName() {
            return name;
        }

        /**
         * Returns a {@code File} representing the full path to the output Java
         * file.
         *
         * @param outputDirectory
         *            The target directory.
         * @return A {@code File} representing the full path to the output Java
         *         file.
         */
        File getOutputFile(final File outputDirectory) {
            final int lastSlash = name.lastIndexOf('/');
            final String parentPath = name.substring(0, lastSlash);
            final String path = parentPath.replace('/', File.separatorChar)
                    + File.separator + getClassName() + ".java";
            return new File(outputDirectory, path);
        }

        /**
         * Returns the name of the package containing the message file.
         *
         * @return The name of the package containing the message file.
         */
        String getPackageName() {
            final int lastSlash = name.lastIndexOf('/');
            final String parentPath = name.substring(0, lastSlash);
            return parentPath.replace('/', '.');
        }

        /**
         * The resource bundle name.
         *
         * @return The resource bundle name.
         */
        String getResourceBundleName() {
            return getPackageName() + "." + getShortName();
        }

        /**
         * Returns a {@code File} representing the full path to this message
         * file.
         *
         * @param resourceDirectory
         *            The resource directory.
         * @return A {@code File} representing the full path to this message
         *         file.
         */
        File getResourceFile(final File resourceDirectory) {
            final String path = name.replace('/', File.separatorChar);
            return new File(resourceDirectory, path);
        }

        /**
         * Returns the name of the message file with the package name and
         * trailing ".properties" suffix stripped.
         *
         * @return The name of the message file with the package name and
         *         trailing ".properties" suffix stripped.
         */
        String getShortName() {
            final int lastSlash = name.lastIndexOf('/');
            final String fileName = name.substring(lastSlash + 1);
            final int lastDot = fileName.lastIndexOf('.');
            return fileName.substring(0, lastDot);
        }

    }

    /**
     * Representation of a format specifier (for example %s).
     */
    private static final class FormatSpecifier {

        private final String[] sa;

        /**
         * Creates a new specifier.
         *
         * @param sa
         *            Specifier components.
         */
        FormatSpecifier(final String[] sa) {
            this.sa = sa;
        }

        /**
         * Returns a java class associated with a particular formatter based on
         * the conversion type of the specifier.
         *
         * @return Class for representing the type of argument used as a
         *         replacement for this specifier.
         */
        Class<?> getSimpleConversionClass() {
            Class<?> c = null;
            final String sa4 = sa[4] != null ? sa[4].toLowerCase() : null;
            final String sa5 = sa[5] != null ? sa[5].toLowerCase() : null;
            if ("t".equals(sa4)) {
                c = Calendar.class;
            } else if ("b".equals(sa5)) {
                c = Boolean.class;
            } else if ("h".equals(sa5) /* Hashcode */) {
                c = Object.class;
            } else if ("s".equals(sa5)) {
                c = Object.class; /* Conversion using toString() */
            } else if ("c".equals(sa5)) {
                c = Character.class;
            } else if ("d".equals(sa5) || "o".equals(sa5) || "x".equals(sa5)
                    || "e".equals(sa5) || "f".equals(sa5) || "g".equals(sa5)
                    || "a".equals(sa5)) {
                c = Number.class;
            } else if ("n".equals(sa5) || "%".equals(sa5)) {
                // ignore literals
            }
            return c;
        }

        /**
         * Returns {@code true} if this specifier uses argument indexes (for
         * example 2$).
         *
         * @return boolean {@code true} if this specifier uses argument indexes.
         */
        boolean specifiesArgumentIndex() {
            return this.sa[0] != null;
        }

    }

    /**
     * Represents a message to be written into the messages files.
     */
    private static final class MessageDescriptorDeclaration {

        private final MessagePropertyKey key;
        private final String formatString;
        private final List<FormatSpecifier> specifiers;
        private final List<Class<?>> classTypes;
        private String[] constructorArgs;

        /**
         * Creates a parameterized instance.
         *
         * @param key
         *            The message key.
         * @param formatString
         *            The message format string.
         */
        MessageDescriptorDeclaration(final MessagePropertyKey key,
                final String formatString) {
            this.key = key;
            this.formatString = formatString;
            this.specifiers = parse(formatString);
            this.classTypes = new ArrayList<Class<?>>();
            for (final FormatSpecifier f : specifiers) {
                final Class<?> c = f.getSimpleConversionClass();
                if (c != null) {
                    classTypes.add(c);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(getComment());
            sb.append(indent(1));
            sb.append("public static final ");
            sb.append(getDescriptorClassDeclaration());
            sb.append(" ");
            sb.append(key.getName());
            sb.append(" =");
            sb.append(EOL);
            sb.append(indent(5));
            sb.append("new ");
            sb.append(getDescriptorClassDeclaration());
            sb.append("(");
            if (constructorArgs != null) {
                for (int i = 0; i < constructorArgs.length; i++) {
                    sb.append(constructorArgs[i]);
                    if (i < constructorArgs.length - 1) {
                        sb.append(", ");
                    }
                }
            }
            sb.append(");");
            return sb.toString();
        }

        /**
         * Returns a string representing the message type class' variable
         * information (for example '<String,Integer>') that is based on the
         * type of arguments specified by the specifiers in this message.
         *
         * @return String representing the message type class parameters.
         */
        String getClassTypeVariables() {
            final StringBuilder sb = new StringBuilder();
            if (classTypes.size() > 0) {
                sb.append("<");
                for (int i = 0; i < classTypes.size(); i++) {
                    final Class<?> c = classTypes.get(i);
                    if (c != null) {
                        sb.append(getShortClassName(c));
                        if (i < classTypes.size() - 1) {
                            sb.append(", ");
                        }
                    }
                }
                sb.append(">");
            }
            return sb.toString();
        }

        /**
         * Returns the javadoc comments that will appear above the messages
         * declaration in the messages file.
         *
         * @return The javadoc comments that will appear above the messages
         *         declaration in the messages file.
         */
        String getComment() {
            final StringBuilder sb = new StringBuilder();
            sb.append(indent(1)).append("/**").append(EOL);

            // Unwrapped so that you can search through the descriptor
            // file for a message and not have to worry about line breaks
            final String ws = formatString; // wrapText(formatString, 70);

            final String[] sa = ws.split(EOL);
            for (final String s : sa) {
                sb.append(indent(1)).append(" * ").append(s).append(EOL);
            }
            sb.append(indent(1)).append(" */").append(EOL);
            return sb.toString();
        }

        /**
         * Returns the name of the Java class that will be used to represent
         * this message's type.
         *
         * @return The name of the Java class that will be used to represent
         *         this message's type.
         */
        String getDescriptorClassDeclaration() {
            final StringBuilder sb = new StringBuilder();
            if (useGenericMessageTypeClass()) {
                sb.append("LocalizableMessageDescriptor");
                sb.append(".");
                sb.append(DESCRIPTOR_CLASS_BASE_NAME);
                sb.append("N");
            } else {
                sb.append("LocalizableMessageDescriptor");
                sb.append(".");
                sb.append(DESCRIPTOR_CLASS_BASE_NAME);
                sb.append(classTypes.size());
                sb.append(getClassTypeVariables());
            }
            return sb.toString();
        }

        /**
         * Sets the arguments that will be supplied in the declaration of the
         * message.
         *
         * @param s
         *            The array of string arguments that will be passed in the
         *            constructor.
         */
        void setConstructorArguments(final String... s) {
            this.constructorArgs = s;
        }

        private void checkText(final String s) {
            int idx = s.indexOf('%');
            // If there are any '%' in the given string, we got a bad format
            // specifier.
            if (idx != -1) {
                final char c = (idx > s.length() - 2 ? '%' : s.charAt(idx + 1));
                throw new UnknownFormatConversionException(String.valueOf(c));
            }
        }

        private String getShortClassName(final Class<?> c) {
            String name;
            final String fqName = c.getName();
            final int i = fqName.lastIndexOf('.');
            if (i > 0) {
                name = fqName.substring(i + 1);
            } else {
                name = fqName;
            }
            return name;
        }

        private String indent(final int indent) {
            final char[] blankArray = new char[4 * indent];
            Arrays.fill(blankArray, ' ');
            return new String(blankArray);
        }

        /**
         * Returns a list of format specifiers contained in the provided format
         * string.
         *
         * @param s
         *            The format string.
         * @return The list of format specifiers.
         */
        private List<FormatSpecifier> parse(final String s) {
            final List<FormatSpecifier> sl = new ArrayList<FormatSpecifier>();
            final Matcher m = SPECIFIER_PATTERN.matcher(s);
            int i = 0;
            while (i < s.length()) {
                if (m.find(i)) {
                    // Anything between the start of the string and the
                    // beginning of the format specifier is either fixed text or contains
                    // an invalid format string.
                    if (m.start() != i) {
                        // Make sure we didn't miss any invalid format
                        // specifiers
                        checkText(s.substring(i, m.start()));

                        // Assume previous characters were fixed text
                        // al.add(new FixedString(s.substring(i, m.start())));
                    }

                    // Expect 6 groups in regular expression
                    final String[] sa = new String[6];
                    for (int j = 0; j < m.groupCount(); j++) {
                        sa[j] = m.group(j + 1);
                    }
                    sl.add(new FormatSpecifier(sa));
                    i = m.end();
                } else {
                    // No more valid format specifiers. Check for possible
                    // invalid format specifiers.
                    checkText(s.substring(i));

                    // The rest of the string is fixed text
                    // al.add(new FixedString(s.substring(i)));
                    break;
                }
            }
            return sl;
        }

        /**
         * Indicates whether the generic message type class should be used. In
         * general this is when a format specifier is more complicated than we
         * support or when the number of arguments exceeds the number of
         * specific message type classes (MessageType0, MessageType1 ...) that
         * are defined.
         *
         * @return boolean {@code true} if the generic message type class should
         *         be used.
         */
        private boolean useGenericMessageTypeClass() {
            if (specifiers.size() > DESCRIPTOR_MAX_ARG_HANDLER) {
                return true;
            } else {
                for (final FormatSpecifier s : specifiers) {
                    if (s.specifiesArgumentIndex()) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    /**
     * Indicates whether or not message files should be regenerated even if they
     * are already up to date.
     *
     * @parameter default-value="false"
     * @required
     */
    private boolean force;

    /**
     * The list of files we want to transfer, relative to the resource
     * directory.
     *
     * @parameter
     * @required
     */
    private String[] messageFiles;

    /**
     * The current Maven project.
     *
     * @parameter default-value="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;

    /**
     * The base name of the specific argument handling subclasses defined below.
     * The class names consist of the base name followed by a number indicating
     * the number of arguments that they handle when creating messages or the
     * letter "N" meaning any number of arguments.
     */
    private static final String DESCRIPTOR_CLASS_BASE_NAME = "Arg";

    /**
     * The maximum number of arguments that can be handled by a specific
     * subclass. If you define more subclasses be sure to increment this number
     * appropriately.
     */
    private static final int DESCRIPTOR_MAX_ARG_HANDLER = 11;

    private static final String SPECIFIER_REGEX =
            "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";

    private static final Pattern SPECIFIER_PATTERN = Pattern
            .compile(SPECIFIER_REGEX);

    /**
     * The end-of-line character for this platform.
     */
    private static final String EOL = System.getProperty("line.separator");

    /**
     * The UTF-8 character set used for encoding/decoding files.
     */
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * {@inheritDoc}
     */
    public final void execute() throws MojoExecutionException {
        final File resourceDirectory = getResourceDirectory();

        if (!resourceDirectory.exists()) {
            throw new MojoExecutionException("Source directory "
                    + resourceDirectory.getPath() + " does not exist");
        } else if (!resourceDirectory.isDirectory()) {
            throw new MojoExecutionException("Source directory "
                    + resourceDirectory.getPath() + " is not a directory");
        }

        final File targetDirectory = getTargetDirectory();

        if (!targetDirectory.exists()) {
            if (targetDirectory.mkdirs()) {
                getLog().info(
                        "Created message output directory: "
                                + targetDirectory.getPath());
            } else {
                throw new MojoExecutionException(
                        "Unable to create message output directory: "
                                + targetDirectory.getPath());
            }
        } else if (!targetDirectory.isDirectory()) {
            throw new MojoExecutionException("Output directory "
                    + targetDirectory.getPath() + " is not a directory");
        }

        if (project != null) {
            getLog().info(
                    "Adding source directory: " + targetDirectory.getPath());
            addNewSourceDirectory(targetDirectory);
        }

        for (final String messageFile : messageFiles) {
            processMessageFile(new MessageFile(messageFile));
        }
    }

    /**
     * Adds the generated source directory to the compilation path.
     *
     * @param targetDirectory
     *            The source directory to be added.
     */
    abstract void addNewSourceDirectory(final File targetDirectory);

    /**
     * Returns the resource directory containing the message files.
     *
     * @return The resource directory containing the message files.
     */
    abstract File getResourceDirectory();

    /**
     * Returns the target directory in which the source files should be
     * generated.
     *
     * @return The target directory in which the source files should be
     *         generated.
     */
    abstract File getTargetDirectory();

    /*
     * Returns the message Java stub file from this plugin's resources.
     */
    private InputStream getStubFile() throws MojoExecutionException {
        return getClass().getResourceAsStream("Messages.java.stub");
    }

    private void processMessageFile(final MessageFile messageFile) throws MojoExecutionException {
        final File resourceDirectory = getResourceDirectory();
        final File targetDirectory = getTargetDirectory();

        final File sourceFile = messageFile.getResourceFile(resourceDirectory);
        final File outputFile = messageFile.getOutputFile(targetDirectory);

        // Decide whether to generate messages based on modification
        // times and print status messages.
        if (!sourceFile.exists()) {
            throw new MojoExecutionException("Message file "
                    + messageFile.getName() + " does not exist");
        }

        if (outputFile.exists()) {
            if (force || sourceFile.lastModified() > outputFile.lastModified()) {
                if (!outputFile.delete()) {
                    throw new MojoExecutionException(
                            "Unable to continue because the old message file "
                                    + messageFile.getName()
                                    + " could not be deleted");
                }

                getLog().info(
                        "Regenerating " + outputFile.getName() + " from "
                                + sourceFile.getName());
            } else {
                getLog().info(outputFile.getName() + " is up to date");
                return;
            }
        } else {
            final File packageDirectory = outputFile.getParentFile();
            if (!packageDirectory.exists()) {
                if (!packageDirectory.mkdirs()) {
                    throw new MojoExecutionException(
                            "Unable to create message output directory: "
                                    + packageDirectory.getPath());
                }
            }
            getLog().info(
                    "Generating " + outputFile.getName() + " from "
                            + sourceFile.getName());
        }

        BufferedReader stubReader = null;
        PrintWriter outputWriter = null;

        try {
            stubReader = new BufferedReader(new InputStreamReader(
                    getStubFile(), UTF_8));
            outputWriter = new PrintWriter(outputFile, "UTF-8");

            final Properties properties = new Properties();
            final FileInputStream propertiesFile = new FileInputStream(
                    sourceFile);
            try {
                properties.load(propertiesFile);
            } finally {
                try {
                    propertiesFile.close();
                } catch (Exception ignored) {
                    // Ignore.
                }
            }

            for (String stubLine = stubReader.readLine(); stubLine != null; stubLine = stubReader
                    .readLine()) {
                if (stubLine.contains("${MESSAGES}")) {
                    final Map<MessagePropertyKey, String> propertyMap =
                            new TreeMap<MessagePropertyKey, String>();

                    for (final Map.Entry<Object, Object> property : properties
                            .entrySet()) {
                        final String propKey = property.getKey().toString();
                        final MessagePropertyKey key = MessagePropertyKey
                                .valueOf(propKey);
                        propertyMap.put(key, property.getValue().toString());
                    }

                    int usesOfGenericDescriptor = 0;

                    for (final Map.Entry<MessagePropertyKey, String> property : propertyMap
                            .entrySet()) {
                        final MessageDescriptorDeclaration message =
                                new MessageDescriptorDeclaration(property.getKey(),
                                        property.getValue());

                        message.setConstructorArguments(
                                messageFile.getClassName() + ".class",
                                "RESOURCE",
                                quote(property.getKey().toString()),
                                String.valueOf(property.getKey().getOrdinal()));
                        outputWriter.println(message.toString());
                        outputWriter.println();

                        // Keep track of when we use the generic descriptor so
                        // that we can report it later
                        if (message.useGenericMessageTypeClass()) {
                            usesOfGenericDescriptor++;
                        }
                    }

                    getLog().debug(
                            "  Generated " + propertyMap.size()
                                    + " LocalizableMessage");
                    getLog().debug(
                            "  Number of LocalizableMessageDescriptor.ArgN: "
                                    + usesOfGenericDescriptor);
                } else {
                    stubLine = stubLine.replace("${PACKAGE}",
                            messageFile.getPackageName());
                    stubLine = stubLine.replace("${CLASS_NAME}",
                            messageFile.getClassName());
                    stubLine = stubLine.replace("${FILE_NAME}",
                            messageFile.getName());
                    stubLine = stubLine.replace("${RESOURCE_BUNDLE_NAME}",
                            messageFile.getResourceBundleName());
                    outputWriter.println(stubLine);
                }
            }
        } catch (final IOException e) {
            // Don't leave a malformed file laying around. Delete it so it will
            // be forced to be regenerated.
            if (outputFile.exists()) {
                outputFile.deleteOnExit();
            }
            throw new MojoExecutionException(
                    "An IO error occurred while generating the message file: "
                            + e);
        } finally {
            if (stubReader != null) {
                try {
                    stubReader.close();
                } catch (final Exception e) {
                    // Ignore.
                }
            }

            if (outputWriter != null) {
                try {
                    outputWriter.close();
                } catch (final Exception e) {
                    // Ignore.
                }
            }
        }

    }

    private String quote(final String s) {
        return new StringBuilder().append("\"").append(s).append("\"")
                .toString();
    }

    /**
     * Returns the Maven project associated with this Mojo.
     *
     * @return The Maven project associated with this Mojo.
     */
    protected MavenProject getMavenProject() {
        return project;
    }
}
