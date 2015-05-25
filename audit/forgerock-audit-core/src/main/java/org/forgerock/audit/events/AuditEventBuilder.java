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
import org.forgerock.util.Reject;

/**
 * Root builder for all audit events.
 *
 * @param <T> the type of the builder
 */
public abstract class AuditEventBuilder<T extends AuditEventBuilder<T>> {

    public static final String TIMESTAMP = "timestamp";
    public static final String TRANSACTION_ID = "transactionId";

    /** Represents the event as a JSON value. */
    protected final JsonValue jsonValue = json(object());

    /** Flag to track if the timestamp was set */
    private boolean timestamp = false;

    /** Flag to track if the transactionId was set */
    private boolean transactionId = false;

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
    protected abstract T self();

    /**
     * Generates the audit event.
     *
     * @return the audit event
     */
    public final AuditEvent toEvent() {
        setDefaults();
        validate();
        return new AuditEvent(jsonValue);
    }

    /**
     * Template method called by {@link #toEvent()} to allow any unset fields to be given their default value.
     */
    protected void setDefaults() {
        if (!timestamp) {
            timestamp(System.currentTimeMillis());
        }
    }

    /**
     * Template method called by {@link #toEvent()} to prevent ensure that the audit event will be created
     * in a valid state.
     */
    protected void validate() {
        if (!transactionId) {
            throw new IllegalStateException("The field transactionId is mandatory.");
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
        this.timestamp = true;
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
        transactionId = true;
        return self();
    }

}
