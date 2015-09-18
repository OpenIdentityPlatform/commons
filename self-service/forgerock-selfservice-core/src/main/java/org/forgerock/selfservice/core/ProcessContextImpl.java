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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.selfservice.core.ServiceUtils.INITIAL_TAG;
import static org.forgerock.selfservice.core.ServiceUtils.emptyJson;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

/**
 * Concrete implementation of process context. Also exposes state to be consumed only by the service.
 *
 * @since 0.1.0
 */
final class ProcessContextImpl implements ProcessContext {

    private static final String STAGE_INDEX_KEY = "stageIndex";
    private static final String STAGE_TAG_KEY = "stageTag";
    private static final String PROCESS_STATE_KEY = "processState";

    private final Context httpContext;
    private final int stageIndex;
    private final String stageTag;
    private final JsonValue input;
    private final JsonValue state;

    private ProcessContextImpl(Builder builder) {
        httpContext = builder.httpContext;
        stageIndex = builder.stageIndex;
        stageTag = builder.stageTag;
        input = builder.input;
        state = builder.state;
    }

    @Override
    public Context getHttpContext() {
        return httpContext;
    }

    @Override
    public String getStageTag() {
        return stageTag;
    }

    @Override
    public JsonValue getInput() {
        return input;
    }

    @Override
    public boolean containsState(String jsonPointer) {
        return state.get(new JsonPointer(jsonPointer)).isNotNull();
    }

    @Override
    public JsonValue getState(String jsonPointer) {
        return state.get(new JsonPointer(jsonPointer));
    }

    @Override
    public void putState(String jsonPointer, Object value) {
        state.put(new JsonPointer(jsonPointer), value);
    }

    int getStageIndex() {
        return stageIndex;
    }

    JsonValue getState() {
        return state;
    }

    JsonValue toJson() {
        return json(
                object(
                        field(STAGE_INDEX_KEY, stageIndex),
                        field(STAGE_TAG_KEY, stageTag),
                        field(PROCESS_STATE_KEY, state)));
    }

    /*
     * Builder assists with the creation of {@link ProcessContext} instance.
     */
    static final class Builder {

        private final Context httpContext;
        private final int stageIndex;
        private String stageTag;
        private JsonValue state;
        private JsonValue input;

        private Builder(Context httpContext, int stageIndex) {
            Reject.ifNull(httpContext);
            Reject.ifTrue(stageIndex < 0);
            this.httpContext = httpContext;
            this.stageIndex = stageIndex;
            stageTag = INITIAL_TAG;
            state = emptyJson();
            input = emptyJson();
        }

        private Builder(ProcessContextImpl previous) {
            Reject.ifNull(previous);
            stageIndex = previous.stageIndex;
            httpContext = previous.httpContext;
            stageTag = previous.stageTag;
            state = previous.state;
            input = previous.input;
        }

        private Builder(Context httpContext, JsonValue jsonContext) {
            Reject.ifNull(httpContext, jsonContext);
            Reject.ifTrue(jsonContext.get(STAGE_INDEX_KEY).isNull(), "Stage index missing");

            this.httpContext = httpContext;

            stageIndex = jsonContext
                    .get(STAGE_INDEX_KEY)
                    .asInteger();
            stageTag = jsonContext
                    .get(STAGE_TAG_KEY)
                    .defaultTo(INITIAL_TAG)
                    .asString();

            state = jsonContext.get(PROCESS_STATE_KEY);
            input = emptyJson();
        }

        Builder setStageTag(String stageTag) {
            Reject.ifNull(stageTag);
            this.stageTag = stageTag;
            return this;
        }

        Builder setState(JsonValue state) {
            Reject.ifNull(state);
            this.state = state;
            return this;
        }

        Builder setInput(JsonValue input) {
            Reject.ifNull(input);
            this.input = input;
            return this;
        }

        ProcessContextImpl build() {
            return new ProcessContextImpl(this);
        }

    }

    static Builder newBuilder(Context httpContext, int stageIndex) {
        return new Builder(httpContext, stageIndex);
    }

    static Builder newBuilder(Context httpContext, JsonValue jsonContext) {
        return new Builder(httpContext, jsonContext);
    }

    static Builder newBuilder(ProcessContextImpl previous) {
        return new Builder(previous);
    }

}
