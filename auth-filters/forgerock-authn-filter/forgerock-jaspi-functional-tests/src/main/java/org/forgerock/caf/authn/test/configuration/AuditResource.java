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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.caf.authn.test.configuration;

import org.forgerock.auth.common.AuditRecord;
import org.forgerock.caf.authn.test.runtime.TestAuditLogger;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;

import javax.inject.Inject;
import javax.security.auth.message.MessageInfo;
import java.util.List;

import static org.forgerock.json.fluent.JsonValue.array;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * <p>CREST resource responsible for exposing the audit records created by the JASPI runtime.</p>
 *
 * <p>The resource offers only two operations, read and an action of "readAndClear". Performing a read will simply
 * read the audit records and performing a "readAndClear" action will read and clear the audit records.</p>
 *
 * @since 1.5.0
 */
public class AuditResource implements SingletonResourceProvider {

    private TestAuditLogger auditLogger;

    /**
     * Constructs a new AuditResource instance.
     *
     * @param auditLogger An instance of the TestAuditLogger, which stores the audit records made by the JASPI runtime.
     */
    @Inject
    public AuditResource(TestAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    /**
     * Only the "readAndClear" action is supported, which will read the audit records and then subsequently clear them.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {

        if ("readAndClear".equalsIgnoreCase(request.getAction())) {

            JsonValue jsonAuditRecords = getAuditRecords();
            auditLogger.clear();

            handler.handleResult(jsonAuditRecords);

        } else {
            handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
        }
    }

    /**
     * Unsupported operation.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

    /**
     * Will perform a read of the audit records.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {

        JsonValue jsonAuditRecords = getAuditRecords();

        handler.handleResult(new Resource("AuditRecords", jsonAuditRecords.hashCode() + "", jsonAuditRecords));
    }

    /**
     * Unsupported operation.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

    /**
     * Gets the audit records from the {@code TestAuditLogger} and converts them into a {@code JsonValue}.
     *
     * @return A {@code JsonValue} representation of the audit records.
     */
    private JsonValue getAuditRecords() {

        List<AuditRecord<MessageInfo>> auditRecords = auditLogger.getAuditRecords();

        JsonValue jsonAuditRecords = json(array());
        for (AuditRecord<MessageInfo> auditRecord : auditRecords) {
            jsonAuditRecords.add(object(
                    field("outcome", auditRecord.getAuthResult().toString()),
                    field("", auditRecord.getAuditObject().getMap())));
        }

        return jsonAuditRecords;
    }
}
