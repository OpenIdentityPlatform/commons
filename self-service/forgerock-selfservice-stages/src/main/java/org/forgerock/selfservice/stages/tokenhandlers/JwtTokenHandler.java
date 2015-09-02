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

package org.forgerock.selfservice.stages.tokenhandlers;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedEncryptedJwt;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

/**
 * Jwt token handler for creating snapshot tokens.
 *
 * @since 0.1.0
 */
public final class JwtTokenHandler implements SnapshotTokenHandler {

    private final static Logger logger = LoggerFactory.getLogger(JwtTokenHandler.class);

    /**
     * JWT snapshot token handle type.
     */
    public static final String TYPE = "JWT_TOKEN_TYPE";

    private final JwtBuilderFactory jwtBuilderFactory;
    private final JweAlgorithm jweAlgorithm;
    private final EncryptionMethod jweMethod;
    private final KeyPair jweKeyPair;
    private final JwsAlgorithm jwsAlgorithm;
    private final SigningHandler jwsHandler;

    /**
     * Constructs a new JWT token handler.
     *
     * @param jweAlgorithm
     *         the JWE algorithm use to construct the key pair
     * @param jweMethod
     *         the encryption method to use
     * @param jweKeyPair
     *         key pair for the purpose of encryption
     * @param jwsAlgorithm
     *         the JWS algorithm to use
     * @param jwsHandler
     *         the signing handler
     */
    public JwtTokenHandler(JweAlgorithm jweAlgorithm, EncryptionMethod jweMethod, KeyPair jweKeyPair,
                           JwsAlgorithm jwsAlgorithm, SigningHandler jwsHandler) {
        Reject.ifNull(jweAlgorithm, jweMethod, jweKeyPair, jwsAlgorithm, jwsHandler);
        jwtBuilderFactory = new JwtBuilderFactory();
        this.jweAlgorithm = jweAlgorithm;
        this.jweMethod = jweMethod;
        this.jweKeyPair = jweKeyPair;
        this.jwsAlgorithm = jwsAlgorithm;
        this.jwsHandler = jwsHandler;
    }

    @Override
    public boolean validate(String snapshotToken) {
        Reject.ifNull(snapshotToken);

        try {
            return jwtBuilderFactory
                    .reconstruct(snapshotToken, SignedJwt.class)
                    .verify(jwsHandler);
        } catch (JwtRuntimeException jwtRE) {
            logger.error("Error parsing JWT snapshot token", jwtRE);
            return false;
        }
    }

    @Override
    public String generate(Map<String, String> state) {
        Reject.ifNull(state);
        JwtClaimsSet claimsSet = jwtBuilderFactory
                .claims()
                .claims(new HashMap<String, Object>(state))
                .build();

        return jwtBuilderFactory
                .jwe(jweKeyPair.getPublic())
                .headers()
                    .alg(jweAlgorithm)
                    .enc(jweMethod)
                    .done()
                .claims(claimsSet)
                .sign(jwsHandler, jwsAlgorithm)
                .build();
    }

    @Override
    public Map<String, String> parse(String snapshotToken) {
        Reject.ifNull(snapshotToken);

        SignedEncryptedJwt signedEncryptedJwt = jwtBuilderFactory
                .reconstruct(snapshotToken, SignedEncryptedJwt.class);

        signedEncryptedJwt.decrypt(jweKeyPair.getPrivate());
        JwtClaimsSet claimsSet = signedEncryptedJwt.getClaimsSet();

        Map<String, String> state = new HashMap<>();
        for (String key : claimsSet.keys()) {
            Object claim = claimsSet.getClaim(key);
            if (claim != null) {
                state.put(key, claim.toString());
            }
        }

        return state;
    }

}
