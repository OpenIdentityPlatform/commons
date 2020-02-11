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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValueFunctions.charset;
import static org.forgerock.json.JsonValueFunctions.duration;
import static org.forgerock.json.JsonValueFunctions.file;
import static org.forgerock.json.JsonValueFunctions.listOf;
import static org.forgerock.json.JsonValueFunctions.pattern;
import static org.forgerock.json.JsonValueFunctions.pointer;
import static org.forgerock.json.JsonValueFunctions.setOf;
import static org.forgerock.json.JsonValueFunctions.uri;
import static org.forgerock.json.JsonValueFunctions.url;
import static org.forgerock.json.JsonValueFunctions.uuid;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.assertj.core.util.Files;
import org.forgerock.util.Function;
import org.forgerock.util.time.DurationAssert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class JsonValueFunctionsTest {

    @DataProvider(name = "functions")
    public static Object[][] functions() {
        return new Object[][] {
                { charset() },
                { duration() },
                { file() },
                { pattern() },
                { pointer() },
                { uri() },
                { url() },
                { uuid() },
        };
    }

    @Test(dataProvider = "functions")
    public void shouldReturnNullIfNullJsonValueParameter(Function<JsonValue, ?, JsonValueException> function) {
        assertThat(function.apply(json(null))).isNull();
    }

    @Test(dataProvider = "functions", expectedExceptions = JsonValueException.class)
    public void shouldThrowExceptionIfJsonValueIsNotString(Function<JsonValue, ?, JsonValueException> function)
            throws Exception {
        function.apply(json(true));
    }

    // --- Charset

    @Test
    public void shouldConvertToCharset() throws Exception {
        assertThat(charset().apply(json("UTF-8"))).isEqualByComparingTo(Charset.forName("UTF-8"));
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void shouldThrowExceptionIfJsonValueStringNotValidWhenConvertingToCharset() throws Exception {
        charset().apply(json("foo"));
    }

    // --- Duration

    @Test
    public void shouldConvertToDuration() throws Exception {
        DurationAssert.assertThat(duration().apply(json("1 second"))).isEqualTo(1, TimeUnit.SECONDS);
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void shouldThrowExceptionIfJsonValueStringNotValidWhenConvertingToDuration() throws Exception {
        duration().apply(json("foo"));
    }

    // --- File

    @Test
    public void shouldConvertToFile() throws Exception {
        File file = Files.newTemporaryFile();
        assertThat(file().apply(json(file.getAbsolutePath()))).isEqualTo(file);
    }

    // --- Pattern

    @Test
    public void shouldConvertToPattern() throws Exception {
        assertThat(pattern().apply(json("ab*c")).pattern()).isEqualTo("ab*c");
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void shouldThrowExceptionIfJsonValueStringNotValidWhenConvertingToPattern() throws Exception {
        pattern().apply(json("a("));
    }

    // --- Pointer

    @Test
    public void shouldConvertToPointer() throws Exception {
        assertThat(pointer().apply(json("/foo"))).isEqualTo(new JsonPointer("/foo"));
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void shouldThrowExceptionIfJsonValueStringNotValidWhenConvertingToPointer() throws Exception {
        pointer().apply(json("%%%"));
    }

    // --- URI

    @Test
    public void shouldConvertToURI() throws Exception {
        assertThat(uri().apply(json(""))).isEqualTo(new URI(""));
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void shouldThrowExceptionIfJsonValueStringNotValidWhenConvertingToURI() throws Exception {
        uri().apply(json("%%%"));
    }

    // --- URL

    @Test
    public void shouldConvertToURL() throws Exception {
        assertThat(url().apply(json("http://localhost:8080/foo"))).isEqualTo(new URL("http://localhost:8080/foo"));
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void shouldThrowExceptionIfJsonValueStringNotValidWhenConvertingToURL() throws Exception {
        url().apply(json("%%%"));
    }

    // --- UUID

    @Test
    public void shouldConvertToUUID() throws Exception {
        String uuid = "45ece8b0-cfdd-475b-b293-c1a800f109be";
        assertThat(uuid().apply(json(uuid))).isEqualTo(UUID.fromString(uuid));
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void shouldThrowExceptionIfJsonValueStringNotValidWhenConvertingToUUID() throws Exception {
        uuid().apply(json("foo"));
    }

    // --- List

    @Test
    public void shouldConvertToList() throws Exception {
        assertThat(listOf(integer).apply(json(array("1", null, "1")))).containsExactly(1, null, 1);
        assertThat(listOf(integer).apply(json(JsonValue.set("1", null)))).containsExactly(1, null);
    }

    @Test
    public void shouldReturnNullIfNullJsonValueWhenConvertingToList() throws Exception {
        assertThat(listOf(integer).apply(json(null))).isNull();
    }

    @Test
    public void shouldReturnNullIfJsonValueIsNotCollectionWhenConvertingToList() throws Exception {
        assertThat(listOf(integer).apply(json(true))).isNull();
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void shouldThrowExceptionIfJsonValueStringNotValidWhenConvertingToList() throws Exception {
        listOf(integer).apply(json(array("foo")));
    }

    // --- Set

    @Test
    public void shouldConvertToSet() throws Exception {
        assertThat(setOf(integer).apply(json(JsonValue.set("1", null)))).containsExactly(1, null);
        assertThat(setOf(integer).apply(json(JsonValue.array("1", null, "1")))).containsExactly(1, null);
    }

    @Test
    public void shouldReturnNullIfNullJsonValueWhenConvertingToSet() throws Exception {
        assertThat(setOf(integer).apply(json(null))).isNull();
    }

    @Test
    public void shouldReturnNullIfJsonValueIsNotCollectionWhenConvertingToSet() throws Exception {
        assertThat(setOf(integer).apply(json(true))).isNull();
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void shouldReturnNullIfJsonValueStringNotValidWhenConvertingToSet() throws Exception {
        setOf(integer).apply(json(JsonValue.set("foo")));
    }

    private Function<JsonValue, Integer, JsonValueException> integer =
        new Function<JsonValue, Integer, JsonValueException>() {
            @Override
            public Integer apply(JsonValue value) throws JsonValueException {
                if (value.isNull()) {
                    return null;
                }
                if (value.isString()) {
                    try {
                        return new Integer(value.asString());
                    } catch (NumberFormatException nfe) {
                        throw new JsonValueException(value, nfe);
                    }
                }
                throw new JsonValueException(value, "Expecting a String");
            }
        };

}
