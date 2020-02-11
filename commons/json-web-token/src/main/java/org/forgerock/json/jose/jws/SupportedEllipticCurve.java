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

import java.math.BigInteger;
import java.security.interfaces.ECKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.util.Objects;
import javax.xml.bind.DatatypeConverter;

/**
 * Enumerates all supported elliptic curve parameters for ESXXX signature formats.
 */
public enum SupportedEllipticCurve {
    /** NIST P-256. */
    P256("P-256", StandardCurve.P_256, 64, JwsAlgorithm.ES256),
    /** NIST P-384. */
    P384("P-384", StandardCurve.P_384, 96, JwsAlgorithm.ES384),
    /** NIST P-521. Please note that this is not a typo: ES512 uses curve <em>P-521</em>, which produces a 132-octet
     * signature value. */
    P521("P-521", StandardCurve.P_521, 132, JwsAlgorithm.ES512);


    private final ECParameterSpec parameters;
    private final String standardName;
    private final int signatureSize;
    private final JwsAlgorithm jwsAlgorithm;

    SupportedEllipticCurve(String standardName, ECParameterSpec curve, int signatureSize, JwsAlgorithm jwsAlgorithm) {
        this.parameters = curve;
        this.standardName = standardName;
        this.signatureSize = signatureSize;
        this.jwsAlgorithm = jwsAlgorithm;
    }

    /**
     * Returns the parameters for the given elliptic curve.
     *
     * @return the elliptic curve algorithm parameters.
     */
    public ECParameterSpec getParameters() {
        return parameters;
    }

    /**
     * Return the name of the curve as used for the "crv" claim in a JWK.
     *
     * @return the standard JWA name for the curve.
     */
    public String getStandardName() {
        return standardName;
    }

    /**
     * Returns the size of the signature produced by this curve in octets.
     *
     * @return the number of octets (bytes) required to hold a signature of this curve.
     */
    public int getSignatureSize() {
        return signatureSize;
    }

    /**
     * Returns the JwsAlgorithm that corresponds to this elliptic curve.
     *
     * @return the corresponding JwsAlgorithm.
     */
    public JwsAlgorithm getJwsAlgorithm() {
        return jwsAlgorithm;
    }

    /**
     * Returns the curve parameters for the given standard curve name (crv claim in a JWK).
     *
     * @param curveName the curve name.
     * @return the curve parameters for the name.
     * @throws IllegalArgumentException if the curve name is not supported.
     */
    public static SupportedEllipticCurve forName(final String curveName) {
        for (SupportedEllipticCurve candidate : values()) {
            if (candidate.getStandardName().equals(curveName)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unsupported curve: " + curveName);
    }

    /**
     * Determines the standard curve that matches the given (private or public) key. This is done by comparing the
     * key parameters for an <em>exact</em> match against one of the standard curves. All parameters much match for a
     * match to succeed.
     *
     * @param key the private or public key to determine the curve for.
     * @return the matching supported curve parameters.
     * @throws IllegalArgumentException if the key does not match any supported curve parameters.
     */
    public static SupportedEllipticCurve forKey(final ECKey key) {
        final ECParameterSpec params = key.getParams();
        for (SupportedEllipticCurve supported : values()) {
            final ECParameterSpec candidateParams = supported.getParameters();
            if (candidateParams.getCofactor() == params.getCofactor()
                    && Objects.equals(candidateParams.getCurve(), params.getCurve())
                    && Objects.equals(candidateParams.getGenerator(), params.getGenerator())
                    && Objects.equals(candidateParams.getOrder(), params.getOrder())) {
                return supported;
            }
        }
        throw new IllegalArgumentException("Unsupported ECKey parameters");
    }

    /**
     * Determines the supported curve parameters for the given signature. This is done purely based on the length of
     * the signature and the behaviour is not specified if multiple curves could have produced this signature.
     *
     * @param signature the signature to match.
     * @return the curve that produced this signature.
     * @throws IllegalArgumentException if the signature does not match any supported curve parameters.
     */
    public static SupportedEllipticCurve forSignature(byte[] signature) {
        for (SupportedEllipticCurve candidate : values()) {
            if (signature.length == candidate.getSignatureSize()) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unsupported signature size: " + signature.length);
    }

    /**
     * NIST standard elliptic curve parameters as specified in the JSON Web Algorithms (JWA) spec and defined in
     * <a href="http://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf">FIPS 186-4</a> section D.1.2.3 (P-256),
     * D.1.2.4 (P-384) and D.1.2.5 (P-521). Defined as a separate inner class to avoid illegal forward-reference
     * problems when constructing the elements of the SupportedEllipticCurve enum.
     *
     * <p>
     * ECDSA uses an elliptic curve defined by all the points from a finite field that satisfy the equation:
     * <em>y</em><sup>2</sup> = <em>x</em><sup>3</sup> + <em>ax</em> + <em>b</em> where <em>a</em> and <em>b</em> are
     * the coefficients. For the curves we are interested in for JWA, the finite fields are produced by the integers
     * modulo some large prime <em>p</em>, so the arithmetic for the above equation is all done modulo <em>p</em>.
     * In addition, we define a base point on the curve, known as the <em>generator</em> and denoted <em>G</em> (with
     * components <em>G<sub>x</sub></em> and <em>G<sub>y</sub></em>), such that the <em>order</em> (number of
     * elements) of the resulting curve is a large prime, <em>n</em>. The number of points on the curve is actually
     * given by <em>hn</em> where <em>h</em> is the cofactor, but h is fixed to be 1 for all NIST curves so we
     * ignore it here.
     *
     * <p>
     * The names of the curves (e.g. P-256) are given by the length of the prime modulus, <em>p</em>, in bits. So for
     * P-256 the prime is 256 bits long when written in binary, etc.
     *
     * <p>
     * The Java {@link ECParameterSpec} expects parameters in a slightly different format from how they are defined
     * in the NIST specification:
     * <table>
     *     <thead>
     *         <tr><th>NIST Parameter</th><th>Java Parameter</th><th>Description</th></tr>
     *     </thead>
     *     <tbody>
     *         <tr><td><em>p</em></td><td><code>p</code></td><td>The prime modulus</td></tr>
     *         <tr><td><em>n</em></td><td><code>n</code></td><td>The order of the field</td></tr>
     *         <tr><td><em>SEED</em></td><td><code>seed</code></td><td>Seed value to SHA-1 used to generate
     *         the coefficients of the curve as per the algorithm in <a
     *         href="http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.202.2977&rep=rep1&type=pdf">ANSI
     *         X9.62</a> Annex A.3.3. This can be used to verify that the coefficients have been generated
     *         pseudo-randomly via the algorithm in A.3.4.2.</td>
     *         </tr>
     *         <tr><td><em>c</em></td><td>n/a</td><td>The output of the SHA-1 curve generation algorithm (this is
     *         called W in the ANSI X9.62 algorithm linked above). It should hold that <em>c</em> *
     *         <em>b</em><sup>2</sup> = <em>a</em><sup>3</sup> (mod p).</td></tr>
     *         <tr><td>n/a</td><td><code>a</code></td><td>The first coefficient of the curve equation. For all the NIST
     *         standard prime curves this is fixed as -3 (mod <em>p</em>).</td>/tr>
     *         <tr><td><em>b</em></td><td><code>b</code></td><td>The second coefficient of the curve equation.</td></tr>
     *         <tr><td><em>G<sub>x</sub></em></td><td><code>x</code></td><td>The x-coordinate of the generator point G.
     *         </td></tr>
     *         <tr><td><em>G<sub>y</sub></em></td><td><code>y</code></td><td>The y-coordinate of the generator point G.
     *         </td></tr>
     *     </tbody>
     * </table>
     * <p>
     * Note that the <em>seed</em> and <em>c</em> values are not required after the coefficients <em>a</em> and
     * <em>b</em> have been generated. They can be used to verify that the coefficients were pseudo-randomly
     * generated and not picked by hand (which might indicate a backdoor). We include the seed value for completeness
     * of the algorithm parameters (ECParameterSpec does not have the ability to specify <em>c</em>, but it can be
     * derived from the seed and the coefficients).
     */
    private static class StandardCurve {
        private static final int H = 1;

        /**
         * The P-256 curve.
         */
        private static final ECParameterSpec P_256 = new ECParameterSpec(
                new EllipticCurve(
                        p("115792089210356248762697446949407573530086143415290314195533631308867097853951"),
                        a("115792089210356248762697446949407573530086143415290314195533631308867097853948"),
                        b("41058363725152142129326129780047268409114441015993725554835256314039467401291"),
                        seed("c49d3608 86e70493 6a6678e1 139d26b7 819f7e90")),
                new ECPoint(x("48439561293906451759052585252797914202762949526041747995844080717082404635286"),
                            y("36134250956749795798585127919587881956611106672985015071877198253568414405109")),
                n("115792089210356248762697446949407573529996955224135760342422259061068512044369"), H);

        /**
         * The P-384 curve.
         */
        private static final ECParameterSpec P_384 = new ECParameterSpec(
                new EllipticCurve(
                        p("3940200619639447921227904010014361380507973927046544666794829340424572177149687032904726"
                                + "6088258938001861606973112319"),
                        a("39402006196394479212279040100143613805079739270465446667948293404245721771496870329047266088"
                                + "258938001861606973112316"),
                        b("27580193559959705877849011840389048093056905856361568521428707301988689241309860865136260764"
                                + "883745107765439761230575"),
                        seed("a335926a a319a27a 1d00896a 6773a482 7acdac73")),
                new ECPoint(
                        x("26247035095799689268623156744566981891852923491109213387815615900925518854738050089022388053"
                                + "975719786650872476732087"),
                        y("83257109614890299855467512895201081792878530488613155947092059024805031998844192244386437603"
                                + "92947333078086511627871")),
                n("3940200619639447921227904010014361380507973927046544666794690527962765939911326356939895630815229491"
                        + "3554433653942643"), H);

        /**
         * The P-521 curve.
         */
        private static final ECParameterSpec P_521 = new ECParameterSpec(
                new EllipticCurve(
                        p("68647976601306097149819007990813932172694353001433054093944634591855431833976560521225596406"
                                + "61454554977296311391480858037121987999716643812574028291115057151"),
                        a("68647976601306097149819007990813932172694353001433054093944634591855431833976560521225596406"
                                + "61454554977296311391480858037121987999716643812574028291115057148"),
                        b("10938490380737342745111123907668055699362075989516837489945863944959531161507350160137087375"
                                + "73759623248592132296706313309438452531591012912142327488478985984"),
                        seed("d09e8800 291cb853 96cc6717 393284aa a0da64ba")),
                new ECPoint(
                        x("26617408020502170632287687167233609607298591687569731477066713684188029449964278084915450806"
                                + "27771902352094241225065558662157113545570916814161637315895999846"),
                        y("37571800257700204635455072244911836035944551347697624866945677796155444774405563166912344050"
                                + "12945539562144444537289428522585666729196580810124344277578376784")),
                n("6864797660130609714981900799081393217269435300143305409394463459185543183397655394245057746333217197"
                        + "532963996371363321113864768612440380340372808892707005449"), H);


        private static ECFieldFp p(final String value) {
            return new ECFieldFp(new BigInteger(value));
        }

        private static BigInteger a(final String value) {
            return new BigInteger(value);
        }

        private static BigInteger b(final String value) {
            return new BigInteger(value);
        }

        private static BigInteger x(final String value) {
            return new BigInteger(value);
        }

        private static BigInteger y(final String value) {
            return new BigInteger(value);
        }

        private static BigInteger n(final String value) {
            return new BigInteger(value);
        }

        private static byte[] seed(final String hex) {
            return DatatypeConverter.parseHexBinary(hex.replaceAll("\\s+", ""));
        }
    }
}
