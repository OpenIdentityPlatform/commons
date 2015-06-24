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
package org.forgerock.audit.events;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Request;

/**
 * Utility functions shared by classes within this package.
 *
 * @since 1.0.0
 */
final class AuditEventBuilderUtil {

    public static final String RESOURCE_OPERATION = "resourceOperation";
    public static final String URI = "uri";
    public static final String PROTOCOL = "protocol";
    public static final String OPERATION = "operation";
    public static final String METHOD = "method";
    public static final String DETAIL = "detail";

    public static final String CREST_PROTOCOL = "CREST";

    /**
     * Assigns the provided resourceOperation details to the provided jsonValue.
     *
     * @param uri the resource identifier.
     * @param protocol the scheme of the resource identifier uri.
     * @param operationMethod the type of operation (e.g. when protocol is CREST, operation type will be one of
     *  CRUDPAQ).
     * @param operationDetail further defines the operation type (e.g. specifies the name of the CRUDPAQ action).
     */
    static JsonValue createResourceOperation(String uri,
                                             String protocol,
                                             String operationMethod,
                                             String operationDetail) {
        return json(object(
                field(URI, uri),
                field(PROTOCOL, protocol),
                field(OPERATION, object(
                        field(METHOD, operationMethod),
                        field(DETAIL, operationDetail)
                ))
        ));
    }

    /**
     * Assigns the provided resourceOperation details to the provided jsonValue.
     *
     * @param uri the resource identifier.
     * @param protocol the scheme of the resource identifier uri.
     * @param operationMethod the type of operation (e.g. when protocol is CREST, operation type will be one of
     *  CRUDPAQ).
     */
    static JsonValue createResourceOperation(String uri, String protocol, String operationMethod) {
        return json(object(
                field(URI, uri),
                field(PROTOCOL, protocol),
                field(OPERATION, object(
                        field(METHOD, operationMethod)
                ))
        ));
    }

    /**
     * Sets resourceOperation method from {@link Request}; iff the provided <code>Request</code>
     * is an {@link ActionRequest} then resourceOperation action will also be set.
     *
     * @param request The CREST request.
     * @return this builder
     */
    static JsonValue createResourceOperationFromRequest(Request request) {
        final String uri = request.getResourceName();
        final String operationMethod = request.getRequestType().name();
        if (request instanceof ActionRequest) {
            final String operationDetail = ((ActionRequest) request).getAction();
            return createResourceOperation(uri, CREST_PROTOCOL, operationMethod, operationDetail);
        } else {
            return createResourceOperation(uri, CREST_PROTOCOL, operationMethod);
        }
    }

    private AuditEventBuilderUtil() {
    }
}
