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
package org.forgerock.audit.rotation;

/**
 * Creates a file size based rotation policy. Once a file is a given size in bytes it is rotated.
 */
public class SizeBasedRotationPolicy implements RotationPolicy {

    private final long maxFileSizeInBytes;

    /**
     * Constructs a SizeBasedRotationPolicy given a max file size in bytes.
     * @param maxFileSizeInBytes A max file size in bytes.
     */
    public SizeBasedRotationPolicy(final long maxFileSizeInBytes) {
        this.maxFileSizeInBytes = maxFileSizeInBytes;
    }

    /**
     * Indicates whether or not a {@link RotatableObject} needs rotation.
     * @param file The {@link RotatableObject} to be checked.
     * @return True - If the {@link RotatableObject} needs rotation.
     *         False - If the {@link RotatableObject} doesn't need rotation.
     */
    @Override
    public boolean shouldRotateFile(RotatableObject file) {
        return maxFileSizeInBytes > 0L && file.getBytesWritten() >= maxFileSizeInBytes;
    }

    /**
     * Gets the maximum size (in bytes) a file may grow to before being rotated.
     *
     * @return the maximum file size permitted by this policy.
     */
    public long getMaxFileSizeInBytes() {
        return maxFileSizeInBytes;
    }
}
