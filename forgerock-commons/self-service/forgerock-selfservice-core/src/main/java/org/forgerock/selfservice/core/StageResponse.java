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

import static org.forgerock.selfservice.core.ServiceUtils.INITIAL_TAG;
import static org.forgerock.selfservice.core.ServiceUtils.emptyJson;

import org.forgerock.json.JsonValue;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenCallback;
import org.forgerock.util.Reject;

/**
 * Stage response represents a response from having invoked a progress stage.
 *
 * @since 0.1.0
 */
public final class StageResponse {

    private final String stageTag;
    private final JsonValue requirements;
    private final SnapshotTokenCallback callback;

    private StageResponse(Builder builder) {
        stageTag = builder.stageTag;
        requirements = builder.requirements;
        callback = builder.callback;
    }

    /**
     * Returns the stage tag.
     *
     * @return the stage tag
     */
    public String getStageTag() {
        return stageTag;
    }

    /**
     * Returns <code>true</code> if the response has any requirements.
     *
     * @return <code>true</code> if the response has any requirements
     */
    public boolean hasRequirements() {
        return requirements.size() > 0;
    }

    /**
     * Returns the requirements.
     *
     * @return the requirements
     */
    public JsonValue getRequirements() {
        return requirements;
    }

    /**
     * Returns <code>true</code> if the response contains a snapshot token callback instance.
     *
     * @return <code>true</code> if the response contains a snapshot token callback instance
     */
    public boolean hasCallback() {
        return callback != null;
    }

    /**
     * Returns the callback instance.
     *
     * @return the callback instance
     */
    public SnapshotTokenCallback getCallback() {
        return callback;
    }

    /**
     * Builder assists with the creation of {@link StageResponse} instances.
     */
    public static final class Builder {

        private String stageTag;
        private JsonValue requirements;
        private SnapshotTokenCallback callback;

        private Builder() {
            stageTag = INITIAL_TAG;
            requirements = emptyJson();
        }

        /**
         * Sets the stage tag.
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
         * Sets the json requirements.
         *
         * @param requirements
         *         the json requirements
         *
         * @return a requirements builder
         */
        public RequirementsBuilder setRequirements(JsonValue requirements) {
            Reject.ifNull(requirements);
            this.requirements = requirements;

            return new RequirementsBuilder() {

                @Override
                public RequirementsBuilder setCallback(SnapshotTokenCallback callback) {
                    Reject.ifNull(callback);
                    Builder.this.callback = callback;
                    return this;
                }

                @Override
                public StageResponse build() {
                    return Builder.this.build();
                }

            };
        }

        /**
         * Builds a stage response instance.
         *
         * @return a stage response instance
         */
        public StageResponse build() {
            return new StageResponse(this);
        }

    }

    /**
     * Requirements builder allows for the definition of a snapshot token
     * callback, which gets invoked with just prior to requirements being
     * sent to the client.
     */
    public interface RequirementsBuilder {

        /**
         * Sets the snapshot token callback.
         *
         * @param callback
         *         the callback
         *
         * @return this builder
         */
        RequirementsBuilder setCallback(SnapshotTokenCallback callback);

        /**
         * Builds a stage response instance.
         *
         * @return a stage response instance
         */
        StageResponse build();

    }

    /**
     * New builder to help construct a stage response.
     *
     * @return new builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

}
