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

package org.forgerock.json.jose.jws;

import static java.math.BigInteger.ONE;
import static javax.xml.bind.DatatypeConverter.printHexBinary;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECFieldFp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SupportedEllipticCurveTest {

    @DataProvider
    public static Iterator<Object[]> allSupportedCurves() {
        List<Object[]> curves = new ArrayList<>();
        for (SupportedEllipticCurve supportedEllipticCurve : SupportedEllipticCurve.values()) {
            curves.add(new Object[] { supportedEllipticCurve });
        }
        return curves.iterator();
    }

    @Test(dataProvider = "allSupportedCurves")
    public void shouldUseMinus3AsFirstCoefficient(SupportedEllipticCurve curve) {
        BigInteger a = curve.getParameters().getCurve().getA();
        BigInteger p = ((ECFieldFp) curve.getParameters().getCurve().getField()).getP();

        // All the NIST prime field curves fix a to be -3 (mod p):
        assertThat(a).isEqualTo(BigInteger.valueOf(-3).mod(p));
    }

    @Test(dataProvider = "allSupportedCurves")
    public void shouldUse160bitSeed(SupportedEllipticCurve curve) {
        assertThat(curve.getParameters().getCurve().getSeed()).hasSize(160 / 8);
    }

    /**
     * See http://cs.ucsb.edu/~koc/ccs130h/notes/ecdsa-cert.pdf Section 5.2 Algorithm 2. This verifies that the
     * parameters of the curve were generated using the SHA-1 pseudo-random generation method. While this is a useful
     * property to verify, we can assume that the NIST curves have already been verified. However, this does also
     * provide strong evidence that we haven't incorrectly entered the parameter values as the chances of this
     * verification succeeding in that case are astronomically small.
     */
    @Test(dataProvider = "allSupportedCurves")
    public void shouldBePsuedoRandomlyGenerated(SupportedEllipticCurve curve) {
        // Given
        byte[] seed = curve.getParameters().getCurve().getSeed();
        BigInteger a = curve.getParameters().getCurve().getA();
        BigInteger b = curve.getParameters().getCurve().getB();
        BigInteger p = ((ECFieldFp) curve.getParameters().getCurve().getField()).getP();

        final int t = p.bitLength();
        final int s = (int) Math.floor((t - 1) / 160.0d);
        final int v = t - 160 * s;

        // When
        final StringBuilder w = new StringBuilder();

        // Use v-1 right-most bits of SHA-1(seed) as the initial part of the answer
        final BigInteger mask = ONE.shiftLeft(v - 1).subtract(ONE);
        final BigInteger w0 = new BigInteger(1, sha1(seed)).and(mask);
        w.append(w0.toString(16));

        final BigInteger z = new BigInteger(seed);
        final int g = seed.length * 8;
        BigInteger mod = BigInteger.valueOf(2).pow(g);
        for (int i = 1; i <= s; ++i) {
            BigInteger si = z.add(BigInteger.valueOf(i)).mod(mod);
            w.append(printHexBinary(sha1(removeSignByte(si.toByteArray(), g))));
        }

        BigInteger r = new BigInteger(w.toString(), 16);

        // Then
        // Ensure that b^2 * r == a^3 (mod p)
        assertThat(b.modPow(BigInteger.valueOf(2), p).multiply(r).mod(p)).isEqualTo(a.modPow(BigInteger.valueOf(3), p));
    }

    private static byte[] removeSignByte(byte[] data, int bits) {
        if (bits % 8 == 0) {
            return Arrays.copyOfRange(data, 1, data.length);
        }
        return data;
    }

    private static byte[] sha1(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}