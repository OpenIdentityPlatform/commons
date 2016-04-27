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
package org.forgerock.util.promise;

/**
 * The {@code NeverThrowsException} class is an uninstantiable placeholder
 * exception which should be used for indicating that a {@link org.forgerock.util.Function} or
 * {@link org.forgerock.util.AsyncFunction} never throws an exception (i.e. the function cannot
 * fail).
 */
public final class NeverThrowsException extends RuntimeException {
    /**
     * Serialization ID.
     */
    private static final long serialVersionUID = 2879912363036916597L;

    // Prevent instantiation.
    private NeverThrowsException() {
        throw new IllegalStateException();
    }
}
