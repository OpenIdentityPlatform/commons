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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.http.header;

import static org.forgerock.http.header.HeaderUtil.parseSingleValuedHeader;

import java.util.Collections;
import java.util.List;

import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Request;
import org.forgerock.services.TransactionId;

/**
 * Processes the transactionId header used mainly for audit purpose.
 */
public final class TransactionIdHeader extends Header {

    /**
     * Constructs a new header, initialized from the specified request.
     *
     * @param request
     *            The request to initialize the header from.
     * @return The parsed header
     * @throws MalformedHeaderException
     *            if the value is not acceptable for a {@code TransactionId}
     */
    public static TransactionIdHeader valueOf(final Request request) throws MalformedHeaderException {
        return valueOf(parseSingleValuedHeader(request, NAME));
    }

    /**
     * Constructs a new header, initialized from the specified string value.
     *
     * @param value
     *            The value to initialize the header from.
     * @return The parsed header.
     * @throws MalformedHeaderException
     *            if the value is not acceptable for a {@code TransactionId}
     */
    public static TransactionIdHeader valueOf(final String value) throws MalformedHeaderException {
        return new TransactionIdHeader(value);
    }

    /** The name of this header. */
    public static final String NAME = "X-ForgeRock-TransactionId";

    /**
     * The TransactionId built from the header value.
     */
    private TransactionId transactionId = null;

    /**
     * Constructs a new header with the provided value for the transaction id. The transactionId will be null if either
     * the value is null or empty.
     *
     * @param value
     *            The value for the transaction id.
     * @throws MalformedHeaderException
     *            if the value is not acceptable for a {@code TransactionId}
     */
    public TransactionIdHeader(String value) throws MalformedHeaderException {
        try {
            this.transactionId = new TransactionId(value);
        } catch (IllegalArgumentException e) {
            throw new MalformedHeaderException(e);
        }
    }

    /**
     * Returns the transaction id.
     *
     * @return The transaction id
     */
    public TransactionId getTransactionId() {
        return transactionId;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<String> getValues() {
        return transactionId != null
                ? Collections.singletonList(transactionId.getValue())
                : Collections.<String>emptyList();
    }

    static class Factory extends AbstractSingleValuedHeaderFactory<TransactionIdHeader> {

        @Override
        public TransactionIdHeader parse(String value) throws MalformedHeaderException {
            return valueOf(value);
        }
    }
}
