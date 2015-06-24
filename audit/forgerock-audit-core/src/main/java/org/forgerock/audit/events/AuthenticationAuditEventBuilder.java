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

import static org.forgerock.json.fluent.JsonValue.*;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.Reject;

/**
 * Builder for audit authentication events.
 * <p>
 * This builder should not be used directly but be specialized for each product to allow to define
 * new specific fields, e.g
 * <pre>
 * <code>
 * class OpenProductAuthenticationAuditEventBuilder{@code <T extends OpenProductAuthenticationAuditEventBuilder<T>>}
 extends AuthenticationAuditEventBuilder{@code <T>} {
 *
 *    protected OpenProductAuthenticationAuditEventBuilder(DnsUtils dnsUtils) {
 *        super(dnsUtils);
 *    }
 *
 *    public static {@code <T>} OpenProductAuthenticationAuditEventBuilder{@code <?>} productAuthenticationEvent() {
 *       return new OpenProductAuthenticationAuditEventBuilder(new DnsUtils());
 *    }
 *
 *    public T someField(String v) {
 *      jsonValue.put("someField", v);
 *      return self();
 *    }
 *
 *    ...
 * }
 * </code>
 * </pre>
 */
public class AuthenticationAuditEventBuilder<T extends AuthenticationAuditEventBuilder<T>>
    extends AuditEventBuilder<T> {

    /**
     * authentication.'operation' field name.
     */
    public static final String OPERATION = "operation";
    /**
     * authentication.'status' field name.
     */
    public static final String STATUS = "status";
    /**
     * authentication.'method' field name.
     */
    public static final String METHOD = "method";
    /**
     * authentication.method.'type' field name.
     */
    public static final String TYPE = "type";
    /**
     * authentication.method.'detail' field name.
     */
    public static final String DETAIL = "detail";
    /**
     * authentication.'message' field name.
     */
    public static final String MESSAGE = "message";

    /**
     * Starts to build an audit authentication event.
     * <p>
     * Note: it is preferable to use a specialized builder that allow to add fields specific to a product.
     *
     * @return an audit authentication event builder
     */
    @SuppressWarnings("rawtypes")
    public static AuthenticationAuditEventBuilder<?> authenticationEvent() {
        return new AuthenticationAuditEventBuilder();
    }

    /**
     * Sets the provided authentication details for the event.
     *
     * @param id the authentication id.
     * @param operation the attempted authentication operation.
     * @param status the current state of the authentication operation.
     * @param type the type of authentication used e.g. LDAP, OpenID Connect, etc.
     * @param typeDetail provides additional detail to clarify the type of operation being performed; for example,
     *                   if the operation type is 'ACTION' then the typeDetail gives the action's name.
     *
     * @return this builder
     */
    public final T authentication(String id, Operation operation, Status status, String type, String typeDetail) {
        return authentication(id, operation, status, type, typeDetail, null);
    }

    /**
     * Sets the provided authentication details for the event.
     *
     * @param id the authentication id.
     * @param operation the attempted authentication operation.
     * @param status the current state of the authentication operation.
     * @param type the type of authentication used e.g. LDAP, OpenID Connect, etc.
     * @param typeDetail provides additional detail to clarify the type of operation being performed; for example,
     *                   if the operation type is 'ACTION' then the typeDetail gives the action's name.
     * @param message Ad hoc message that can be logged to add information regarding to the authentication.
     *
     * @return this builder
     */
    public final T authentication(String id,
                                  Operation operation,
                                  Status status,
                                  String type,
                                  String typeDetail,
                                  String message) {
        Reject.<Object>ifNull(id, operation, status, type, typeDetail);
        JsonValue object = json(object(
                field(ID, id),
                field(OPERATION, operation.toString()),
                field(STATUS, status.toString()),
                field(METHOD, object(
                        field(TYPE, type),
                        field(DETAIL, typeDetail)
                )),
                field(MESSAGE, message == null ? "" : message)));
        jsonValue.put(AUTHENTICATION, object);
        return self();
    }

    /**
     * Defines a fixed set of authentication operations that can be logged.
     */
    public enum Operation {
        /**
         * Login Operation.
         */
        LOGIN,
        /**
         * Logout Operation.
         */
        LOGOUT
    }

    /**
     * Defines a fixed set of authentication statuses that can be logged.
     */
    public enum Status {
        /**
         * Authentication operation has not yet completed.
         */
        ONGOING,
        /**
         * Authentication operation has completed successfully.
         */
        SUCCEEDED,
        /**
         * Authentication operation has completed unsuccessfully.
         */
        FAILED
    }

}