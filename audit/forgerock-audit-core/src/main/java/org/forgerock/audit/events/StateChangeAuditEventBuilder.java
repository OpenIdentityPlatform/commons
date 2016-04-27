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

import static org.forgerock.json.JsonValue.array;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Request;

/**
 * Base class for {@link ActivityAuditEventBuilder} and {@link ConfigAuditEventBuilder}.
 */
abstract class StateChangeAuditEventBuilder<T extends StateChangeAuditEventBuilder<T>> extends AuditEventBuilder<T> {

    public static final String RUN_AS = "runAs";
    public static final String OBJECT_ID = "objectId";
    public static final String OPERATION = "operation";
    public static final String BEFORE = "before";
    public static final String AFTER = "after";
    public static final String REVISION = "revision";
    public static final String CHANGED_FIELDS = "changedFields";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setDefaults() {
        super.setDefaults();
        if (!jsonValue.isDefined(RUN_AS)) {
            runAs("");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate() {
        super.validate();
        requireField(RUN_AS);
        requireField(OBJECT_ID);
        requireField(OPERATION);
    }

    /**
     * Sets the provided runAs for the event.
     *
     * @param id the id of user this operation was performed as.
     * @return this builder
     */
    public final T runAs(String id) {
        jsonValue.put(RUN_AS, id);
        return self();
    }

    /**
     * Sets the provided objectId for the event.
     *
     * @param objectId the resource identifier.
     * @return this builder
     */
    public final T objectId(String objectId) {
        jsonValue.put(OBJECT_ID, objectId);
        return self();
    }

    /**
     * Sets the provided operation for the event.
     *
     * @param operation the type of operation (e.g. CREATE, READ, UPDATE, DELETE, PATCH, QUERY, or the ACTION name).
     * @return this builder
     */
    public final T operation(String operation) {
        jsonValue.put(OPERATION, operation);
        return self();
    }

    /**
     * Records the previous state of the modified object as a String.
     *
     * @param state A String representation of the object's previous state.
     * @return this builder
     */
    public final T before(String state) {
        jsonValue.put(BEFORE, state);
        return self();
    }

    /**
     * Records the previous state of the modified object as JSON.
     *
     * @param state A JSON representation of the object's previous state.
     * @return this builder
     */
    public final T before(JsonValue state) {
        jsonValue.put(BEFORE, state.getObject());
        return self();
    }

    /**
     * Records the new state of the modified object as a String.
     *
     * @param state A String representation of the object's new state.
     * @return this builder
     */
    public final T after(String state) {
        jsonValue.put(AFTER, state);
        return self();
    }

    /**
     * Records the new state of the modified object as JSON.
     *
     * @param state A JSON representation of the object's new state.
     * @return this builder
     */
    public final T after(JsonValue state) {
        jsonValue.put(AFTER, state.getObject());
        return self();
    }

    /**
     * Sets the revision for the event.
     *
     * @param newRevision resulting revision of the affected object.
     * @return this builder
     */
    public final T revision(String newRevision) {
        jsonValue.put(REVISION, newRevision);
        return self();
    }

    /**
     * Sets the list of fields that were changed by this update.
     *
     * @param fieldNames of the object that were updated.
     * @return this builder
     */
    public final T changedFields(String... fieldNames) {
        jsonValue.put(CHANGED_FIELDS, array((Object[]) fieldNames));
        return self();
    }

    /**
     * Sets objectId method from {@link Request}.
     *
     * @param request The CREST request.
     * @return this builder
     */
    public final T objectIdFromCrestRequest(Request request) {
        objectId(request.getResourcePath());
        return self();
    }

    /**
     * Sets operation method from {@link Request}.
     *
     * @param request The CREST request.
     * @return this builder
     */
    public final T operationFromCrestRequest(Request request) {
        if (request instanceof ActionRequest) {
            String action = ((ActionRequest) request).getAction();
            operation(action);
        } else {
            final String method = request.getRequestType().name();
            operation(method);
        }
        return self();
    }
}
