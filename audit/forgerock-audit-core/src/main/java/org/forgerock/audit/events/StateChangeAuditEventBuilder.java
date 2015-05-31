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

/**
 * Base class for {@link ActivityAuditEventBuilder} and {@link ConfigAuditEventBuilder}.
 */
abstract class StateChangeAuditEventBuilder<T extends StateChangeAuditEventBuilder<T>> extends AuditEventBuilder<T> {

    public static final String RUN_AS = "runAs";
    public static final String RESOURCE_OPERATION = "resourceOperation";
    public static final String URI = "uri";
    public static final String PROTOCOL = "protocol";
    public static final String OPERATION = "operation";
    public static final String METHOD = "method";
    public static final String DETAIL = "detail";
    public static final String BEFORE = "before";
    public static final String AFTER = "after";
    public static final String REVISION = "revision";
    public static final String CHANGED_FIELDS = "changedFields";

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
        JsonValue object = json(object(
                field(URI, uri),
                field(PROTOCOL, protocol),
                field(OPERATION, object(
                        field(METHOD, operationMethod),
                        field(DETAIL, operationDetail)
                ))
        ));
        jsonValue.put(RESOURCE_OPERATION, object);
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
        JsonValue object = json(object(
                field(URI, uri),
                field(PROTOCOL, protocol),
                field(OPERATION, object(
                        field(METHOD, operationMethod)
                ))
        ));
        jsonValue.put(RESOURCE_OPERATION, object);
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

}
