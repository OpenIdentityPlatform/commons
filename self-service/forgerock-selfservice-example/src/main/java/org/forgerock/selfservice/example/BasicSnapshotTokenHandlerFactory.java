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

package org.forgerock.selfservice.example;

import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenType;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandler;

/**
 * Basic token handler factory that always returns the same handler.
 *
 * @since 0.1.0
 */
public final class BasicSnapshotTokenHandlerFactory implements SnapshotTokenHandlerFactory {

    private final byte[] sharedKey;

    /**
     * Creates a new snapshot token handler factory.
     *
     * @param sharedKey
     *         shared key used by the underlying token handler
     */
    public BasicSnapshotTokenHandlerFactory(byte[] sharedKey) {
        this.sharedKey = sharedKey;
    }

    @Override
    public SnapshotTokenHandler get(SnapshotTokenType tokenType) {
        if (tokenType == JwtTokenHandler.TYPE) {
            SigningManager signingManager = new SigningManager();
            SigningHandler signingHandler = signingManager.newHmacSigningHandler(sharedKey);
            return new JwtTokenHandler(JwsAlgorithm.HS256, signingHandler, signingHandler);
        }

        throw new IllegalArgumentException("Unknown type " + tokenType.getName());
    }

}
