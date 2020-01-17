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

import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

/**
 * Represents an audit event.
 */
public class AuditEvent {

    private final JsonValue value;

    /**
     * Creates an audit event for the provided non-null Json value.
     *
     * @param value the json value representing the event
     */
    AuditEvent(JsonValue value) {
        Reject.ifNull(value);
        this.value = value;
    }

    /**
     * Returns the Json value of this event.
     *
     * @return the event Json value.
     */
    public JsonValue getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "AuditEvent [value=" + value + "]";
    }

}
