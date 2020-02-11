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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.audit.rotation;

import java.io.IOException;

import org.forgerock.audit.retention.RetentionPolicy;
import org.joda.time.DateTime;

/**
 * Interface defining methods a rotatable file needs.
 */
public interface RotatableObject {
    /**
     * Retrieves the number of bytes written to the file.
     *
     * @return The number of bytes written to the file.
     */
    long getBytesWritten();

    /**
     * Retrieves the last time the file was rotated. If a file rotation never
     * occurred, this value will be the time the server started.
     *
     * @return The last time file rotation occurred.
     */
    DateTime getLastRotationTime();

    /**
     * Checks the rotatable file for rotation then retention. The file is rotated if the configured
     * {@link RotationPolicy}'s are true. The old audit files are retained/deleted according to
     * the {@link RetentionPolicy}'s configured.
     * @throws IOException If unable to rotateIfNeeded the audit file.
     */
    void rotateIfNeeded() throws IOException;

    /**
     * Closes the rotatable file.
     * @throws IOException If an exception occurs while closing.
     */
    void close() throws IOException;

    /**
     * Registers hooks into the rotation process.
     * @param rotationHooks The {@link RotationHooks} into the rotation process.
     */
    void registerRotationHooks(final RotationHooks rotationHooks);

}
