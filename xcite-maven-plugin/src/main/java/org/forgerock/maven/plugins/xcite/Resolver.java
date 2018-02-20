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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.maven.plugins.xcite;

import org.forgerock.maven.plugins.xcite.utils.FileUtils;
import org.forgerock.maven.plugins.xcite.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolve citation strings in target files into quotes from source files.
 */
public class Resolver {

    private boolean                         escapeXml;
    private String                          indent;
    private boolean                         outdent;
    private File                            outputDirectory;

    /**
     * Construct a resolver.
     *
     * @param outputDirectory   Where to write files with quotes.
     *                          Specify the source directory to replace files.
     * @param escapeXml         Escape XML when quoting.
     * @param indent            Indent quotes by this number of single spaces.
     * @param outdent           Outdent to the left margin.
     *                          When you specify
     *                          both {@code indent} and {@code outdent},
     *                          quotes are first outdented, then indented.
     */
    Resolver(File outputDirectory, boolean escapeXml, int indent, boolean outdent) {
        this.outputDirectory    = outputDirectory;
        this.escapeXml          = escapeXml;
        this.indent             = new String(new char[indent]).replace('\0', ' ');
        this.outdent            = outdent;
    }

    /**
     * Resolve citation strings in a set of target files.
     *
     * @param sourceDirectory   Where to find files with citations.
     * @param files             Relative file paths.
     * @throws IOException      Failed to read or write a file.
     */
    void resolve(File sourceDirectory, String[] files) throws IOException {
        for (String relativePath: files) {
            resolve(sourceDirectory, new File(relativePath));
        }
    }

    /**
     * Resolve citation strings in a target file.
     *
     * @param baseDir       Where to find the file.
     * @param file          Relative path to file.
     * @throws IOException  Failed to read or write the file.
     */
    void resolve(File baseDir, File file) throws IOException {
        File absFile = new File(baseDir, file.getPath());

        if (!absFile.isFile()) {
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        String prefix = "";
        for (String line: FileUtils.getStrings(absFile)) {
            stringBuilder.append(prefix);
            prefix = System.getProperty("line.separator");
            stringBuilder.append(resolve(absFile, line));
        }

        File out = new File(outputDirectory, file.getPath());
        org.codehaus.plexus.util.FileUtils.fileWrite(
                out, stringBuilder.toString());
    }

    /**
     * Resolve citation strings a line of text.
     *
     * @param file          Absolute path to current file.
     * @param line          Line potentially containing citations.
     * @return              Line with quotes resolved.
     * @throws IOException  Failed to process the line.
     */
    String resolve(File file, String line) throws IOException {

        // Split the line into parts, where some are citations, some not.
        String[] parts = split(line);

        // For part that are citations, replace them with quotes.
        int i = 0;
        for (String part: parts) {
            if (part == null) {
                parts[i] = "";
                ++i;
                continue;
            }

            // Try to construct a Citation with both delimiters,
            // the alternative % and also the default :.
            Citation citation = null;
            if (part.contains("%")) {
                citation = Citation.valueOf(part, "%");
            }
            if (citation == null) {
                citation = Citation.valueOf(part);
            }

            if (citation == null) { // The part is not a citation.
                parts[i] = part;
            } else {
                String quote = getQuote(file, citation);

                // If the quote is the same the original citation string,
                // return the part unchanged.
                parts[i] = (quote.equals(citation.toString())) ? part : quote;
            }

            ++i;
        }

        // Put the line back together into a single string.
        StringBuilder stringBuilder = new StringBuilder();
        for (String part: parts) {
            stringBuilder.append(part);
        }
        return stringBuilder.toString();
    }

    /**
     * Split a line into strings, where citation strings are separate.
     *
     * @param line The line to split.
     * @return     The line split into strings. Null for null input.
     */
    String[] split(String line) {
        if (line == null) {
            return null;
        }

        if (line.isEmpty()) {
            return new String[1];
        }

        ArrayList<String> parts = new ArrayList<String>();

        // The line is composed of parts,
        // possibly with text preceding each citation,
        // possibly with trailing text following the last citation.
        // For example, "pre [/test] [/test] post" splits into
        // ["pre ", "[/test]", " ", "[/test]", " post"].

        // open-bracket 1*(exclude close-bracket) close-bracket
        Pattern citationCandidate = Pattern.compile("(\\[[^\\]]+\\])");
        Matcher matcher = citationCandidate.matcher(line);
        int index = 0;
        while (matcher.find()) {
            String before = line.substring(index, matcher.start());
            if (!before.isEmpty()) {
                parts.add(before);
            }
            parts.add(matcher.group());
            index = matcher.end();
        }
        if (index < line.length()) {
            parts.add(line.substring(index));
        }

        String[] results = new String[parts.size()];
        return parts.toArray(results);
    }

    /**
     * Return the quote for a Citation in the specified file.
     *
     * @param file          The file where the citation is found.
     * @param citation      The citation to resolve.
     * @return              The quote from the resolved citation.
     * @throws IOException  Failed to read the quote from the file.
     */
    String getQuote(File file, Citation citation) throws IOException {

        // Citations can have relative paths for the files they cite.
        // Get an absolute path instead in order to read the quote file.
        File citedFile = new File(citation.getPath());
        if (!citedFile.isAbsolute()) {
            String currentDirectory = file.getParent();
            citedFile = new File(currentDirectory, citedFile.getPath());
        }

        // Either this is not a citation, or it is a broken citation.
        if (!citedFile.exists() || !citedFile.isFile()) {
            return citation.toString();
        }

        // Extract the raw quote from the cited file.
        ArrayList<String> quoteLines = StringUtils.extractQuote(
                FileUtils.getStrings(citedFile), citation.getStart(), citation.getEnd());

        if (escapeXml) {
            quoteLines = StringUtils.escapeXml(quoteLines);
        }

        if (outdent) {
            quoteLines = StringUtils.outdent(quoteLines);
        }

        if (!indent.isEmpty()) {
            quoteLines = StringUtils.indent(quoteLines, indent);
        }

        // Quotes can contain citations.
        // Resolve any citations in the quote before returning it as a string.
        StringBuilder stringBuilder = new StringBuilder();
        String prefix = "";
        for (String quoteLine: quoteLines) {
            stringBuilder.append(prefix);
            prefix = System.getProperty("line.separator");
            stringBuilder.append(resolve(citedFile, quoteLine)); // TODO: loop?
        }
        return stringBuilder.toString();
    }
}
