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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.json.jose.utils;

import java.net.URI;

public final class StringOrURI {

    private final String string;
    private final URI uri;

    public StringOrURI(String string) {
        this(string, null);
    }

    public StringOrURI(URI uri) {
        this(null, uri);
    }

    private StringOrURI(String string, URI uri) {
        this.string = string;
        this.uri = uri;
    }

    @Override
    public String toString() {
        if (string != null) {
            return string;
        } else {
            return uri.toString();
        }
    }
}
