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
import static org.forgerock.api.util.ValidationUtil.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.util.Reject;

/**
 * AsciiDoc table builder [<a href="http://asciidoctor.org/docs/user-manual/#tables">ref</a>], which defers insertion
 * of the table, at the end of the parent document, until {@link #tableEnd()} is called.
 * <p>
 * This class is not thread-safe.
 * </p>
 */
public class AsciiDocTable {

    /**
     * <em>Small</em> column-width for use with {@link #columnWidths(int...)}.
     */
    public static final int COLUMN_WIDTH_SMALL = 1;

    /**
     * <em>Medium</em> column-width (2x {@link #COLUMN_WIDTH_SMALL}) for use with {@link #columnWidths(int...)}.
     */
    public static final int COLUMN_WIDTH_MEDIUM = 2;

    private static final Pattern TABLE_CELL_SYMBOL_PATTERN = Pattern.compile("\\|");

    private final AsciiDoc asciiDoc;
    private final StringBuilder builder;
    private final List<String> cells;
    private int[] columnWidths;
    private Integer columnsPerRow;
    private String title;
    private boolean hasHeader;

    AsciiDocTable(final AsciiDoc asciiDoc, final StringBuilder builder) {
        this.asciiDoc = Reject.checkNotNull(asciiDoc);
        this.builder = Reject.checkNotNull(builder);
        cells = new ArrayList<>();
    }

    /**
     * Sets a table-title.
     *
     * @param title Table-title
     * @return Table builder
     */
    public AsciiDocTable title(final String title) {
        if (isEmpty(title)) {
            throw new AsciiDocException("title required");
        }
        if (this.title != null) {
            throw new AsciiDocException("title already defined");
        }
        this.title = title;
        return this;
    }

    /**
     * Sets the column headers, where blank entries can be null/empty, but the length of the headers array must
     * be equal to the number of columns in the table.
     *
     * @param columnHeaders Column headers
     * @return Table builder
     */
    public AsciiDocTable headers(final List<String> columnHeaders) {
        return headers(columnHeaders.toArray(new String[columnHeaders.size()]));
    }

    /**
     * Sets the column headers, where blank entries can be null/empty, but the length of the headers array must
     * be equal to the number of columns in the table.
     *
     * @param columnHeaders Column headers
     * @return Table builder
     */
    public AsciiDocTable headers(final String... columnHeaders) {
        if (isEmpty(columnHeaders)) {
            throw new AsciiDocException("columnHeaders required");
        }
        if (hasHeader) {
            throw new AsciiDocException("headers already defined");
        }
        if (columnsPerRow == null) {
            columnsPerRow = columnHeaders.length;
        } else if (columnsPerRow != columnHeaders.length) {
            throw new AsciiDocException("columnHeaders.length != columnsPerRow");
        }
        hasHeader = true;

        // add to front of cells
        cells.add(null);
        for (int i = columnHeaders.length - 1; i > -1; --i) {
            cells.add(0, TABLE_CELL + normalizeColumnCell(columnHeaders[i]));
        }
        return this;
    }

    /**
     * Sets number of columns per row, which is implicitly set by {@link #headers(String...)} and
     * {@link #columnWidths(int...)}.
     * <p>
     * This value can only be set once.
     * </p>
     *
     * @param columnsPerRow Columns per row
     * @return Table builder
     */
    public AsciiDocTable columnsPerRow(final int columnsPerRow) {
        if (this.columnsPerRow != null) {
            throw new AsciiDocException("columnsPerRow already defined");
        }
        if (this.columnsPerRow < 1) {
            throw new AsciiDocException("columnsPerRow < 1");
        }
        this.columnsPerRow = columnsPerRow;
        return this;
    }

    /**
     * Sets the widths for all columns-per-row, which can be a proportional integer (the default is 1) or a
     * percentage (1 to 99).
     *
     * @param columnWidths An entry for each column-per row in value range [1,99]
     * @return Table builder
     */
    public AsciiDocTable columnWidths(final List<Integer> columnWidths) {
        final int[] array = new int[columnWidths.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = columnWidths.get(i);
        }
        return columnWidths(array);
    }

    /**
     * Sets the widths for all columns-per-row, which can be a proportional integer (the default is 1) or a
     * percentage (1 to 99).
     *
     * @param columnWidths An entry for each column-per row in value range [1,99]
     * @return Table builder
     */
    public AsciiDocTable columnWidths(final int... columnWidths) {
        if (columnWidths == null || columnWidths.length == 0) {
            throw new AsciiDocException("columnWidths required");
        }
        for (final int w : columnWidths) {
            if (w < 1 || w > 99) {
                throw new AsciiDocException("columnWidths values must be within range [1,99]");
            }
        }
        if (columnsPerRow != null) {
            if (columnsPerRow != columnWidths.length) {
                throw new AsciiDocException("columnWidths.length != columnsPerRow");
            }
        } else {
            columnsPerRow = columnWidths.length;
        }
        this.columnWidths = columnWidths;
        return this;
    }

    /**
     * Inserts a column-cell.
     *
     * @param columnCell Column-cell or {@code null} for empty cell
     * @return Table builder
     */
    public AsciiDocTable columnCell(final String columnCell) {
        cells.add(TABLE_CELL + normalizeColumnCell(columnCell));
        return this;
    }

    /**
     * Inserts a column-cell, with a style.
     *
     * @param columnCell Column-cell or {@code null} for empty cell
     * @param style Column-style
     * @return Table builder
     */
    public AsciiDocTable columnCell(final String columnCell, final AsciiDocTableColumnStyles style) {
        cells.add(style.toString() + TABLE_CELL + normalizeColumnCell(columnCell));
        return this;
    }

    /**
     * Adds an optional space to visually delineate the end of a row in the generated markup. The intention is that
     * this method would be called after adding all columns for a given row.
     *
     * @return table builder
     */
    public AsciiDocTable rowEnd() {
        cells.add(null);
        return this;
    }

    private String normalizeColumnCell(final String columnCell) {
        if (isEmpty(columnCell)) {
            // allow for empty cells
            return "";
        }
        // escape TABLE_CELL symbols
        final Matcher m = TABLE_CELL_SYMBOL_PATTERN.matcher(columnCell);
        return m.find() ? m.replaceAll("\\" + TABLE_CELL) : columnCell;
    }

    /**
     * Completes the table being built, and inserts it at the end of the parent document.
     *
     * @return Doc builder
     */
    public AsciiDoc tableEnd() {
        if (columnsPerRow == null) {
            throw new AsciiDocException("columnsPerRow has not be defined");
        }

        // table configuration (e.g., [cols="2*", caption="", options="header"])
        builder.append("[cols=\"");
        if (columnWidths != null) {
            // unique column widths
            builder.append(columnWidths[0]);
            for (int i = 1; i < columnWidths.length; ++i) {
                builder.append(',').append(columnWidths[i]);
            }
        } else {
            // each column same width
            builder.append(columnsPerRow).append("*");
        }
        builder.append("\", caption=\"\", options=\"");
        if (hasHeader) {
            builder.append("header");
        }
        builder.append("\"]").append(NEWLINE);

        // optional title
        if (title != null) {
            builder.append(".").append(title).append(NEWLINE);
        }

        // cells
        builder.append(TABLE).append(NEWLINE);
        if (cells.get(cells.size() - 1) == null) {
            // remove trailing "row spacer"
            cells.remove(cells.size() - 1);
        }
        for (final String item : cells) {
            if (item != null) {
                // null is an optional "row spacer" (see endRow), otherwise cells will be non-null
                builder.append(item);
            }
            builder.append(NEWLINE);
        }
        builder.append(TABLE).append(NEWLINE);

        return asciiDoc;
    }

}
