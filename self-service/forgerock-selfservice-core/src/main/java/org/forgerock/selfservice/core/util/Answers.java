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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.selfservice.core.util;

import static org.forgerock.util.crypto.CryptoConstants.ALGORITHM_SHA_256;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.selfservice.core.crypto.CryptoService;
import org.forgerock.selfservice.core.crypto.JsonCryptoException;

/**
 * Utility methods for hashing and normalising answers to KBA questions.
 *
 * @since 0.9.0
 */
public final class Answers {

    private Answers() {
        // prevent construction
    }

    /**
     * Hashes the given answer.  If already hashed, it is returned unchanged.  Throws
     * {@link InternalServerErrorException} if the provided answer is not a String or cannot be hashed.
     *
     * @param cryptoService
     *            an instance of the {@link CryptoService} to perform the hashing
     * @param answer
     *            the value to be hashed.
     * @return the hashed string value.
     * @throws InternalServerErrorException
     *            if the provided answer is not a String or cannot be hashed
     */
    public static JsonValue hashAnswer(CryptoService cryptoService, JsonValue answer)
            throws InternalServerErrorException {
        try {
            if (cryptoService.isHashed(answer)) {
                return answer;
            }
            if (answer.isString()) {
                return cryptoService.hash(normaliseAnswer(answer.asString()), ALGORITHM_SHA_256);
            }
            throw new InternalServerErrorException("Provided answer is neither a string, nor an already hashed value.");
        } catch (JsonCryptoException e) {
            throw new InternalServerErrorException("Error while hashing the answer", e);
        }
    }

    /**
     * Normalises the given answer.
     *
     * @param answer
     *            the string value to be normalised.
     * @return normalised string value.
     */
    public static String normaliseAnswer(String answer) {
        return (answer == null ? null : answer.toLowerCase());
    }

}
