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

/**
 * Enumeration of AsciiDoc markup symbols.
 */
public enum AsciiDocSymbols {
    /**
     * Cross-reference <a href="http://asciidoctor.org/docs/user-manual/#anchordef">anchor</a> start.
     */
    ANCHOR_START("[["),
    /**
     * Cross-reference <a href="http://asciidoctor.org/docs/user-manual/#anchordef">anchor</a> end.
     */
    ANCHOR_END("]]"),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#title">Block title</a>.
     */
    BLOCK_TITLE("."),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#bold-and-italic">Bold</a> text.
     */
    BOLD("*"),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#internal-cross-references">Cross-reference</a> start.
     */
    CROSS_REF_START("<<"),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#internal-cross-references">Cross-reference</a> end.
     */
    CROSS_REF_END(">>"),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#document-title">Document title</a>.
     */
    DOC_TITLE("= "),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#line-breaks">Hardbreaks</a> attribute preserves line-breaks
     * below its location in the document.
     */
    HARDBREAKS(":hardbreaks:"),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#built-in-blocks-summary">Example</a> block.
     */
    EXAMPLE("===="),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#horizontal-rules">Horizontal rule</a>.
     */
    HORIZONTAL_RULE("'''"),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#include-directive">Include</a>-directive.
     */
    INCLUDE("include::"),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#bold-and-italic">Italic</a> text.
     */
    ITALIC("_"),
    /**
     * Single <a href="http://asciidoctor.org/docs/user-manual/#line-breaks">line-break</a>.
     */
    LINE_BREAK(" +"),
    /**
     * Pre-formatted <a href="http://asciidoctor.org/docs/user-manual/#listing-blocks">listing</a> block.
     */
    LISTING("----"),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#complex-list-content">List continuation</a> marker, for
     * including complex markup in a list-item.
     */
    LIST_CONTINUATION("+"),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#built-in-blocks-summary">Literal</a> block.
     */
    LITERAL("...."),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#mono">Monospaced</a> text.
     */
    MONO("`"),
    /**
     * Unordered <a href="http://asciidoctor.org/docs/user-manual/#unordered-lists">list item</a> at level-1.
     *
     * @see #LIST_CONTINUATION
     */
    UNORDERED_LIST_1("* "),
    /**
     * UNIX newline character.
     */
    NEWLINE("\n"),
     /**
     * <a href="http://asciidoctor.org/docs/user-manual/#built-in-blocks-summary">Pass-through</a> block.
     */
    PASSTHROUGH("++++"),
    /**
     * Start/end of <a href="http://asciidoctor.org/docs/user-manual/#tables">table</a>.
     */
    TABLE("|==="),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#tables">Table</a>-cell delimiter.
     */
    TABLE_CELL("|"),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#user-toc">Table-of-Contents</a> directive.
     */
    TABLE_OF_CONTENTS(":toc:"),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#sections">Section</a> title, level 1.
     */
    SECTION_TITLE_1("== "),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#sections">Section</a> title, level 2.
     */
    SECTION_TITLE_2("=== "),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#sections">Section</a> title, level 3.
     */
    SECTION_TITLE_3("==== "),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#sections">Section</a> title, level 4.
     */
    SECTION_TITLE_4("===== "),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#sections">Section</a> title, level 5.
     */
    SECTION_TITLE_5("====== "),
    /**
     * <a href="http://asciidoctor.org/docs/user-manual/#built-in-blocks-summary">Sidebar</a> block.
     */
    SIDEBAR("****");

    private final String s;

    AsciiDocSymbols(final String s) {
        this.s = s;
    }

    /**
     * Returns the AsciiDocSymbols markup symbol associated with this item.
     *
     * @return AsciiDocSymbols markup symbol
     */
    @Override
    public String toString() {
        return s;
    }
}
