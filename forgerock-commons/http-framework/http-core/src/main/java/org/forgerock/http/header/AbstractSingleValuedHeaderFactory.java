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

package org.forgerock.http.header;

import java.util.List;

import org.forgerock.http.protocol.Header;

/**
 * Convenience {@link HeaderFactory} for headers that can only have a single value.
 * @param <H> The type of {@link Header} produced by the factory.
 */
abstract class AbstractSingleValuedHeaderFactory<H extends Header> extends HeaderFactory<H> {

    @Override
    protected final H parse(List<String> values) throws MalformedHeaderException {
        switch (values.size()) {
        case 0:
            return null;
        case 1:
            return parse(values.get(0));
        default:
            throw new MalformedHeaderException("Do not expect more than 1 value.");
        }
    }

}
