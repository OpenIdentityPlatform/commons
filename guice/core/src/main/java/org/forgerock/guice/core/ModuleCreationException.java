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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.guice.core;

/**
 * Thrown by the GuiceModuleCreator when a Guice module could not be instantiated.
 *
 * @since 1.0.0
 */
public final class ModuleCreationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a ModuleCreationException with the given Throwable.
     *
     * @param cause The Throwable which caused this exception.
     */
    ModuleCreationException(Throwable cause) {
        super(cause);
    }
}
