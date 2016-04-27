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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.audit.handlers.csv;

import java.io.IOException;
import java.io.Writer;

import org.forgerock.audit.rotation.RotationContext;
import org.forgerock.audit.rotation.RotationHooks;

/**
 * Creates a {@link RotationHooks} for super csv.
 */
class CsvRotationHooks implements RotationHooks {

    private final String[] headers;
    private final CsvFormatter formatter;

    public CsvRotationHooks(final CsvFormatter formatter, final String... headers) {
        this.formatter = formatter;
        this.headers = headers;
    }

    @Override
    public void postRotationAction(RotationContext context) throws IOException {
        Writer writer = context.getWriter();
        writer.write(formatter.formatHeader(headers));
        // In case of low traffic we still want the headers to be written into the file
        writer.flush();
    }

    @Override
    public void preRotationAction(RotationContext context) throws IOException {
        // do nothing
    }
}
