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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.http.util;

import static java.lang.Long.*;
import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.util.Json.*;
import static org.forgerock.json.JsonValue.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class JsonTest {

    private static final String INVALID_JSON = "invalid json";
    private static final String JSON_CONTENT =
            "{ 'name': 'outputHandler', 'type': 'ClientHandler' }";

    @DataProvider
    private Object[][] emptyJson() {
        return new Object[][] {
            { "" },
            { " " },
            { "\n" },
            { "\r\n" } };
    }

    @DataProvider
    private Object[][] newLine() {
        return new Object[][] {
            { "\r\n" },
            { "\n" } };
    }

    @DataProvider
    private Object[][] invalidJson() {
        return new Object[][] {
            { "{ 'name':'outputHandler', 'type': 'ClientHandler }," }, // Missing quote within the sample.
            { "{ 'name':, 'type': 'ClientHandler' }," }, // A comma too many.
            { "{ 'name':, 'type': 'ClientHandler' }" }, // Missing attribute value.
            { "{ :'outputHandler', 'type': 'ClientHandler' }" }}; // Missing attribute name.
    }

    @Test(dataProvider = "emptyJson",
          expectedExceptions = IOException.class)
    public void shouldFailWhenSerializingEmptyString(final String json) throws Exception {
        assertThat(readJson(from(json))).isNull();
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldFailToReadInvalidJson() throws Exception {
        readJson(from(INVALID_JSON));
    }

    @DataProvider
    private Object[][] primitiveJsonValues() {
        // @Checkstyle:off
        return new Object[][] {
                { "\"Hello World\"", "Hello World" },
                { "true", Boolean.TRUE },
                { "false", Boolean.FALSE },
                { "null", null },
                { "42", 42 },
                { "-42", -42 },
                { "1.3", 1.3 },
        };
        // @Checkstyle:on
    }

    @Test(dataProvider = "primitiveJsonValues")
    public void shouldReadPrimitiveValues(String input, Object expected) throws Exception {
        assertThat(readJson(from(input))).isEqualTo(expected);
    }

    @Test(dataProvider = "invalidJson", expectedExceptions = IOException.class)
    public void testFailParsingInvalidJson(final String invalid)
            throws Exception {
        readJson(from(invalid));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldParsingSucceedWhenCommentsEnabledBlockCommentInsideBlock() throws Exception {
        final String sample =
                "{ 'name': 'outputHandler', 'type': 'ClientHandler' /* This is a comment */ } ";
        final Map<String, Object> json = (Map<String, Object>) readJsonLenient(from(sample));
        assertThat(json.get("name")).isEqualTo("outputHandler");
        assertThat(json.get("type")).isEqualTo("ClientHandler");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldParsingSucceedWhenCommentsEnabledBlockCommentOutsideBlock() throws Exception {
        final String sample =
                "{ 'name': 'outputHandler', 'type': 'ClientHandler' } /* This is a comment */ ";
        final Map<String, Object> json = (Map<String, Object>) readJsonLenient(from(sample));
        assertThat(json.get("name")).isEqualTo("outputHandler");
        assertThat(json.get("type")).isEqualTo("ClientHandler");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldParsingSucceedWhenCommentsEnabledLineCommentOutsideBlock() throws Exception {
        final String sample =
                "{ 'name': 'outputHandler', 'type': 'ClientHandler' } // This is a comment";
        final Map<String, Object> json = (Map<String, Object>) readJsonLenient(from(sample));
        assertThat(json.get("name")).isEqualTo("outputHandler");
        assertThat(json.get("type")).isEqualTo("ClientHandler");
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldParsingFailWhenCommentsEnabledLineCommentInsideBlock() throws Exception {
        final String sample =
                "{ 'name': 'outputHandler', 'type': 'ClientHandler' // This is a comment }";
        readJsonLenient(from(sample));
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldParsingFailWhenCommentsDisabled() throws Exception {
        final String sample =
                "{ 'name': 'outputHandler', 'type': 'ClientHandler' /* This is a comment */ } ";
        readJson(from(sample));
    }

    @Test
    public void shouldCommentsIgnoredByParsing() throws IOException {
        final String sample =
                "{ 'name': 'outputHandler', 'type': 'ClientHandler' /* This is a comment */ } ";

        final String sample2 =
                "{ 'name': 'outputHandler', 'type': 'ClientHandler' } ";

        assertThat(readJsonLenient(from(sample))).isEqualTo(readJson(from(sample2)));
    }

    @Test(dataProvider = "newLine", expectedExceptions = IOException.class)
    public void shouldThrowIoExceptionWhenContainingEol(final String newLine) throws IOException {
        readJson(singleQuotesToDouble("{ 'comment': 'Two lines " + newLine + "of comment' }"));
    }

    /**
     * The lenient parser should allow use of EOL (included in the string
     * value). It also allows use of ASCII chars < 32.
     * e.g.
     *
     * <pre>
     * "source": "import org.forgerock.json.fluent.JsonValue;
     *            logger.info(exchange.token.asJsonValue() as String);"
     * </pre>
     */
    @Test(dataProvider = "newLine")
    @SuppressWarnings("unchecked")
    public void shouldSucceedWhenContainingEol(final String newLine) throws IOException {
        final Map<String, Object> jackson =
                (Map<String, Object>) readJsonLenient(from("{ 'comment': 'Two lines " + newLine + "of comment' }"));
        assertThat((String) jackson.get("comment")).matches("Two lines [\r\n]{1,2}of comment$");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAllowsSimpleQuote() throws Exception {
        final String sample = "{ 'condition': true } ";
        final Map<String, Object> jackson = (Map<String, Object>) readJsonLenient(new StringReader(sample));

        assertThat((Boolean) jackson.get("condition")).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFromString() throws Exception {
        final Map<String, Object> jackson = (Map<String, Object>) readJson(from(JSON_CONTENT));

        assertThat(jackson.get("name")).isEqualTo("outputHandler");
        assertThat(jackson.get("type")).isEqualTo("ClientHandler");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStringAllowNullAttributeValue() throws Exception {

        final String rawJson = "{ 'name': null, 'type': 'ClientHandler' } ";
        final Map<String, Object> jackson = (Map<String, Object>) readJson(from(rawJson));

        assertThat(jackson.get("name")).isNull();
        assertThat(jackson.get("type")).isEqualTo("ClientHandler");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testJsonValueNumber() throws Exception {

        final String rawJson = "{ 'number': 23, 'other': '27' } ";
        final Map<String, Object> jackson = (Map<String, Object>) readJson(from(rawJson));

        assertThat(jackson.get("number")).isEqualTo(23);
        assertThat(jackson.get("other")).isEqualTo("27");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testJsonValueSupportsLong() throws Exception {

        final String rawJson = "{ 'long': '" + MAX_VALUE + "'}";
        final Map<String, Object> jackson = (Map<String, Object>) readJson(from(rawJson));

        assertThat(parseLong(jackson.get("long").toString())).isEqualTo(MAX_VALUE);
    }

    @Test
    public void shouldSucceedToParseArray() throws IOException {
        final String rawJson = singleQuotesToDouble("['The', 100 ]");
        final Object jsonNode = readJson(rawJson);
        assertThat(jsonNode).isInstanceOf(List.class);
        final List<?> values = (List<?>) jsonNode;
        assertThat(values.get(0)).isEqualTo("The");
        assertThat(values.get(1)).isEqualTo(100);
    }

    @Test
    public void shouldSucceedToParseArray2() throws IOException {
        final String rawJson = singleQuotesToDouble("['The', { 'data':'outputHandler', 'array':[]} ]");
        final Object jsonNode = readJson(rawJson);
        assertThat(jsonNode).isInstanceOf(List.class);
        final List<?> values = (List<?>) jsonNode;
        assertThat(values.get(0)).isEqualTo("The");
        assertThat(values.get(1)).isInstanceOf(LinkedHashMap.class);
        @SuppressWarnings("unchecked")
        final LinkedHashMap<String, Object> val2 = (LinkedHashMap<String, Object>) values.get(1);
        assertThat(val2.get("data")).isEqualTo("outputHandler");
        //assertThat(val2.get("array")).isInstanceOf(LinkedList.class);
    }

    @Test
    public void testParsesBoolean() throws IOException {
        final Object jsonNode = readJson("true");
        assertThat(jsonNode).isInstanceOf(Boolean.class).isEqualTo(true);
    }

    /**
     * Returns a string reader from the rawJson attribute.
     *
     * @param rawJson
     *            Json string to read (can be single quoted).
     * @return a string reader.
     */
    private Reader from(final String rawJson) {
        return new StringReader(singleQuotesToDouble(rawJson));
    }

    /** Remove single quotes from a given string. */
    private static String singleQuotesToDouble(final String value) {
        return value.replace('\'', '\"');
    }

    @Test
    public void testJsonCompatibilityBoxedPrimitiveType() throws Exception {
        checkJsonCompatibility("boolean", true);
        checkJsonCompatibility("integer", 1);
        checkJsonCompatibility("short", (short) 12);
        checkJsonCompatibility("long", -42L);
        checkJsonCompatibility("float", 42.3F);
        checkJsonCompatibility("double", 3.14159D);
        checkJsonCompatibility("char", 'a');
        checkJsonCompatibility("byte", (byte) 'c');
    }

    @Test
    public void testJsonCompatibilityWithCharSequences() throws Exception {
        checkJsonCompatibility("string", "a string");
        checkJsonCompatibility("string-buffer", new StringBuffer("a string buffer"));
        checkJsonCompatibility("string-builder", new StringBuilder("a string builder"));
    }

    @Test
    public void testJsonCompatibilityWithArrayOfString() throws Exception {
        String[] strings = {"one", "two", "three"};
        checkJsonCompatibility("array", strings);
    }

    @Test
    public void testJsonCompatibilityWithListOfString() throws Exception {
        String[] strings = {"one", "two", "three"};
        checkJsonCompatibility("array", asList(strings));
    }

    @Test
    public void testJsonCompatibilityWithMapOfString() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("one", "one");
        map.put("two", "two");
        map.put("three", "three");
        checkJsonCompatibility("map", map);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldNotAcceptUnsupportedTypes() throws Exception {
        checkJsonCompatibility("object", new Object());
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = ".*'list\\[1\\]'.*")
    public void shouldWriteErrorTrailForIncorrectList() throws Exception {
        checkJsonCompatibility("list", asList("one", new Object(), "three"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = ".*'map/object'.*")
    public void shouldWriteErrorTrailForIncorrectMap() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("one", "one");
        map.put("object", new Object());
        checkJsonCompatibility("map", map);
    }

    @Test
    public void shouldWriteJsonValueObject() throws Exception {
        JsonValue value = json(object(field("key", "value")));
        assertThat(new String(Json.writeJson(value), StandardCharsets.UTF_8)).isEqualTo("{\"key\":\"value\"}");
    }
}
