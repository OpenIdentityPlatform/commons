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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.audit.handlers.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class CsvSecureUtilsTest {

    private static final Logger logger = LoggerFactory.getLogger(CsvSecureUtilsTest.class);

    @Test
    public void shouldConcatenateNonNullValuesInHeaderOrder() {
        // the data that the HMAC of a row covered before, which the files written then still need
        Map<String, String> values = new HashMap<>();
        values.put("a", "x");
        values.put("b", null);
        values.put("c", "z");

        assertThat(CsvSecureUtils.concatenatedValues(values, "c", "b", "a"))
                .isEqualTo("zx".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void shouldSignUtf8LengthsAndEmptyNamesAndValues() {
        Map<String, String> values = new HashMap<>();
        values.put("a", "é");

        assertThat(CsvSecureUtils.dataToSign(logger, values, "a", "b", null))
                .isEqualTo(new byte[] {
                    (byte) 0xFF,
                    0, 0, 0, 1, 'a', 0, 0, 0, 2, (byte) 0xC3, (byte) 0xA9,
                    0, 0, 0, 1, 'b', 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0 });
    }

    @Test
    public void shouldSignLineBreaksAsLf() {
        Map<String, String> values = new HashMap<>();
        values.put("a\rb", "c\r\nd\r\r\ne\nf");

        assertThat(CsvSecureUtils.dataToSign(logger, values, "a\rb"))
                .isEqualTo(new byte[] {
                    (byte) 0xFF,
                    0, 0, 0, 3, 'a', '\n', 'b',
                    0, 0, 0, 8, 'c', '\n', 'd', '\n', '\n', 'e', '\n', 'f' });
    }
}
