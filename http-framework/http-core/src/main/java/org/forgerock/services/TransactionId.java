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
package org.forgerock.services;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;
import org.forgerock.util.generator.IdGenerator;

/**
 * TransactionId value should be unique per request coming from an external agent so that all events occurring in
 * response to the same external stimulus can be tied together.
 *
 * Calls to external systems should propagate the value returned by {@link #createSubTransactionId()} so that Audit
 * events reported by the external system can also be tied back to the original stimulus.
 *
 * Due to the fact that each TransactionId instance creates a sequence of sub-transaction IDs, the same TransactionId
 * object should be used while fulfilling a given request; it is not appropriate to create multiple instances of
 * TransactionId with the same value as this would lead to duplicate sub-transaction ID values. As such, two instances
 * of TransactionId with the same value are not considered equal.
 *
 * This class is thread-safe.
 *
 * @since 2.0
 */
public final class TransactionId {

    private final String value;
    private final AtomicInteger subTransactionIdCounter;

    /**
     * Construct a {@code TransactionId} with a random value.
     */
    public TransactionId() {
        this(IdGenerator.DEFAULT.generate());
    }

    /**
     * Construct a {@code TransactionId} with the specified value. The value must not be null nor empty.
     *
     * @param value The value to initialize the transactionId from.
     */
    public TransactionId(String value) {
        this(value, 0);
    }

    /**
     * Construct a {@code TransactionId} with the specified value.
     */
    private TransactionId(String value, int counter) {
        Reject.ifTrue(value == null || value.isEmpty(), "The value must not be null nor empty.");
        this.value = value;
        this.subTransactionIdCounter = new AtomicInteger(counter);
    }

    /**
     * Returns the value of this TransactionId.
     * @return Non-null, {@code TransactionId} value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Creates a new TransactionId, child of this one.
     * @return Non-null, {@code TransactionId} value that can be passed to an external system.
     */
    public TransactionId createSubTransactionId() {
        final String subTransactionId = value + "/" + subTransactionIdCounter.getAndIncrement();
        return new TransactionId(subTransactionId);
    }

    /**
     * Returns a representation of this TransactionId as a JsonValue.
     * The JsonValue will be composed of 2 fields : value and subTransactionIdCounter.
     * @return a representation of this TransactionId as a JsonValue.
     */
    public JsonValue toJson() {
        return json(object(field("value", value), field("subTransactionIdCounter", subTransactionIdCounter.get())));
    }

    /**
     * Creates a TransactionId from a JsonValue.
     * @param value the JsonValue used to create the TransactionId, composed of 2 fields : value and
     * subTransactionIdCounter.
     * @return a TransactionId initialized with the values provided by the JsonValue.
     */
    public static TransactionId valueOf(JsonValue value) {
        return new TransactionId(value.get("value").required().asString(),
                value.get("subTransactionIdCounter").required().asInteger());
    }

}