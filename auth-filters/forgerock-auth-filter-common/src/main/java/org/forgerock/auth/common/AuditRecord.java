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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.auth.common;

import javax.servlet.http.HttpServletRequest;

/**
 * Contains all required information for the AuthN or AuthZ request to be audited.
 *
 * @author Phill Cunnington
 * @since 1.0.0
 */
public class AuditRecord {

    private final AuthResult authResult;
    private final HttpServletRequest request;

    /**
     * Constructs a new AuditRecord with the given AuthResult and HttpServletRequest.
     *
     * @param authResult The AuthResult of the auth operation.
     * @param request The HttpServletRequest of the auth operation.
     */
    public AuditRecord(AuthResult authResult, HttpServletRequest request) {
        this.authResult = authResult;
        this.request = request;
    }

    /**
     * Gets the AuthResult of the auth operation.
     *
     * @return The AuthResult.
     */
    public AuthResult getAuthResult() {
        return authResult;
    }

    /**
     * Gets the HttpServletRequest of the auth operation.
     *
     * @return The HttpServletRequest.
     */
    public HttpServletRequest getHttpServletRequest() {
        return request;
    }
}
