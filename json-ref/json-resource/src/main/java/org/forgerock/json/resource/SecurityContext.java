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
 * Copyright 2012-2014 ForgeRock AS.
 */
package org.forgerock.json.resource;

import static org.forgerock.util.Reject.checkNotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;

/**
 * A {@link Context} containing information about the client performing the
 * request which may be used when performing authorization decisions. A security
 * context will typically be created for each REST request and comprises of two
 * fields:
 * <ul>
 * <li>an {@link #getAuthenticationId authentication ID} which is the principal
 * that the client used during authentication. This might be a user name, an
 * email address, etc. The authentication ID may be used for logging or auditing
 * but SHOULD NOT be used when performing authorization decisions.
 * <li>an {@link #getAuthorizationId authorization ID} which is a map containing
 * additional principals associated with the client and which MAY be used when
 * performing authorization decisions. Examples of principals include a unique
 * identifier for the user, roles, or an LDAP distinguished name (DN).
 * </ul>
 * The following code illustrates how an application may obtain the realm
 * associated with a user:
 *
 * <pre>
 * Context context = ...;
 * String realm = (String) context.asContext(SecurityContext.class).getAuthorizationId(AUTHZID_REALM);
 * </pre>
 *
 * Here is an example of the JSON representation of a security context:
 *
 * <pre>
 * {
 *   "id"     : "56f0fb7e-3837-464d-b9ec-9d3b6af665c3",
 *   "class"  : "org.forgerock.json.resource.SecurityContext",
 *   "parent" : {
 *       ...
 *   },
 *   "authenticationId" : "bjensen@example.com",
 *   "authorizationId" : {
 *       "id"        : "1230fb7e-f83b-464d-19ef-789b6af66456",
 *       "component" : "users",
 *       "roles"     : [
 *           "administrators"
 *       ],
 *       "dn"        : "cn=bjensen,ou=people,dc=example,dc=com"
 *   }
 * }
 * </pre>
 */
public final class SecurityContext extends AbstractContext {

    /** the client-friendly name of this context. */
    private static final String CONTEXT_NAME = "security";

    /**
     * The authorization ID name reserved for the name of the component in which
     * a user's resource is located, e.g. "users".
     */
    public static final String AUTHZID_COMPONENT = "component";

    /**
     * The authorization ID name reserved for the user's LDAP distinguished
     * name.
     */
    public static final String AUTHZID_DN = "dn";

    /**
     * The authorization ID principal name reserved for a user's unique
     * identifier.
     */
    public static final String AUTHZID_ID = "id";

    /**
     * The authorization ID name reserved for a user's realm.
     */
    public static final String AUTHZID_REALM = "realm";

    /**
     * The authorization ID name reserved for the array of roles associated with
     * the user.
     */
    public static final String AUTHZID_ROLES = "roles";

    // Persisted attribute names.
    private static final String ATTR_AUTHENTICATION_ID = "authenticationId";
    private static final String ATTR_AUTHORIZATION_ID = "authorizationId";

    /**
     * Creates a new security context having the provided parent and an ID
     * automatically generated using {@code UUID.randomUUID()}.
     *
     * @param parent
     *            The parent context.
     * @param authenticationId
     *            The authentication ID that the user provided during
     *            authentication, which may be {@code null} or empty indicating
     *            that the client is unauthenticated.
     * @param authorizationId
     *            The authorization information which should be used for
     *            authorizing requests may by the user, which may be
     *            {@code null} or empty indicating that the client is is to be
     *            treated as an anonymous user when performing authorization
     *            decisions. The provided map will be copied defensively and
     *            must only contain values which can be serialized as JSON
     *            values.
     */
    public SecurityContext(final Context parent,
            final String authenticationId, final Map<String, Object> authorizationId) {
        this(null, parent, authenticationId, authorizationId); // no id
    }

    /**
     * Creates a new security context having the provided ID, and parent.
     *
     * @param id
     *            The context ID.
     * @param parent
     *            The parent context.
     * @param authenticationId
     *            The authentication ID that the user provided during
     *            authentication, which may be {@code null} or empty indicating
     *            that the client is unauthenticated.
     * @param authorizationId
     *            The authorization information which should be used for
     *            authorizing requests may by the user, which may be
     *            {@code null} or empty indicating that the client is is to be
     *            treated as an anonymous user when performing authorization
     *            decisions. The provided map will be copied defensively and
     *            must only contain values which can be serialized as JSON
     *            values.
     */
    public SecurityContext(final String id, final Context parent,
            final String authenticationId, final Map<String, Object> authorizationId) {
        super(id, checkNotNull(parent, "Cannot instantiate SecurityContext with null parent Context"));
        data.put(ATTR_AUTHENTICATION_ID, authenticationId != null ? authenticationId : "");
        data.put(ATTR_AUTHORIZATION_ID, authorizationId != null
                ? Collections.unmodifiableMap(new LinkedHashMap<String, Object>(authorizationId))
                : Collections.<String, Object>emptyMap());
    }

    /**
     * Restore from JSON representation.
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @throws ResourceException
     *             If the JSON representation could not be parsed.
     */
    SecurityContext(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        super(savedContext, config);
    }

    /**
     * Returns the name of this context.
     *
     * @return The name of this context.
     */
    public String getContextName() {
        return CONTEXT_NAME;
    }

    /**
     * Returns the principal that the client used during authentication. This
     * might be a user name, an email address, etc. The authentication ID may be
     * used for logging or auditing but SHOULD NOT be used for authorization
     * decisions.
     *
     * @return The principal that the client used during authentication, which
     *         may be empty (but never {@code null}) indicating that the client
     *         is unauthenticated.
     */
    public String getAuthenticationId() {
        return data.get(ATTR_AUTHENTICATION_ID).asString();
    }

    /**
     * Returns an unmodifiable map containing additional principals associated
     * with the client which MAY be used when performing authorization
     * decisions. Examples of principals include a unique identifier for the
     * user, roles, or an LDAP distinguished name (DN). The following code
     * illustrates how an application may obtain the realm associated with a
     * user:
     *
     * <pre>
     * Context context = ...;
     * String realm = (String) context.asContext(SecurityContext.class).getAuthorizationId(AUTHZID_REALM);
     * </pre>
     *
     * @return An unmodifiable map containing additional principals associated
     *         with the client which MAY be used when performing authorization
     *         decisions. The returned map may be empty (but never {@code null})
     *         indicating that the client is is to be treated as an anonymous
     *         user.
     */
    public Map<String, Object> getAuthorizationId() {
        return data.get(ATTR_AUTHORIZATION_ID).asMap();
    }
}
