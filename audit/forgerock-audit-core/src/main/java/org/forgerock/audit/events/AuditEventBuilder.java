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

import org.forgerock.audit.util.DateUtil;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.util.Reject;

/**
 * Root builder for all audit events.
 *
 * @param <T> the type of the builder
 */
public abstract class AuditEventBuilder<T extends AuditEventBuilder<T>> {

    public static final String TIMESTAMP = "timestamp";
    public static final String TRANSACTION_ID = "transactionId";
    public static final String AUTHENTICATION = "authentication";
    public static final String ID = "id";

    /** Represents the event as a JSON value. */
    protected JsonValue jsonValue = json(object());

    /** Flag used to ensure super class implementations of validate() get called by subclasses. */
    private boolean superValidateCalled = false;

    /** Flag used to ensure super class implementations of setDefaults() get called by subclasses. */
    private boolean superSetDefaultsCalled = false;

    /**
     * Creates the builder.
     */
    protected AuditEventBuilder() {
        // Reduce visibility of the default constructor
    }

    /**
     * Returns this object, as its actual type.
     *
     * @return this object
     */
    @SuppressWarnings("unchecked")
    protected final T self() {
        return (T) this;
    }

    /**
     * Generates the audit event.
     *
     * As a side-effect of calling this method, this builder is reset to its starting state.
     *
     * @return the audit event
     */
    public final AuditEvent toEvent() {

        superSetDefaultsCalled = false;
        setDefaults();
        if (!superSetDefaultsCalled) {
            throw new IllegalStateException("Subclasses overriding setDefaults() must call super.setDefaults()");
        }

        superValidateCalled = false;
        validate();
        if (!superValidateCalled) {
            throw new IllegalStateException("Subclasses overriding validate() must call super.validate()");
        }

        AuditEvent auditEvent = new AuditEvent(jsonValue);
        jsonValue = json(object());
        return auditEvent;
    }

    /**
     * Called by {@link #toEvent()} to allow any unset fields to be given their default value.
     *
     * When overriding this method, the super class implementation must be called.
     */
    protected void setDefaults() {
        if (!jsonValue.isDefined(TIMESTAMP)) {
            timestamp(System.currentTimeMillis());
        }
        superSetDefaultsCalled = true;
    }

    /**
     * Called by {@link #toEvent()} to ensure that the audit event will be created in a valid state.
     *
     * When overriding this method, the super class implementation must be called.
     *
     * @throws IllegalStateException if a required field has not been populated.
     */
    protected void validate() {
        requireField(TRANSACTION_ID);
        requireField(AUTHENTICATION);
        superValidateCalled = true;
    }

    /**
     * Helper method to be used when overriding {@link #validate()}.
     *
     * @param rootFieldName The name of the field that must be populated.
     * @throws IllegalStateException if the required field has not been populated.
     */
    protected void requireField(String rootFieldName) {
        if (!jsonValue.isDefined(rootFieldName)) {
            throw new IllegalStateException("The field " + rootFieldName + " is mandatory.");
        }
    }

    /**
     * Sets the provided time stamp for the event.
     *
     * @param timestamp the time stamp.
     * @return this builder
     */
    public final T timestamp(long timestamp) {
        Reject.ifTrue(timestamp <= 0, "The timestamp has to be greater than 0.");
        jsonValue.put(TIMESTAMP, DateUtil.getDateUtil("UTC").formatDateTime(timestamp));
        return self();
    }

    /**
     * Sets the provided transactionId for the event.
     *
     * @param id the transaction id.
     * @return this builder
     */
    public final T transactionId(String id) {
        Reject.ifNull(id);
        jsonValue.put(TRANSACTION_ID, id);
        return self();
    }

    /**
     * Sets the provided authentication id for the event.
     *
     * @param id the authentication id.
     * @return this builder
     */
    public final T authentication(String id) {
        Reject.ifNull(id);
        JsonValue object = json(object(field(ID, id)));
        jsonValue.put(AUTHENTICATION, object);
        return self();
    }

    /**
     * Sets transactionId from ID of {@link RootContext}, iff the provided
     * <code>Context</code> contains a <code>RootContext</code>.
     *
     * @param context The CREST context.
     * @return this builder
     */
    public final T transactionIdFromRootContext(Context context) {
        if (context.containsContext(RootContext.class)) {
            RootContext rootContext = context.asContext(RootContext.class);
            transactionId(rootContext.getId());
        }
        return self();
    }

    /**
     * Sets authentication from {@link SecurityContext}, iff the provided
     * <code>Context</code> contains a <code>SecurityContext</code>.
     *
     * @param context The CREST context.
     * @return this builder
     */
    public final T authenticationFromSecurityContext(Context context) {
        if (context.containsContext(SecurityContext.class)) {
            SecurityContext securityContext = context.asContext(SecurityContext.class);
            authentication(securityContext.getAuthenticationId());
        }
        return self();
    }

}
