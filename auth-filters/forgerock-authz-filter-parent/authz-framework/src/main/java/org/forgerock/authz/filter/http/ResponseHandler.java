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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.authz.filter.http;

import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.json.JsonValue;

/**
 * Class for creating a valid JSON object response for error cases.
 *
 * @since 1.5.0
 */
class ResponseHandler {

    /**
     * Returns the exception in a JSON object structure, suitable for inclusion in the entity of an HTTP error response.
     * The JSON representation looks like this:
     *
     * <pre>
     * {
     *     "code"    : 403,
     *     "reason"  : "Forbidden",
     *     "message" : "...",
     *     "detail"  : { ... } // optional
     * }
     * </pre>
     *
     * @param message The detail message.
     * @param detail Additional detail which can be evaluated by applications.
     * @return The exception in a JSON object structure, suitable for inclusion in the entity of an HTTP error response.
     */
    JsonValue getJsonForbiddenResponse(String message, JsonValue detail) {
        return getJsonExceptionResponse(403, "Forbidden", message, detail);
    }

    /**
     * Returns the exception in a JSON object structure, suitable for inclusion in the entity of an HTTP error response.
     * The JSON representation looks like this:
     *
     * <pre>
     * {
     *     "code"    : 500,
     *     "reason"  : "Internal Error",
     *     "message" : "...",
     *     "detail"  : { ... } // optional
     * }
     * </pre>
     *
     * @param message The detail message.
     * @param detail Additional detail which can be evaluated by applications.
     * @return The exception in a JSON object structure, suitable for inclusion in the entity of an HTTP error response.
     */
    JsonValue getJsonErrorResponse(String message, JsonValue detail) {
        return getJsonExceptionResponse(500, "Internal Error", message, detail);
    }

    /**
     * Returns the exception in a JSON object structure, suitable for inclusion in the entity of an HTTP error response.
     * The JSON representation looks like this:
     *
     * <pre>
     * {
     *     "code"    : 404,
     *     "reason"  : "...",  // optional
     *     "message" : "...",
     *     "detail"  : { ... } // optional
     * }
     * </pre>
     *
     * @param code The numeric code of the exception.
     * @param reason The short reason phrase of the exception.
     * @param message The detail message.
     * @param detail Additional detail which can be evaluated by applications.
     * @return The exception in a JSON object structure, suitable for inclusion in the entity of an HTTP error response.
     */
    private JsonValue getJsonExceptionResponse(int code, String reason, String message, JsonValue detail) {

        final Map<String, Object> result = new LinkedHashMap<>(4);
        result.put("code", code); // required
        result.put("reason", reason); // required
        if (message != null) { // should always be present
            result.put("message", message);
        }
        if (detail != null) {
            result.put("detail", detail.getObject());
        }
        return new JsonValue(result);
    }
}
