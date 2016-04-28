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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.caf.authn.test.configuration;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.util.promise.Promises.*;

import javax.inject.Inject;

import org.forgerock.caf.authn.test.runtime.TestAuditApi;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
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
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
        if ("readAndClear".equalsIgnoreCase(request.getAction())) {
            JsonValue jsonAuditRecords = getAuditRecords();
            auditApi.clear();
            return newResultPromise(newActionResponse(jsonAuditRecords));
        } else {
            return new NotSupportedException().asPromise();
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
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * Will perform a read of the audit records.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        JsonValue jsonAuditRecords = getAuditRecords();
        return newResultPromise(newResourceResponse("AuditRecords", Integer.toString(jsonAuditRecords.hashCode()),
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
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        return new NotSupportedException().asPromise();
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
