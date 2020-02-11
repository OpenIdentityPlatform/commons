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

package org.forgerock.selfservice.core;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Request;
import org.forgerock.services.context.Context;

/**
 * Process context represents the current state of the workflow.
 *
 * @since 0.2.0
 */
public interface ProcessContext {

    /**
     * Gets the request context.
     *
     * @return the request context
     */
    Context getRequestContext();

    /**
     * Gets the original request.
     *
     * @return the request
     */
    Request getRequest();

    /**
     * Gets the current stage tag defined by the previously invoked stage.
     *
     * @return the stage tag
     */
    String getStageTag();

    /**
     * Gets the input provided by the client. Empty json object represents no input.
     *
     * @return the input
     */
    JsonValue getInput();

    /**
     * Determines whether state defined by the json pointer exists.
     *
     * @param jsonPointer
     *         json pointer to the state
     *
     * @return whether of the a value exists
     */
    boolean containsState(String jsonPointer);

    /**
     * Allows retrieval of state persisted throughout the flow.
     *
     * @param jsonPointer
     *         json pointer to the state
     *
     * @return the corresponding value
     */
    JsonValue getState(String jsonPointer);

    /**
     * Puts a value into the state referenced by the json pointer.
     *
     * @param jsonPointer
     *         json pointer to where the state should be added
     * @param value
     *         the value to be added
     */
    void putState(String jsonPointer, Object value);

    /**
     * Puts a value into the additions referenced by the json pointer.
     * <p/>
     * Additions are included in the response of successfully completing a flow.
     *
     * @param jsonPointer
     *         json point to additions value
     * @param value
     *         the corresponding value
     */
    void putSuccessAddition(String jsonPointer, Object value);

}
