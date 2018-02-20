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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.doc.maven.utils;

import org.forgerock.doc.maven.utils.helper.FileFilterFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Map;

/**
 * Offers an alternative to <a href="http://www.sagehill.net/docbookxsl/Profiling.html">DocBook profiling</a>.
 * <br>
 * docbkx-tools relies on support in the DocBook XSL distribution,
 * which does not appear to include profiling support for webhelp output (v1.78.1).
 * docbkx-tools also handles the profiling in the same pass as the build,
 * meaning that dangling links might result in the built output.
 * <br>
 * This implementation can be used to handle profiling (conditional text) during pre-processing.
 * Build tools and validators then need not deal with DocBook profiles.
 */
public class Profiler {

    /** Profile maps: keys == attr names; values == attr values. **/
    private final Map<String, String> inclusions;
    private final Map<String, String> exclusions;

    /**
     * Constructs a profiler based on a profile inclusions configuration.
     * <br>
     * The profile maps do not restrict the keys (attribute names) to those supported by DocBook profiling.
     *
     * @param inclusions    Profile map: keys == attr names; values == attr values.
     *                      Lists of attr values must be space-separated.
     *                      If no inclusions are specified, set this to null.
     * @param exclusions    Profile map: keys == attr names; values == attr values.
     *                      Lists of attr values must be space-separated.
     *                      If no exclusions are specified, set this to null.
     */
    public Profiler(final Map<String, String> inclusions, final Map<String, String> exclusions) {
        this.inclusions = inclusions;
        this.exclusions = exclusions;
    }

    /**
     * Applies inclusions and exclusions if any to the XML files under the source directory.
     * <br>
     * This method changes the files in place, so the source should be a modifiable copy.
     *
     * @param xmlSourceDirectory    Source directory for XML files to profile.
     * @throws IOException          Failed to transform an XML file.
     */
    public void applyProfiles(final File xmlSourceDirectory) throws IOException {
        if (inclusions == null && exclusions == null) { // Nothing to do.
            return;
        }

        final FileFilter fileFilter = FileFilterFactory.getXmlFileFilter();
        Transformer transformer = new Transformer(getXsl(), fileFilter);
        transformer.update(xmlSourceDirectory);
    }

    /**
     * Returns an XSLT stylesheet suitable for profiling.
     * <br>
     * If no inclusions or exclusions are set, this makes a copy (identity transform).
     *
     * @return An XSLT stylesheet suitable for profiling.
     */
    private String getXsl() {
        final StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n")
                .append("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n")
                .append("  <xsl:template match=\"node()|@*\">\n")
                .append("    <xsl:copy>\n")
                .append("      <xsl:apply-templates select=\"node()|@*\"/>\n")
                .append("    </xsl:copy>\n")
                .append("  </xsl:template>\n");

        if (inclusions != null) {
            for (final String attribute : inclusions.keySet()) {
                sb.append("<xsl:template match=\"//*[")
                        .append(getInclusionsMatch(attribute, inclusions.get(attribute).split("\\s+")))
                        .append("]\"/>\n");
            }
        }

        if (exclusions != null) {
            for (final String attribute : exclusions.keySet()) {
                sb.append("<xsl:template match=\"//*[")
                        .append(getExclusionsMatch(attribute, exclusions.get(attribute).split("\\s+")))
                        .append("]\"/>\n");
            }
        }

        sb.append("</xsl:stylesheet>\n");
        return sb.toString();
    }

    /**
     * Returns a partial XPath expression to match inclusions.
     * <br>
     * This follows a sort of reversed logic to <em>exclude non-matching elements</em>:
     * <br>
     * attr "os", values { "linux", "unix" } results in "@os != 'linux' and @os != 'unix'"
     * <br>
     * attr "condition", values { "release" } results in "@condition != 'release'"
     *
     * @param attribute The attribute for which to include elements with matching values.
     * @param values    The attribute values to match for inclusion.
     * @return          A partial XPath expression to match inclusions.
     */
    private String getInclusionsMatch(final String attribute, final String[] values) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }

        if (values == null || values.length == 0) {
            return "";
        }

        if (values.length == 1) {
            return "@" + attribute + " != '" + values[0] + "'";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = values.length - 1; i > 0; i--) {
                sb.append("@").append(attribute).append(" != '").append(values[i]).append("' and ");
            }
            sb.append("@").append(attribute).append(" != '").append(values[0]).append("'");
            return sb.toString();
        }
    }

    /**
     * Returns a partial XPath expression to match exclusions.
     * <br>
     * attr "os", values { "linux", "unix" } results in "@os = 'linux' or @os = 'unix'"
     * <br>
     * attr "condition", values { "draft" } results in "@condition = 'draft'"
     *
     * @param attribute The attribute for which to exclude elements with matching values.
     * @param values    The attribute values to match for exclusion.
     * @return          A partial XPath expression to match exclusions.
     */
    private String getExclusionsMatch(final String attribute, final String[] values) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }

        if (values == null || values.length == 0) {
            return "";
        }

        if (values.length == 1) {
            return "@" + attribute + " = '" + values[0] + "'";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = values.length - 1; i > 0; i--) {
                sb.append("@").append(attribute).append(" = '").append(values[i]).append("' or ");
            }
            sb.append("@").append(attribute).append(" = '").append(values[0]).append("'");
            return sb.toString();
        }
    }

    /**
     * Applies an XSL transformation to the matching files.
     */
    private class Transformer extends XmlTransformer {
        /**
         * Constructs an updater to match DocBook XML files.
         * <br>
         * The files are updated in place.
         *
         * @param xsl           XSL as a String.
         * @param filterToMatch Filter to match XML files.
         */
        public Transformer(String xsl, FileFilter filterToMatch) {
            super(xsl, filterToMatch);
        }
    }
}
