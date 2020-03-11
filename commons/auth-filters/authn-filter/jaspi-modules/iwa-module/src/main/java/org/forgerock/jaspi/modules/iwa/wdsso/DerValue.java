//@Checkstyle:ignoreFor 29
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: DerValue.java,v 1.2 2008/06/25 05:42:07 qcheng Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */
package org.forgerock.jaspi.modules.iwa.wdsso;

import static java.lang.Integer.toHexString;

import java.io.ByteArrayInputStream;

/**
 * A dedicated implementation of handling <code>ASN1/DER</code> for the
 * Kerberos desktop authentication module. It parses the byte array
 * into [Tag, Length, Data] form.
 */
public class DerValue {
    private byte tag = 0;
    private int length = 0;
    private byte[] data = null;

    /**
     * Construct from a byte-array.
     * @param data The data.
     */
    public DerValue(byte[] data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        init(stream);
    }

    /**
     * Construct from a byte input stream.
     * @param input The data.
     */
    public DerValue(ByteArrayInputStream input) {
        init(input);
    }

    /**
     * Get the tag.
     * @return The tag.
     */
    public byte getTag() {
        return tag;
    }

    /**
     * Get the length.
     * @return The length.
     */
    public int getLength() {
        return length;
    }

    /**
     * Get the data.
     * @return The data.
     */
    public byte[] getData() {
        return data;
    }

    private void init(ByteArrayInputStream input) {
        tag = (byte) input.read();
        length = getLength(input);
        data = new byte[length];
        input.read(data, 0, length);
    }

    private int getLength(ByteArrayInputStream input) {
        int value = 0;
        int tmp;
        byte tmpbyte;

        tmp = input.read();

        if ((tmp & 0x80) == 0) {
            value = tmp;
        } else {
            tmp &= 0x7f;
            for (value = 0; tmp > 0; tmp--) {
                tmpbyte = (byte) input.read();
                value = value * 256 + (tmpbyte & 0xff);
            }
        }
        return value;
    }

    /**
     * Print the byte as a hex string.
     * @param code The byte.
     * @return The hex string.
     */
    public static String printByte(byte code) {
        return toHexString(((code & 0xf0) >> 4) & 0x0f) + toHexString((code & 0x0f) & 0x0f);
    }

    /**
     * Convert a byte array to a hex string.
     * @param token The array.
     * @param from Starting point.
     * @param len End point.
     * @return The hex string.
     */
    public static String printByteArray(byte[] token, int from, int len) {
        StringBuilder buf = new StringBuilder();
        int bytePerLine = 16;
        int line;
        for (line = 1; line * bytePerLine < len; line++) {
            for (int j = 0; j < bytePerLine; j++) {
                buf.append(printByte(token[(line - 1) * bytePerLine + j + from])).append(" ");
            }
            buf.append("\n");
        }
        line--;
        if (line * bytePerLine < len) {
            for (int j = line * bytePerLine; j < len; j++) {
                buf.append(printByte(token[j + from])).append(" ");
            }
        }
        return buf.toString();
    }
}
