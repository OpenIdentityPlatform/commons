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

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Request;
import org.forgerock.services.context.Context;
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
    private static final String SUCCESS_ADDITIONS_KEY = "_successAdditions";
    private static final String CONFIG_VERSION_KEY = "versionKey";

    private final Context requestContext;
    private final Request request;
    private final int stageIndex;
    private final int configVersion;
    private final String stageTag;
    private final JsonValue input;
    private final JsonValue state;

    private ProcessContextImpl(Builder builder) {
        requestContext = builder.requestContext;
        request = builder.request;
        stageIndex = builder.stageIndex;
        configVersion = builder.configVersion;
        stageTag = builder.stageTag;
        input = builder.input;
        state = builder.state;
    }

    @Override
    public Context getRequestContext() {
        return requestContext;
    }

    @Override
    public Request getRequest() {
        return request;
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
        return state.get(new JsonPointer(jsonPointer)) != null;
    }

    @Override
    public JsonValue getState(String jsonPointer) {
        return state.get(new JsonPointer(jsonPointer));
    }

    @Override
    public void putState(String jsonPointer, Object value) {
        state.put(new JsonPointer(jsonPointer), value);
    }

    @Override
    public void putSuccessAddition(String jsonPointer, Object value) {
        if (!state.isDefined(SUCCESS_ADDITIONS_KEY)) {
            state.add(SUCCESS_ADDITIONS_KEY, emptyJson());
        }

        JsonValue successAdditions = state.get(SUCCESS_ADDITIONS_KEY);
        successAdditions.put(new JsonPointer(jsonPointer), value);
    }

    int getStageIndex() {
        return stageIndex;
    }

    int getConfigVersion() {
        return configVersion;
    }

    JsonValue getState() {
        return state;
    }

    boolean hasSuccessAdditions() {
        return state.isDefined(SUCCESS_ADDITIONS_KEY);
    }

    JsonValue getSuccessAdditions() {
        return state.get(SUCCESS_ADDITIONS_KEY);
    }

    JsonValue toJson() {
        return json(
                object(
                        field(STAGE_INDEX_KEY, stageIndex),
                        field(CONFIG_VERSION_KEY, configVersion),
                        field(STAGE_TAG_KEY, stageTag),
                        field(PROCESS_STATE_KEY, state)));
    }

    /*
     * Builder assists with the creation of {@link ProcessContext} instance.
     */
    static final class Builder {

        private final Context requestContext;
        private final Request request;
        private int stageIndex;
        private int configVersion;
        private String stageTag;
        private JsonValue state;
        private JsonValue input;

        private Builder(Context requestContext, Request request) {
            Reject.ifNull(requestContext, request);

            this.requestContext = requestContext;
            this.request = request;
            stageTag = INITIAL_TAG;
            state = emptyJson();
            input = emptyJson();
        }

        private Builder(ProcessContextImpl previous) {
            Reject.ifNull(previous);

            stageIndex = previous.stageIndex;
            configVersion = previous.configVersion;
            requestContext = previous.requestContext;
            request = previous.request;
            stageTag = previous.stageTag;
            state = previous.state;
            input = previous.input;
        }

        private Builder(Context requestContext, Request request, JsonValue jsonContext) {
            Reject.ifNull(requestContext, request, jsonContext);

            this.requestContext = requestContext;
            this.request = request;

            stageIndex = jsonContext
                    .get(STAGE_INDEX_KEY)
                    .asInteger();
            configVersion = jsonContext
                    .get(CONFIG_VERSION_KEY)
                    .asInteger();
            stageTag = jsonContext
                    .get(STAGE_TAG_KEY)
                    .defaultTo(INITIAL_TAG)
                    .asString();

            state = jsonContext.get(PROCESS_STATE_KEY);
            input = emptyJson();
        }

        Builder setStageIndex(int stageIndex) {
            Reject.ifTrue(stageIndex < 0);
            this.stageIndex = stageIndex;
            return this;
        }

        Builder setConfigVersion(int configVersion) {
            this.configVersion = configVersion;
            return this;
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

    static Builder newBuilder(Context requestContext, Request request) {
        return new Builder(requestContext, request);
    }

    static Builder newBuilder(Context requestContext, Request request, JsonValue jsonContext) {
        return new Builder(requestContext, request, jsonContext);
    }

    static Builder newBuilder(ProcessContextImpl previous) {
        return new Builder(previous);
    }

}
