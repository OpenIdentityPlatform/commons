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
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenType;

import java.util.HashMap;
import java.util.Map;

/**
 * Jwt token handler for creating snapshot tokens.
 *
 * @since 0.1.0
 */
public class JwtTokenHandler implements SnapshotTokenHandler {

    /**
     * JWT snapshot token handle type.
     */
    public static final SnapshotTokenType TYPE = new SnapshotTokenType() {

        @Override
        public String getName() {
            return "JWT";
        }

    };

    private final JwtBuilderFactory jwtBuilderFactory;
    private final JwsAlgorithm jwsAlgorithm;
    private final SigningHandler signingHandler;
    private final SigningHandler verificationHandler;

    /**
     * Constructs a new JWT token handler.
     *
     * @param jwsAlgorithm
     *         the JWS algorithm to use
     * @param signingHandler
     *         the signing handler
     * @param verificationHandler
     *         the verification handler
     */
    public JwtTokenHandler(JwsAlgorithm jwsAlgorithm, SigningHandler signingHandler,
                           SigningHandler verificationHandler) {
        jwtBuilderFactory = new JwtBuilderFactory();
        this.jwsAlgorithm = jwsAlgorithm;
        this.signingHandler = signingHandler;
        this.verificationHandler = verificationHandler;
    }

    @Override
    public boolean validate(String snapshotToken) {
        return jwtBuilderFactory
                .reconstruct(snapshotToken, SignedJwt.class)
                .verify(verificationHandler);
    }

    @Override
    public String generate(Map<String, String> state) {
        JwtClaimsSet claimsSet = jwtBuilderFactory
                .claims()
                .claims(new HashMap<String, Object>(state))
                .build();

        return jwtBuilderFactory
                .jws(signingHandler)
                .headers()
                .alg(jwsAlgorithm)
                .done()
                .claims(claimsSet)
                .build();
    }

    @Override
    public Map<String, String> parse(String snapshotToken) {
        JwtClaimsSet claimsSet = jwtBuilderFactory
                .reconstruct(snapshotToken, SignedJwt.class)
                .getClaimsSet();

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
