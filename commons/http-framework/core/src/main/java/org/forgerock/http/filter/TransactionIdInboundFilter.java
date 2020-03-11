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
package org.forgerock.http.filter;


import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.header.MalformedHeaderException;
import org.forgerock.http.header.TransactionIdHeader;
import org.forgerock.http.protocol.Headers;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.services.context.Context;
import org.forgerock.services.TransactionId;
import org.forgerock.services.context.TransactionIdContext;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This filter is responsible to create the {@link TransactionIdContext} in the context's chain. If the incoming request
 * contains the header "X-ForgeRock-TransactionId" then it uses that value as the transaction id otherwise a new one is
 * generated.
 */
public class TransactionIdInboundFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionIdInboundFilter.class);

    /**
     * The system property to allow to trust the HTTP header X-ForgeRock-TransactionId.
     */
    public static final String SYSPROP_TRUST_TRANSACTION_HEADER = "org.forgerock.http.TrustTransactionHeader";

    private final boolean trustTransactionIdHeader = Boolean.getBoolean(SYSPROP_TRUST_TRANSACTION_HEADER);

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
        if (context.containsContext(TransactionIdContext.class)) {
            logger.trace("A TransactionIdContext already exists in the context's chain.");
        }
        final TransactionId transactionId = trustTransactionIdHeader
                ? createTransactionId(request.getHeaders())
                : new TransactionId();
        final Context newContext = new TransactionIdContext(context, transactionId);
        return next.handle(newContext, request);
    }

    @VisibleForTesting
    static TransactionId createTransactionId(Headers headers) {
        try {
            TransactionIdHeader txHeader = headers.get(TransactionIdHeader.class);
            return txHeader == null ? new TransactionId() :  txHeader.getTransactionId();
        } catch (MalformedHeaderException ex) {
            logger.trace("The TransactionId header is malformed.", ex);
            return new TransactionId();
        }
    }
}
