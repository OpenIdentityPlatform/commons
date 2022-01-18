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

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

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
            return validator.verify(JOSEToDER(signature));
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

    byte[] JOSEToDER(byte[] joseSignature) throws SignatureException {
    	SupportedEllipticCurve curve = SupportedEllipticCurve.forSignature(joseSignature);

        final int ecNumberSize = curve.getSignatureSize() >> 1;

        // Retrieve R and S number's length and padding.
        int rPadding = countPadding(joseSignature, 0, ecNumberSize);
        int sPadding = countPadding(joseSignature, ecNumberSize, joseSignature.length);
        int rLength = ecNumberSize - rPadding;
        int sLength = ecNumberSize - sPadding;

        int length = 2 + rLength + 2 + sLength;
        if (length > 255) {
            throw new SignatureException("Invalid JOSE signature format.");
        }

        final byte[] derSignature;
        int offset;
        if (length > 0x7f) {
            derSignature = new byte[3 + length];
            derSignature[1] = (byte) 0x81;
            offset = 2;
        } else {
            derSignature = new byte[2 + length];
            offset = 1;
        }

        // DER Structure: http://crypto.stackexchange.com/a/1797
        // Header with signature length info
        derSignature[0] = (byte) 0x30;
        derSignature[offset++] = (byte) (length & 0xff);

        // Header with "min R" number length
        derSignature[offset++] = (byte) 0x02;
        derSignature[offset++] = (byte) rLength;

        // R number
        if (rPadding < 0) {
            //Sign
            derSignature[offset++] = (byte) 0x00;
            System.arraycopy(joseSignature, 0, derSignature, offset, ecNumberSize);
            offset += ecNumberSize;
        } else {
            int copyLength = Math.min(ecNumberSize, rLength);
            System.arraycopy(joseSignature, rPadding, derSignature, offset, copyLength);
            offset += copyLength;
        }

        // Header with "min S" number length
        derSignature[offset++] = (byte) 0x02;
        derSignature[offset++] = (byte) sLength;

        // S number
        if (sPadding < 0) {
            //Sign
            derSignature[offset++] = (byte) 0x00;
            System.arraycopy(joseSignature, ecNumberSize, derSignature, offset, ecNumberSize);
        } else {
            System.arraycopy(joseSignature, ecNumberSize + sPadding, derSignature, offset, Math.min(ecNumberSize, sLength));
        }

        return derSignature;
    }
    
    private int countPadding(byte[] bytes, int fromIndex, int toIndex) {
        int padding = 0;
        while (fromIndex + padding < toIndex && bytes[fromIndex + padding] == 0) {
            padding++;
        }
        return (bytes[fromIndex + padding] & 0xff) > 0x7f ? padding - 1 : padding;
    }
}