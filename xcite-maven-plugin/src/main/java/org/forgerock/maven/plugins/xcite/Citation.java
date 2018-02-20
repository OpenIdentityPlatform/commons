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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.maven.plugins.xcite;

import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * A citation references part of a text file to quote.
 *
 * <p>
 *
 * The string representation includes the path to the file to cite,
 * and optionally start and end markers to frame the text to quote.
 *
 * <p>
 *
 * For example, {@code [/path/to/script.sh:# start:# end]}
 *
 * <p>
 *
 * Notice that the example has a UNIX-style path, {@code /path/to/script.sh},
 * a start marker, {@code # start}, and an end marker, {@code # end}.
 * The file, {@code script.sh}, could have text to quote between the markers.
 * For example, {@code script.sh} might have the following content:
 *
 * <pre>
 * #!/bin/bash
 *
 * # start
 * wall &lt;&lt;EOM
 *     Hello world
 * EOM
 * # end
 *
 * exit
 * </pre>
 *
 * In this case the quote would be the following:
 *
 * <pre>
 * wall &lt;&lt;EOM
 *     Hello world
 * EOM
 * </pre>
 *
 * Start and end markers depend on the language of the source text.
 * Markers should make sense to people who mark up the source text,
 * and so they should be composed of visible characters.
 * Markers must not include the delimiter character
 * that is used to separate the path and the markers.
 *
 * <p>
 *
 * Markers either both be on the same line as the text to quote,
 * or should be on separate lines from the text to quote.
 * In other words, you can prepare a text to quote a single line
 * either by adding a start marker on the line before
 * and an end marker on the line after,
 * or by prepending a start marker to the line before the text to quote
 * and appending an end marker after.
 *
 * <p>
 *
 * The following example shows markers on separate lines.
 *
 * <pre>
 * #start
 * This is the text to quote.
 * #end
 * </pre>
 *
 * <p>
 *
 * The following example shows markers on the same line as the text to quote.
 *
 * <pre>
 * #start This is the text to quote. #end
 * </pre>
 *
 * More formally, the citation string representation is as follows.
 *
 * <pre>
 *
 * citation     = "[" path delimiter start-marker delimiter end-marker "]"
 * citation     / "[" path delimiter start-marker "]"
 * citation     / "[" path "]"
 *
 * path         = File.getPath()                ; Depends on the OS,
 *                                              ; does not include delimiter
 *
 * delimiter    = ":" / "%"                     ; Default: ":"
 *
 * start-marker = 1*(VCHAR excluding delimiter) ; Depends on source language
 *
 * end-marker   = 1*(VCHAR excluding delimiter) ; Depends on source language
 *
 * </pre>
 *
 * Most systems allow file names that can cause problems with this scheme.
 * Working around the problem is an exercise for the reader.
 */
public class Citation {

    /**
     * Initial wrapper to open the citation string representation.
     */
    private static final String OPEN  = "[";

    /**
     * Final wrapper to close the citation string representation.
     */
    private static final String CLOSE = "]";

    private String path;        // Pathname for the file to quote
    private char   delimiter;   // Delimiter for path, markers
    private String start;       // Start marker
    private String end;         // End marker

    /**
     * Construct a citation from a path alone.
     *
     * <p>
     *
     * The path cannot be null.
     *
     * @param path      The pathname string for the file to quote. Not null.
     * @throws IllegalArgumentException Path is broken somehow.
     */
    public Citation(final String path) {
        build(path, ':', null, null);
    }

    /**
     * Construct a citation from a path, delimiter, and start marker,
     * when the start marker and the end marker are the same.
     *
     * <p>
     *
     * The path cannot be null.
     *
     * <p>
     *
     * The start marker cannot contain the delimiter.
     *
     * @param path      The pathname string for the file to quote. Not null.
     * @param delimiter The delimiter for path, markers.
     * @param start     The start marker. If null or "", only path is useful.
     * @throws IllegalArgumentException Path or marker is broken somehow.
     */
    public Citation(final String path, final char delimiter, final String start) {
        build(path, delimiter, start, null);
    }

    /**
     * Construct a citation from a path, delimiter, start marker, and end marker.
     *
     * <p>
     *
     * The path cannot be null.
     *
     * <p>
     *
     * The markers cannot contain the delimiter.
     *
     * @param path      The pathname string for the file to quote. Not null.
     * @param delimiter The delimiter for path, markers.
     * @param start     The start marker. If null or "", only path is useful.
     * @param end       The end marker. If null or "", {@code start} is both.
     * @throws IllegalArgumentException Path or marker is broken somehow.
     */
    public Citation(final String path, final char delimiter,
                    final String start, final String end) {
        build(path, delimiter, start, end);
    }

    /**
     * Builder for constructing an instance.
     *
     * @param path      The pathname string for the file to quote. Not null.
     * @param delimiter The delimiter for path, markers.
     * @param start     The start marker. If null or "", only path is useful.
     * @param end       The end marker. If null or "", {@code start} is both.
     * @throws IllegalArgumentException Path or marker is broken somehow.
     */
    private void build(final String path, final char delimiter,
                       final String start, final String end) {
        setPath(path);
        setDelimiter(delimiter);
        setStart(start);
        setEnd(end);
    }

    /**
     * Returns the value of the path.
     *
     * @return The value of the path.
     */
    public String getPath() {
        return this.path;
    }

    /**
     * Sets the pathname string for the file to quote.
     *
     * @param path The pathname string for the file to quote.
     * @throws IllegalArgumentException Path cannot be null.
     */
    public void setPath(final String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null.");
        } else {
            this.path = path;
        }
    }

    /**
     * Returns the value of the delimiter.
     *
     * @return The value of the delimiter.
     */
    public char getDelimiter() {
        return this.delimiter;
    }

    /**
     * Sets the value of the delimiter.
     *
     * @param delimiter The delimiter for path, markers.
     */
    public void setDelimiter(char delimiter) {
        if (delimiter == ':' || delimiter == '%') {
            this.delimiter = delimiter;
        } else {
            this.delimiter = ':';
        }
    }

    /**
     * Returns the value of the start marker, which can be empty.
     *
     * @return The value of the start marker, which can be empty.
     */
    public String getStart() {
        return this.start;
    }

    /**
     * Sets the value of the start marker, which must not contain the delimiter.
     *
     * @param start The value of the start marker.
     * @throws IllegalArgumentException Start marker contains the delimiter.
     */
    public void setStart(String start) {
        if (isNullOrEmpty(start)) {
            this.start = "";
        } else if (!start.contains(Character.toString(getDelimiter()))) {
            this.start = start;
        } else {
            throw new IllegalArgumentException("Start marker: " + start
                    + " contains delimiter: " + getDelimiter());
        }
    }

    /**
     * Returns the value of the start marker, which can be empty.
     *
     * @return The value of the start marker, which can be empty.
     */
    public String getEnd() {
        return this.end;
    }

    /**
     * Sets the value of the end marker, which must not contain the delimiter.
     *
     * @param end The value of the end marker.
     * @throws IllegalArgumentException End marker contains the delimiter.
     */
    public void setEnd(String end) {
        if (isNullOrEmpty(end)) {
            this.end = getStart();
        } else if (!end.contains(Character.toString(getDelimiter()))) {
            this.end = end;
        } else {
            throw new IllegalArgumentException("End marker: " + end
                    + " contains delimiter: " + getDelimiter());
        }
    }

    /**
     * Returns true if the string is null or the string is empty.
     *
     * @param string The string.
     * @return Whether the string is null or empty.
     */
    private static boolean isNullOrEmpty(final String string) {
        return (string == null || string.isEmpty());
    }

    /**
     * Returns the string representation of this citation.
     *
     * @return The string representation of this citation.
     */
    public String toString() {
        String start =
                (isNullOrEmpty(getStart()) ? "" : getDelimiter() + getStart());

        String end;
        if (isNullOrEmpty(getStart()) || isNullOrEmpty(getEnd())) {
            end = "";
        } else {
            end = getDelimiter() + getEnd();
        }

        return OPEN + getPath() + start + end + CLOSE;
    }

    /**
     * Returns a Citation from the string representation.
     *
     * @param citation  The string representation.
     * @param delimiter One of ":" or "%".
     * @return A Citation corresponding to the string representation,
     *         or null if the string representation does not parse.
     */
    public static Citation valueOf(final String citation, final String delimiter) {

        // No null delimiters.
        if (delimiter == null) {
            return null;
        }

        // No illegal delimiters.
        if (!(delimiter.equals(":") || delimiter.equals("%"))) {
            return null;
        }

        // OPEN marks the start of the citation string.
        if (!citation.startsWith(OPEN)) {
            return null;
        }

        // CLOSE marks the end of the citation string.
        if (!citation.endsWith(CLOSE)) {
            return null;
        }

        // Remove the OPEN & CLOSE wrappers.
        String unwrapped = citation
                .replaceFirst(Pattern.quote(OPEN), "")
                .replaceFirst(Pattern.quote(CLOSE), "");

        // Starting with a delimiter means path is null.
        if (unwrapped.startsWith(delimiter)) {
            return null;
        }

        // Delimiters should delimit actual values, not empty strings.
        if (unwrapped.contains(delimiter + delimiter)) {
            return null;
        }

        // Tokenize using the delimiter, as values do not contain the delimiter.
        StringTokenizer st = new StringTokenizer(unwrapped, delimiter);
        String path  = null;
        String start = null;
        String end   = null;

        if (st.hasMoreTokens()) {    // path
            path = st.nextToken();
        }
        if (st.hasMoreTokens()) {    // start
            start = st.nextToken();
        }
        if (st.hasMoreTokens()) {    // end
            end = st.nextToken();
        }
        if (st.hasMoreTokens()) {
            return null;
        }

        // Path must not be null.
        if (isNullOrEmpty(path)) {
            return null;
        }

        return new Citation(path, ':', start, end);
    }

    /**
     * Returns a Citation from the string representation,
     * assuming the default delimiter, {@code :}.
     *
     * @param citation The string representation.
     * @return A Citation corresponding to the string representation,
     *         or null if the string representation does not parse.
     */
    public static Citation valueOf(final String citation) {
        return valueOf(citation, ":");
    }
}
