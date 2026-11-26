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

package org.forgerock.json.jose.jwe.handlers.compression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;

import org.forgerock.json.jose.exceptions.JweCompressionException;
import org.testng.annotations.Test;

public class DeflateCompressionHandlerTest {

    private static final int ONE_MIB = 1024 * 1024;
    private final DeflateCompressionHandler handler = new DeflateCompressionHandler();

    @Test
    public void decompressesPayloadAtLimit() {
        byte[] payload = new byte[ONE_MIB];
        new Random(0).nextBytes(payload);

        assertThat(handler.decompress(handler.compress(payload))).isEqualTo(payload);
    }

    @Test
    public void rejectsPayloadOneByteOverLimit() {
        byte[] payload = new byte[ONE_MIB + 1];
        new Random(0).nextBytes(payload);
        byte[] compressed = handler.compress(payload);

        assertThatThrownBy(() -> handler.decompress(compressed)).isInstanceOf(JweCompressionException.class);
    }
}
