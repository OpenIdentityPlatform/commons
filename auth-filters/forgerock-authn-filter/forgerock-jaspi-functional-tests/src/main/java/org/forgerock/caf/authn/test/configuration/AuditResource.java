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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.caf.authn.test.configuration;

import static org.forgerock.json.fluent.JsonValue.array;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.resource.ResourceException.newNotSupportedException;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;

import org.forgerock.caf.authn.test.runtime.TestAuditApi;
import org.forgerock.http.context.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;

/**
 * <p>CREST resource responsible for exposing the audit records created by the JASPI runtime.</p>
 *
 * <p>The resource offers only two operations, read and an action of "readAndClear". Performing a read will simply
 * read the audit records and performing a "readAndClear" action will read and clear the audit records.</p>
 *
 * @since 1.5.0
 */
public class AuditResource implements SingletonResourceProvider {

    private TestAuditApi auditApi;

    /**
     * Constructs a new AuditResource instance.
     *
     * @param auditApi An instance of the TestAuditApi, which stores the audit records made by the JASPI runtime.
     */
    @Inject
    public AuditResource(TestAuditApi auditApi) {
        this.auditApi = auditApi;
    }

    /**
     * Only the "readAndClear" action is supported, which will read the audit records and then subsequently clear them.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<JsonValue, ResourceException> actionInstance(ServerContext context, ActionRequest request) {
        if ("readAndClear".equalsIgnoreCase(request.getAction())) {
            JsonValue jsonAuditRecords = getAuditRecords();
            auditApi.clear();
            return newResultPromise(jsonAuditRecords);
        } else {
            return newExceptionPromise(newNotSupportedException());
        }
    }

    /**
     * Unsupported operation.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> patchInstance(ServerContext context, PatchRequest request) {
        return newExceptionPromise(newNotSupportedException());
    }

    /**
     * Will perform a read of the audit records.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> readInstance(ServerContext context, ReadRequest request) {
        JsonValue jsonAuditRecords = getAuditRecords();
        return newResultPromise(new Resource("AuditRecords", Integer.toString(jsonAuditRecords.hashCode()),
                jsonAuditRecords));
    }

    /**
     * Unsupported operation.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> updateInstance(ServerContext context, UpdateRequest request) {
        return newExceptionPromise(newNotSupportedException());
    }

    /**
     * Gets the audit records from the {@code TestAuditLogger} and converts them into a {@code JsonValue}.
     *
     * @return A {@code JsonValue} representation of the audit records.
     */
    private JsonValue getAuditRecords() {
        JsonValue auditRecords = json(array());
        for (JsonValue auditRecord : auditApi.getAuditRecords()) {
            auditRecords.add(auditRecord.getObject());
        }
        return auditRecords;
    }
}
