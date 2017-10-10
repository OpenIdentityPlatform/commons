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

package org.forgerock.json.jose.jws.handlers;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import org.forgerock.json.jose.exceptions.JwsException;
import org.forgerock.json.jose.exceptions.JwsSigningException;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithmType;
import org.forgerock.json.jose.jws.SupportedEllipticCurve;
import org.forgerock.json.jose.utils.DerUtils;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.Reject;

/**
 * Elliptic Curve Digital Signature Algorithm (ECDSA) signing and verification.
 */
public class ECDSASigningHandler implements SigningHandler {
    private final ECPrivateKey signingKey;
    private final ECPublicKey verificationKey;
    private final SupportedEllipticCurve curve;

    /**
     * Constructs the ECDSA signing handler for signing only.
     *
     * @param signingKey the private key to use for signing. Must not be null.
     */
    public ECDSASigningHandler(final ECPrivateKey signingKey) {
        this.signingKey = signingKey;
        this.verificationKey = null;
        this.curve = validateKey(signingKey);
    }

    /**
     * Constructs the ECDSA signing handler for verification only.
     *
     * @param verificationKey the public key to use for verification. Must not be null.
     */
    public ECDSASigningHandler(final ECPublicKey verificationKey) {
        this.signingKey = null;
        this.verificationKey = verificationKey;
        this.curve = validateKey(verificationKey);
    }

    @Override
    public byte[] sign(final JwsAlgorithm algorithm, final String data) {
        return sign(algorithm, data.getBytes(Utils.CHARSET));
    }

    @Override
    public byte[] sign(final JwsAlgorithm algorithm, final byte[] data) {
        validateAlgorithm(algorithm);

        try {
            final Signature signature = Signature.getInstance(algorithm.getAlgorithm());
            signature.initSign(signingKey);
            signature.update(data);
            return derDecode(signature.sign(), curve.getSignatureSize());
        } catch (SignatureException | InvalidKeyException e) {
            throw new JwsSigningException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new JwsSigningException("Unsupported Signing Algorithm, " + algorithm.getAlgorithm(), e);
        }
    }

    @Override
    public boolean verify(final JwsAlgorithm algorithm, final byte[] data, final byte[] signature) {
        validateAlgorithm(algorithm);

        try {
            final Signature validator = Signature.getInstance(algorithm.getAlgorithm());
            validator.initVerify(verificationKey);
            validator.update(data);
            return validator.verify(derEncode(signature));
        } catch (SignatureException | InvalidKeyException e) {
            throw new JwsSigningException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new JwsSigningException("Unsupported Signing Algorithm, " + algorithm.getAlgorithm(), e);
        }
    }

    private void validateAlgorithm(JwsAlgorithm algorithm) {
        Reject.ifNull(algorithm, "Algorithm must not be null.");
        Reject.ifTrue(algorithm.getAlgorithmType() != JwsAlgorithmType.ECDSA, "Not an ECDSA algorithm.");
    }

    /**
     * Validate that the parameters of the key match the standard P-256 curve as required by the ES256 JWA standard.
     * @param key the key to validate.
     */
    private SupportedEllipticCurve validateKey(final ECKey key) {
        Reject.ifNull(key);
        try {
            return SupportedEllipticCurve.forKey(key);
        } catch (IllegalArgumentException ex) {
            throw new JwsException(ex);
        }
    }

    /**
     * Minimal DER decoder for the format returned by the SunEC signature provider.
     */
    private static byte[] derDecode(final byte[] signature, final int signatureSize) {
        final ByteBuffer buffer = ByteBuffer.wrap(signature);
        if (buffer.get() != DerUtils.SEQUENCE_TAG) {
            throw new JwsSigningException("Unable to decode DER signature");
        }
        // Skip overall size
        DerUtils.readLength(buffer);

        final byte[] output = new byte[signatureSize];
        final int componentSize = signatureSize >> 1;
        DerUtils.readUnsignedInteger(buffer, output, 0, componentSize);
        DerUtils.readUnsignedInteger(buffer, output, componentSize, componentSize);
        return output;
    }

    /**
     * Minimal DER encoder for the format expected by the SunEC signature provider.
     */
    private static byte[] derEncode(final byte[] signature) {
        Reject.ifNull(signature);
        SupportedEllipticCurve curve = SupportedEllipticCurve.forSignature(signature);

        final int midPoint = curve.getSignatureSize() >> 1;
        final BigInteger r = new BigInteger(Arrays.copyOfRange(signature, 0, midPoint));
        final BigInteger s = new BigInteger(Arrays.copyOfRange(signature, midPoint, signature.length));

        // Each integer component needs at most 2 bytes for the length field and 1 byte for the tag, for a total of 6
        // bytes for both integers.
        final ByteBuffer params = ByteBuffer.allocate(signature.length + 6);
        DerUtils.writeInteger(params, r.toByteArray());
        DerUtils.writeInteger(params, s.toByteArray());

        final int size = params.position();
        // The overall sequence may need up to 4 bytes for the length field plus 1 byte for the sequence tag.
        final ByteBuffer sequence = ByteBuffer.allocate(size + 5);
        sequence.put(DerUtils.SEQUENCE_TAG);
        DerUtils.writeLength(sequence, size);
        sequence.put((ByteBuffer) params.flip());

        final byte[] result = new byte[sequence.position()];
        ((ByteBuffer) sequence.flip()).get(result);
        return result;
    }

}
