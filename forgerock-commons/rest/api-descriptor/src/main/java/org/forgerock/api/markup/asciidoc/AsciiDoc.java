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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.api.markup.asciidoc;

import static org.forgerock.api.markup.asciidoc.AsciiDocSymbols.*;
import static org.forgerock.api.util.ValidationUtil.containsWhitespace;
import static org.forgerock.api.util.ValidationUtil.isEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.forgerock.util.Reject.checkNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Root builder for AsciiDoc markup. All operations may be applied at the current linear position within the
 * document being built, such that markup that must appear at the top should be added first.
 * <p>
 * This class is not thread-safe.
 * </p>
 */
public final class AsciiDoc {

    // TODO http://asciidoctor.org/docs/user-manual/#preventing-substitutions

    /**
     * Regex for finding <a href="http://asciidoctor.org/docs/user-manual/#include-directive">Include</a>-directives,
     * where group 1 contains the path-value.
     *
     * @see #include(String...)
     */
    public static final Pattern INCLUDE_PATTERN = Pattern.compile("include[:]{2}([^\\[]+)\\[\\]");

    /**
     * Underscore-character is used as the namespace-part delimiter.
     */
    private static final String NAMESPACE_DELIMITER = "_";

    /**
     * Characters that must be replaced/removed for a filename to be a POSIX "Fully portable filename"
     * <a href="https://en.wikipedia.org/wiki/Filename#Reserved_characters_and_words">[ref]</a>.
     */
    private static final Pattern POSIX_FILENAME_REPLACEMENT_PATTERN = Pattern.compile("^[-]|[^A-Za-z0-9._-]");

    /**
     * Pattern for replacing multiple underscores with a single underscore.
     */
    private static final Pattern SQUASH_UNDERSCORES_PATTERN = Pattern.compile("[_]{2,}");

    private final StringBuilder builder;

    private AsciiDoc() {
        builder = new StringBuilder();
    }

    /**
     * Creates a new builder instance.
     *
     * @return builder
     */
    public static AsciiDoc asciiDoc() {
        return new AsciiDoc();
    }

    /**
     * Prefixes the given line-of-content with an AsciiDoc symbol.
     *
     * @param symbol AsciiDoc symbol
     * @param content Content
     * @return Doc builder
     */
    private AsciiDoc line(final AsciiDocSymbols symbol, final String content) {
        if (isEmpty(content)) {
            throw new AsciiDocException("content required");
        }
        builder.append(NEWLINE).append(checkNotNull(symbol)).append(content).append(NEWLINE);
        return this;
    }

    /**
     * Surrounds the given content-block with block symbols.
     *
     * @param symbol AsciiDoc symbol
     * @param content Content
     * @return Doc builder
     */
    private AsciiDoc block(final AsciiDocSymbols symbol, final String content) {
        if (isEmpty(content)) {
            throw new AsciiDocException("content required");
        }
        builder.append(checkNotNull(symbol)).append(NEWLINE)
                .append(content).append(NEWLINE)
                .append(checkNotNull(symbol)).append(NEWLINE);
        return this;
    }

    /**
     * Inserts a UNIX newline character, where two adjacent newlines will create a new
     * <a href="http://asciidoctor.org/docs/user-manual/#paragraph">paragraph</a>.
     * As a best-practice, they suggest one-sentence-per-line style.
     *
     * @return builder
     */
    public AsciiDoc newline() {
        builder.append(NEWLINE);
        return this;
    }

    /**
     * Inserts raw text (may contain markup or only whitespace).
     *
     * @param text Raw text/markup
     * @return builder
     */
    public AsciiDoc rawText(final String text) {
        if (text == null) {
            throw new AsciiDocException("text required");
        }
        builder.append(text);
        return this;
    }

    /**
     * Inserts raw line (may contain markup), and will insert one newline-characters above and below, if those
     * newlines do not already exist.
     *
     * @param text Raw text/markup
     * @return builder
     */
    public AsciiDoc rawLine(final String text) {
        if (isEmpty(text)) {
            throw new AsciiDocException("text required");
        }

        final int newlinesAbove = requireTrailingNewlines(1, builder);
        if (newlinesAbove == 1) {
            builder.append(NEWLINE);
        }

        builder.append(text);

        final int newlinesBelow = requireTrailingNewlines(1, text);
        if (newlinesBelow == 1) {
            builder.append(NEWLINE);
        }
        return this;
    }

    /**
     * Inserts raw paragraph (may contain markup), and will insert two newline-characters above and below, if those
     * newlines do not already exist [<a href="http://asciidoctor.org/docs/user-manual/#paragraph">ref</a>].
     *
     * @param text Raw text/markup
     * @return builder
     */
    public AsciiDoc rawParagraph(final String text) {
        if (isEmpty(text)) {
            throw new AsciiDocException("text required");
        }

        int newlinesAbove = requireTrailingNewlines(2, builder);
        while (--newlinesAbove > -1) {
            builder.append(NEWLINE);
        }

        builder.append(text);

        int newlinesBelow = requireTrailingNewlines(2, text);
        while (--newlinesBelow > -1) {
            builder.append(NEWLINE);
        }
        return this;
    }

    /**
     * Checks for a minimum-number of trailing newline-characters.
     *
     * @param newlines Minimum number of required, trailing newline-characters (1 or higher)
     * @param text Text to check
     * @return Number of newline-characters that need to be added to the end of the text
     */
    private static int requireTrailingNewlines(int newlines, final CharSequence text) {
        if (newlines < 0) {
            throw new IllegalArgumentException("newlines must be positive");
        }
        for (int i = 0; newlines > 0; ++i) {
            if (text.length() > i && text.charAt(text.length() - (i + 1)) == '\n') {
                --newlines;
            } else {
                break;
            }
        }
        return newlines;
    }

    /**
     * Inserts bold text.
     *
     * @param text Text to make bold
     * @return builder
     */
    public AsciiDoc boldText(final String text) {
        if (isEmpty(text)) {
            throw new AsciiDocException("text required");
        }
        builder.append(BOLD).append(text).append(BOLD);
        return this;
    }

    /**
     * Inserts italic text.
     *
     * @param text Text to make bold
     * @return Doc builder
     */
    public AsciiDoc italic(final String text) {
        if (isEmpty(text)) {
            throw new AsciiDocException("text required");
        }
        builder.append(ITALIC).append(text).append(ITALIC);
        return this;
    }

    /**
     * Inserts monospaced (e.g., code) text.
     *
     * @param text Text to make monospaced
     * @return Doc builder
     */
    public AsciiDoc mono(final String text) {
        if (isEmpty(text)) {
            throw new AsciiDocException("text required");
        }
        builder.append(MONO).append(text).append(MONO);
        return this;
    }

    /**
     * Inserts a document title.
     *
     * @param title Document title
     * @return Doc builder
     */
    public AsciiDoc documentTitle(final String title) {
        return line(AsciiDocSymbols.DOC_TITLE, title);
    }

    /**
     * Inserts a block title.
     *
     * @param title Block title
     * @return Doc builder
     */
    public AsciiDoc blockTitle(final String title) {
        return line(AsciiDocSymbols.BLOCK_TITLE, title);
    }

    /**
     * Inserts a section title, at a given level.
     *
     * @param title Section title
     * @param level Section level [1-5]
     * @return Doc builder
     */
    public AsciiDoc sectionTitle(final String title, final int level) {
        final AsciiDocSymbols symbol;
        // @Checkstyle:off
        switch (level) {
            case 1:
                return line(AsciiDocSymbols.SECTION_TITLE_1, title);
            case 2:
                return line(AsciiDocSymbols.SECTION_TITLE_2, title);
            case 3:
                return line(AsciiDocSymbols.SECTION_TITLE_3, title);
            case 4:
                return line(AsciiDocSymbols.SECTION_TITLE_4, title);
            case 5:
                return line(AsciiDocSymbols.SECTION_TITLE_5, title);
            default:
                throw new AsciiDocException("Unsupported section-level: " + level);
        }
        // @Checkstyle:on
    }

    /**
     * Inserts a section title, level 1.
     *
     * @param title Section title
     * @return Doc builder
     */
    public AsciiDoc sectionTitle1(final String title) {
        return line(AsciiDocSymbols.SECTION_TITLE_1, title);
    }

    /**
     * Inserts a section title, level 2.
     *
     * @param title Section title
     * @return Doc builder
     */
    public AsciiDoc sectionTitle2(final String title) {
        return line(AsciiDocSymbols.SECTION_TITLE_2, title);
    }

    /**
     * Inserts a section title, level 3.
     *
     * @param title Section title
     * @return Doc builder
     */
    public AsciiDoc sectionTitle3(final String title) {
        return line(AsciiDocSymbols.SECTION_TITLE_3, title);
    }

    /**
     * Inserts a section title, level 4.
     *
     * @param title Section title
     * @return Doc builder
     */
    public AsciiDoc sectionTitle4(final String title) {
        return line(AsciiDocSymbols.SECTION_TITLE_4, title);
    }

    /**
     * Inserts a section title, level 5.
     *
     * @param title Section title
     * @return Doc builder
     */
    public AsciiDoc sectionTitle5(final String title) {
        return line(AsciiDocSymbols.SECTION_TITLE_5, title);
    }

    /**
     * Inserts an example-block.
     *
     * @param content Content
     * @return Doc builder
     */
    public AsciiDoc exampleBlock(final String content) {
        return block(EXAMPLE, content);
    }

    /**
     * Inserts a listing-block.
     *
     * @param content Content
     * @return Doc builder
     */
    public AsciiDoc listingBlock(final String content) {
        return block(LISTING, content);
    }

    /**
     * Inserts a listing-block, with the source-code type (e.g., java, json, etc.) noted for formatting purposes.
     *
     * @param content Content
     * @param sourceType Type of source-code in the listing
     * @return Doc builder
     */
    public AsciiDoc listingBlock(final String content, final String sourceType) {
        if (isEmpty(content) || isEmpty(sourceType)) {
            throw new AsciiDocException("content and sourceType required");
        }
        builder.append("[source,")
                .append(sourceType)
                .append("]")
                .append(NEWLINE);
        return block(LISTING, content);
    }

    /**
     * Inserts a literal-block.
     *
     * @param content Content
     * @return Doc builder
     */
    public AsciiDoc literalBlock(final String content) {
        return block(LITERAL, content);
    }

    /**
     * Inserts a pass-through-block.
     *
     * @param content Content
     * @return Doc builder
     */
    public AsciiDoc passthroughBlock(final String content) {
        return block(PASSTHROUGH, content);
    }

    /**
     * Inserts a sidebar-block.
     *
     * @param content Content
     * @return Doc builder
     */
    public AsciiDoc sidebarBlock(final String content) {
        return block(SIDEBAR, content);
    }

    /**
     * Inserts a cross-reference anchor.
     *
     * @param id Anchor ID
     * @return Doc builder
     */
    public AsciiDoc anchor(final String id) {
        if (isEmpty(id)) {
            throw new AsciiDocException("id required");
        }
        if (containsWhitespace(id)) {
            throw new AsciiDocException("id contains whitespace");
        }
        builder.append(AsciiDocSymbols.ANCHOR_START)
                .append(id)
                .append(AsciiDocSymbols.ANCHOR_END);
        return this;
    }

    /**
     * Inserts a cross-reference anchor, with a custom
     * <a href="http://asciidoctor.org/docs/user-manual/#anchordef">xreflabel</a>.
     *
     * @param id Anchor ID
     * @param xreflabel Custom cross-reference link
     * @return Doc builder
     */
    public AsciiDoc anchor(final String id, final String xreflabel) {
        if (isEmpty(id) || isEmpty(xreflabel)) {
            throw new AsciiDocException("id and xreflabel required");
        }
        if (containsWhitespace(id)) {
            throw new AsciiDocException("id contains whitespace");
        }
        builder.append(AsciiDocSymbols.ANCHOR_START)
                .append(id)
                .append(',').append(xreflabel)
                .append(AsciiDocSymbols.ANCHOR_END);
        return this;
    }

    /**
     * Inserts a cross-reference link.
     *
     * @param anchorId Anchor ID
     * @return Doc builder
     */
    public AsciiDoc link(final String anchorId) {
        if (isEmpty(anchorId)) {
            throw new AsciiDocException("anchorId required");
        }
        if (containsWhitespace(anchorId)) {
            throw new AsciiDocException("anchorId contains whitespace");
        }
        builder.append(AsciiDocSymbols.CROSS_REF_START)
                .append(anchorId)
                .append(AsciiDocSymbols.CROSS_REF_END);
        return this;
    }

    /**
     * Inserts a cross-reference link, with a custom
     * <a href="http://asciidoctor.org/docs/user-manual/#anchordef">xreflabel</a>.
     *
     * @param anchorId Anchor ID
     * @param xreflabel Custom cross-reference link
     * @return Doc builder
     */
    public AsciiDoc link(final String anchorId, final String xreflabel) {
        if (isEmpty(anchorId) || isEmpty(xreflabel)) {
            throw new AsciiDocException("anchorId and xreflabel required");
        }
        if (containsWhitespace(anchorId)) {
            throw new AsciiDocException("anchorId contains whitespace");
        }
        builder.append(AsciiDocSymbols.CROSS_REF_START)
                .append(anchorId)
                .append(',').append(xreflabel)
                .append(AsciiDocSymbols.CROSS_REF_END);
        return this;
    }

    /**
     * Inserts a line for an unordered list, at level 1 indentation.
     *
     * @param content Line of content
     * @return Doc builder
     */
    public AsciiDoc unorderedList1(final String content) {
        line(UNORDERED_LIST_1, content);
        return this;
    }

    /**
     * Inserts a <a href="http://asciidoctor.org/docs/user-manual/#complex-list-content">list-continuation</a>,
     * for adding complex formatted content to a list.
     *
     * @return Doc builder
     */
    public AsciiDoc listContinuation() {
        final int newlinesAbove = requireTrailingNewlines(1, builder);
        if (newlinesAbove == 1) {
            builder.append(NEWLINE);
        }
        builder.append(LIST_CONTINUATION);
        return this;
    }

    /**
     * Inserts a horizontal-rule divider.
     *
     * @return Doc builder
     */
    public AsciiDoc horizontalRule() {
        final int newlinesAbove = requireTrailingNewlines(1, builder);
        if (newlinesAbove == 1) {
            builder.append(NEWLINE);
        }
        builder.append(HORIZONTAL_RULE)
            .append(NEWLINE);
        return this;
    }

    /**
     * Starts a table at the current position.
     *
     * @return Table builder
     */
    public AsciiDocTable tableStart() {
        return new AsciiDocTable(this, builder);
    }

    /**
     * Inserts an include-directive, given a relative path to a file.
     *
     * @param path Relative path segments
     * @return Doc builder
     */
    public AsciiDoc include(final String... path) {
        if (isEmpty(path)) {
            throw new AsciiDocException("path required");
        }
        builder.append(INCLUDE);
        builder.append(path[0]);
        for (int i = 1; i < path.length; ++i) {
            builder.append('/').append(path[i]);
        }
        builder.append("[]").append(NEWLINE);
        return this;
    }

    /**
     * Saves builder content to a file.
     *
     * @param outputDirPath Output directory
     * @param filename Filename
     * @throws IOException When error occurs while saving.
     */
    public void toFile(final Path outputDirPath, final String filename) throws IOException {
        final Path filePath = outputDirPath.resolve(filename);
        Files.createDirectories(outputDirPath);
        Files.createFile(filePath);
        Files.write(filePath, toString().getBytes(UTF_8));
    }

    /**
     * Converts builder content to a {@code String}.
     * <p>
     *
     * @return Doc builder content
     */
    @Override
    public String toString() {
        return builder.toString();
    }

    /**
     * Normalizes a name such that it can be used as a unique
     * <a href="https://en.wikipedia.org/wiki/Filename#Reserved_characters_and_words">filename</a>
     * and/or anchor in AsciiDoc. Names are converted to lower-case, unsupported characters are collapsed to a single
     * underscore-character, and parts are separated by an underscore.
     *
     * @param parts Name-parts to normalize
     * @return Normalized name
     */
    public static String normalizeName(final String... parts) {
        if (isEmpty(parts)) {
            throw new AsciiDocException("parts required");
        }
        String s = parts[0].toLowerCase(Locale.ROOT);
        for (int i = 1; i < parts.length; ++i) {
            s += NAMESPACE_DELIMITER + parts[i].toLowerCase(Locale.ROOT);
        }
        final String normalized;
        final Matcher m = POSIX_FILENAME_REPLACEMENT_PATTERN.matcher(s);
        if (m.find()) {
            normalized = m.replaceAll(NAMESPACE_DELIMITER);
        } else {
            normalized = s;
        }
        final Matcher mm = SQUASH_UNDERSCORES_PATTERN.matcher(normalized);
        return mm.find() ? mm.replaceAll(NAMESPACE_DELIMITER) : normalized;
    }
}
