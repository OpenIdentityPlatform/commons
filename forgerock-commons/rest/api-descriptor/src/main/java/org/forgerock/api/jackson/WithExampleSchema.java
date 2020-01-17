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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.api.jackson;

import java.io.IOException;

/**
 * Extension for CREST and OpenAPI schemas to express an example value.
 *
 * @param <T> The type of the example.
 */
public interface WithExampleSchema<T> {

    /**
     * Gets {@code example} JSON Schema field.
     *
     * @return The example value, or null if not defined.
     */
    T getExample();

    /**
     * Sets {@code example} JSON Schema field.
     *
     * @param example The example value.
     * @throws IOException When the type of schema cannot be parsed from the String value.
     */
    void setExample(String example) throws IOException;
}
