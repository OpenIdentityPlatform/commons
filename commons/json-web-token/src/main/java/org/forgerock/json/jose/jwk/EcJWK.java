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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.json.jose.jwk;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.List;

import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jws.SupportedEllipticCurve;
import org.forgerock.util.encode.Base64url;

/**
 * This class implements an Elliptical Curve Json Web Key storage and manipulation class.
 */
public class EcJWK extends JWK {
    /**
     * The X value for the EC point.
     */
    private final static String X = "x";

    /**
     * The Y value for the EC Point.
     */
    private final static String Y = "y";

    /**
     * The private key value.
     */
    private final static String D = "d";

    /**
     * The Curve of the ECC.
     */
    private final static String CURVE = "crv";

    /**
     * Creates a public and private EcJWK.
     * @param use The value of the use JWK parameter
     * @param alg The value of the alg JWK parameter
     * @param kid The key id of the JWK
     * @param x The x value for the elliptical curve point
     * @param y The y value for the elliptical curve point
     * @param d The d value for the elliptical curve private key
     * @param curve The known curve to use. For example "NIST P-256".
     * @param x5u the x509 url for the key
     * @param x5t the x509 thumbnail for the key
     * @param x5c the x509 chain as a list of Base64 encoded strings
     */
    public EcJWK(KeyUse use, String alg, String kid, String x, String y, String d, String curve,
                 String x5u, String x5t, List<String> x5c) {
        super(KeyType.EC, use, alg, kid, x5u, x5t, x5c);
        if (x == null || x.isEmpty()) {
            throw new JsonException("x is required for an EcJWK");
        }
        put(X, x);

        if (y == null || y.isEmpty()) {
            throw new JsonException("y is required for an EcJWK");
        }
        put(Y, y);

        if (curve == null || curve.isEmpty()) {
            throw new JsonException("curve is required for an EcJWK");
        }
        put(CURVE, curve);

        put(D, d);
    }

    /**
     * Creates a public EcJWK.
     * @param use The value of the use JWK parameter
     * @param alg The value of the alg JWK parameter
     * @param kid The key id of the JWK
     * @param x The x value for the elliptical curve point
     * @param y The y value for the elliptical curve point
     * @param curve The known curve to use. For example "NIST P-256".
     * @param x5u the x509 url for the key
     * @param x5t the x509 thumbnail for the key
     * @param x5c the x509 chain as a list of Base64 encoded strings
     */
    public EcJWK(KeyUse use, String alg, String kid, String x, String y, String curve, String x5u, String x5t,
                 List<String> x5c) {
        this (use, alg, kid, x, y, null, curve, x5u, x5t, x5c);
    }

    /**
     * Gets the x value for the elliptical curve point.
     * @return x value for the elliptical curve point
     */
    public String getX() {
        return get(X).asString();
    }

    /**
     * Gets the y value for the elliptical curve point.
     * @return y value for the elliptical curve point
     */
    public String getY() {
        return get(Y).asString();
    }

    /**
     * Gets the d value for the elliptical curve private key.
     * @return d value for the elliptical curve point
     */
    public String getD() {
        return get(D).asString();
    }

    /**
     * Gets the known curve to use. For example "NIST P-256".
     * @return the known curve of the JWK
     */
    public String getCurve() {
        return get(CURVE).asString();
    }

    /**
     * Parses a JWK from a string json object.
     * @param json string json object
     * @return a EcJWK object
     */
    public static EcJWK parse(String json) {
        JsonValue jwk = new JsonValue(toJsonValue(json));
        return parse(jwk);
    }

    /**
     * Parses a JWK from a JsonValue json object.
     * @param json JsonValue json object
     * @return a EcJWK object
     */
    public static EcJWK parse(JsonValue json) {
        if (json == null) {
            throw new JsonException("Cant parse OctJWK. No json data.");
        }

        KeyType kty = null;
        KeyUse use = null;
        String x = null, y = null, d = null, curve = null, alg = null, kid = null;
        String x5u = null, x5t = null;
        List<String> x5c = null;

        kty = KeyType.getKeyType(json.get(KTY).asString());

        if (!kty.equals(KeyType.EC)) {
            throw new JsonException("Invalid key type. Not an EC JWK");
        }

        x = json.get(X).asString();
        y = json.get(Y).asString();
        d = json.get(D).asString();
        curve = json.get(CURVE).asString();

        use = KeyUse.getKeyUse(json.get(USE).asString());
        alg = json.get(ALG).asString();
        kid = json.get(KID).asString();

        x5u = json.get(X5U).asString();
        x5t = json.get(X5T).asString();
        x5c = json.get(X5C).asList(String.class);

        return new EcJWK(use, alg, kid, x, y, d, curve, x5u, x5t, x5c);
    }

    /**
     * Prints the JWK as a String json object.
     * @return a json string object
     */
    public String toJsonString() {
        return super.toString();
    }

    /**
     * Converts the JWK to a ECPublicKey.
     * @return an ECPublicKey
     */
    public ECPublicKey toECPublicKey() {
        try {
            final SupportedEllipticCurve curve = SupportedEllipticCurve.forName(getCurve());

            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            ECPoint point = new ECPoint(new BigInteger(Base64url.decode(getX())),
                    new BigInteger(Base64url.decode(getY())));
            return (ECPublicKey) keyFactory.generatePublic(new ECPublicKeySpec(point, curve.getParameters()));
        } catch (GeneralSecurityException e) {
            throw new JsonException("Unable to create EC Public Key", e);
        }
    }

    /**
     * Converts the JWK to a ECPrivateKey.
     * @return an ECPrivateKey
     */
    public ECPrivateKey toECPrivateKey() {
        try {
            final SupportedEllipticCurve curve = SupportedEllipticCurve.forName(getCurve());

            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            final BigInteger s = new BigInteger(Base64url.decode(getD()));
            return (ECPrivateKey) keyFactory.generatePrivate(new ECPrivateKeySpec(s, curve.getParameters()));
        } catch (GeneralSecurityException e) {
            throw new JsonException("Unable to create EC Private Key", e);
        }
    }

    /**
     * Converts the JWK to a KeyPair.
     * @return an KeyPair
     */
    public KeyPair toKeyPair() {
        return new KeyPair(toECPublicKey(), toECPrivateKey());
    }
}
