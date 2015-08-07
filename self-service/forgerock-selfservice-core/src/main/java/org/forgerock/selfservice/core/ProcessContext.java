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

import static org.forgerock.selfservice.core.ServiceUtils.EMPTY_TAG;
import static org.forgerock.selfservice.core.ServiceUtils.emptyJson;

import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Process context represents the current state of the workflow.
 *
 * @since 0.1.0
 */
public final class ProcessContext {

    private static final String STAGE_INDEX_KEY = "stageIndex";
    private static final String STAGE_TAG_KEY = "stageTag";

    private final int stageIndex;
    private final String stageTag;
    private final JsonValue input;
    private final Map<String, String> state;

    private ProcessContext(Builder builder) {
        stageIndex = builder.stageIndex;
        stageTag = builder.stageTag;
        input = builder.input;
        state = builder.state;
    }

    /**
     * @return the current index in the flow
     */
    public int getStageIndex() {
        return stageIndex;
    }

    /**
     * @return the current stage tag defined by the stage
     */
    public String getStageTag() {
        return stageTag;
    }

    /**
     * @return input provided by the client, empty json object if none
     */
    public JsonValue getInput() {
        return input;
    }

    /**
     * Allows retrieval of state persisted throughout the flow.
     *
     * @param key
     *         the state key
     *
     * @return the corresponding value
     */
    public String getState(String key) {
        return state.get(key);
    }

    Map<String, String> getState() {
        return Collections.unmodifiableMap(state);
    }

    Map<String, String> toFlattenedMap() {
        Map<String, String> flattenedContext = new HashMap<>(state);
        flattenedContext.put(STAGE_INDEX_KEY, String.valueOf(stageIndex));
        flattenedContext.put(STAGE_TAG_KEY, stageTag);
        return flattenedContext;
    }

    /**
     * Builder assists with the creation of {@link ProcessContext} instance.
     */
    public static final class Builder {

        private final int stageIndex;
        private String stageTag;
        private final Map<String, String> state;
        private JsonValue input;

        private Builder(int stageIndex) {
            Reject.ifTrue(stageIndex < 0);
            this.stageIndex = stageIndex;
            stageTag = EMPTY_TAG;
            state = new HashMap<>();
            input = emptyJson();
        }

        private Builder(ProcessContext previous) {
            Reject.ifNull(previous);
            stageIndex = previous.stageIndex;
            stageTag = previous.stageTag;
            state = new HashMap<>(previous.state);
            input = previous.input;
        }

        private Builder(Map<String, String> flattenedContext) {
            Reject.ifNull(flattenedContext);
            Reject.ifFalse(flattenedContext.containsKey(STAGE_INDEX_KEY), "Stage index missing");

            Map<String, String> localCopy = new HashMap<>(flattenedContext);
            stageIndex = Integer.parseInt(localCopy.remove(STAGE_INDEX_KEY));
            stageTag = localCopy.containsKey(STAGE_TAG_KEY)
                    ? localCopy.remove(STAGE_TAG_KEY) : EMPTY_TAG;

            state = localCopy;
            input = emptyJson();
        }

        /**
         * Set the stage tag.
         *
         * @param stageTag
         *         the stage tag
         *
         * @return this builder
         */
        public Builder setStageTag(String stageTag) {
            Reject.ifNull(stageTag);
            this.stageTag = stageTag;
            return this;
        }

        /**
         * Add state that should be preserved throughout the flow.
         *
         * @param state
         *         state to be preserved
         *
         * @return this builder
         */
        public Builder addState(Map<String, String> state) {
            Reject.ifNull(state);
            this.state.putAll(state);
            return this;
        }

        /**
         * Add state that should be preserved throughout the flow.
         *
         * @param key
         *         state key
         * @param value
         *         corresponding state value
         *
         * @return this builder
         */
        public Builder addState(String key, String value) {
            Reject.ifNull(key, value);
            this.state.put(key, value);
            return this;
        }

        Builder setInput(JsonValue input) {
            Reject.ifNull(input);
            this.input = input;
            return this;
        }

        public ProcessContext build() {
            return new ProcessContext(this);
        }

    }

    static Builder newBuilder(int stageIndex) {
        return new Builder(stageIndex);
    }

    static Builder newBuilder(Map<String, String> flattenedContext) {
        return new Builder(flattenedContext);
    }

    /**
     * Creates a new process context builder based of a previous context.
     *
     * @param previous
     *         the previous context
     *
     * @return a new builder
     */
    public static Builder newBuilder(ProcessContext previous) {
        return new Builder(previous);
    }

}
