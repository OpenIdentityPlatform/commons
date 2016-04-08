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
 * AsciiDoc table column-styles.
 */
public enum AsciiDocTableColumnStyles {
    /**
     * Block-level elements (paragraphs, delimited blocks and block macros) AsciiDoc content.
     */
    ASCII_DOC_CELL("a"),
    /**
     * Italic text.
     */
    EMPHASIS_CELL("e"),
    /**
     * Header styles applied.
     */
    HEADER_CELL("h"),
    /**
     * Literal block style.
     */
    LITERAL_CELL("l"),
    /**
     * Monospaced font.
     */
    MONO_CELL("m"),
    /**
     * No additional styles (default).
     */
    DEFAULT_CELL("d"),
    /**
     * Bold text.
     */
    STRONG_CELL("s"),
    /**
     * Cell treated like it was a verse block.
     */
    VERSE_CELL("v");

    private final String s;

    /**
     * Constructor with column-style character.
     *
     * @param s Column-style character
     */
    AsciiDocTableColumnStyles(String s) {
        this.s = s;
    }

    /**
     * Returns the character associated with the table column-style.
     *
     * @return column-style character
     */
    @Override
    public String toString() {
        return s;
    }
}
