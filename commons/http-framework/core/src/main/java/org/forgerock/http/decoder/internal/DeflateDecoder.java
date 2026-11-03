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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2010–2011 ApexIdentity Inc.
 * Portions Copyright 2011-2015 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.http.decoder.internal;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import org.forgerock.http.decoder.Decoder;

/**
 * Decodes an entity encoded with <strong>{@code deflate}</strong> encoding. See
 * <a href="https://www.rfc-editor.org/rfc/rfc9110.html#section-8.4.1.2">RFC 9110 §8.4.1.2</a>.
 * Accepts the zlib format and, for compatibility, raw DEFLATE when no zlib header is present.
 */
public class DeflateDecoder implements Decoder {

    @Override
    public String getKey() {
        return "deflate";
    }

    @Override
    public InputStream decode(InputStream in) throws IOException {
        PushbackInputStream input = new PushbackInputStream(in, 2);
        int cmf = input.read();
        int flg = input.read();
        if (flg == -1) {
            throw new EOFException("Truncated deflate header");
        }
        input.unread(flg);
        input.unread(cmf);

        // RFC 1950: DEFLATE compression, a window of at most 32 KiB, and valid FCHECK bits.
        boolean zlib = (cmf & 0x0f) == 8 && (cmf >>> 4) <= 7 && ((cmf << 8) | flg) % 31 == 0;
        if (zlib && (flg & 0x20) != 0) {
            throw new ZipException("Preset dictionaries are not supported for deflate content encoding");
        }
        // Do not retry corrupt zlib data as raw DEFLATE: preserve checksum and truncation errors.
        return new InflaterInputStream(input, new Inflater(!zlib)) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    // InflaterInputStream does not end an explicitly supplied Inflater.
                    inf.end();
                }
            }
        };
    }
}
