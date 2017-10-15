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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.audit.events;

import static org.forgerock.json.JsonValue.*;

import org.forgerock.audit.util.DateUtil;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.TransactionIdContext;
import org.forgerock.util.Reject;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Root builder for all audit events.
 *
 * @param <T> the type of the builder
 */
public abstract class AuditEventBuilder<T extends AuditEventBuilder<T>> {

    /** Event name event payload field name. */
    public static final String EVENT_NAME = "eventName";
    /** Timestamp event payload field name. */
    public static final String TIMESTAMP = "timestamp";
    /** Transaction ID event payload field name. */
    public static final String TRANSACTION_ID = "transactionId";
    /** User ID event payload field name. */
    public static final String USER_ID = "userId";
    /** Tracking IDs event payload field name. */
    public static final String TRACKING_IDS = "trackingIds";

    /** Represents the event as a JSON value. */
    protected JsonValue jsonValue = json(object());

    /** Flag used to ensure super class implementations of validate() get called by subclasses. */
    private boolean superValidateCalled = false;

    /** Flag used to ensure super class implementations of setDefaults() get called by subclasses. */
    private boolean superSetDefaultsCalled = false;

    /** Accumulates trackingId entries. */
    private final Set<String> trackingIdEntries = new LinkedHashSet<>();

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
        if (!trackingIdEntries.isEmpty()) {
            jsonValue.put(TRACKING_IDS, array(trackingIdEntries.toArray()));
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
        requireField(EVENT_NAME);
        requireField(TRANSACTION_ID);
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
     * Sets the provided name for the event.
     *
     * An event's name will usually be of the form {product}-{component}-{operation}. For example,
     * AM-SESSION-CREATED, AM-CREST-SUCCESSFUL, etc.
     *
     * @param name the event's name.
     * @return this builder
     */
    public final T eventName(String name) {
        jsonValue.put(EVENT_NAME, name);
        return self();
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
     * Sets the provided userId for the event.
     *
     * @param id the user id.
     * @return this builder
     */
    public final T userId(String id) {
        jsonValue.put(USER_ID, id);
        return self();
    }

    /**
     * Adds an entry to trackingIds for the event.
     *
     * @param trackingIdValue the unique value associated with the object being tracked.
     * @return this builder
     */
    public final T trackingId(String trackingIdValue) {
        Reject.ifNull(trackingIdValue, "trackingId value cannot be null");
        trackingIdEntries.add(trackingIdValue);
        return self();
    }

    /**
     * Adds the specified entries to trackingIds for the event.
     *
     * @param trackingIdValues the set of trackingId entries to be recorded (see {@link #trackingId}.
     * @return this builder
     */
    public final T trackingIds(Set<String> trackingIdValues) {
        // iterate the entries so that each can be validated
        for (String trackingIdValue : trackingIdValues) {
            trackingId(trackingIdValue);
        }
        return self();
    }

    /**
     * Sets transactionId from ID of {@link TransactionIdContext}, if the provided
     * <code>Context</code> contains a <code>TransactionIdContext</code>.
     *
     * @param context The CREST context.
     * @return this builder
     */
    public final T transactionIdFromContext(Context context) {
        if (context.containsContext(TransactionIdContext.class)) {
            TransactionIdContext transactionIdContext = context.asContext(TransactionIdContext.class);
            transactionId(transactionIdContext.getTransactionId().getValue());
        }
        return self();
    }
}
