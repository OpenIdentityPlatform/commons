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

import static org.forgerock.selfservice.stages.utils.JsonUtils.toJsonValue;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedEncryptedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.util.Reject;

import java.security.KeyPair;
import java.util.Date;

/**
 * Jwt token handler for creating snapshot tokens.
 *
 * @since 0.1.0
 */
public final class JwtTokenHandler implements SnapshotTokenHandler {

    private final JwtBuilderFactory jwtBuilderFactory;
    private final JweAlgorithm jweAlgorithm;
    private final EncryptionMethod jweMethod;
    private final KeyPair jweKeyPair;
    private final JwsAlgorithm jwsAlgorithm;
    private final SigningHandler jwsHandler;
    private final long tokenLifeTimeInSeconds;

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
     * @param tokenLifeTimeInSeconds
     *         token life time in seconds
     */
    public JwtTokenHandler(JweAlgorithm jweAlgorithm, EncryptionMethod jweMethod, KeyPair jweKeyPair,
                           JwsAlgorithm jwsAlgorithm, SigningHandler jwsHandler, long tokenLifeTimeInSeconds) {
        Reject.ifNull(jweAlgorithm, jweMethod, jweKeyPair, jwsAlgorithm, jwsHandler);
        Reject.ifFalse(tokenLifeTimeInSeconds > 0);
        jwtBuilderFactory = new JwtBuilderFactory();
        this.jweAlgorithm = jweAlgorithm;
        this.jweMethod = jweMethod;
        this.jweKeyPair = jweKeyPair;
        this.jwsAlgorithm = jwsAlgorithm;
        this.jwsHandler = jwsHandler;
        this.tokenLifeTimeInSeconds = tokenLifeTimeInSeconds;
    }

    @Override
    public String generate(JsonValue state) throws ResourceException {
        Reject.ifNull(state);

        long expirationTime = System.currentTimeMillis() + (tokenLifeTimeInSeconds * 1000L);

        try {
            JwtClaimsSet claimsSet = jwtBuilderFactory
                    .claims()
                    .claim("state", state.toString())
                    .exp(new Date(expirationTime))
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
        } catch (JwtRuntimeException jwtRE) {
            throw new InternalServerErrorException("Error constructing snapshot token", jwtRE);
        }
    }

    @Override
    public void validate(String snapshotToken) throws ResourceException {
        try {
            validateAndExtractClaims(snapshotToken);
        } catch (JwtRuntimeException jwtRE) {
            throw new InternalServerErrorException("Error deconstructing snapshot token", jwtRE);
        }
    }

    @Override
    public JsonValue validateAndExtractState(String snapshotToken) throws ResourceException {
        Reject.ifNull(snapshotToken);

        try {
            JwtClaimsSet claimsSet = validateAndExtractClaims(snapshotToken);
            return toJsonValue(claimsSet.getClaim("state").toString());

        } catch (JwtRuntimeException jwtRE) {
            throw new InternalServerErrorException("Error deconstructing snapshot token", jwtRE);
        }
    }

    private JwtClaimsSet validateAndExtractClaims(String snapshotToken) throws ResourceException {
        Date currentTime = new Date();

        SignedEncryptedJwt signedEncryptedJwt = jwtBuilderFactory
                .reconstruct(snapshotToken, SignedEncryptedJwt.class);

        if (!signedEncryptedJwt.verify(jwsHandler)) {
            throw new BadRequestException("Invalid snapshot token");
        }

        signedEncryptedJwt.decrypt(jweKeyPair.getPrivate());

        JwtClaimsSet claimsSet = signedEncryptedJwt.getClaimsSet();
        Date expirationTime = claimsSet.getExpirationTime();

        if (expirationTime.before(currentTime)) {
            throw new BadRequestException("Snapshot token has expired");
        }

        return claimsSet;
    }

}
