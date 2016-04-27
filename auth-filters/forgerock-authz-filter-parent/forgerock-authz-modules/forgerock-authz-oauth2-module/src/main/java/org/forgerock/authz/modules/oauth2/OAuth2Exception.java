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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.authz.modules.oauth2;

/**
 * Represents an exception whilst process an OAuth2 access token.
 *
 * @since 1.4.0
 */
public class OAuth2Exception extends Exception {

    /**
     * Serial Version UID.
     */
    public static final long serialVersionUID = -1L;

    /**
     * Constructs a new OAuth2Exception with message.
     *
     * @param message The exception message.
     */
    public OAuth2Exception(String message) {
        super(message);
    }

    /**
     * Constructs a new OAuth2Exception with message and underlying cause.
     *
     * @param message The exception message.
     * @param throwable The underlying cause.
     */
    public OAuth2Exception(String message, Throwable throwable) {
        super(message, throwable);
    }
}
