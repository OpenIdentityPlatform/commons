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

package org.forgerock.json.resource.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

class IOUtils {

    private static final int BUFFER_SIZE = 1024;
    private static final int EOF = -1;

    static void copy(final InputStream inputStream, final OutputStream outputStream) throws IOException {
        byte data[] = new byte[BUFFER_SIZE];
        int size;

        while ((size = inputStream.read(data)) != EOF) {
            outputStream.write(data, 0, size);
        }

        outputStream.flush();
    }

    static byte[] toByteArray(final InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        copy(inputStream, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    static InputStream toInputStream(final String input) throws UnsupportedEncodingException {
        return toInputStream(input, "UTF-8");
    }

    static InputStream toInputStream(final String input, final String encoding) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(input.getBytes(encoding));
    }

    private IOUtils() {

    }
}
