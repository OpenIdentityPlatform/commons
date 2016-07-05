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
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAMultiPrimePrivateCrtKeySpec;
import java.security.spec.RSAOtherPrimeInfo;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.util.encode.Base64url;

/**
 * Implements a RsaJWK.
 */
public class RsaJWK extends JWK {

    private static final int BIG_INTEGER_POSITIVE = 1;

    /**
     * Holds the other prime factors.
     */
    public static class OtherFactors extends JsonValue {
        /**
         * The R key value.
         */
        private final static String R = "r";

        /**
         * The D key value.
         */
        private final static String D = "d";

        /**
         * The T key value.
         */
        private final static String T = "t";

        /**
         * Creates the other prime factors.
         * @param r r value
         * @param d d value
         * @param t t value
         */
        public OtherFactors(String r, String d, String t) {
            super(new HashMap<>());
            put(R, r);
            put(D, d);
            put(T, t);
        }

        /**
         * Create other prime factors.
         * @param info RSAOtherPrimeInfo used to create the other prime factors object.
         */
        public OtherFactors(RSAOtherPrimeInfo info) {
            super(new HashMap<>());
            put(R, Base64url.encode(info.getPrime().toByteArray()));
            put(D, Base64url.encode(info.getExponent().toByteArray()));
            put(T, Base64url.encode(info.getCrtCoefficient().toByteArray()));
        }

        /**
         * Get the R value.
         * @return the R value
         */
        public String getFactor() {
            return get(R).asString();
        }

        /**
         * Get the D value.
         * @return the D value.
         */
        public String getCRTExponent() {
            return get(D).asString();
        }

        /**
         * Get the T value.
         * @return the T value
         */
        public String getCRTCoefficient() {
            return get(T).asString();
        }
    }

    /**
     * The N key.
     */
    private final static String N = "n";

    /**
     * The E key.
     */
    private final static String E = "e";

    /**
     * The D key.
     */
    private final static String D = "d";

    /**
     * The P key.
     */
    private final static String P = "p";

    /**
     * The Q key.
     */
    private final static String Q = "q";

    /**
     * The DP key.
     */
    private final static String DP = "dp";

    /**
     * The DQ key.
     */
    private final static String DQ = "dq";

    /**
     * The QI key.
     */
    private final static String QI = "qi";

    /**
     * The factors key.
     */
    private final static String FACTORS = "factors";

    /**
     * Creates a RsaJWK.
     * @param use the use of the JWK
     * @param alg the alg of the JWK
     * @param kid the key id of the JWK
     * @param n the modulus of the JWK
     * @param e the public exponent JWK
     * @param x5u the x509 url for the key
     * @param x5t the x509 thumbnail for the key
     * @param x5c the x509 chain as a list of Base64 encoded strings
     */
    public RsaJWK(KeyUse use, String alg, String kid, String n, String e, String x5u, String x5t, List<String> x5c) {
        this (use, alg, kid, n, e, null, null, null, null, null, null, null, x5u, x5t, x5c);
    }

    /**
     * Creates a RsaJWK.
     * @param use the use of the JWK
     * @param alg the alg of the JWK
     * @param kid the key id of the JWK
     * @param n the modulus of the JWK
     * @param e the public exponent JWK
     * @param d the private exponent JWK
     * @param x5u the x509 url for the key
     * @param x5t the x509 thumbnail for the key
     * @param x5c the x509 chain as a list of Base64 encoded strings
     */
    public RsaJWK(KeyUse use, String alg, String kid, String n, String e, String d, String x5u, String x5t,
                  List<String> x5c) {
        this (use, alg, kid, n, e, d, null, null, null, null, null, null, x5u, x5t, x5c);
    }

    /**
     * Creates a RsaJWK.
     * @param use the use of the JWK
     * @param alg the alg of the JWK
     * @param kid the key id of the JWK
     * @param n the modulus of the JWK
     * @param e the public exponent JWK
     * @param p the first prime factor of the JWK
     * @param q the second prime factor of the JWK
     * @param dp the first factor exponent of the JWK
     * @param dq the second factor exponent of the JWK
     * @param qi the first CRT Coefficient of the JWK
     * @param x5u the x509 url for the key
     * @param x5t the x509 thumbnail for the key
     * @param x5c the x509 chain as a list of Base64 encoded strings
     */
    public RsaJWK(KeyUse use, String alg, String kid, String n, String e, String p, String q, String dp,
                  String dq, String qi, String x5u, String x5t, List<String> x5c) {
        this (use, alg, kid, n, e, null, p, q, dp, dq, qi, null, x5u, x5t, x5c);
    }

    /**
     * Creates a RsaJWK.
     * @param use the use of the JWK
     * @param alg the alg of the JWK
     * @param kid the key id of the JWK
     * @param n the modulus of the JWK
     * @param e the public exponent JWK
     * @param d the private exponent JWK
     * @param p the first prime factor of the JWK
     * @param q the second prime factor of the JWK
     * @param dp the first factor exponent of the JWK
     * @param dq the second factor exponent of the JWK
     * @param qi the first CRT Coefficient of the JWK
     * @param factors the extra factors of the JWK
     * @param x5u the x509 url for the key
     * @param x5t the x509 thumbnail for the key
     * @param x5c the x509 chain as a list of Base64 encoded strings
     */
    public RsaJWK(KeyUse use, String alg, String kid, String n, String e, String d, String p, String q,
                  String dp, String dq, String qi, List<OtherFactors> factors,
                  String x5u, String x5t, List<String> x5c) {
        super(KeyType.RSA, use, alg, kid, x5u, x5t, x5c);
        if (n != null && !n.isEmpty()) {
            put(N, n);
        }
        if (e != null && !e.isEmpty()) {
            put(E, e);
        }
        if (d != null && !d.isEmpty()) {
            put(D, d);
        }
        if (p != null && !p.isEmpty()) {
            put(P, p);
        }
        if (q != null && !q.isEmpty()) {
            put(Q, q);
        }
        if (dp != null && !dp.isEmpty()) {
            put(DP, dp);
        }
        if (dq != null && !dq.isEmpty()) {
            put(DQ, dq);
        }
        if (qi != null && !qi.isEmpty()) {
            put(QI, qi);
        }
        if (factors == null) {
            put(FACTORS, Collections.EMPTY_LIST);
        } else {
            put(FACTORS, factors);
        }
        if (x5u != null && !x5u.isEmpty()) {
            put(X5U, x5u);
        }
        if (x5t != null && !x5t.isEmpty()) {
            put(X5T, x5t);
        }
        if (x5c != null && !x5c.isEmpty()) {
            put(X5C, x5c);
        }


    }

    /**
     * Creates a RsaJWK.
     * @param use the use of the JWK
     * @param alg the alg of the JWK
     * @param kid the key id of the JWK
     * @param key the RSAPublicKey to use
     * @param x5u the x509 url for the key
     * @param x5t the x509 thumbnail for the key
     * @param x5c the x509 chain as a list of Base64 encoded strings
     */
    public RsaJWK(RSAPublicKey key, KeyUse use, String alg, String kid, String x5u, String x5t, List<String> x5c) {
        this(use, alg, kid,
                Base64url.encode(key.getModulus().toByteArray()),
                Base64url.encode(key.getPublicExponent().toByteArray()),
                x5u, x5t, x5c);
    }
    /**
     * Creates a RsaJWK.
     * @param use the use of the JWK
     * @param alg the alg of the JWK
     * @param kid the key id of the JWK
     * @param pubKey the RSAPublicKey to use
     * @param privKey the RSAPrivateKey to use
     * @param x5u the x509 url for the key
     * @param x5t the x509 thumbnail for the key
     * @param x5c the x509 chain as a list of Base64 encoded strings
     */
    public RsaJWK(RSAPublicKey pubKey, RSAPrivateKey privKey, KeyUse use, String alg, String kid,
                  String x5u, String x5t, List<String> x5c) {
        this(use, alg, kid,
                Base64url.encode(pubKey.getModulus().toByteArray()),
                Base64url.encode(pubKey.getPublicExponent().toByteArray()),
                Base64url.encode(privKey.getPrivateExponent().toByteArray()),
                x5u, x5t, x5c);
    }
    /**
     * Creates a RsaJWK.
     * @param use the use of the JWK
     * @param alg the alg of the JWK
     * @param kid the key id of the JWK
     * @param pubKey the RSAPublicKey to use
     * @param privCert the RSAPrivateCrtKey to use
     * @param x5u the x509 url for the key
     * @param x5t the x509 thumbnail for the key
     * @param x5c the x509 chain as a list of Base64 encoded strings
     */
    public RsaJWK(RSAPublicKey pubKey, RSAPrivateCrtKey privCert, KeyUse use, String alg, String kid,
                  String x5u, String x5t, List<String> x5c) {
        this(use, alg, kid,
                Base64url.encode(pubKey.getModulus().toByteArray()),
                Base64url.encode(pubKey.getPublicExponent().toByteArray()),
                Base64url.encode(privCert.getPrivateExponent().toByteArray()),
                Base64url.encode(privCert.getPrimeP().toByteArray()),
                Base64url.encode(privCert.getPrimeQ().toByteArray()),
                Base64url.encode(privCert.getPrimeExponentP().toByteArray()),
                Base64url.encode(privCert.getPrimeExponentQ().toByteArray()),
                Base64url.encode(privCert.getCrtCoefficient().toByteArray()),
                null, x5u, x5t, x5c);

    }

    /**
     * Get the RSA modulus value.
     * @return a Base64url modulus value
     */
    public String getModulus() {
        return get(N).asString();
    }

    /**
     * Get the RSA Public Exponent.
     * @return a Base64url Public Exponent value
     */
    public String getPublicExponent() {
        return get(E).asString();
    }

    /**
     * Get the RSA Private Exponent value.
     * @return a Base64url Private Exponent value
     */
    public String getPrivateExponent() {
        return get(D).asString();
    }

    /**
     * Get the RSA First Prime Factor value.
     * @return a Base64url First Prime Factor value
     */
    public String getPrimeP() {
        return get(P).asString();
    }

    /**
     * Get the RSA Second Prime Factor value.
     * @return a Base64url Second Prime Factor value
     */
    public String getPrimeQ() {
        return get(Q).asString();
    }

    /**
     * Get the RSA First Factor CRT Exponent value.
     * @return a Base64url First Factor CRT Exponent value
     */
    public String getPrimePExponent() {
        return get(DP).asString();
    }

    /**
     * Get the RSA Second factor CRT Exponent value.
     * @return a Base64url Second factor CRT Exponent value
     */
    public String getPrimeQExponent() {
        return get(DQ).asString();
    }

    /**
     * Get the RSA First CRT Coefficient value.
     * @return a Base64url First CRT Coefficient value
     */
    public String getCRTCoefficient() {
        return get(QI).asString();
    }

    /**
     * Get the RSA other factors value.
     * @return a Base64url other factors value
     */
    public List<Object> getOtherFactors() {
        return get(FACTORS).asList();
    }

    /**
     * Creates a RSAPublicKey from the JWK.
     * @return a RSAPublicKey
     */
    public RSAPublicKey toRSAPublicKey() {
        try {
            RSAPublicKeySpec spec = new RSAPublicKeySpec(asPositiveBigInteger(getModulus()),
                    asPositiveBigInteger(getPublicExponent()));
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) factory.generatePublic(spec);
        } catch (Exception e) {
            throw new JsonException("Unable to create RSA Public Key", e);
        }
    }

    /**
     * Creates a RSAPrivateKey from the JWK.
     * @return a RSAPrivateKey
     */
    public RSAPrivateKey toRSAPrivateKey() {

        if (getPrivateExponent() == null) {
            return null;
        }

        BigInteger modulus = asPositiveBigInteger(getModulus());
        BigInteger privateExponent = asPositiveBigInteger(getPrivateExponent());

        RSAPrivateKeySpec spec;

        if (getPrimeP() == null) {

            spec = new RSAPrivateKeySpec(modulus, privateExponent);

        } else {

            BigInteger publicExponent = asPositiveBigInteger(getPublicExponent());
            BigInteger p = asPositiveBigInteger(getPrimeP());
            BigInteger q = asPositiveBigInteger(getPrimeQ());
            BigInteger dp = asPositiveBigInteger(getPrimePExponent());
            BigInteger dq = asPositiveBigInteger(getPrimeQExponent());
            BigInteger qi = asPositiveBigInteger(getCRTCoefficient());

            if (getOtherFactors() != null && !getOtherFactors().isEmpty()) {

                RSAOtherPrimeInfo[] otherInfo = new RSAOtherPrimeInfo[getOtherFactors().size()];

                for (int i = 0; i < getOtherFactors().size(); i++) {

                    OtherFactors factor = (OtherFactors) getOtherFactors().get(i);

                    BigInteger factorR = asPositiveBigInteger(factor.getFactor());
                    BigInteger factorD = asPositiveBigInteger(factor.getCRTExponent());
                    BigInteger factorT = asPositiveBigInteger(factor.getCRTCoefficient());

                    otherInfo[i] = new RSAOtherPrimeInfo(factorR, factorD, factorT);
                }

                spec = new RSAMultiPrimePrivateCrtKeySpec(modulus, publicExponent, privateExponent, p, q, dp, dq, qi,
                        otherInfo);
            } else {
                spec = new RSAPrivateCrtKeySpec(modulus, publicExponent, privateExponent, p, q, dp, dq, qi);
            }
        }

        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPrivateKey priv = (RSAPrivateKey) factory.generatePrivate(spec);
            return priv;
        } catch (Exception e) {
            throw new JsonException("Unable to create private RSA Key", e);
        }
    }

    /**
     * Create a KeyPair using the JWK.
     * @return a KeyPair
     */
    public KeyPair toKeyPair() {
        return new KeyPair(toRSAPublicKey(), toRSAPrivateKey());
    }

    /**
     * Parses a RsaJWK from a json string.
     * @param json a string json object
     * @return a RsaJWK
     */
    public static RsaJWK parse(String json) {
        JsonValue jwk = new JsonValue(toJsonValue(json));
        return parse(jwk);
    }

    /**
     * Parses a RsaJWK from a jsonValue Object.
     * @param json a jsonValue object
     * @return a RsaJWK
     */
    public static RsaJWK parse(JsonValue json) {

        String n = null, e = null, d = null, p = null, q = null, dq = null, dp = null, qi = null;
        String x5u = null, x5t = null;
        List<String> x5c = null;
        List<Object> factors = null;
        List<OtherFactors> listOfFactors = null;

        KeyType kty = null;
        KeyUse use = null;
        String alg = null, kid = null;

        kty = KeyType.getKeyType(json.get(KTY).asString());
        if (!kty.equals(KeyType.RSA)) {
            throw new JsonException("Unable to parse RSA JWK; Not an RSA type");
        }

        use = KeyUse.getKeyUse(json.get(USE).asString());
        alg = json.get(ALG).asString();
        kid = json.get(KID).asString();

        n = json.get(N).asString();
        e = json.get(E).asString();
        d = json.get(D).asString();
        p = json.get(P).asString();
        q = json.get(Q).asString();
        dp = json.get(DP).asString();
        dq = json.get(DQ).asString();
        qi = json.get(QI).asString();
        factors = json.get(FACTORS).asList();
        x5u = json.get(X5U).asString();
        x5t = json.get(X5T).asString();
        x5c = json.get(X5C).asList(String.class);
        if (factors != null && !factors.isEmpty()) {
            listOfFactors = new ArrayList<>(factors.size());
            for (Object factor : factors) {
                String r = null, dd = null, t = null;
                r = ((JsonValue) factor).get("r").asString();
                dd = ((JsonValue) factor).get("d").asString();
                t = ((JsonValue) factor).get("t").asString();
                OtherFactors of = new OtherFactors(r, dd, t);
                listOfFactors.add(of);
            }
        }

        return new RsaJWK(use, alg, kid, n, e, d, p, q, dp, dq, qi, listOfFactors, x5u, x5t, x5c);
    }

    /**
     * Prints the RsaJWK object as a json string.
     * @return json string
     */
    public String toJsonString() {
        return super.toString();
    }

    /**
     * Base64 decodes the string, and then returns its positive BigInteger representation.
     * @return a Base64 decoded, positively-forced BigInteger representation of the provided String.
     */
    private BigInteger asPositiveBigInteger(String toConvert) {
        return new BigInteger(BIG_INTEGER_POSITIVE, Base64url.decode(toConvert));
    }
}
