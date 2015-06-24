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
package org.forgerock.audit;

/**
 * Root class of all exceptions in the Commons Audit Framework.
 */
public class AuditException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an AuditException using the given parent {@code cause}.
     *
     * @param cause Error cause
     */
    public AuditException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an AuditException using the given {@code message}.
     *
     * @param message Error message
     */
    public AuditException(String message) {
        super(message);
    }

    /**
     * Constructs an AuditException using the given {@code message} and parent {@code cause}.
     *
     * @param message Error message
     * @param cause Error cause
     */
    public AuditException(String message, Throwable cause) {
        super(message, cause);
    }

}
