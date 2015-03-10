/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the License.
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

package org.forgerock.util.test.fest;

import static org.forgerock.util.test.fest.FestPromiseAssert.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fest.assertions.MapAssert;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.testng.annotations.Test;

public class FestPromiseAssertTest {

    @Test(expectedExceptions = AssertionError.class)
    public void testIncorrectFailed() throws Exception {
        // Given
        Promise<String, Exception> promise = Promises.newSuccessfulPromise("fred");

        // When/Then
        assertThat(promise).failedWithException();
    }

    @Test
    public void testFailed() throws Exception {
        // Given
        Promise<String, ? extends Exception> promise = Promises.newFailedPromise(new RuntimeException("bleugh"));

        // When/Then
        assertThat(promise).failedWithException().hasMessage("bleugh");
    }

    @Test
    public void testSuccessString() throws Exception {
        // Given
        Promise<String, Exception> promise = Promises.newSuccessfulPromise("fred");

        // When/Then
        assertThat(promise).succeeded().withString().isEqualTo("fred");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testIncorrectSuccess() throws Exception {
        // Given
        Promise<String, Exception> promise = Promises.newFailedPromise(new Exception());

        // When/Then
        assertThat(promise).succeeded();
    }

    @Test
    public void testSuccessList() throws Exception {
        // Given
        Promise<List<String>, Exception> promise = Promises.newSuccessfulPromise(Arrays.asList("abc", "def"));

        // When/Then
        assertThat(promise).succeeded().withList().containsExactly("abc", "def");
    }

    @Test
    public void testSuccessCollection() throws Exception {
        // Given
        Promise<List<String>, Exception> promise = Promises.newSuccessfulPromise(Arrays.asList("abc", "def"));

        // When/Then
        assertThat(promise).succeeded().withCollection().contains("abc");
    }

    @Test
    public void testSuccessInteger() throws Exception {
        // Given
        Promise<Integer, Exception> promise = Promises.newSuccessfulPromise(5);

        // When/Then
        assertThat(promise).succeeded().withInteger().isEqualTo(5);
    }

    @Test
    public void testSuccessLong() throws Exception {
        // Given
        Promise<Long, Exception> promise = Promises.newSuccessfulPromise(5L);

        // When/Then
        assertThat(promise).succeeded().withLong().isEqualTo(5L);
    }

    @Test
    public void testSuccessDouble() throws Exception {
        // Given
        Promise<Double, Exception> promise = Promises.newSuccessfulPromise(5.0D);

        // When/Then
        assertThat(promise).succeeded().withDouble().isEqualTo(5.0D);
    }

    @Test
    public void testSuccessBoolean() throws Exception {
        // Given
        Promise<Boolean, Exception> promise = Promises.newSuccessfulPromise(true);

        // When/Then
        assertThat(promise).succeeded().withBoolean().isTrue();
    }

    @Test
    public void testSuccessObject() throws Exception {
        // Given
        Promise<Boolean, Exception> promise = Promises.newSuccessfulPromise(true);

        // When/Then
        assertThat(promise).succeeded().withObject().isNotNull();
    }

    @Test
    public void testSuccessObjectArray() throws Exception {
        // Given
        Promise<Object[], Exception> promise = Promises.newSuccessfulPromise(new Object[5]);

        // When/Then
        assertThat(promise).succeeded().withObjectArray().hasSize(5);
    }

    @Test
    public void testSuccessMap() throws Exception {
        // Given
        Map<String, String> value = new HashMap<String, String>();
        value.put("test", "fred");
        Promise<Map<String, String>, Exception> promise = Promises.newSuccessfulPromise(value);

        // When/Then
        assertThat(promise).succeeded().withMap().includes(MapAssert.entry("test", "fred"));
    }

    @Test
    public void testSuccessFile() throws Exception {
        // Given
        Promise<File, Exception> promise = Promises.newSuccessfulPromise(new File("fred"));

        // When/Then
        assertThat(promise).succeeded().withFile().isRelative().doesNotExist();
    }

}