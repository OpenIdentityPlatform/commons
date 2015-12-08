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
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.TransactionIdContext;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This filter aims to create a sub-transaction's id and inserts that value as a header of the request.
 *
 * Its main usage will be like this :
 * <pre>
 * {@code
 * Handler handler = Handlers.chainOf(new HttpClientHandler(httpClient), new TransactionIdOutboundFilter());
 * }
 * </pre>
 *
 */
public class TransactionIdOutboundFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionIdOutboundFilter.class);

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
        if (context.containsContext(TransactionIdContext.class)) {
            TransactionIdContext txContext = context.asContext(TransactionIdContext.class);
            final String subTxId = txContext.getTransactionId().createSubTransactionId().getValue();
            try {
                request.getHeaders().put(new TransactionIdHeader(subTxId));
            } catch (MalformedHeaderException ex) {
                // Should not happen as the value is always valid.
                logger.error("An error occured while building the TransactionIdHeader", ex);
            }
        } else {
            logger.trace("Expecting to find an instance of TransactionIdContext in the chain, but there was none.");
        }

        return next.handle(context, request);
    }

}
