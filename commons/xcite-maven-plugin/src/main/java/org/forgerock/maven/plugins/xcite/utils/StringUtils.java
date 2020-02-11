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

package org.forgerock.maven.plugins.xcite.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for handling strings.
 */
public final class StringUtils {

    /**
     * Return lines joined with line separators as a String.
     *
     * @param lines Lines to join with line separators.
     * @return Lines joined with line separators as a String.
     */
    public static String asString(final ArrayList<String> lines) {
        StringBuilder stringBuilder = new StringBuilder();

        String prefix = "";
        for (String line: lines) {
            stringBuilder.append(prefix);
            prefix = System.getProperty("line.separator");
            stringBuilder.append(line);
        }

        return stringBuilder.toString();
    }

    /**
     * Escape an array of quote strings, for safe inclusion in an XML document.
     *
     * <p>
     *
     * This method deals only with {@code &amp;, &lt;, &gt;, &quot;, &apos;}.
     *
     * @param strings   The array of strings to escape.
     * @return          The array of escaped strings.
     */
    public static ArrayList<String> escapeXml(final ArrayList<String> strings) {
        ArrayList<String> result = new ArrayList<String>();
        if (strings == null || strings.isEmpty()) {
            return result;
        }

        for (String string: strings) {
            result.add(escapeXml(string));
        }

        return result;
    }

    /**
     * Escape a quote string to be safely included in an XML document.
     *
     * <p>
     *
     * This method deals only with {@code &amp;, &lt;, &gt;, &quot;, &apos;}.
     *
     * @param string    The string to escape.
     * @return          The escaped string.
     */
    public static String escapeXml(final String string) {
        return string
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Extract a single quote from an array of strings.
     *
     * <p>
     *
     * The quote is surrounded by a string marking the start of the quote,
     * and a string marking the end of the quote.
     *
     * <p>
     *
     * If the start marker is null or empty,
     * then this method assumes the entire input text is the quote.
     *
     * <p>
     *
     * If the start marker exists but the end marker is null or empty,
     * then this method assumes everything after the start marker is the quote.
     *
     * <p>
     *
     * This method does not allow a null or empty end marker.
     * To quote from the start marker to the end of the text,
     * use an end marker that does not show up in the text.
     *
     * <p>
     *
     * Unless the start marker and end marker are found on the same line,
     * this method assumes the start marker and end marker lines
     * are separate from the quote.
     * In other words, for this case the method ignores
     * substrings following the start marker on the same line
     * and substrings preceding the end marker on the same line,
     * unless both markers are on the same line.
     *
     * @param text                      Strings possibly containing a quote.
     * @param start                     String marking start of quote.
     * @param end                       String marking end of quote.
     * @return                          Array of strings containing the quote.
     * @throws IllegalArgumentException End marker was null or empty.
     */
    public static ArrayList<String> extractQuote(final ArrayList<String> text,
                                                 final String start,
                                                 final String end) {

        if (text == null || text.isEmpty()) {
            return new ArrayList<String>();
        }

        // No start marker: assume the whole text is the quote.
        if (start == null || start.isEmpty()) {
            return text;
        }

        if (end == null || end.isEmpty()) {
            throw new IllegalArgumentException(
                    "End marker cannot be null or empty");
        }


        ArrayList<String> quote = new ArrayList<String>();

        final String literalStart = "^.*" + Pattern.quote(start);
        final String literalEnd = Pattern.quote(end) + ".*$";
        final String inline = literalStart + "(.+)" + literalEnd;
        final Pattern inlinePattern = Pattern.compile(inline);

        boolean inQuote = false;

        for (String line: text) {

            // Start and end markers on same line: single line quote.
            if (!inQuote && line.matches(inline)) {
                Matcher matcher = inlinePattern.matcher(line);
                if (matcher.find()) {
                    quote.add(matcher.group(1).trim());
                }
                return quote;
            }

            // Only start marker in the line: next line is in the quote.
            if (!inQuote && line.contains(start)) {
                inQuote = true;
                continue;
            }

            // End marker in the line: done with the quote.
            if (inQuote && line.contains(end)) {
                break;
            }

            // Inside the quote: add the line to the quote.
            if (inQuote) {
                quote.add(line);
            }
        }

        return stripEmpties(quote);
    }

    /**
     * Extract a single quote from an array of strings.
     *
     * <p>
     *
     * The quote is surrounded by a single string
     * that marks both the start of the quote and the end of the quote.
     *
     * <p>
     *
     * If the marker is null or empty, then the entire input text is the quote.
     *
     * <p>
     *
     * If the start marker exists but end marker is null or empty,
     * then this method assumes everything after the start marker is the quote.
     *
     * <p>
     *
     * Unless the markers are found on the same line,
     * this method assumes the markers lines are separate from the quote.
     * In other words, for this case the method ignores
     * substrings following the initial marker on the same line
     * and substrings preceding the final marker on the same line,
     * unless both markers are on the same line.
     *
     * @param text      The array of strings supposed to contain a quote.
     * @param marker    The string marking the start and end of the quote.
     * @return          The array of strings containing the quote.
     */
    public static ArrayList<String> extractQuote(final ArrayList<String> text,
                                                 final String marker) {
        return extractQuote(text, marker, marker);
    }

    /**
     * Strip leading and trailing "empty" lines,
     * where empty either means an empty string or string with only whitespace.
     *
     * @param text  The array of strings from which to strip empty lines.
     * @return      Array of strings with leading and trailing empties removed.
     */
    private static ArrayList<String> stripEmpties(ArrayList<String> text) {
        ArrayList<String> result = new ArrayList<String>();
        if (text == null || text.isEmpty()) {
            return result;
        }

        // Strip trailing empties
        Collections.reverse(text);
        result = stripLeadingEmpties(text);

        // Strip leading empties
        Collections.reverse(result);
        result = stripLeadingEmpties(result);

        return result;
    }

    /**
     * Strip leading "empty" lines,
     * where empty either means an empty string or string with only whitespace.
     *
     * @param text  The array of strings from which to strip empty lines.
     * @return      The array of strings with leading empties removed.
     */
    private static ArrayList<String> stripLeadingEmpties(ArrayList<String> text) {
        ArrayList<String> result = new ArrayList<String>();
        if (text == null || text.isEmpty()) {
            return result;
        }

        boolean inText = false;
        for (String line: text) {
            if (!inText && !line.trim().isEmpty()) {
                inText = true;
            }

            if (inText) {
                result.add(line);
            }
        }

        return result;
    }

    /**
     * Indent an array of strings.
     *
     * @param text      Array of strings to indent.
     * @param indent    The indentation string, usually a series of spaces.
     * @return          The indented array of strings.
     */
    public static ArrayList<String> indent(final ArrayList<String> text,
                                           final String indent) {
        ArrayList<String> result = new ArrayList<String>();
        if (text == null || text.isEmpty()) {
            return result;
        }

        if (indent == null || indent.isEmpty()) {
            return text;
        }

        for (String line: text) {
            result.add(indent + line);
        }

        return result;
    }

    /**
     * Outdent an array of strings,
     * removing an equal number of leftmost spaces from each string
     * until at least one string starts with a non-space character.
     *
     * <p>
     *
     * Tab width (or height) depends and is subject to debate,
     * so throw an exception if the initial whitespace includes a tab.
     *
     * @param indented                  Array of strings with leftmost spaces.
     * @return                          Strings with leftmost spaces removed.
     * @throws IllegalArgumentException Initial whitespace included a tab.
     */
    public static ArrayList<String> outdent(final ArrayList<String> indented) {
        int indent = getIndent(indented);
        return outdent(indented, indent);
    }

    /**
     * Return the minimum indentation of an array of strings.
     *
     * <p>
     *
     * Tab width (or height) depends and is subject to debate,
     * so throw an exception if the initial whitespace includes a tab.
     *
     * <p>
     *
     * Ignore lines that are nothing but spaces and a newline or CRLF.
     *
     * @param indented                  Array of strings with leftmost spaces.
     * @return                          Min. number of consecutive indent spaces.
     * @throws IllegalArgumentException Initial whitespace included a tab.
     */
    private static int getIndent(final ArrayList<String> indented) {

        if (indented == null || indented.isEmpty()) {
            return 0;
        }

        // Start with a huge theoretical indentation, then whittle it down.
        int indent = Integer.MAX_VALUE;

        int currentIndent;
        for (String line: indented) {

            Pattern initialWhitespace = Pattern.compile("^([ \\t]+)");
            Matcher matcher = initialWhitespace.matcher(line);
            String initialSpaces = "";
            if (matcher.find()) {
                initialSpaces = matcher.group();

                if (initialSpaces.contains("\\t")) {
                    throw new IllegalArgumentException(
                            "Line has a tab in leading space: " + line);
                }
            }

            // If the current indentation is the smallest so far, record it.
            currentIndent = initialSpaces.length();
            if (currentIndent < indent) {
                indent = currentIndent;
            }

            // No sense in checking every line when there's nothing to do.
            if (indent == 0) {
                return indent;
            }
        }

        return indent;
    }

    /**
     * Outdent an array of strings,
     * removing an equal number of leftmost spaces from each string
     * until at least one string starts with a non-space character.
     *
     * @param indented  The array of strings with possible leftmost whitespace.
     * @param indent    Min. number of leading white spaces on non-empty lines.
     * @return          Array of strings with leftmost spaces removed.
     */
    private static ArrayList<String> outdent(final ArrayList<String> indented,
                                             final int indent) {

        if (indented == null || indented.isEmpty()) {
            return new ArrayList<String>();
        }

        if (indent == 0) {
            return indented;
        }

        String spaces = new String(new char[indent]).replace('\0', ' ');
        ArrayList<String> outdented = new ArrayList<String>();
        for (String line: indented) {
            outdented.add(line.replaceFirst(spaces, ""));
        }
        return outdented;
    }

    /**
     * Return an empty string if the string is only whitespace.
     * Otherwise return the original string.
     *
     * @param string    The input string.
     * @return          An empty string if the input string is only whitespace,
     *                  otherwise the original string.
     */
    public static String removeEmptySpace(final String string) {
        return (string.matches("^\\s+$")) ? "" : string;
    }

    /**
     * Constructor not used.
     */
    private StringUtils() {
        // Not used
    }
}
