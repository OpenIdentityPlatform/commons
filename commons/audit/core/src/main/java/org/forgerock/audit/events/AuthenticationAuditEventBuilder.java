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

import java.util.List;
import java.util.Map;

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
 * @param <T> the type of the builder
 */
public class AuthenticationAuditEventBuilder<T extends AuthenticationAuditEventBuilder<T>>
    extends AuditEventBuilder<T> {

    /** Defines the authentication result key. */
    public static final String RESULT = "result";
    /** Defines the principal key. */
    public static final String PRINCIPAL = "principal";
    /** Defines the context key. */
    public static final String CONTEXT = "context";
    /** Defines the entries key. */
    public static final String ENTRIES = "entries";

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
     * Sets the authentication audit event overall result.
     *
     * @param result the authentication overall result.
     * @return an audit authentication event builder
     */
    public T result(Status result) {
        jsonValue.put(RESULT, result == null ? null : result.toString());
        return self();
    }

    /**
     * Sets the principals of the authentication event.
     *
     * @param principals the list of principals
     * @return an audit authentication event builder
     */
    public T principal(List<String> principals) {
        jsonValue.put(PRINCIPAL, principals);
        return self();
    }

    /**
     * Sets the context used in the authentication event.
     *
     * @param context the authentication event context
     * @return an audit authentication event builder
     */
    public T context(Map<String, Object> context) {
        jsonValue.put(CONTEXT, context);
        return self();
    }

    /**
     * Sets the list of auth modules used in the authentication event and their state.
     *
     * @param entries the list of authentication modules and their state
     * @return an audit authentication event builder
     */
    public T entries(List<?> entries) {
        jsonValue.put(ENTRIES, entries);
        return self();
    }

    /**
     * Defines a fixed set of authentication statuses that can be logged.
     */
    public enum Status {
        /**
         * Authentication operation has not yet completed.
         */
        CONTINUE,
        /**
         * Authentication operation has completed successfully.
         */
        SUCCESSFUL,
        /**
         * Authentication operation has completed unsuccessfully.
         */
        FAILED
    }

}
