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

package org.forgerock.authz.filter.api;

import org.forgerock.json.JsonValue;

/**
 * Represents the result of the authorization of a request.
 *
 * @since 1.5.0
 */
public final class AuthorizationResult {

    /**
     * Creates a new {@code AuthorizationResult} instance which indicates that access to the requested protected
     * resource is allowed.
     *
     * @return A successful {@code AuthorizationResult} instance.
     */
    public static AuthorizationResult accessPermitted() {
        return new AuthorizationResult(true, null, null);
    }

    /**
     * Creates a new {@code AuthorizationResult} instance which indicates that access to the request protected
     * resource is denied, for the given reason.
     *
     * @param reason The reason why authorization has failed.
     * @return A failed {@code AuthorizationResult} instance.
     */
    public static AuthorizationResult accessDenied(String reason) {
        return new AuthorizationResult(false, reason, null);
    }

    /**
     * Creates a new {@code AuthorizationResult} instance which indicates that access to the request protected
     * resource is denied, for the given reason and detail.
     *
     * @param reason The reason why authorization failed.
     * @param detail A {@code JsonValue} containing additional detail on why authorization failed.
     * @return A failed {@code AuthorizationResult} instance.
     */
    public static AuthorizationResult accessDenied(String reason, JsonValue detail) {
        return new AuthorizationResult(false, reason, detail);
    }

    private final boolean authorized;
    private final String reason;
    private final JsonValue detail;

    /**
     * Constructs a new {@code AuthorizationResult} instance.
     *
     * @param authorized {@code true} when the request is authorized.
     * @param reason The reason why authorization failed. {@code null} if the request is authorized.
     * @param detail A {@code JsonValue} containing additional detail on why authorization failed. Maybe {@code null}
     *               when the request is unauthorized or {@code null} if the request is authorized.
     */
    private AuthorizationResult(boolean authorized, String reason, JsonValue detail) {
        this.authorized = authorized;
        this.reason = reason;
        this.detail = detail;
    }

    /**
     * Whether the request is authorized to access the requested resource.
     *
     * @return {@code true} if the request is authorized.
     */
    public boolean isAuthorized() {
        return authorized;
    }

    /**
     * Gets the reason why the request is not authorized to access the requested resource.
     *
     * @return The reason why authorization failed. {@code null} if the request is authorized.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Gets the detail of why the request is not authorized to access the requested resource.
     *
     * @return A {@code JsonValue} containing additional detail on why authorization failed. Maybe {@code null} when the
     * request is unauthorized or {@code null} if the request is authorized.
     */
    public JsonValue getDetail() {
        return detail;
    }
}
