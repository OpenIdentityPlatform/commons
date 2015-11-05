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
package org.forgerock.services.context;

import org.forgerock.services.TransactionId;
import org.forgerock.json.JsonValue;

/**
 * This context aims to hold the {@link TransactionId}.
 */
public class TransactionIdContext extends AbstractContext {

    private static final String ATTR_TRANSACTION_ID = "transactionId";

    private final TransactionId transactionId;

    /**
     * Constructs a new TransactionIdContext.
     *
     * @param parent The parent context
     * @param transactionId The transaction id to use in this context
     */
    public TransactionIdContext(Context parent, TransactionId transactionId) {
        super(transactionId.getValue(), "transactionId", parent);
        this.transactionId = transactionId;
    }

    /**
     * Restores a saved context.
     * @param savedContext The saved state.
     * @param classLoader The {@code ClassLoader} to use.
     */
    public TransactionIdContext(JsonValue savedContext, ClassLoader classLoader) {
        super(savedContext, classLoader);
        this.transactionId = TransactionId.valueOf(data.get(ATTR_TRANSACTION_ID));
    }

    /**
     * Returns the transaction id.
     * @return the transaction id
     */
    public TransactionId getTransactionId() {
        return transactionId;
    }

    /**
     * Updates the data object to have the current transactionId state.
     * {@inheritDoc}
     */
    @Override
    public JsonValue toJsonValue() {
        data.put(ATTR_TRANSACTION_ID, transactionId.toJson().getObject());
        return super.toJsonValue();
    }
}
