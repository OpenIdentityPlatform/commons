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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.jose.jws.handlers;

import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.utils.Utils;

/**
 * An implementation of the SigningHandler which does not perform any signing or verifying.
 *
 * @since 2.0.0
 */
public class NOPSigningHandler implements SigningHandler {

    /**
     * Simply returns a byte array of a UTF-8 empty string.
     *
     * @param algorithm {@inheritDoc}
     * @param data {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public byte[] sign(JwsAlgorithm algorithm, String data) {
        return "".getBytes(Utils.CHARSET);
    }

    /**
     * Returns an empty byte array.
     *
     * @param algorithm {@inheritDoc}
     * @param data {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public byte[] sign(final JwsAlgorithm algorithm, final byte[] data) {
        return new byte[0];
    }

    /**
     * Verifies that the signature length is zero.
     *
     * @param algorithm {@inheritDoc}
     * @param data {@inheritDoc}
     * @param signature {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean verify(JwsAlgorithm algorithm, byte[] data, byte[] signature) {
        return signature.length == 0;
    }
}
