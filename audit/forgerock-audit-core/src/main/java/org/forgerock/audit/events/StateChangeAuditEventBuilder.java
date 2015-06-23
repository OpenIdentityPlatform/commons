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

import static org.forgerock.audit.events.AuditEventBuilderUtil.*;
import static org.forgerock.json.fluent.JsonValue.*;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link ActivityAuditEventBuilder} and {@link ConfigAuditEventBuilder}.
 */
abstract class StateChangeAuditEventBuilder<T extends StateChangeAuditEventBuilder<T>> extends AuditEventBuilder<T> {

    public static final String RUN_AS = "runAs";
    public static final String RESOURCE_OPERATION = AuditEventBuilderUtil.RESOURCE_OPERATION;
    public static final String URI = AuditEventBuilderUtil.URI;
    public static final String PROTOCOL = AuditEventBuilderUtil.PROTOCOL;
    public static final String OPERATION = AuditEventBuilderUtil.OPERATION;
    public static final String METHOD = AuditEventBuilderUtil.METHOD;
    public static final String DETAIL = AuditEventBuilderUtil.DETAIL;
    public static final String BEFORE = "before";
    public static final String AFTER = "after";
    public static final String REVISION = "revision";
    public static final String CHANGED_FIELDS = "changedFields";

    private static final Logger logger = LoggerFactory.getLogger(StateChangeAuditEventBuilder.class);

    /**
     * {@inheritDoc}
     */
    protected void setDefaults() {
        super.setDefaults();
        if (!jsonValue.isDefined(RUN_AS)) {
            runAs("");
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void validate() {
        super.validate();
        requireField(RUN_AS);
        requireField(RESOURCE_OPERATION);
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
     * Sets the provided resourceOperation details for the event.
     *
     * @param uri the resource identifier.
     * @param protocol the scheme of the resource identifier uri.
     * @param operationMethod the type of operation (e.g. when protocol is CREST, operation type will be one of CRUDPAQ).
     * @param operationDetail further defines the operation type (e.g. specifies the name of the CRUDPAQ action).
     * @return this builder
     */
    public final T resourceOperation(String uri, String protocol, String operationMethod, String operationDetail) {
        jsonValue.put(RESOURCE_OPERATION, createResourceOperation(uri, protocol, operationMethod, operationDetail));
        return self();
    }

    /**
     * Sets the provided resourceOperation details for the event.
     *
     * @param uri the resource identifier.
     * @param protocol the scheme of the resource identifier uri.
     * @param operationMethod the type of operation (e.g. when protocol is CREST, operation type will be one of CRUDPAQ).
     * @return this builder
     */
    public final T resourceOperation(String uri, String protocol, String operationMethod) {
        jsonValue.put(RESOURCE_OPERATION, createResourceOperation(uri, protocol, operationMethod));
        return self();
    }

    /**
     * Sets the provided before state as JSON for the event.
     *
     * @param stateAsJson A JSON representation of the object's previous state.
     * @return this builder
     */
    public final T before(String stateAsJson) {
        jsonValue.put(BEFORE, stateAsJson);
        return self();
    }

    /**
     * Sets the provided after state as JSON for the event.
     *
     * @param stateAsJson A JSON representation of the object's new state.
     * @return this builder
     */
    public final T after(String stateAsJson) {
        jsonValue.put(AFTER, stateAsJson);
        return self();
    }

    /**
     * Sets the revision for the event.
     *
     * @param newRevision resulting revision of the affected object.
     * @return this builder
     */
    public final T revision(long newRevision) {
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
     * Sets resourceOperation method from {@link Request}; iff the provided <code>Request</code>
     * is an {@link ActionRequest} then resourceOperation action will also be set.
     *
     * @param request The CREST request.
     * @return this builder
     */
    public final T resourceOperationFromRequest(Request request) {
        JsonValue object = createResourceOperationFromRequest(request);
        jsonValue.put(RESOURCE_OPERATION, object);
        return self();
    }
}
