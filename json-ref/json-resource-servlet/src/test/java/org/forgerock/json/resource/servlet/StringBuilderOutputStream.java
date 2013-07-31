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
 * Copyright 2013 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

import java.io.IOException;

import javax.servlet.ServletOutputStream;

class StringBuilderOutputStream extends ServletOutputStream {

    private final StringBuilder output;

    StringBuilderOutputStream(StringBuilder output) {
        this.output = output;
    }

    @Override
    public void write(int b) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void write(byte[] b) throws IOException {
        output.append(new String(b));
    }

    @Override
    public void write(byte[] buf, int offset, int len) throws IOException {
        if (!(len >= 0)) {
            throw new IllegalArgumentException("len parameter must be positive");
        }
        output.append(new String(buf, offset, len));
    }

}
