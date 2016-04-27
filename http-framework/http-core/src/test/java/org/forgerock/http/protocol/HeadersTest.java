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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.http.protocol;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.forgerock.http.header.ConnectionHeader;
import org.forgerock.http.header.ContentLengthHeader;
import org.forgerock.http.header.CookieHeader;
import org.forgerock.http.header.GenericHeader;
import org.forgerock.http.io.IO;
import org.testng.annotations.Test;

public class HeadersTest {

    @Test
    public void testScriptability() throws Exception {
        // Given
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("groovy");
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        IO.stream(HeadersTest.class.getResourceAsStream("HeadersTest.scriptability.groovy"), content);

        // When
        engine.eval(new String(content.toByteArray(), "UTF-8"));
    }

    @Test
    public void testPutListStrings() throws Exception {
        // Given
        Headers headers = new Headers();

        // When
        headers.put("Header", asList("One", "Two", "Three"));

        // Then
        assertThat(headers.get("Header").getValues()).containsExactly("One", "Two", "Three");
    }

    @Test
    public void testGetHeaders() throws Exception {
        // Given
        Headers headers = new Headers();
        headers.put("Header", asList("One", "Two", "Three"));

        // When
        Header result = headers.get("Header");

        // Then
        assertThat(result).isInstanceOf(GenericHeader.class);
        assertThat(result.getValues()).hasSize(3).containsExactly("One", "Two", "Three");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetUnknownHeader() throws Exception {
        // Given
        Headers headers = new Headers();
        headers.put("Header", asList("One", "Two", "Three"));

        // When
        headers.get(GenericHeader.class);

        // Then - exception
    }

    @Test
    public void testGetSingle() throws Exception {
        // Given
        Headers headers = new Headers();
        headers.put("Cookie", asList("One"));

        // When
        CookieHeader result = headers.get(CookieHeader.class);

        // Then
        assertThat(result.getCookies()).hasSize(1);
    }

    @Test
    public void testGetSingleWithNone() throws Exception {
        // Given
        Headers headers = new Headers();

        // When
        CookieHeader result = headers.get(CookieHeader.class);

        // Then
        assertThat(result).isNull();
    }

    @Test
    public void testGetFirstString() throws Exception {
        // Given
        Headers headers = new Headers();
        headers.put("Header", asList("One", "Two", "Three"));

        // When
        String result = headers.get("Header").getValues().iterator().next();

        // Then
        assertThat(result).isEqualTo("One");
    }

    @Test
    public void testGetFirstStringWithNone() throws Exception {
        // Given
        Headers headers = new Headers();

        // When
        Header result = headers.get("Header");

        // Then
        assertThat(result).isNull();
    }

    @Test
    public void testPutSingleWithString() throws Exception {
        // Given
        Headers headers = new Headers();

        // When
        headers.put("Header", "Value");

        // Then
        assertThat(headers.get("Header").getValues()).containsExactly("Value");
    }

    @Test
    public void testPutSingleHeader() throws Exception {
        // Given
        Headers headers = new Headers();

        // When
        headers.add(new GenericHeader("Header", "Value"));

        // Then
        assertThat(headers.get("Header").getValues()).containsExactly("Value");
    }

    @Test
    public void testPutSingleWithStringAsObject() throws Exception {
        // Given
        Headers headers = new Headers();

        // When
        headers.put("Header", (Object) "Value");

        // Then
        assertThat(headers.get("Header").getValues()).containsExactly("Value");
    }

    @Test
    public void testPutSingleWithHeader() throws Exception {
        // Given
        Headers headers = new Headers();

        // When
        headers.put("Header", new GenericHeader("Header", "Value"));

        // Then
        assertThat(headers.get("Header").getValues()).containsExactly("Value");
    }

    @Test
    public void testAddEmptyValue() throws Exception {
        // Given
        Headers headers = new Headers();

        // When
        headers.add(new GenericHeader("Header", ""));

        // Then
        assertThat(headers.get("Header").getValues()).containsOnly("");
    }

    @Test
    public void testPutSingleWithListOverExisting() throws Exception {
        // Given
        Headers headers = new Headers();
        headers.put("Cookie", asList("One", "Two", "Three"));

        // When
        headers.put("Cookie", asList("Four", "Five", "Six"));

        // Then
        assertThat(headers.get("Cookie").getValues()).containsExactly("Four=null; Five=null; Six=null");
    }

    @Test
    public void testPutSingleWithListOverExistingWithNoFactory() throws Exception {
        // Given
        Headers headers = new Headers();
        headers.put("Header", asList("A", "B", "C"));

        // When
        headers.put("Header", asList("One", "Two", "Three"));

        // Then
        assertThat(headers.get("Header").getValues()).containsExactly("One", "Two", "Three");
    }

    @Test
    public void testPutSingleWithArray() throws Exception {
        // Given
        Headers headers = new Headers();

        // When
        headers.put("Header", new String[]{"One", "Two", "Three"});

        // Then
        assertThat(headers.get("Header").getValues()).containsExactly("One", "Two", "Three");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPutSingleWithObject() throws Exception {
        // Given
        Headers headers = new Headers();

        // When
        headers.put("Header", new Object());

        // Then - exception
    }

    @Test
    public void testPutListMultipleNotSupported() throws Exception {
        // Given
        Headers headers = new Headers();

        // When
        headers.put("Connection", asList("Four", "Five", "Six"));

        // Then
        assertThat(headers.get(ConnectionHeader.class).getValues()).containsOnly("Four", "Five", "Six");
    }

    @Test
    public void testPutListStringsExisting() throws Exception {
        // Given
        Headers headers = new Headers();
        headers.put("Header", asList("One", "Two", "Three"));

        // When
        headers.put("Header", asList("Four", "Five", "Six"));

        // Then
        assertThat(headers.get("Header").getValues()).containsExactly("Four", "Five", "Six");
    }

    @Test
    public void testPutStrings() throws Exception {
        // Given
        Headers headers = new Headers();

        // When
        Header result = headers.put("Header", Arrays.asList("Four", "Five", "Six"));

        // Then
        assertThat(headers.get("Header").getValues()).containsExactly("Four", "Five", "Six");
        assertThat(result).isNull();
    }

    @Test
    public void testPutEmptyListOverExisting() throws Exception {
        // Given
        Headers headers = new Headers();
        headers.put("Header", asList("One", "Two", "Three"));

        // When
        Header result = headers.put("Header", emptyList());

        // Then
        assertThat(headers.get("Header")).isNull();
        assertThat(result.getValues()).containsExactly("One", "Two", "Three");
    }

    @Test
    public void testAddAllMap() throws Exception {
        // Given
        Headers headers = new Headers();
        headers.put("Header", asList("One"));
        Map<String, List<Object>> map = new HashMap<>();
        map.put("Header", asList("Two", "Three"));
        map.put("Cookie", asList("One", "Two"));

        // When
        headers.addAll(map);

        // Then
        assertThat(headers.get("Header").getValues()).containsExactly("One", "Two", "Three");
        assertThat(headers.get(CookieHeader.class).getCookies()).hasSize(2);
    }

    @Test
    public void testPutAllMap() throws Exception {
        // Given
        Headers headers = new Headers();
        headers.put("Header", asList("One"));
        Map<String, List<Object>> map = new HashMap<>();
        map.put("Header", asList("Two", "Three"));
        map.put("Cookie", asList("One", "Two"));

        // When
        headers.putAll(map);

        // Then
        assertThat(headers.get("Header").getValues()).containsExactly("Two", "Three");
        assertThat(headers.get(CookieHeader.class).getCookies()).hasSize(2);
    }

    @Test
    public void testCopyAsMultiMapOfStringsIsDisconnectedFromHeadersWhileUpdatingHeader() {
        // Given
        Headers headers = new Headers();
        headers.put("Header", asList("One", "Two", "Three"));

        // When
        Map<String, List<String>> copyAsMapOfList = headers.copyAsMultiMapOfStrings();
        headers.put("Header", asList("One", "Two"));

        // Then
        assertThat(copyAsMapOfList).containsExactly(entry("Header", Arrays.<String>asList("One", "Two", "Three")));
    }

    @Test
    public void testCopyAsMultiMapOfStringsIsDisconnectedFromHeadersWhileAddingHeader() {
        // Given
        Headers headers = new Headers();
        headers.put("Header", asList("One", "Two", "Three"));

        // When
        Map<String, List<String>> copyAsMapOfList = headers.copyAsMultiMapOfStrings();
        headers.put("Header2", asList("A", "B"));

        // Then
        assertThat(copyAsMapOfList).containsExactly(entry("Header", Arrays.<String>asList("One", "Two", "Three")));
    }

    @Test
    public void testCopyAsMultiMapOfStringsIsDisconnectedFromHeadersWhileModifyingList() {
        // Given
        Headers headers = new Headers();
        headers.put("Header", asList("One", "Two", "Three"));
        Map<String, List<String>> copyAsMapOfList = headers.copyAsMultiMapOfStrings();

        // When
        copyAsMapOfList.get("Header").remove(0);

        // Then
        assertThat(headers.get("Header").getValues()).containsExactly("One", "Two", "Three");
    }


    public List<Object> asList(Object... values) {
        return Arrays.asList(values);
    }

    @Test
    public void testDefensiveCopyHeadersAreDetached() throws Exception {
        Headers headers = new Headers();
        headers.put(new MutableHeader("Hello World"));

        Headers copy = new Headers(headers);

        // Update the original
        headers.get(MutableHeader.NAME).getValues().add("Bonjour");
        headers.put(new ContentLengthHeader(42));

        // Check that defensive copy has not changed
        assertThat(copy.get(MutableHeader.NAME).getValues()).containsExactly("Hello World");
    }

    private static class MutableHeader extends Header {

        public static final String NAME = "MutableHeader";

        private List<String> values = new ArrayList<>();

        MutableHeader(String initialValue) {
            values.add(initialValue);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public List<String> getValues() {
            return values;
        }
    }
}
