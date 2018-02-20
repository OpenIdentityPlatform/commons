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

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("javadoc")
public class StringUtilsTest {

    private ArrayList<String> textExtract;
    private ArrayList<String> toBeIncluded;
    private ArrayList<String> startMarkerOnly;
    private ArrayList<String> roughIndentation;
    private ArrayList<String> json;
    private ArrayList<String> jsonExtract;

    @BeforeTest
    public void prepareTextExtract() {
        textExtract = new ArrayList<String>();
        textExtract.add("// To be included");
        textExtract.add("");
        textExtract.add("    This is some arbitrary text");
        textExtract.add("    that includes an ampersand &,");
        textExtract.add("    that is surrounded by whitespace,");
        textExtract.add("        and that has an indent on the last line. ");
        textExtract.add("");
        textExtract.add("// To be included");
        textExtract.add("");
        textExtract.add(" // Rough indentation");
        textExtract.add(" This text is indented.");
        textExtract.add("  But the indentation is not regular.");
        textExtract.add("       So some of the lines");
        textExtract.add("    Are indented further than others.");
        textExtract.add("  And yet all the lines");
        textExtract.add("               have at least a space of indentation.");
        textExtract.add(" // Rough indentation");
        textExtract.add("");
        textExtract.add("// Start marker only");
        textExtract.add("Perhaps leading tabs should cause an error,");
        textExtract.add("because in general we don't know");
        textExtract.add("how many spaces a tab represents in your editor.");
        textExtract.add("Convert leading tabs to spaces.");
        textExtract.add("");
        textExtract.add("# Tabs and spaces");
        textExtract.add("");
        /* @Checkstyle:ignoreFor 1 */
        textExtract.add("	This line starts with a tab.");
        textExtract.add("    This line starts with spaces.");
        textExtract.add("");
        textExtract.add("# Tabs and spaces");

        toBeIncluded = new ArrayList<String>();
        toBeIncluded.add("    This is some arbitrary text");
        toBeIncluded.add("    that includes an ampersand &,");
        toBeIncluded.add("    that is surrounded by whitespace,");
        toBeIncluded.add("        and that has an indent on the last line. ");

        startMarkerOnly = new ArrayList<String>();
        startMarkerOnly.add("Perhaps leading tabs should cause an error,");
        startMarkerOnly.add("because in general we don't know");
        startMarkerOnly.add("how many spaces a tab represents in your editor.");
        startMarkerOnly.add("Convert leading tabs to spaces.");
        startMarkerOnly.add("");
        startMarkerOnly.add("# Tabs and spaces");
        startMarkerOnly.add("");
        /* @Checkstyle:ignoreFor 1 */
        startMarkerOnly.add("	This line starts with a tab.");
        startMarkerOnly.add("    This line starts with spaces.");
        startMarkerOnly.add("");
        startMarkerOnly.add("# Tabs and spaces");

        // Outdented 1 space
        roughIndentation = new ArrayList<String>();
        roughIndentation.add("This text is indented.");
        roughIndentation.add(" But the indentation is not regular.");
        roughIndentation.add("      So some of the lines");
        roughIndentation.add("   Are indented further than others.");
        roughIndentation.add(" And yet all the lines");
        roughIndentation.add("              have at least a space of indentation.");

        json = new ArrayList<String>();
        json.add("{");
        json.add("    \"heap\": {");
        json.add("        \"objects\": [");
        json.add("            {\"comment\": \"#start\"},");
        json.add("            {");
        json.add("                \"name\": \"LoginChain\",");
        json.add("                \"type\": \"Chain\",");
        json.add("                \"config\": {");
        json.add("                    \"filters\": [");
        json.add("                        {");
        json.add("                            \"type\": \"StaticRequestFilter\",");
        json.add("                            \"config\": {");
        json.add("                                \"method\": \"POST\",");
        json.add("                                \"uri\": \"http://www.example.com:8081\",");
        json.add("                                \"form\": {");
        json.add("                                    \"username\": [");
        json.add("                                        \"demo\"");
        json.add("                                    ],");
        json.add("                                    \"password\": [");
        json.add("                                        \"changeit\"");
        json.add("                                    ]");
        json.add("                                }");
        json.add("                            }");
        json.add("                        }");
        json.add("                    ],");
        json.add("                    \"handler\": \"ClientHandler\"");
        json.add("                }");
        json.add("            }");
        json.add("            ,{\"comment\": \"#end\"}");
        json.add("        ]");
        json.add("    },");
        json.add("    \"handler\": \"LoginChain\",");
        json.add("    \"condition\": \"${matches(exchange.request.uri.path, '^/static')}\"");
        json.add("}");

        jsonExtract = new ArrayList<String>();
        jsonExtract.add("            {");
        jsonExtract.add("                \"name\": \"LoginChain\",");
        jsonExtract.add("                \"type\": \"Chain\",");
        jsonExtract.add("                \"config\": {");
        jsonExtract.add("                    \"filters\": [");
        jsonExtract.add("                        {");
        jsonExtract.add("                            \"type\": \"StaticRequestFilter\",");
        jsonExtract.add("                            \"config\": {");
        jsonExtract.add("                                \"method\": \"POST\",");
        jsonExtract.add("                                \"uri\": \"http://www.example.com:8081\",");
        jsonExtract.add("                                \"form\": {");
        jsonExtract.add("                                    \"username\": [");
        jsonExtract.add("                                        \"demo\"");
        jsonExtract.add("                                    ],");
        jsonExtract.add("                                    \"password\": [");
        jsonExtract.add("                                        \"changeit\"");
        jsonExtract.add("                                    ]");
        jsonExtract.add("                                }");
        jsonExtract.add("                            }");
        jsonExtract.add("                        }");
        jsonExtract.add("                    ],");
        jsonExtract.add("                    \"handler\": \"ClientHandler\"");
        jsonExtract.add("                }");
        jsonExtract.add("            }");
    }

    private String inputLine = "<tag xml:id='id'>&amp; \"quotes\"</tag>";
    private String escapedInputLine =
            "&lt;tag xml:id=&apos;id&apos;&gt;&amp;amp; &quot;quotes&quot;&lt;/tag&gt;";

    @Test
    public void escapeSomeXml() {
        String outputXml = StringUtils.escapeXml(inputLine);
        assertThat(outputXml).isEqualTo(escapedInputLine);
    }

    @Test
    public void escapeEmptyLines() {
        ArrayList<String> empty = new ArrayList<String>();
        ArrayList<String> output = StringUtils.escapeXml(empty);
        assertThat(output).isEmpty();
    }

    @Test
    public void escapeMultipleLines() {
        String anotherLine = "Nothing to see here. Move along.";

        ArrayList<String> inputLines = new ArrayList<String>();
        inputLines.add(inputLine);
        inputLines.add(anotherLine);

        ArrayList<String> escapedLines = new ArrayList<String>();
        escapedLines.add(escapedInputLine);
        escapedLines.add(anotherLine);

        ArrayList<String> outputLines = StringUtils.escapeXml(inputLines);
        assertThat(outputLines).isEqualTo(escapedLines);
    }

    @Test
    public void extractQuoteNoMarkers() throws IOException, URISyntaxException {

        ArrayList<String> fileContent = FileUtils.getStrings(
                new File(getClass().getResource("/file.txt").toURI()));
        ArrayList<String> theExtract =
                StringUtils.extractQuote(fileContent, null, null);

        assertThat(theExtract).isEqualTo(textExtract);
    }

    @Test
    public void extractInlineQuote() {
        // "Perhaps leading tabs should cause an error,"
        ArrayList<String> theQuote = new ArrayList<String>();
        theQuote.add("leading tabs should cause an");
        ArrayList<String> theExtract =
                StringUtils.extractQuote(textExtract, "Perhaps", "error");

        assertThat(theExtract).isEqualTo(theQuote);
    }

    @Test
    public void extractQuoteStartAndEndSeparateLines() {
        ArrayList<String> theExtract =
                StringUtils.extractQuote(textExtract, "// To be included");

        assertThat(theExtract).isEqualTo(toBeIncluded);
    }

    @Test
    public void extractQuoteStartAndEndInMiddleOfSeparateLines() {
        ArrayList<String> theExtract =
                StringUtils.extractQuote(json, "#start", "#end");

        assertThat(theExtract).isEqualTo(jsonExtract);
    }

    @Test
    public void extractQuoteOnlyStartMarker() {
        ArrayList<String> theExtract =
                StringUtils.extractQuote(textExtract, "// Start marker only");

        assertThat(theExtract).isEqualTo(startMarkerOnly);
    }

    @Test
    public void extractQuoteEmbeddedStartMarker() {
        ArrayList<String> theExtract =
                StringUtils.extractQuote(textExtract, "marker only");

        assertThat(theExtract).isEqualTo(startMarkerOnly);
    }

    @Test
    public void extractQuoteOnlyEndMarker() {
        ArrayList<String> theExtract =
                StringUtils.extractQuote(textExtract, "DOES NOT EXIST", "end");

        assertThat(theExtract).isEmpty();
    }

    @Test
    public void indentLines() {
        ArrayList<String> text = new ArrayList<String>();
        text.add("A line.");
        text.add("Another line.");
        text.add("");

        final String indent = "    ";

        ArrayList<String> result = StringUtils.indent(text, indent);

        ArrayList<String> indentedText = new ArrayList<String>();
        indentedText.add("    A line.");
        indentedText.add("    Another line.");
        indentedText.add("    ");

        assertThat(result).isEqualTo(indentedText);
    }

    @Test
    public void indentIsNull() {
        ArrayList<String> text = new ArrayList<String>();
        text.add("A line.");
        text.add("Another line.");
        text.add("");

        ArrayList<String> result = StringUtils.indent(text, null);

        assertThat(result).isEqualTo(text);
    }

    @Test
    public void outdentExtract() {
        ArrayList<String> theExtract =
                StringUtils.outdent(
                        StringUtils.extractQuote(
                                textExtract, "// Rough indentation"));

        assertThat(theExtract).isEqualTo(roughIndentation);
    }

    @Test
    void outdentWithInitialTabFail() {
        try {
            StringUtils.outdent(
                    StringUtils.extractQuote(textExtract, "# Tabs and spaces"));
        } catch (IllegalArgumentException e) {
            /* @Checkstyle:ignoreFor 2 */
            assertThat(e.getMessage())
                    .matches("	This line starts with a tab.");
        }
    }

    @Test
    void removeEmptySpace() {
        String test = "This is a test.";
        String empty = " \t";

        assertThat(test).isEqualTo(StringUtils.removeEmptySpace(test));
        assertThat(StringUtils.removeEmptySpace(empty)).isEmpty();
    }
}
